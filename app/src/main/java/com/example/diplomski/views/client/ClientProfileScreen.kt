package com.example.diplomski.views.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ClientProfileScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val userId = currentUser?.uid ?: ""

    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var editingField by remember { mutableStateOf<String?>(null) } // Praćenje aktivnog uređivanja
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Dohvaćanje trenutnih podataka iz Firestore
    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    phone = document.getString("phone") ?: ""
                    location = document.getString("location") ?: ""
                }
                .addOnFailureListener {
                    errorMessage = "Failed to fetch user data: ${it.message}"
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // Krug s inicijalima
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser?.email?.take(2)?.uppercase() ?: "UN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profilne stavke
            ProfileRow(
                label = "Phone",
                value = phone,
                isEditing = editingField == "phone",
                onEditToggle = {
                    if (editingField == "phone") {
                        updateField("phone", phone, userId, db, onSuccess = {
                            successMessage = "Phone updated successfully!"
                            editingField = null
                        }, onFailure = {
                            errorMessage = it
                        })
                    } else {
                        editingField = "phone"
                    }
                },
                onValueChange = { phone = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileRow(
                label = "Location",
                value = location,
                isEditing = editingField == "location",
                onEditToggle = {
                    if (editingField == "location") {
                        updateField("location", location, userId, db, onSuccess = {
                            successMessage = "Location updated successfully!"
                            editingField = null
                        }, onFailure = {
                            errorMessage = it
                        })
                    } else {
                        editingField = "location"
                    }
                },
                onValueChange = { location = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Poruke o uspjehu ili grešci
            if (successMessage.isNotEmpty()) {
                Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
            }

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        // Gumb za Sign Out (dolje desno)
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun ProfileRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(2f)
            )
        }
        IconButton(onClick = onEditToggle) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit $label"
            )
        }
    }
}

fun updateField(
    field: String,
    value: String,
    userId: String,
    db: FirebaseFirestore,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    db.collection("users").document(userId)
        .update(field, value)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e.message ?: "Failed to update $field") }
}

