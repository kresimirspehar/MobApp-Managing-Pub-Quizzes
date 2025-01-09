package com.example.diplomski.views.client

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.example.diplomski.views.admin.Quiz
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen() {

    val selectedLocation = remember { mutableStateOf("") }
    val selectedQuizType = remember { mutableStateOf("") }

    val allLocations = remember { mutableStateOf<List<String>>(emptyList()) }
    val allQuizTypes = remember { mutableStateOf<List<String>>(emptyList()) }

    val filteredQuizzes = remember { mutableStateOf<List<Quiz>>(emptyList()) }

    val quizzes = remember { mutableStateOf<List<Quiz>>(emptyList()) }
    val errorMessage = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fetchAllQuizzes(
            onQuizzesFetched = { quizList ->
                quizzes.value = quizList
                filteredQuizzes.value = quizList

                allLocations.value = quizList.map { it.location }.distinct()
                allQuizTypes.value = quizList.map { it.quizType }.distinct()
            },
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

        FilterDropdown(
            label = "Location",
            options = allLocations.value,
            selectedOption = selectedLocation.value,
            onOptionSelected = {
                selectedLocation.value = it
                applyFilters(quizzes.value, selectedLocation.value, selectedQuizType.value, filteredQuizzes)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilterDropdown(
            label = "Quiz Type",
            options = allQuizTypes.value,
            selectedOption = selectedQuizType.value,
            onOptionSelected = {
                selectedQuizType.value = it
                applyFilters(quizzes.value, selectedLocation.value, selectedQuizType.value, filteredQuizzes)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(filteredQuizzes.value) { quiz ->
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
    val remainingSeats = remember { mutableStateOf(quiz.seats) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            checkRegistrationStatus(it.uid, quiz.id) { status ->
                registrationStatus.value = status
            }

            fetchCurrentRegistrations(quiz.id) { currentRegistrations  ->
                remainingSeats.value = quiz.seats - currentRegistrations
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
            Text(
                text = if (remainingSeats.value > 0)
                    "Application available: ${remainingSeats.value}"
                else
                    "All places filled",
                style = MaterialTheme.typography.bodyMedium,
                color = if (remainingSeats.value > 0) Color.Green else Color.Red
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = teamSize,
                    onValueChange = {
                        val size = it.toIntOrNull() ?: 0
                        if (size in 1..5) {
                            teamSize = it
                            // Adjust teamMemberNames size dynamically
                            teamMemberNames = if (size > teamMemberNames.size) {
                                teamMemberNames + List(size - teamMemberNames.size) { "" }
                            } else {
                                teamMemberNames.take(size)
                            }
                        } else {
                            Toast.makeText(context, "Team size must be between 1 and 5.", Toast.LENGTH_SHORT).show()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val size = teamSize.toIntOrNull()
                        if (size == null || size <= 0 || size > 5) {
                            Toast.makeText(
                                context,
                                "Team size must be between 1 and 5.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (teamMemberNames.size != size || teamMemberNames.any { it.isBlank() }) {
                            Toast.makeText(
                                context,
                                "Please fill in all team member names.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            registerForQuiz(context, quiz.id, size, teamMemberNames) {
                                registrationStatus.value = "pending"
                                isExpanded = false
                            }
                        }
                    },
                    enabled = registrationStatus.value == "not_registered" && remainingSeats.value > 0,
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
            }
        }
    }
}


fun registerForQuiz(
    context: Context,
    quizId: String,
    teamSize: Int,
    teamMembers: List<String>,
    onSuccess: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        return
    }

    if (teamSize > 5) {
        Toast.makeText(context, "Team size cannot exceed 5 members.", Toast.LENGTH_SHORT).show()
        return
    }

    if (teamMembers.size != teamSize) {
        Toast.makeText(context, "Mismatch between team size and team member names provided.", Toast.LENGTH_SHORT).show()
        return
    }


    db.collection("users").document(currentUser.uid).get()
        .addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name").orEmpty()

            if (userName.isEmpty()) {
                Toast.makeText(context, "User name not found. Please contact support.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            db.collection("registrations")
                .whereEqualTo("quizId", quizId)
                .get()
                .addOnSuccessListener { documents ->
                    val totalRegistrations = documents.size()

                    db.collection("quizzes").document(quizId).get()
                        .addOnSuccessListener { quizDocument ->
                            val seats = quizDocument.getLong("seats")?.toInt() ?: 0

                            if (totalRegistrations >= seats) {
                                Toast.makeText(context, "This quiz is full.", Toast.LENGTH_SHORT).show()
                            } else {
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
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to register: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to fetch quiz data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to fetch registrations: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch user details: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

fun fetchCurrentRegistrations(quizId: String, onResult: (Int) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("registrations")
        .whereEqualTo("quizId", quizId)
        .get()
        .addOnSuccessListener { documents ->
            onResult(documents.size())
        }
        .addOnFailureListener {
            onResult(0)
        }
}

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand filter options",
                    modifier = Modifier.clickable { expanded = true }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }

            // Reset opcija (prikaz svih)
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onOptionSelected("")
                    expanded = false
                }
            )
        }
    }
}

fun applyFilters(
    quizzes: List<Quiz>,
    locationFilter: String,
    quizTypeFilter: String,
    filteredQuizzes: MutableState<List<Quiz>>
) {
    filteredQuizzes.value = quizzes.filter { quiz ->
        (locationFilter.isEmpty() || quiz.location == locationFilter) &&
                (quizTypeFilter.isEmpty() || quiz.quizType == quizTypeFilter)
    }
}

