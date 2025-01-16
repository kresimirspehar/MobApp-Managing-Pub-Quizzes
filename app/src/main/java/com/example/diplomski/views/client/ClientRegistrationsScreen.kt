package com.example.diplomski.views.client

import android.util.Log
import android.widget.Toast
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
import com.google.firebase.firestore.Query

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
                ClientQuizCard(
                    quiz = quiz,
                    registeredQuizzes = registeredQuizzes,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

@Composable
fun ClientQuizCard(
    quiz: RegisteredQuiz,
    registeredQuizzes: MutableState<List<RegisteredQuiz>>,
    errorMessage: MutableState<String>
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var registrationStatus by remember { mutableStateOf(quiz.status) } // Osvežava se na osnovu baze
    val buttonEnabled = registrationStatus == "rejected"
    val buttonColor =
        if (buttonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.5f
        )

    var teamName by remember { mutableStateOf("") }
    var teamSize by remember { mutableStateOf("") }
    var teamMemberNames by remember { mutableStateOf(listOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Osveži status prijave svaki put kada se ekran prikaže
    LaunchedEffect(quiz.id) {
        currentUser?.let {
            db.collection("registrations")
                .whereEqualTo("userId", it.uid)
                .whereEqualTo("quizId", quiz.id)
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    registrationStatus = documents.firstOrNull()?.getString("status") ?: "unknown"
                }
                .addOnFailureListener { exception ->
                    if (exception != null) {
                        Log.e("Error", "Failed to fetch registration status: ${exception.message}")
                    }
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Redak za naziv kviza i ikonicu za brisanje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = quiz.name, style = MaterialTheme.typography.headlineSmall)

                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Delete Registration",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Type: ${quiz.quizType}")
            Text(text = "Location: ${quiz.location}")
            Text(text = "Date: ${quiz.dateTime}")

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicator(status = registrationStatus)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Status: ${registrationStatus.capitalize()}")
            }

            if (registrationStatus == "rejected") {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = teamSize,
                    onValueChange = { size ->
                        teamSize = size
                        val sizeInt = size.toIntOrNull() ?: 0
                        if (sizeInt in 1..5) {
                            teamMemberNames = List(sizeInt) { "" }
                        }
                    },
                    label = { Text("Team Size (1-5)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                teamMemberNames.forEachIndexed { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            val updatedNames = teamMemberNames.toMutableList()
                            updatedNames[index] = newName
                            teamMemberNames = updatedNames
                        },
                        label = { Text("Member ${index + 1}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (teamName.isBlank()) {
                            Toast.makeText(context, "Please enter a team name.", Toast.LENGTH_SHORT).show()
                        } else if (teamSize.isNotEmpty() && teamMemberNames.size == teamSize.toInt() && teamMemberNames.all { it.isNotBlank() }) {
                            registerForQuiz(
                                context = context,
                                quizId = quiz.id,
                                teamName = teamName,
                                teamSize = teamSize.toInt(),
                                teamMembers = teamMemberNames,
                                onSuccess = {
                                    registrationStatus = "pending"
                                    fetchClientRegistrations(
                                        userId = currentUser?.uid.orEmpty(),
                                        onQuizzesFetched = { quizzes -> registeredQuizzes.value = quizzes },
                                        onError = { errorMessage.value = it }
                                    )
                                }
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Please ensure team size and member names are properly filled.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = registrationStatus == "rejected" || registrationStatus == "not_registered",
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (registrationStatus) {
                            "pending" -> "Pending Approval"
                            "accepted" -> "Registered"
                            "rejected" -> "Register Again"
                            else -> "Register"
                        }
                    )
                }
            }
        }

        // Dialog za potvrdu brisanja
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this registration?") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteRegistration(
                            quizId = quiz.id,
                            userId = currentUser?.uid.orEmpty(),
                            onSuccess = {
                                Toast.makeText(context, "Registration deleted successfully!", Toast.LENGTH_SHORT).show()
                                fetchClientRegistrations(
                                    userId = currentUser?.uid.orEmpty(),
                                    onQuizzesFetched = { quizzes -> registeredQuizzes.value = quizzes },
                                    onError = { errorMessage.value = it }
                                )
                                showDeleteConfirmation = false
                            },
                            onError = { error ->
                                Toast.makeText(context, "Failed to delete registration: $error", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmation = false
                            }
                        )
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("No")
                    }
                }
            )
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
        .orderBy("timeStamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { registrationDocs ->
            registrationDocs.documents.forEach {
            }
            val registeredQuizIdsWithStatus = registrationDocs.documents.groupBy { it.getString("quizId") }
                .mapNotNull { (quizId, docs) ->
                    // Pronađi najnoviji dokument za svaki `quizId`
                    val latestDoc = docs.maxByOrNull { it.getLong("timeStamp") ?: 0L }
                    Log.d("Debug", "QuizId: $quizId, Latest TimeStamp: ${latestDoc?.getLong("timeStamp")}, Status: ${latestDoc?.getString("status")}")
                    val status = latestDoc?.getString("status")
                    if (quizId != null && status != null) quizId to status else null
                }.toMap()


            if (registeredQuizIdsWithStatus.isEmpty()) {
                onQuizzesFetched(emptyList())
                return@addOnSuccessListener
            }

            val quizIds = registeredQuizIdsWithStatus.keys.toList()
            val statusMap = registeredQuizIdsWithStatus

            db.collection("quizzes")
                .whereIn(FieldPath.documentId(), quizIds)
                .get()
                .addOnSuccessListener { quizDocs ->
                    val quizzes = quizDocs.mapNotNull { document ->
                        val quizId = document.id
                        RegisteredQuiz(
                            id = quizId,
                            name = document.getString("name").orEmpty(),
                            quizType = document.getString("quizType").orEmpty(),
                            location = document.getString("location").orEmpty(),
                            dateTime = document.getString("dateTime").orEmpty(),
                            dateObject = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .parse(document.getString("dateTime").orEmpty()) ?: Date(),
                            status = statusMap[quizId] ?: "unknown" // Povlačimo status iz `registrations`
                        )
                    }.sortedBy { it.dateObject }
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

fun deleteRegistration(
    quizId: String,
    userId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("registrations")
        .whereEqualTo("quizId", quizId)
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            val document = snapshot.documents.firstOrNull()
            if (document != null) {
                db.collection("registrations").document(document.id)
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "Error deleting registration") }
            } else {
                onError("Registration not found")
            }
        }
        .addOnFailureListener { e -> onError(e.message ?: "Error fetching registration") }
}




