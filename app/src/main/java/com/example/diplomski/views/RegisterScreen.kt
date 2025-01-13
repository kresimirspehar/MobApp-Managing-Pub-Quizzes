package com.example.diplomski.views

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.diplomski.views.registerWithEmailAndPassword
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Client") } // Zadana vrijednost
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedLabelColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedLabelColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // RadioButtonGroup za odabir role
            Text(
                text = "Select Role",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "Client",
                    onClick = { role = "Client" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                )
                Text(
                    text = "Client",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = role == "Admin",
                    onClick = { role = "Admin" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                )
                Text(
                    text = "Admin",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        errorMessage = "Please fill in all fields"
                    } else if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                    } else {
                        registerWithEmailAndPassword(context, email, password, role, navController) {
                            errorMessage = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2)
                )
            ) {
                Text("Register")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Already have an account?",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Go to Login", color = Color.White)
            }
        }
    }
}

fun registerWithEmailAndPassword(
    context: Context,
    email: String,
    password: String,
    role: String,
    navController: NavController, // Dodano za navigaciju
    onRegisterSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val user = hashMapOf(
                    "email" to email,
                    "role" to role,
                    "name" to email.substringBefore("@")
                )

                // Dodavanje korisnika u Firestore
                db.collection("users")
                    .document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        onRegisterSuccess()
                        navigateToHomeFromRegister(navController, role)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
}


fun navigateToHomeFromRegister(navController: NavController, role: String) {
    when (role) {
        "Client" -> {
            navController.navigate("client_home") {
                popUpTo(0) { inclusive = true }
            }
        }
        "Admin" -> {
            navController.navigate("admin_home") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}







