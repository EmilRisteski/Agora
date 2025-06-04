package com.example.agoraapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.room.*
import com.example.agoraapp.ui.auth.LoginActivity
import com.example.agoraapp.ui.theme.AgoraAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.example.agoraapp.util.LocaleHelper


@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: LocalDate,
    val location: String,
    val isLiked: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)
}

@Database(entities = [Event::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}

fun Event.toFirebaseMap(): Map<String, Any> = mapOf(
    "title" to title,
    "date" to date.toString(),
    "location" to location,
    "isLiked" to isLiked
)

fun deleteEventFromFirebase(title: String, date: LocalDate) {
    val db = FirebaseFirestore.getInstance()
    db.collection("events")
        .whereEqualTo("title", title)
        .whereEqualTo("date", date.toString())
        .get()
        .addOnSuccessListener { result ->
            result.documents.forEach { it.reference.delete() }
        }
}

class HomeActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("selected_language", "en") ?: "mk"
        val localeUpdatedContext = com.example.agoraapp.util.LocaleHelper.setLocale(newBase, lang)
        super.attachBaseContext(localeUpdatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "events-db"
        ).build()

        setContent {
            AgoraAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        database = db,
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

sealed class HomeTab(val labelResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Discover : HomeTab(R.string.tab_discover, Icons.Filled.Home)
    object MyEvents : HomeTab(R.string.tab_my_events, Icons.Filled.Favorite)
    object Logout : HomeTab(R.string.tab_logout, Icons.Filled.ExitToApp)
}

@Composable
fun HomeScreen(
    database: AppDatabase,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.Discover) }
    val tabs = listOf(HomeTab.Discover, HomeTab.MyEvents, HomeTab.Logout)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val allEvents = remember { mutableStateListOf<Event>() }
    val likedEvents = remember { mutableStateListOf<Event>() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val localEvents = withContext(Dispatchers.IO) {
            database.eventDao().getAllEvents()
        }
        allEvents.addAll(localEvents)
        likedEvents.addAll(localEvents.filter { it.isLiked })

        FirebaseFirestore.getInstance()
            .collection("events")
            .get()
            .addOnSuccessListener { result ->
                val remoteEvents = result.documents.mapNotNull { doc ->
                    val title = doc.getString("title")
                    val location = doc.getString("location")
                    val dateStr = doc.getString("date")
                    val isLiked = doc.getBoolean("isLiked") ?: false
                    try {
                        if (title != null && location != null && dateStr != null) {
                            val date = LocalDate.parse(dateStr)
                            Event(title = title, location = location, date = date, isLiked = isLiked)
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }

                val newEvents = remoteEvents.filterNot { re ->
                    allEvents.any { it.title == re.title && it.date == re.date }
                }

                scope.launch {
                    withContext(Dispatchers.IO) {
                        newEvents.forEach { database.eventDao().insertEvent(it) }
                    }
                    allEvents.addAll(newEvents)
                }
            }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelResId)) },
                        label = { Text(stringResource(tab.labelResId)) },
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab is HomeTab.Logout) onLogout()
                            else selectedTab = tab
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab is HomeTab.Discover) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                is HomeTab.Discover -> DiscoverSection(allEvents, likedEvents, database)
                is HomeTab.MyEvents -> MyEventsSection(likedEvents)
                else -> {}
            }
        }

        if (showDialog) {
            AddEventDialog(
                onDismiss = { showDialog = false },
                onAddEvent = { event ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            database.eventDao().insertEvent(event)
                        }
                        allEvents.add(event)
                        FirebaseFirestore.getInstance()
                            .collection("events")
                            .add(event.toFirebaseMap())
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun DiscoverSection(
    events: MutableList<Event>,
    likedEvents: MutableList<Event>,
    database: AppDatabase
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(events) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(event.title, style = MaterialTheme.typography.titleLarge)
                    Text("${stringResource(R.string.date_label)}: ${event.date.format(formatter)}")
                    Text("${stringResource(R.string.location_label)}: ${event.location}")
                    if (event.date.isBefore(today)) {
                        Text(
                            stringResource(R.string.event_passed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val updated = event.copy(isLiked = !event.isLiked)
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    database.eventDao().insertEvent(updated)
                                }
                                val index = events.indexOfFirst { it.title == event.title && it.date == event.date }
                                if (index != -1) {
                                    events[index] = updated
                                }
                                if (updated.isLiked) {
                                    if (likedEvents.none { it.title == updated.title && it.date == updated.date }) {
                                        likedEvents.add(updated)
                                    }
                                } else {
                                    likedEvents.removeAll { it.title == updated.title && it.date == updated.date }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (event.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(R.string.like_event)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    database.eventDao().deleteEvent(event)
                                    deleteEventFromFirebase(event.title, event.date)
                                }
                                events.remove(event)
                                likedEvents.removeAll { it.title == event.title && it.date == event.date }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_event))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyEventsSection(likedEvents: List<Event>) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    if (likedEvents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_liked_events))
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(likedEvents) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge)
                        Text("${stringResource(R.string.date_label)}: ${event.date.format(formatter)}")
                        Text("${stringResource(R.string.location_label)}: ${event.location}")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAddEvent: (Event) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                try {
                    val date = LocalDate.parse(dateInput, formatter)
                    if (title.isNotBlank() && location.isNotBlank()) {
                        onAddEvent(Event(title = title.trim(), location = location.trim(), date = date))
                    }
                } catch (_: Exception) {}
            }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.add_new_event)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.location_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text(stringResource(R.string.date_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
