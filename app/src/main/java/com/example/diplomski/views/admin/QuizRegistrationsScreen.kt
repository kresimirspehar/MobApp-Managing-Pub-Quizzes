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
    val teamName: String = "",
    val teamSize: Int = 0,
    val teamMembers: List<String> = emptyList()
)

@Composable
fun QuizRegistrationsScreen(navController: NavController, quizId: String, quizName: String, quizDateTime: String) {
    val decodedQuizName = URLDecoder.decode(quizName, StandardCharsets.UTF_8.toString())
    val decodedQuizDateTime = URLDecoder.decode(quizDateTime, StandardCharsets.UTF_8.toString())

    val groupedRegistrations = remember { mutableStateOf<Map<String, List<Registration>>>(emptyMap()) }
    val errorMessage = remember { mutableStateOf("") }
    var listener by remember { mutableStateOf<ListenerRegistration?>(null) }

    val db = FirebaseFirestore.getInstance()
    var acceptedCount by remember { mutableStateOf(0) }
    var totalSeats by remember { mutableStateOf(0) }

    LaunchedEffect(quizId) {
        db.collection("registrations")
            .whereEqualTo("quizId", quizId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                acceptedCount = documents.size()
            }
            .addOnFailureListener { errorMessage.value = "Failed to fetch accepted registrations: ${it.message}" }

        db.collection("quizzes").document(quizId)
            .get()
            .addOnSuccessListener { document ->
                totalSeats = document.getLong("seats")?.toInt() ?: 0
            }
            .addOnFailureListener { errorMessage.value = "Failed to fetch quiz details: ${it.message}" }
    }

    // Start listening on component mount
    LaunchedEffect(quizId) {
        listener = fetchRegistrationsForQuiz(
            quizId,
            onSuccess = { registrations, count ->
                groupedRegistrations.value = registrations
                acceptedCount = count
            },
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
        Text(
            text = "Teams: $acceptedCount/$totalSeats",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                navController.navigate("accepted_teams/${quizId}")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Accepted Teams")
        }

        if (errorMessage.value.isNotEmpty()) {
            Text(text = errorMessage.value, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn {
            groupedRegistrations.value.forEach { (userId, registrations) ->
                item {
                    Text(
                        text = "User: ${registrations.firstOrNull()?.userName.orEmpty()}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(registrations) { registration ->
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
}

fun fetchRegistrationsForQuiz(
    quizId: String,
    onSuccess: (Map<String, List<Registration>>, Int) -> Unit,
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
                val userFetchTasks = mutableListOf<Task<DocumentSnapshot>>()
                var acceptedCount = 0 // Broj prihvaćenih prijava

                snapshot.documents.forEach { document ->
                    val userId = document.getString("userId").orEmpty()
                    val userName = document.getString("userName").orEmpty()
                    val status = document.getString("status").orEmpty()

                    // Povećajte broj prihvaćenih prijava ako je status "accepted"
                    if (status == "accepted") {
                        acceptedCount++
                    }

                    if (userName.isNotEmpty()) {
                        // Ako userName postoji, dodaj ga odmah
                        registrations.add(
                            Registration(
                                id = document.id,
                                userId = userId,
                                userName = userName,
                                status = status,
                                teamName = document.getString("teamName").orEmpty(),
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
                                    status = status,
                                    teamName = document.getString("teamName").orEmpty(),
                                    teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                                    teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                                )
                            )
                        }.addOnFailureListener {
                            registrations.add(
                                Registration(
                                    id = document.id,
                                    userId = userId,
                                    userName = "Unknown User",
                                    status = status,
                                    teamName = document.getString("teamName").orEmpty(),
                                    teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                                    teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                                )
                            )
                        }
                    }
                }

                // Čekaj da svi dohvatni zadaci završe prije povrata rezultata
                Tasks.whenAllComplete(userFetchTasks).addOnCompleteListener {
                    val groupedRegistrations = registrations.groupBy { it.userId }
                    onSuccess(groupedRegistrations, acceptedCount)
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
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Team Name: ${registration.teamName}", style = MaterialTheme.typography.bodyMedium)
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




