package com.example.diplomski.views.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.net.URLDecoder
import java.nio.charset.StandardCharsets



data class Registration(
    val id: String,
    val userId: String,
    val userName: String,
    val status: String,
    val teamSize: Int = 0,
    val teamMembers: List<String> = emptyList()
)

@Composable
fun QuizRegistrationsScreen(navController: NavController, quizId: String, quizName: String, quizDateTime: String) {
    val decodedQuizName = URLDecoder.decode(quizName, StandardCharsets.UTF_8.toString())
    val decodedQuizDateTime = URLDecoder.decode(quizDateTime, StandardCharsets.UTF_8.toString())

    val registrations = remember { mutableStateOf<List<Registration>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }
    var listener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Start listening on component mount
    LaunchedEffect(quizId) {
        listener = fetchRegistrationsForQuiz(
            quizId,
            onSuccess = { registrations.value = it },
            onError = { errorMessage.value = it }
        )
    }

    // Stop listening on component unmount
    DisposableEffect(Unit) {
        onDispose {
            listener?.remove() // Prekid slušanja
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Registrations for: $decodedQuizName",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Date and Time: $decodedQuizDateTime",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (errorMessage.value.isNotEmpty()) {
            Text(text = errorMessage.value, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn {
            items(registrations.value) { registration ->
                RegistrationCard(registration) { action ->
                    updateRegistrationStatus(
                        registration.id,
                        action,
                        onSuccess = { /* Success callback */ },
                        onError = { errorMessage.value = it }
                    )
                }
            }
        }
    }
}

fun fetchRegistrationsForQuiz(
    quizId: String,
    onSuccess: (List<Registration>) -> Unit,
    onError: (String) -> Unit
): ListenerRegistration {
    val db = FirebaseFirestore.getInstance()
    return db.collection("registrations")
        .whereEqualTo("quizId", quizId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError("Failed to fetch registrations: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val registrations = mutableListOf<Registration>()

                // Koristimo batch operacije za paralelno dohvaćanje korisničkih imena
                val userFetchTasks = mutableListOf<Task<DocumentSnapshot>>()

                snapshot.documents.forEach { document ->
                    val userId = document.getString("userId").orEmpty()
                    val userName = document.getString("userName").orEmpty()

                    if (userName.isNotEmpty()) {
                        // Ako userName postoji, dodaj ga odmah
                        registrations.add(
                            Registration(
                                id = document.id,
                                userId = userId,
                                userName = userName,
                                status = document.getString("status").orEmpty(),
                                teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                                teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                            )
                        )
                    } else {
                        // Ako userName ne postoji, dohvatimo ga iz "users" kolekcije
                        val userFetchTask = db.collection("users").document(userId).get()
                        userFetchTasks.add(userFetchTask)
                        userFetchTask.addOnSuccessListener { userDoc ->
                            val fetchedUserName = userDoc.getString("name").orEmpty()
                            registrations.add(
                                Registration(
                                    id = document.id,
                                    userId = userId,
                                    userName = fetchedUserName,
                                    status = document.getString("status").orEmpty(),
                                    teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                                    teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                                )
                            )
                        }.addOnFailureListener {
                            // Ako dohvat korisničkog imena ne uspije, koristimo default vrijednost
                            registrations.add(
                                Registration(
                                    id = document.id,
                                    userId = userId,
                                    userName = "Unknown User",
                                    status = document.getString("status").orEmpty(),
                                    teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                                    teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                                )
                            )
                        }
                    }
                }

                // Čekaj da svi dohvatni zadaci završe prije povrata rezultata
                Tasks.whenAllComplete(userFetchTasks).addOnCompleteListener {
                    onSuccess(registrations)
                }.addOnFailureListener { exception ->
                    onError("Failed to fetch user details: ${exception.message}")
                }
            }
        }
}



fun updateRegistrationStatus(
    registrationId: String,
    status: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("registrations").document(registrationId)
        .update("status", status)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onError(e.message ?: "Failed to update status") }
}

@Composable
fun RegistrationCard(
    registration: Registration,
    onAction: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "User: ${registration.userName}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Team Size: ${registration.teamSize}")
            Text(text = "Team Members: ${registration.teamMembers.joinToString(", ")}")
            Text(text = "Status: ${registration.status}", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onAction("accepted") },
                    enabled = registration.status == "pending",
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Accept")
                }
                Button(
                    onClick = { onAction("rejected") },
                    enabled = registration.status == "pending"
                ) {
                    Text("Decline")
                }
            }
        }
    }
}




