package com.example.diplomski.views.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Prikaz imena tima
                            Text(
                                text = "Team: ${team.userName}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            // Prikaz telefonskog broja
                            Text(
                                text = "Phone: ${team.phone}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            // Prikaz veličine tima
                            Text(
                                text = "Team Size: ${team.teamSize} members",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
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
            val userFetchTasks = mutableListOf<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>>()
            val teams = mutableListOf<Registration>()

            snapshot.documents.forEach { document ->
                val userId = document.getString("userId").orEmpty()
                val userName = document.getString("teamName").orEmpty()
                val status = document.getString("status").orEmpty()
                val teamSize = document.getLong("teamSize")?.toInt() ?: 0
                val teamMembers = document.get("teamMembers") as? List<String> ?: emptyList()

                // Dohvaćanje korisničkog telefonskog broja iz kolekcije "users"
                val userFetchTask = db.collection("users").document(userId).get()
                userFetchTasks.add(userFetchTask)

                userFetchTask.addOnSuccessListener { userDoc ->
                    val phone = userDoc.getString("phone").orEmpty() // Dohvati telefon
                    teams.add(
                        Registration(
                            id = document.id,
                            userId = userId,
                            userName = userName,
                            status = status,
                            teamSize = teamSize,
                            teamMembers = teamMembers,
                            phone = phone // Dodaj telefon
                        )
                    )
                }.addOnFailureListener {
                    teams.add(
                        Registration(
                            id = document.id,
                            userId = userId,
                            userName = userName,
                            status = status,
                            teamSize = teamSize,
                            teamMembers = teamMembers,
                            phone = "N/A" // Ako nije moguće dohvatiti telefon
                        )
                    )
                }
            }

            // Čekanje da svi upiti za korisnike završe prije nego što pozovemo onSuccess
            com.google.android.gms.tasks.Tasks.whenAllComplete(userFetchTasks).addOnCompleteListener {
                onSuccess(teams)
            }.addOnFailureListener { exception ->
                onError("Failed to fetch user details: ${exception.message}")
            }
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Failed to fetch accepted teams")
        }
}
