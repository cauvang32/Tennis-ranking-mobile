package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.TennisRepository
import com.example.notification.NotificationHelper
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Core Tennis System
        TennisRepository.initialize(applicationContext)

        // Initialize notification system
        NotificationHelper.initialize(applicationContext)
        com.example.repository.NotificationRepository.initialize(applicationContext)

        enableEdgeToEdge()
        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val themeOverride by TennisRepository.themeOverride.collectAsState()
            val isDark = themeOverride ?: systemDark
            
            MyApplicationTheme(darkTheme = isDark) {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    var activeTab by remember { mutableStateOf(0) } // 0: Live, 1: Rankings, 2: Seasons, 3: Players
    var isLoginOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val brandPrimary = MaterialTheme.colorScheme.primary

    // Trigger API Initial bootstrap load
    LaunchedEffect(Unit) {
        TennisRepository.fetchInitData()
    }

    // Start background sync on resume (power saving optimization)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        TennisRepository.startBackgroundSync()
    }

    // Stop background sync on pause
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        TennisRepository.stopBackgroundSync()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    // TAB 0: LIVE / MATCHES
                    NavigationBarItem(
                        selected = activeTab == 0 && !isLoginOpen,
                        onClick = {
                            activeTab = 0
                            isLoginOpen = false
                        },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Trực tiếp") },
                        label = { Text("Trực tiếp", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_live")
                    )

                    // TAB 1: STANDINGS
                    NavigationBarItem(
                        selected = activeTab == 1 && !isLoginOpen,
                        onClick = {
                            activeTab = 1
                            isLoginOpen = false
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "BXH") },
                        label = { Text("BXH", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_rankings")
                    )

                    // TAB 2: SEASONS
                    NavigationBarItem(
                        selected = activeTab == 2 && !isLoginOpen,
                        onClick = {
                            activeTab = 2
                            isLoginOpen = false
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Mùa giải") },
                        label = { Text("Mùa giải", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_seasons")
                    )

                    // TAB 3: PLAYERS
                    NavigationBarItem(
                        selected = activeTab == 3 && !isLoginOpen,
                        onClick = {
                            activeTab = 3
                            isLoginOpen = false
                        },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Người chơi") },
                        label = { Text("Người chơi", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_players")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoginOpen) {
                    LoginScreen(
                        onLoginSuccess = { isLoginOpen = false }
                    )
                } else {
                    when (activeTab) {
                        0 -> DashboardScreen(onShowLogin = { isLoginOpen = true })
                        1 -> RankingsScreen(onShowLogin = { isLoginOpen = true })
                        2 -> SeasonsScreen(onShowLogin = { isLoginOpen = true })
                        3 -> PlayersScreen(onShowLogin = { isLoginOpen = true })
                    }
                }
            }
        }

        // Floating Close Login overlay button if active
        if (isLoginOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 24.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .clickable { isLoginOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Login",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
