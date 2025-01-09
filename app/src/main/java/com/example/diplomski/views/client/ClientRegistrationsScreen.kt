package com.example.diplomski.views.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Help

data class RegisteredQuiz(
    val id: String,
    val name: String,
    val quizType: String,
    val location: String,
    val dateTime: String,
    val dateObject: Date,
    val status: String
)

@Composable
fun ClientRegistrationsScreen() {
    val registeredQuizzes = remember { mutableStateOf<List<RegisteredQuiz>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        fetchClientRegistrations(
            userId = currentUser?.uid.orEmpty(),
            onQuizzesFetched = { registeredQuizzes.value = it },
            onError = { errorMessage.value = it }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("My Registered Quizzes", style = MaterialTheme.typography.headlineMedium)

        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn {
            items(registeredQuizzes.value) { quiz ->
                ClientQuizCard(quiz)
            }
        }
    }
}

@Composable
fun ClientQuizCard(quiz: RegisteredQuiz) {
    val context = LocalContext.current
    var buttonEnabled by remember { mutableStateOf(quiz.status == "rejected") } // Aktivno samo za "rejected"
    val buttonColor = if (quiz.status == "rejected") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = quiz.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Type: ${quiz.quizType}")
            Text(text = "Location: ${quiz.location}")
            Text(text = "Date: ${quiz.dateTime}")

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicator(status = quiz.status)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Status: ${quiz.status.capitalize()}")
            }

            if (quiz.status == "rejected") {
                Button(
                    onClick = {
                        registerForQuiz(
                            context = context,
                            quizId = quiz.id,
                            teamSize = 1,
                            teamMembers = listOf(),
                            onSuccess = {
                                buttonEnabled = false
                            }
                        )
                        buttonEnabled = false // Onemogućavanje gumba
                    },
                    enabled = buttonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register Again")
                }
            }
        }
    }
}




fun fetchClientRegistrations(
    userId: String,
    onQuizzesFetched: (List<RegisteredQuiz>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("registrations")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { registrationDocs ->
            val registeredQuizIds = registrationDocs.documents.map { doc ->
                doc.getString("quizId") to doc.getString("status")
            }.filterNotNull()

            if (registeredQuizIds.isEmpty()) {
                onQuizzesFetched(emptyList())
                return@addOnSuccessListener
            }

            db.collection("quizzes")
                .whereIn(FieldPath.documentId(), registeredQuizIds.map { it.first })
                .get()
                .addOnSuccessListener { quizDocs ->
                    val quizzes = quizDocs.mapNotNull { document ->
                        val quizId = document.id
                        val status = registeredQuizIds.find { it.first == quizId }?.second.orEmpty()
                        RegisteredQuiz(
                            id = quizId,
                            name = document.getString("name").orEmpty(),
                            quizType = document.getString("quizType").orEmpty(),
                            location = document.getString("location").orEmpty(),
                            dateTime = document.getString("dateTime").orEmpty(),
                            dateObject = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .parse(document.getString("dateTime").orEmpty()) ?: Date(),
                            status = status // Dodan status prijave
                        )
                    }
                    onQuizzesFetched(quizzes)
                }
                .addOnFailureListener { e -> onError("Error fetching quizzes: ${e.message}") }
        }
        .addOnFailureListener { e -> onError("Error fetching registrations: ${e.message}") }
}

@Composable
fun StatusIndicator(status: String) {
    val (color, icon) = when (status) {
        "accepted" -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        "rejected" -> Pair(MaterialTheme.colorScheme.error, Icons.Default.Cancel)
        "pending" -> Pair(MaterialTheme.colorScheme.secondary, Icons.Default.HourglassEmpty)
        else -> Pair(MaterialTheme.colorScheme.onSurface, Icons.Default.Help)
    }

    Icon(
        imageVector = icon,
        contentDescription = status,
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}



