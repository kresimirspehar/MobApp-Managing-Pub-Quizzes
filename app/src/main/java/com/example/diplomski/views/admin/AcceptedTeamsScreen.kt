package com.example.diplomski.views.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AcceptedTeamsScreen(navController: NavController, quizId: String) {
    val acceptedTeams = remember { mutableStateOf<List<Registration>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }

    LaunchedEffect(quizId) {
        fetchAcceptedTeams(
            quizId = quizId,
            onSuccess = { acceptedTeams.value = it },
            onError = { errorMessage.value = it }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Accepted Teams", style = MaterialTheme.typography.headlineMedium)

        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(acceptedTeams.value) { team ->
                    Text(
                        text = "${team.userName} (${team.teamSize} members)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

fun fetchAcceptedTeams(
    quizId: String,
    onSuccess: (List<Registration>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("registrations")
        .whereEqualTo("quizId", quizId)
        .whereEqualTo("status", "accepted")
        .get()
        .addOnSuccessListener { snapshot ->
            val teams = snapshot.map { document ->
                Registration(
                    id = document.id,
                    userId = document.getString("userId").orEmpty(),
                    userName = document.getString("teamName").orEmpty(),
                    status = document.getString("status").orEmpty(),
                    teamSize = document.getLong("teamSize")?.toInt() ?: 0,
                    teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()
                )
            }
            onSuccess(teams)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Failed to fetch accepted teams")
        }
}
