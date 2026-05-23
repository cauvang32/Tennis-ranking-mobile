package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.api.CreateMatchRequest
import com.example.api.Match
import com.example.api.Player
import com.example.api.Season
import com.example.repository.TennisRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onShowLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initData by TennisRepository.initData.collectAsState()
    val currentUser by TennisRepository.currentUser.collectAsState()
    val isAuthenticated by TennisRepository.isAuthenticated.collectAsState()
    val isLoading by TennisRepository.isLoading.collectAsState()
    val errorMessage by TennisRepository.errorMessage.collectAsState()

    val scope = rememberCoroutineScope()
    var isCreateMatchOpen by remember { mutableStateOf(false) }
    var selectedFilterDate by remember { mutableStateOf<String?>(null) }
    var selectedFilterSeasonId by remember { mutableStateOf<Int?>(null) }
    var filterType by remember { mutableStateOf(0) } // 0: Theo ngày, 1: Theo mùa
    var matchesList by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isFilterMatchesLoading by remember { mutableStateOf(false) }

    val isSandboxMode by TennisRepository.isSandboxMode.collectAsState()

    // Colors aligned with High-Density CSS
    val brandPrimary = MaterialTheme.colorScheme.primary
    val textBase = MaterialTheme.colorScheme.onSurface
    val cardStroke = MaterialTheme.colorScheme.outline
    
    // Load matches on selected date or defaultDate or selected season
    LaunchedEffect(initData, selectedFilterDate, selectedFilterSeasonId, filterType, isSandboxMode) {
        if (filterType == 0) {
            val date = selectedFilterDate ?: initData?.defaultDate
            if (date != null) {
                isFilterMatchesLoading = true
                val matches = TennisRepository.fetchMatchesByDate(date)
                if (matches != null) {
                    matchesList = matches
                } else if (selectedFilterDate == null) {
                    // Fallback to initData's matches
                    matchesList = initData?.defaultDateMatches ?: emptyList()
                }
                isFilterMatchesLoading = false
            } else {
                matchesList = initData?.defaultDateMatches ?: emptyList()
            }
        } else {
            val seasons = initData?.seasons ?: emptyList()
            val seasonId = selectedFilterSeasonId ?: initData?.activeSeason?.id ?: seasons.firstOrNull()?.id
            if (seasonId != null) {
                isFilterMatchesLoading = true
                val matches = TennisRepository.fetchMatchesBySeason(seasonId)
                if (matches != null) {
                    matchesList = matches
                }
                isFilterMatchesLoading = false
            } else {
                matchesList = emptyList()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            val role = currentUser?.role
            if (isAuthenticated && (role == "admin" || role == "editor")) {
                FloatingActionButton(
                    onClick = { isCreateMatchOpen = true },
                    containerColor = brandPrimary,
                    contentColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("add_match_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Match")
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
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(brandPrimary, CircleShape)
                            .clickable { onShowLogin() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }
                    Column {
                        Text(
                            text = if (isAuthenticated) currentUser?.displayName ?: currentUser?.username ?: "User" else "Chế độ Khách",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textBase
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.onTertiaryContainer, CircleShape)
                            )
                            Text(
                                text = "api.hungsanity.com",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dynamic Authentication Controls
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
                                .testTag("login_button_top_dashboard")
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
                                .testTag("logout_button_top_dashboard")
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

                    // Theme Toggle
                    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val themeOverride by TennisRepository.themeOverride.collectAsState()
                    val isDark = themeOverride ?: systemDark

                    IconButton(onClick = { TennisRepository.toggleThemeOption() }) {
                        Text(
                            text = if (isDark) "☀️" else "🌙",
                            fontSize = 18.sp
                        )
                    }

                    IconButton(onClick = { scope.launch { TennisRepository.fetchInitData() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = brandPrimary)
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Administrative Status Banner
                item {
                    if (!isAuthenticated) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowLogin() }
                                .testTag("admin_login_banner"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Admin info",
                                    tint = brandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chế độ Khách (Chỉ xem)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandPrimary
                                    )
                                    Text(
                                        text = "Đăng nhập với tư cách Quản trị viên/Biên tập viên để quản lý giải đấu, cập nhật điểm số và lưu trận đấu.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Go to login",
                                    tint = brandPrimary
                                )
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Logged in",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Đã xác thực: ${currentUser?.role?.uppercase()} PANEL ACTIVE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Welcome ${currentUser?.displayName ?: currentUser?.username}! You have full access to record live matches, register players, and end tournament seasons.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // High Density Featured Live Match Card
                item {
                    val featuredMatch = initData?.defaultDateMatches?.firstOrNull()
                    if (featuredMatch != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "TRẬN ĐẤU MỚI NHẤT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = if (featuredMatch != null) "NGÀY THI ĐẤU • ${featuredMatch.playDate}" else "LIVE DEMO • COURT 1",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = brandPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Competitor 1
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(2.dp, brandPrimary, CircleShape)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "VĐV 1",
                                            tint = brandPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = featuredMatch?.player1_name ?: "Competitor A",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textBase,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    if (featuredMatch?.matchType == "duo" && featuredMatch.player2_name != null) {
                                        Text(
                                            text = "& ${featuredMatch.player2_name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Score Container
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = featuredMatch?.team1Score?.toString() ?: "6",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "|",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = featuredMatch?.team2Score?.toString() ?: "4",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        text = if (featuredMatch != null) {
                                            "ĐỘI THẮNG: TEAM ${featuredMatch.winningTeam}"
                                        } else {
                                            "MATCH POINT"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandPrimary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Competitor 2
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(2.dp, Color.Transparent, CircleShape)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "VĐV 2",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = featuredMatch?.player3_name ?: "Competitor B",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textBase,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    if (featuredMatch?.matchType == "duo" && featuredMatch.player4_name != null) {
                                        Text(
                                            text = "& ${featuredMatch.player4_name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                } // This closes the 'item' block

                if (!isLoading && !isFilterMatchesLoading) {
                    if (matchesList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có trận đấu nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(matchesList, key = { it.id }) { match ->
                            MatchListItem(
                                match = match,
                                isAdminOrEditor = isAuthenticated && (currentUser?.role == "admin" || currentUser?.role == "editor"),
                                onDeleteClick = {
                                    scope.launch {
                                        TennisRepository.deleteMatch(match.id)
                                    }
                                }
                            )
                        }
                    }
                }

                // Error and Loading indicators
                item {
                    if (isLoading || isFilterMatchesLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = brandPrimary)
                        }
                    }

                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { TennisRepository.clearErrorMessage() }) {
                                    Text("Đóng", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // CREATE MATCH DIALOG
    if (isCreateMatchOpen) {
        val seasons = initData?.seasons ?: emptyList()
        val allPlayers = initData?.players ?: emptyList()

        CreateMatchDialog(
            seasons = seasons,
            players = allPlayers,
            onDismiss = { isCreateMatchOpen = false },
            onConfirm = { req ->
                scope.launch {
                    val success = TennisRepository.createMatch(req)
                    if (success) {
                        isCreateMatchOpen = false
                    }
                }
            }
        )
    }
}

@Composable
fun MatchListItem(
    match: Match,
    isAdminOrEditor: Boolean,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode: ${match.matchType.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.playDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (isAdminOrEditor) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDeleteClick() }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = match.player1_name ?: "Player A",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (match.winningTeam == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (match.matchType == "duo" && match.player2_name != null) {
                        Text(
                            text = match.player2_name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (match.winningTeam == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Scores
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.team1Score.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (match.winningTeam == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = match.team2Score.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (match.winningTeam == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Team 2
                Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = match.player3_name ?: "Player B",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (match.winningTeam == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (match.matchType == "duo" && match.player4_name != null) {
                        Text(
                            text = match.player4_name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (match.winningTeam == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchDialog(
    seasons: List<Season>,
    players: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (CreateMatchRequest) -> Unit
) {
    var selectedSeasonId by remember { mutableStateOf(seasons.firstOrNull()?.id ?: 0) }
    var playDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var matchType by remember { mutableStateOf("solo") } // solo or duo
    
    var player1Id by remember { mutableStateOf(players.firstOrNull()?.id ?: 0) }
    var player2Id by remember { mutableStateOf<Int?>(null) }
    var player3Id by remember { mutableStateOf(players.getOrNull(1)?.id ?: players.firstOrNull()?.id ?: 0) }
    var player4Id by remember { mutableStateOf<Int?>(null) }

    var team1ScoreStr by remember { mutableStateOf("") }
    var team2ScoreStr by remember { mutableStateOf("") }
    var winningTeam by remember { mutableStateOf(1) }

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi kết quả trận đấu", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    if (validationError != null) {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Season Chọnor
                item {
                    Text("Chọn mùa giải:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        val activeSeason = seasons.find { it.id == selectedSeasonId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Text(
                                text = activeSeason?.name ?: "Không có mùa giải",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            seasons.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedSeasonId = s.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Play Date Entry
                item {
                    TextField(
                        value = playDate,
                        onValueChange = { playDate = it },
                        label = { Text("Ngày thi đấu (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.background, unfocusedContainerColor = MaterialTheme.colorScheme.background)
                    )
                }

                // Match Mode Chọnor (Solo / Duo)
                item {
                    Text("Thể thức thi đấu:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = matchType == "solo",
                            onClick = { 
                                matchType = "solo"
                                player2Id = null
                                player4Id = null
                            },
                            label = { Text("Đơn (1v1)") }
                        )
                        FilterChip(
                            selected = matchType == "duo",
                            onClick = { 
                                matchType = "duo"
                                if (player2Id == null) player2Id = players.getOrNull(2)?.id ?: players.getOrNull(0)?.id
                                if (player4Id == null) player4Id = players.getOrNull(3)?.id ?: players.getOrNull(1)?.id
                            },
                            label = { Text("Đôi (2v2)") }
                        )
                    }
                }

                // Player selectors
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("VĐV ĐỘI 1", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            
                            // VĐV 1
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("VĐV 1:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    Text(
                                        text = players.find { it.id == player1Id }?.name ?: "Chọn",
                                        modifier = Modifier
                                            .clickable { expanded = true }
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.heightIn(max = 240.dp)
                                    ) {
                                        players.forEach { p ->
                                            DropdownMenuItem(text = { Text(p.name) }, onClick = { player1Id = p.id; expanded = false })
                                        }
                                    }
                                }
                            }

                            // VĐV 2 if Duo
                            if (matchType == "duo") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("VĐV 2:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Box {
                                        var expanded by remember { mutableStateOf(false) }
                                        Text(
                                            text = players.find { it.id == player2Id }?.name ?: "Chọn",
                                            modifier = Modifier
                                                .clickable { expanded = true }
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.heightIn(max = 240.dp)
                                        ) {
                                            players.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name) }, onClick = { player2Id = p.id; expanded = false })
                                            }
                                        }
                                    }
                                }
                            }

                            TextField(
                                value = team1ScoreStr,
                                onValueChange = { team1ScoreStr = it },
                                label = { Text("Điểm trận Đội 1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("VĐV ĐỘI 2", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // VĐV 3
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("VĐV 3:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    Text(
                                        text = players.find { it.id == player3Id }?.name ?: "Chọn",
                                        modifier = Modifier
                                            .clickable { expanded = true }
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.heightIn(max = 240.dp)
                                    ) {
                                        players.forEach { p ->
                                            DropdownMenuItem(text = { Text(p.name) }, onClick = { player3Id = p.id; expanded = false })
                                        }
                                    }
                                }
                            }

                            // VĐV 4 if Duo
                            if (matchType == "duo") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("VĐV 4:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Box {
                                        var expanded by remember { mutableStateOf(false) }
                                        Text(
                                            text = players.find { it.id == player4Id }?.name ?: "Chọn",
                                            modifier = Modifier
                                                .clickable { expanded = true }
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.heightIn(max = 240.dp)
                                        ) {
                                            players.forEach { p ->
                                                DropdownMenuItem(text = { Text(p.name) }, onClick = { player4Id = p.id; expanded = false })
                                            }
                                        }
                                    }
                                }
                            }

                            TextField(
                                value = team2ScoreStr,
                                onValueChange = { team2ScoreStr = it },
                                label = { Text("Điểm trận Đội 2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }

                // Winner selector
                item {
                    Text("Đội thắng:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = winningTeam == 1, onClick = { winningTeam = 1 })
                            Text("Đội 1 Thắng")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = winningTeam == 2, onClick = { winningTeam = 2 })
                            Text("Đội 2 Thắng")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t1Score = team1ScoreStr.toIntOrNull()
                    val t2Score = team2ScoreStr.toIntOrNull()
                    if (t1Score == null || t2Score == null) {
                        validationError = "Please enter valid numeric scores for both teams."
                        return@Button
                    }

                    // Check player duplicates
                    val activePlayerIds = mutableListOf(player1Id, player3Id)
                    if (matchType == "duo") {
                        if (player2Id == null || player4Id == null) {
                            validationError = "Please select all four players for doubles match."
                            return@Button
                        }
                        activePlayerIds.add(player2Id!!)
                        activePlayerIds.add(player4Id!!)
                    }
                    if (activePlayerIds.size != activePlayerIds.distinct().size) {
                        validationError = "Error: A player cannot be selected multiple times in a match."
                        return@Button
                    }

                    validationError = null
                    onConfirm(
                        CreateMatchRequest(
                            seasonId = selectedSeasonId,
                            playDate = playDate,
                            player1Id = player1Id,
                            player2Id = player2Id,
                            player3Id = player3Id,
                            player4Id = player4Id,
                            team1Score = t1Score,
                            team2Score = t2Score,
                            winningTeam = winningTeam,
                            matchType = matchType
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Lưu kết quả")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
