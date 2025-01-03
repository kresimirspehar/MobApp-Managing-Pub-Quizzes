package com.example.diplomski.views.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration



data class Registration(
    val id: String,
    val userId: String,
    val userName: String,
    val status: String
)

@Composable
fun QuizRegistrationsScreen(navController: NavController, quizId: String) {
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
        Text("Registrations for Quiz", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

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
                val registrations = snapshot.documents.map { document ->
                    Registration(
                        id = document.id,
                        userId = document.getString("userId").orEmpty(),
                        userName = document.getString("userName").orEmpty(),
                        status = document.getString("status").orEmpty()
                    )
                }
                onSuccess(registrations)
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
            Text(text = "Status: ${registration.status}", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Gumb za prihvaćanje
                Button(
                    onClick = { onAction("accepted") },
                    enabled = registration.status == "pending",
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Accept")
                }
                // Gumb za odbijanje
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




