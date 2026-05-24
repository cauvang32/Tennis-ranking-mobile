package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RankingEntry
import com.example.api.Season
import com.example.repository.TennisRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingsScreen(
    modifier: Modifier = Modifier,
    onShowLogin: (() -> Unit)? = null
) {
    val initData by TennisRepository.initData.collectAsState()
    val isLoading by TennisRepository.isLoading.collectAsState()

    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) } // 0: Tổng, 1: Season, 2: Daily
    var selectedSeasonId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    var rankingsList by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var isLocalLoading by remember { mutableStateOf(false) }

    val brandPrimary = MaterialTheme.colorScheme.primary
    val cardStroke = MaterialTheme.colorScheme.outlineVariant

    // P6: Use stable keys to avoid redundant API calls on every background sync
    val lifetimeRankings = initData?.lifetimeRankings
    val defaultDateRankings = initData?.defaultDateRankings
    val currentDefaultDate = initData?.defaultDate
    val dataVersion = initData?.version ?: 0L

    // Reactively update lifetime rankings (Tab 0) without API calls
    LaunchedEffect(lifetimeRankings, activeTab) {
        if (activeTab == 0) {
            rankingsList = lifetimeRankings ?: emptyList()
        }
    }

    // Reactively update default date rankings (Tab 2 with no custom selection)
    LaunchedEffect(defaultDateRankings, activeTab, selectedDate) {
        if (activeTab == 2 && (selectedDate == null || selectedDate == currentDefaultDate)) {
            rankingsList = defaultDateRankings ?: emptyList()
            if (selectedDate == null && currentDefaultDate != null) selectedDate = currentDefaultDate
        }
    }

    // Fetch from API when user changes tab/filter selections OR when background sync updates data version
    LaunchedEffect(activeTab, selectedSeasonId, selectedDate, dataVersion) {
        isLocalLoading = true
        when (activeTab) {
            0 -> {
                // Handled reactively above
            }
            1 -> {
                val seasonId = selectedSeasonId ?: initData?.activeSeason?.id ?: initData?.seasons?.firstOrNull()?.id
                if (seasonId != null) {
                    if (selectedSeasonId == null) selectedSeasonId = seasonId
                    val res = TennisRepository.fetchSeasonRankings(seasonId)
                    if (res != null) rankingsList = res
                } else {
                    rankingsList = emptyList()
                }
            }
            2 -> {
                val date = selectedDate ?: return@LaunchedEffect
                // Use cached data when selected date matches default date
                if (date == currentDefaultDate && defaultDateRankings != null) {
                    // Already handled reactively above
                } else {
                    val res = TennisRepository.fetchDateRankings(date)
                    if (res != null) rankingsList = res
                }
            }
        }
        isLocalLoading = false
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Screen title & Top Navigation Đăng Nhập/Out action
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
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = brandPrimary, modifier = Modifier.size(28.dp))
                    Text(
                        text = "Bảng xếp hạng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Header Đăng Nhập/Out button
                onShowLogin?.let { showLoginAction ->
                    val isAuthenticated by TennisRepository.isAuthenticated.collectAsState()
                    if (!isAuthenticated) {
                        Button(
                            onClick = { showLoginAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandPrimary,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("login_button_rankings")
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
                                .testTag("logout_button_rankings")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
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
            }

            // Tabs selector (High-Density Styled)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = brandPrimary,
                divider = {}
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Text("Tổng", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Text("Theo mùa", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Text("Theo ngày", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // Filters selectors based on active tabs - Outlined highly-visible dropdown buttons for Phone
            AnimatedVisibility(visible = activeTab == 1) {
                val seasons = initData?.seasons ?: emptyList()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Season:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val activeSeason = seasons.find { it.id == selectedSeasonId }
                        
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = brandPrimary
                            ),
                            border = BorderStroke(1.5.dp, brandPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("season_filter_dropdown")
                        ) {
                            Text(
                                text = activeSeason?.name ?: "Chọn tournament season...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Indicator",
                                modifier = Modifier.size(20.dp),
                                tint = brandPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            seasons.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedSeasonId = s.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = activeTab == 2) {
                val dates = initData?.playDateStrings ?: emptyList()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Play Date:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = brandPrimary
                            ),
                            border = BorderStroke(1.5.dp, brandPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("date_filter_dropdown")
                        ) {
                            Text(
                                text = selectedDate ?: "Choose summary date...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Indicator",
                                modifier = Modifier.size(20.dp),
                                tint = brandPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            dates.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedDate = d
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // High Density Metrics Header Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(28.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("NGƯỜI CHƠI", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("ĐIỂM", modifier = Modifier.weight(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("T-B", modifier = Modifier.weight(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("THẮNG %", modifier = Modifier.weight(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("TIỀN PHẠT", modifier = Modifier.weight(1.1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading || isLocalLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = brandPrimary)
                }
            } else if (rankingsList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Không có bảng xếp hạng nào.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    // Sorting rankings primarily by points, then win percentage, then name
                    val sortedRankings = rankingsList.sortedWith(
                        compareByDescending<RankingEntry> { it.points }
                            .thenByDescending { it.winPercentage ?: 0.0 }
                            .thenBy { it.name }
                    )

                    itemsIndexed(sortedRankings, key = { _, item -> item.id }, contentType = { _, _ -> "ranking_row" }) { index, entry ->
                        RankingRowItem(
                            pos = index + 1,
                            entry = entry,
                            brandPrimary = brandPrimary,
                            borderDivider = cardStroke
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingRowItem(
    pos: Int,
    entry: RankingEntry,
    brandPrimary: Color,
    borderDivider: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderDivider, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pos
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(28.dp)
                ) {
                    val posColor = when (pos) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> Color.Transparent
                    }
                    if (posColor != Color.Transparent) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(posColor, CircleShape)
                        ) {
                            Text(
                                text = pos.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    } else {
                        Text(
                            text = pos.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Player name
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = entry.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Points Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(
                        text = entry.points.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = brandPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // WL Label
                Text(
                    text = "${entry.wins}-${entry.losses}",
                    modifier = Modifier.weight(0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Win percentage
                val winPct = entry.winPercentage ?: 0.0
                val formattedWinPercent = String.format(Locale.US, "%.0f%%", winPct)
                Text(
                    text = formattedWinPercent,
                    modifier = Modifier.weight(0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (winPct >= 50.0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.error
                )

                // Money Lost indicator
                val moneyRaw = entry.moneyLost ?: 0
                val format = NumberFormat.getIntegerInstance(Locale.getDefault())
                val formattedLosing = if (moneyRaw > 0) "-${format.format(moneyRaw)}đ" else "0đ"
                Text(
                    text = formattedLosing,
                    modifier = Modifier.weight(1.1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End,
                    color = if (moneyRaw > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Display dynamic form matches if available: e.g. [W, L, W, W]
            if (!entry.formStrings.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 28.dp)
                ) {
                    Text("Phong độ:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    entry.formStrings.take(5).forEach { f ->
                        val isWin = f == "W"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .background(if (isWin) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.error, CircleShape)
                        ) {
                            Text(
                                text = f,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}
