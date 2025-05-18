package com.example.agoraapp.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.agoraapp.MainActivity
import com.example.agoraapp.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show splash UI - you can customize this with Compose or a layout
        setContent {
            // Simple Splash UI (you can improve this)
            androidx.compose.material3.Text(text = "Welcome to AgoraApp")
        }

        // Wait for 2 seconds then navigate
        lifecycleScope.launch {
            delay(2000)

            // TODO: Check if user is logged in, for now always go to LoginActivity
            val nextActivity = Intent(this@SplashActivity, LoginActivity::class.java)

            startActivity(nextActivity)
            finish() // close splash so user can't go back to it
        }
    }
}
