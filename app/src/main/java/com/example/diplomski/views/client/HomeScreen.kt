package com.example.diplomski.views.client

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.diplomski.views.admin.Quiz
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen() {
    val quizzes = remember { mutableStateOf<List<Quiz>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fetchAllQuizzes(
            onQuizzesFetched = { quizzes.value = it },
            onError = { errorMessage.value = it }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Available Quizzes", style = MaterialTheme.typography.headlineMedium)

        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = Color.Red)
        }

        LazyColumn {
            items(quizzes.value) { quiz ->
                QuizCard(quiz)
            }
        }
    }
}

fun checkRegistrationStatus(
    userId: String,
    quizId: String,
    onResult: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("registrations")
        .whereEqualTo("userId", userId)
        .whereEqualTo("quizId", quizId)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                onResult("not_registered")
            } else {
                val status = documents.first().getString("status") ?: "not_registered"
                onResult(status)
            }
        }
        .addOnFailureListener {
            onResult("not_registered") // Pretpostavi da nije registriran u slučaju greške
        }
}

fun fetchAllQuizzes(
    onQuizzesFetched: (List<Quiz>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("quizzes")
        .orderBy("dateTime")
        .get()
        .addOnSuccessListener { result ->
            val quizzes = result.map { document ->
                Quiz(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    quizType = document.getString("quizType") ?: "",
                    location = document.getString("location") ?: "",
                    fee = document.getLong("fee")?.toInt() ?: 0,
                    seats = document.getLong("seats")?.toInt() ?: 0,
                    dateTime = document.getString("dateTime") ?: ""
                )
            }
            onQuizzesFetched(quizzes)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Failed to fetch quizzes")
        }
}

@Composable
fun QuizCard(quiz: Quiz) {

    var isExpanded by remember { mutableStateOf(false) }
    var teamSize by remember { mutableStateOf("") }
    var teamMemberNames by remember { mutableStateOf(listOf<String>()) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val registrationStatus = remember { mutableStateOf("not_registered") }

    LaunchedEffect(Unit) {
        currentUser?.let {
            checkRegistrationStatus(it.uid, quiz.id) { status ->
                registrationStatus.value = status
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = quiz.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Type: ${quiz.quizType}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Location: ${quiz.location}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Date: ${quiz.dateTime}", style = MaterialTheme.typography.bodyMedium)

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = teamSize,
                    onValueChange = {
                        teamSize = it
                        val size = it.toIntOrNull() ?: 0
                        // Adjust teamMemberNames size dynamically
                        teamMemberNames = if (size > teamMemberNames.size) {
                            teamMemberNames + List(size - teamMemberNames.size) { "" }
                        } else {
                            teamMemberNames.take(size)
                        }
                    },
                    label = { Text("Team Size") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter Team Member Names:")
                teamMemberNames.forEachIndexed { index, name ->
                    TextField(
                        value = name,
                        onValueChange = { newName ->
                            val updatedNames = teamMemberNames.toMutableList()
                            updatedNames[index] = newName
                            teamMemberNames = updatedNames
                        },
                        label = { Text("Member ${index + 1}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Validacija unosa
                        if (teamSize.isBlank() || teamSize.toIntOrNull() == null || teamSize.toInt() <= 0) {
                            Toast.makeText(
                                context,
                                "Please enter a valid team size.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (teamMemberNames.size != teamSize.toInt() || teamMemberNames.any { it.isBlank() }) {
                            Toast.makeText(
                                context,
                                "Please fill in all team member names.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Ako je validacija uspješna, poziv funkcije za registraciju
                            registerForQuiz(
                                context,
                                quiz.id,
                                teamSize.toIntOrNull() ?: 1,
                                teamMemberNames
                            )
                            registrationStatus.value = "pending"
                        }
                    },
                    enabled = registrationStatus.value == "not_registered",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (registrationStatus.value) {
                            "pending" -> "Pending Approval"
                            "accepted" -> "Registered"
                            else -> "Register"
                        }
                    )
                }
            }}}}

fun registerForQuiz(
    context: Context,
    quizId: String,
    teamSize: Int,
    teamMembers: List<String>
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        return
    }

    db.collection("users").document(currentUser.uid).get()
        .addOnSuccessListener { userDocument ->
            val userName = userDocument.getString("name") ?: "Unknown User"

            val registration = hashMapOf(
                "userId" to currentUser.uid,
                "userName" to userName,
                "quizId" to quizId,
                "status" to "pending",
                "teamSize" to teamSize,
                "teamMembers" to teamMembers,
                "timeStamp" to System.currentTimeMillis()
            )

            db.collection("registrations")
                .add(registration)
                .addOnSuccessListener {
                    Toast.makeText(context, "Successfully registered for the quiz!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to register: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch user data: ${e.message}", Toast.LENGTH_LONG).show()
        }
}