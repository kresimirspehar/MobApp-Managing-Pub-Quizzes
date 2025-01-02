package com.example.diplomski.views.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Quiz(
    val id: String,
    val name: String,
    val quizType: String,
    val location: String,
    val fee: Int,
    val seats: Int,
    val dateTime: String
)



fun fetchQuizzes(
    onQuizzesFetched: (List<Quiz>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        onError("User not authenticated")
        return
    }

    db.collection("quizzes")
        .whereEqualTo("authorId", currentUser.uid)
        .orderBy("dateTime")
        .get()
        .addOnSuccessListener { result ->
            val quizzes = result.map { document ->
                Quiz(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    quizType = document.getString("quizType") ?: "",
                    location = document.getString("quizType") ?: "",
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
fun AdminHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val quizzes = remember { mutableStateOf<List<Quiz>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }
    val expandedCardIds = remember { mutableStateOf(setOf<String>()) }

    // Dohvaćanje kvizova
    LaunchedEffect(Unit) {
        fetchQuizzes(
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
        Text("Welcome, Admin!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("add_quiz") }) {
            Text("Add quiz")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = Color.Red)
        }

        LazyColumn {
            items(quizzes.value) { quiz ->
                ExpandableCard(quiz, expandedCardIds, navController) // Prikaz kartice
            }
        }
    }
}

@Composable
fun ExpandableCard(quiz: Quiz, expandedCardIds: MutableState<Set<String>>, navController: NavController) {
    val isExpanded = expandedCardIds.value.contains(quiz.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = {
            expandedCardIds.value = if (isExpanded) {
                expandedCardIds.value - quiz.id
            } else {
                expandedCardIds.value + quiz.id
            }
        }
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

                // Dodavanje gumba za pregled prijava
                Button(
                    onClick = {
                        navController.navigate("quiz_registrations/${quiz.id}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Registrations")
                }
            }
        }
    }
}


@Composable
fun AddQuizScreen(navController: NavController) {
    var quizType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") } // Polje za datum i vrijeme
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = quizType,
            onValueChange = { quizType = it },
            label = { Text("Quiz Type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Quiz Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = fee,
            onValueChange = { fee = it },
            label = { Text("Fee") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = seats,
            onValueChange = { seats = it },
            label = { Text("Number of Seats") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = dateTime,
            onValueChange = { dateTime = it },
            label = { Text("Date and Time (yyyy-MM-dd HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
        }

        Button(
            onClick = {
                if (quizType.isEmpty() || location.isEmpty() || name.isEmpty() || fee.isEmpty() || seats.isEmpty() || dateTime.isEmpty()) {
                    errorMessage = "All fields are required!"
                } else {
                    addQuizToFirestore(
                        quizType,
                        location,
                        name,
                        fee.toIntOrNull() ?: 0,
                        seats.toIntOrNull() ?: 0,
                        dateTime,
                        context,
                        onSuccess = {
                            Toast.makeText(context, "Quiz added successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onFailure = {
                            Toast.makeText(context, "Failed to add quiz: $it", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        ) {
            Text("Submit")
        }
    }
}


fun addQuizToFirestore(
    quizType: String,
    location: String,
    name: String,
    fee: Int,
    seats: Int,
    dateTime: String,
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        onFailure("User not authenticated")
        return
    }

    val quiz = hashMapOf(
        "quizType" to quizType,
        "location" to location,
        "name" to name,
        "fee" to fee,
        "seats" to seats,
        "dateTime" to dateTime,
        "authorId" to currentUser.uid
    )

    db.collection("quizzes")
        .add(quiz)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to add quiz: ${e.message}", Toast.LENGTH_LONG).show()
            onFailure(e.message ?: "Unknown error")
        }
}



