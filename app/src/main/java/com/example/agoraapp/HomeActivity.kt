package com.example.agoraapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agoraapp.ui.theme.AgoraAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.agoraapp.ui.auth.LoginActivity
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgoraAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
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

data class Event(
    val title: String,
    val date: LocalDate,
    val location: String
)

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.Discover) }
    val tabs = listOf(HomeTab.Discover, HomeTab.MyEvents, HomeTab.Logout)

    val events = listOf(
        Event(
            title = "BIJELO DUGME",
            date = LocalDate.of(2025, 5, 31),
            location = "Arena Boris Trajkoski, Skopje"
        ),
        Event(
            title = "Teatarski Igri",
            date = LocalDate.of(2025, 6, 8),
            location = "Prilep, North Macedonia"
        )
    )

    val likedEvents = remember { mutableStateListOf<Event>() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab is HomeTab.Logout) onLogout()
                            else selectedTab = tab
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            when (selectedTab) {
                is HomeTab.Discover -> DiscoverSection(events, likedEvents)
                is HomeTab.MyEvents -> MyEventsSection(likedEvents)
                else -> {}
            }
        }
    }
}

@Composable
fun DiscoverSection(events: List<Event>, likedEvents: MutableList<Event>) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val today = LocalDate.now()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(events) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = event.title, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Date: ${event.date.format(formatter)}")
                    Text(text = "Location: ${event.location}")
                    if (event.date.isBefore(today)) {
                        Text(text = "This event has passed", color = MaterialTheme.colorScheme.error)
                    }

                    IconButton(
                        onClick = {
                            if (event in likedEvents) likedEvents.remove(event)
                            else likedEvents.add(event)
                        }
                    ) {
                        Icon(
                            imageVector = if (event in likedEvents) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyEventsSection(likedEvents: List<Event>) {
    if (likedEvents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You haven't liked any events yet.")
        }
    } else {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(likedEvents) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = event.title, style = MaterialTheme.typography.titleLarge)
                        Text(text = "Date: ${event.date.format(formatter)}")
                        Text(text = "Location: ${event.location}")
                    }
                }
            }
        }
    }
}
