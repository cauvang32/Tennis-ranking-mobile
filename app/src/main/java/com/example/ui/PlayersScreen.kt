package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.Player
import com.example.repository.TennisRepository
import com.example.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onShowLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initData by TennisRepository.initData.collectAsState()
    val isAuthenticated by TennisRepository.isAuthenticated.collectAsState()
    val currentUser by TennisRepository.currentUser.collectAsState()
    val isLoading by TennisRepository.isLoading.collectAsState()

    val scope = rememberCoroutineScope()
    var isCreatePlayerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var playerPendingDelete by remember { mutableStateOf<Player?>(null) }

    val brandPrimary = MaterialTheme.colorScheme.primary

    Scaffold(
        floatingActionButton = {
            if (isAuthenticated && currentUser?.role == "admin") {
                FloatingActionButton(
                    onClick = { isCreatePlayerOpen = true },
                    containerColor = brandPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Competitor")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Screen Header Title & dynamic authentication
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = brandPrimary, modifier = Modifier.size(28.dp))
                    Text(
                        text = "Danh sách VĐV",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isAuthenticated) {
                    Button(
                        onClick = { onShowLogin() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandPrimary,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("login_button_players_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng Nhập",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                TennisRepository.logout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("logout_button_players_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng Xuất",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val themeOverride by TennisRepository.themeOverride.collectAsState()
                val isDark = themeOverride ?: systemDark

                IconButton(onClick = { TennisRepository.toggleThemeOption() }) {
                    Text(
                        text = if (isDark) "☀️" else "🌙",
                        fontSize = 18.sp
                    )
                }
            }

            // Credentials & Authentication Help banner
            if (!isAuthenticated) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clickable { onShowLogin() }
                        .testTag("admin_login_banner_players"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Admin info",
                            tint = brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Đăng nhập với quyền Quản trị viên để quản lý danh sách.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Login",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandPrimary
                        )
                    }
                }
            }

            // High Density Search Anchor
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Lọc theo tên...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = brandPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = brandPrimary)
                }
            } else {
                val allPlayers = initData?.players ?: emptyList()
                val filteredPlayers = allPlayers.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                if (filteredPlayers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No players found." else "No players match '$searchQuery'",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Modern Responsive List/Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredPlayers, key = { it.id }, contentType = { "player_card" }) { player ->
                            val onDelete = remember(player.id) {
                                { playerPendingDelete = player }
                            }
                            PlayerGridCard(
                                player = player,
                                isAdmin = isAuthenticated && currentUser?.role == "admin",
                                onDeleteClick = onDelete,
                                brandColor = brandPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // CREATE NGƯỜI CHƠI DIALOG
    if (isCreatePlayerOpen) {
        CreatePlayerDialog(
            onDismiss = { isCreatePlayerOpen = false },
            onConfirm = { name ->
                scope.launch {
                    val success = TennisRepository.createPlayer(name)
                    if (success) {
                        isCreatePlayerOpen = false
                    }
                }
            }
        )
    }

    // CONFIRM DELETE PLAYER DIALOG
    if (playerPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { playerPendingDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_player_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_delete_player_body, playerPendingDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = playerPendingDelete ?: return@Button
                        scope.launch {
                            TennisRepository.deletePlayer(target.id)
                            playerPendingDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_delete_player))
                }
            },
            dismissButton = {
                TextButton(onClick = { playerPendingDelete = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
fun PlayerGridCard(
    player: Player,
    isAdmin: Boolean,
    onDeleteClick: () -> Unit,
    brandColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = player.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: #${player.id}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isAdmin) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Player",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng Ký VĐV", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị VĐV") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter a valid player name."
                        return@Button
                    }
                    validationError = null
                    onConfirm(name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Đăng Ký")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
