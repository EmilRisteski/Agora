package com.example.agoraapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.agoraapp.auth.RegisterActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            LoginScreen(
                onEmailLoginClick = { email, password ->
                    loginUser(email, password)
                },
                onAnonymousLoginClick = {
                    loginAnonymously()
                },
                onNavigateToRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                }
            )
        }
    }

    private fun loginUser(email: String, password: String) {
        if(email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to your main/home screen
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    Toast.makeText(this, "Anonymous login successful!", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to your main/home screen
                } else {
                    Toast.makeText(this, "Anonymous login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onEmailLoginClick: (email: String, password: String) -> Unit,
    onAnonymousLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onEmailLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onAnonymousLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue as Guest")
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register here")
        }
    }
}
