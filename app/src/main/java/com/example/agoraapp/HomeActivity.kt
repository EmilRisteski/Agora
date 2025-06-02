package com.example.agoraapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.agoraapp.ui.theme.AgoraAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.agoraapp.ui.auth.LoginActivity
import androidx.compose.material.icons.filled.ExitToApp



class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgoraAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

sealed class HomeTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Discover : HomeTab("Discover", Icons.Filled.Home)
    object MyEvents : HomeTab("My Events", Icons.Filled.Favorite)
    object Logout : HomeTab("Logout", Icons.Filled.ExitToApp)
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.Discover) }

    val tabs = listOf(HomeTab.Discover, HomeTab.MyEvents, HomeTab.Logout)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab is HomeTab.Logout) {
                                onLogout()
                            } else {
                                selectedTab = tab
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            when (selectedTab) {
                is HomeTab.Discover -> DiscoverSection()
                is HomeTab.MyEvents -> MyEventsSection()
                else -> {}
            }
        }
    }
}

@Composable
fun DiscoverSection() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Discover Cultural Events & Museums")
    }
}

@Composable
fun MyEventsSection() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Your Favorite Events")
    }
}
