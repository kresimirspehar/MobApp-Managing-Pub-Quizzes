package com.example.diplomski.views.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import kotlin.math.exp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class Quiz(
    val id: String,
    val name: String,
    val quizType: String,
    val location: String,
    val fee: Int,
    val seats: Int,
    val dateTime: String,
    val additionalInfo: String = ""
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
                    location = document.getString("location") ?: "",
                    fee = document.getLong("fee")?.toInt() ?: 0,
                    seats = document.getLong("seats")?.toInt() ?: 0,
                    dateTime = document.getString("dateTime") ?: "",
                    additionalInfo = document.getString("additionalInfo") ?: ""
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

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "Unknown User"
    val userName = userEmail.substringBefore("@")

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
        Text("Manage Your Quizzes, $userName!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("add_quiz") }) {
            Text("Add Quiz")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = Color.Red)
        }

        LazyColumn {
            items(quizzes.value) { quiz ->
                ExpandableCard(
                    quiz,
                    expandedCardIds,
                    navController,
                    onDelete = { quizId ->
                        deleteQuiz(
                            quizId,
                            onSuccess = {
                                Toast.makeText(context, "Quiz deleted successfully", Toast.LENGTH_SHORT).show()
                                // Refresh quiz list
                                fetchQuizzes(
                                    onQuizzesFetched = { quizzes.value = it },
                                    onError = { errorMessage.value = it }
                                )
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ExpandableCard(
    quiz: Quiz,
    expandedCardIds: MutableState<Set<String>>,
    navController: NavController,
    onDelete: (String) -> Unit
){
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
            Text(text = "Date and Time: ${quiz.dateTime}", style = MaterialTheme.typography.bodyMedium)

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Fee: ${quiz.fee} EUR", style = MaterialTheme.typography.bodySmall)
                Text(text = "Number of teams available: ${quiz.seats}", style = MaterialTheme.typography.bodySmall)

                if (quiz.additionalInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Additional Info: ${quiz.additionalInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dodavanje gumba za pregled prijava
                Button(
                    onClick = {
                        val encodedQuizName = URLEncoder.encode(quiz.name, StandardCharsets.UTF_8.toString())
                        val encodedQuizDateTime = URLEncoder.encode(quiz.dateTime, StandardCharsets.UTF_8.toString())
                        navController.navigate("quiz_registrations/${quiz.id}/$encodedQuizName/$encodedQuizDateTime")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Registrations")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        navController.navigate("edit_quiz/${quiz.id}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Quiz")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onDelete(quiz.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Quiz")
                }


            }
        }
    }
}


@Composable
fun AddQuizScreen(navController: NavController) {
    val quizTypes = listOf(
        "Općeniti kviz", "Sportski kviz", "Glazbeni kviz", "Filmski kviz",
        "Tehnološki kviz", "Povijesni kviz", "Geografski kviz", "Literarni kviz",
        "Kviz o pop kulturi", "Tematski kvizov", "Ostalo"
    )
    var selectedQuizType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("") }
    var dateTime = remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val feeError = remember { mutableStateOf<String?>(null) }
    val seatsError = remember { mutableStateOf<String?>(null) }
    var additionalInfo by remember { mutableStateOf("") }
    val context = LocalContext.current

    val calendar = java.util.Calendar.getInstance()
    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault())

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(java.util.Calendar.MINUTE, minute)

            // Provjera je li vrijeme u budućnosti
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(context, "Date and time must be in the future.", Toast.LENGTH_SHORT).show()
            } else {
                dateTime.value = dateFormat.format(calendar.time)
            }
        },
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE),
        true
    )

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(java.util.Calendar.YEAR, year)
            calendar.set(java.util.Calendar.MONTH, month)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show() // Otvori TimePicker nakon odabira datuma
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    ).apply {
        // Ograničenje na buduće datume
        datePicker.minDate = System.currentTimeMillis()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Add New Quiz", style = MaterialTheme.typography.headlineMedium)

        // Dropdown za selekciju tipa kviza
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedQuizType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Quiz Type")},
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Quiz Type",
                        modifier = Modifier.clickable { expanded = true }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                quizTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedQuizType = type
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Polje za lokaciju
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ostala polja
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Quiz Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = fee,
            onValueChange = {
                fee = it
                feeError.value = if (it.toIntOrNull() == null || it.toInt() < 0) {
                    "Fee must be a non-negative number"
                } else {
                    null
                }
            },
            label = { Text("Fee") },
            modifier = Modifier.fillMaxWidth(),
            isError = feeError.value != null
        )
        if (feeError.value != null) {
            Text(feeError.value!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = seats,
            onValueChange = {
                seats = it
                seatsError.value = if (it.toIntOrNull() == null || it.toInt() <= 0) {
                    "Teams must be a positive number"
                } else {
                    null
                }
            },
            label = { Text("Number of Teams") },
            modifier = Modifier.fillMaxWidth(),
            isError = seatsError.value != null
        )
        if (seatsError.value != null) {
            Text(seatsError.value!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateTime.value,
            onValueChange = {},
            label = { Text("Date and Time") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    datePickerDialog.show() // Prvo otvori DatePickerDialog
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = additionalInfo,
            onValueChange = { additionalInfo = it },
            label = { Text("Additional Information (Optional)")},
            modifier = Modifier.fillMaxWidth()
        )



        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
        }

        Button(
            onClick = {
                if (selectedQuizType.isEmpty() || location.isEmpty() || name.isEmpty() || fee.isEmpty() || seats.isEmpty()
                    || dateTime.value.isEmpty()) {
                    errorMessage = "All fields are required!"
                } else {
                    addQuizToFirestore(
                        quizType = selectedQuizType,
                        location = location,
                        name = name,
                        fee = fee.toIntOrNull() ?: 0,
                        seats = seats.toIntOrNull() ?: 0,
                        dateTime = dateTime.value,
                        additionalInfo = additionalInfo,
                        context = context,
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
    additionalInfo: String,
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
        "additionalInfo" to additionalInfo,
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

fun deleteQuiz(
    quizId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("quizzes").document(quizId)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onError(e.message ?: "Failed to delete quiz") }
}



