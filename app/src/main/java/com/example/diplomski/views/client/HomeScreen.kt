package com.example.diplomski.views.client

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

fun checkIfRegistered(
    userId: String,
    quizId: String,
    onResult: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("registrations")
        .whereEqualTo("userId", userId)
        .whereEqualTo("quizId", quizId)
        .get()
        .addOnSuccessListener { documents ->
            onResult(!documents.isEmpty) // Ako dokumenti postoje, korisnik je već prijavljen
        }
        .addOnFailureListener {
            onResult(false) // U slučaju greške, pretpostavimo da nije prijavljen
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
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isRegistered = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            checkIfRegistered(it.uid, quiz.id) { registered ->
                isRegistered.value = registered
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
                Text(text = "Fee: ${quiz.fee} USD", style = MaterialTheme.typography.bodySmall)
                Text(text = "Seats: ${quiz.seats}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (isRegistered.value) {
                    Text(
                        text = "Already Registered",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green
                    )
                } else {
                    Button(onClick = {
                        registerForQuiz(context, quiz.id)
                    }) {
                        Text("Prijavi se")
                    }
                }
            }
        }
    }
}

fun registerForQuiz(context: android.content.Context, quizId: String) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        return
    }

    val registration = hashMapOf(
        "userId" to currentUser.uid,
        "quizId" to quizId,
        "status" to "pending", // Ponovno postavljanje statusa
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