package com.example.diplomski.views.admin

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditQuizScreen(navController: NavController, quizId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val quizTypes = listOf(
        "General pub quiz", "Sports quiz", "Music quiz", "Movie quiz",
        "Technology quiz", "History quiz", "Geography quiz", "Literature quiz",
        "Pop culture quiz", "Themed quiz", "Other type of quiz"
    )

    // Stanja za podatke o kvizu
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var quizType by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false)}

    val calendar = java.util.Calendar.getInstance()
    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault())

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(java.util.Calendar.MINUTE, minute)

            // Provjera je li vrijeme u budućnosti
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(context, "Date and time must be in the future.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                dateTime = dateFormat.format(calendar.time)
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

    // Učitaj podatke o kvizu iz Firestorea
    LaunchedEffect(quizId) {
        db.collection("quizzes").document(quizId).get()
            .addOnSuccessListener { document ->
                name = document.getString("name").orEmpty()
                location = document.getString("location").orEmpty()
                quizType = document.getString("quizType").orEmpty()
                fee = document.getLong("fee")?.toString().orEmpty()
                seats = document.getLong("seats")?.toString().orEmpty()
                dateTime = document.getString("dateTime").orEmpty()
                additionalInfo = document.getString("additionalInfo").orEmpty()

                // Postavi inicijalni datum i vrijeme u kalendar
                dateFormat.parse(dateTime)?.let {
                    calendar.time = it
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load quiz: ${e.message}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Edit Quiz", style = MaterialTheme.typography.headlineMedium)

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = quizType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Quiz Type") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Quiz Type",
                        modifier = Modifier.clickable { expanded = true }
                    )
                },
                enabled = true
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                quizTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            quizType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Quiz Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = fee,
            onValueChange = {
                if (it.length <= 3 && it.all { c -> c.isDigit() }) fee = it
            },
            label = { Text("Fee") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = seats,
            onValueChange = {
                if (it.all { c -> c.isDigit() }) seats = it
            },
            label = { Text("Teams") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    datePickerDialog.show()
                }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .clickable { datePickerDialog.show() }
        ) {
            OutlinedTextField(
                value = dateTime,
                onValueChange = {},
                label = { Text("Date and Time") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { datePickerDialog.show() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = additionalInfo,
            onValueChange = { additionalInfo = it },
            label = { Text("Additional Information (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                // Validacija unosa
                val feeValue = fee.toIntOrNull()
                val seatsValue = seats.toIntOrNull()

                when {
                    name.isBlank() || location.isBlank() || quizType.isBlank() || fee.isBlank() || seats.isBlank() || dateTime.isBlank() -> {
                        Toast.makeText(context, "All fields must be filled in.", Toast.LENGTH_SHORT)
                            .show()
                    }

                    feeValue == null || feeValue <= 0 -> {
                        Toast.makeText(
                            context,
                            "Fee must be a positive number.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    seatsValue == null || seatsValue <= 0 -> {
                        Toast.makeText(
                            context,
                            "Teams must be a positive number.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        // Ako je validacija uspješna, spremi promjene u Firestore
                        val updatedQuiz = mapOf(
                            "name" to name,
                            "location" to location,
                            "quizType" to quizType,
                            "fee" to feeValue,
                            "seats" to seatsValue,
                            "dateTime" to dateTime,
                            "additionalInfo" to additionalInfo
                        )
                        db.collection("quizzes").document(quizId).update(updatedQuiz)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Quiz updated successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update quiz: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}

