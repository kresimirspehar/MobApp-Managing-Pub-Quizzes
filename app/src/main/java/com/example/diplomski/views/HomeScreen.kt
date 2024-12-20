package com.example.diplomski.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    val quizzes = remember { mutableStateOf<List<Quiz>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }

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

@Composable
fun QuizCard(quiz: Quiz) {

    var isExpanded by remember { mutableStateOf(false) }

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

            if(isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Fee: ${quiz.fee} USD", style = MaterialTheme.typography.bodySmall)
                Text(text = "Seats: ${quiz.seats}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}