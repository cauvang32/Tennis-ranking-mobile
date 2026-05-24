package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.api.CreateSeasonRequest
import com.example.api.Player
import com.example.api.Season
import com.example.repository.TennisRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonsScreen(
    onShowLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initData by TennisRepository.initData.collectAsState()
    val isAuthenticated by TennisRepository.isAuthenticated.collectAsState()
    val currentUser by TennisRepository.currentUser.collectAsState()
    val isLoading by TennisRepository.isLoading.collectAsState()

    val scope = rememberCoroutineScope()
    var isCreateSeasonOpen by remember { mutableStateOf(false) }
    var selectedSeasonForRoster by remember { mutableStateOf<Season?>(null) }
    var seasonPendingDelete by remember { mutableStateOf<Season?>(null) }
    var seasonPendingEnd by remember { mutableStateOf<Season?>(null) }

    val brandPrimary = MaterialTheme.colorScheme.primary
    val cardStroke = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        floatingActionButton = {
            if (isAuthenticated && currentUser?.role == "admin") {
                FloatingActionButton(
                    onClick = { isCreateSeasonOpen = true },
                    containerColor = brandPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Season")
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
            // Title Header with Dynamic Sign action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = brandPrimary, modifier = Modifier.size(28.dp))
                    Text(
                        text = "Mùa giải",
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
                            .testTag("login_button_seasons_top")
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
                            .testTag("logout_button_seasons_top")
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
                        .padding(bottom = 12.dp)
                        .clickable { onShowLogin() }
                        .testTag("admin_login_banner_seasons"),
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
                            text = "Đăng nhập với tư cách Quản trị viên/Biên tập viên để tạo và kết thúc mùa giải.",
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

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = brandPrimary)
                }
            } else {
                val seasonsList = initData?.seasons ?: emptyList()
                if (seasonsList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Chưa có giải đấu nào được thêm.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(seasonsList, key = { it.id }, contentType = { "season_card" }) { season ->
                            val onEnd = remember(season.id) { { seasonPendingEnd = season } }
                            val onReactivate = remember(season.id) { { scope.launch { TennisRepository.reactivateSeason(season.id) }; Unit } }
                            val onDelete = remember(season.id) { { seasonPendingDelete = season } }
                            val onManage = remember(season.id) { { selectedSeasonForRoster = season } }

                            SeasonListItem(
                                season = season,
                                isAdmin = isAuthenticated && currentUser?.role == "admin",
                                isEditorOrAdmin = isAuthenticated && (currentUser?.role == "editor" || currentUser?.role == "admin"),
                                onEndClick = onEnd,
                                onReactivateClick = onReactivate,
                                onDeleteClick = onDelete,
                                onManageRosterClick = onManage
                            )
                        }
                    }
                }
            }
        }
    }

    // CREATE SEASON MODEL DIALOG
    if (isCreateSeasonOpen) {
        val players = initData?.players ?: emptyList()
        CreateSeasonDialog(
            playersList = players,
            onDismiss = { isCreateSeasonOpen = false },
            onConfirm = { req ->
                scope.launch {
                    val success = TennisRepository.createSeason(req)
                    if (success) {
                        isCreateSeasonOpen = false
                    }
                }
            }
        )
    }

    // ROSTER MANAGER DIALOG
    if (selectedSeasonForRoster != null) {
        val players = initData?.players ?: emptyList()
        RosterManagerDialog(
            season = selectedSeasonForRoster!!,
            allPlayers = players,
            onDismiss = { selectedSeasonForRoster = null },
            onConfirm = { listIds ->
                scope.launch {
                    val success = TennisRepository.updateSeasonPlayers(selectedSeasonForRoster!!.id, listIds)
                    if (success) {
                        selectedSeasonForRoster = null
                    }
                }
            }
        )
    }

    // CONFIRM DELETE SEASON DIALOG
    if (seasonPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { seasonPendingDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_season_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_delete_season_body, seasonPendingDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = seasonPendingDelete ?: return@Button
                        scope.launch {
                            TennisRepository.deleteSeason(target.id)
                            seasonPendingDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_delete_season))
                }
            },
            dismissButton = {
                TextButton(onClick = { seasonPendingDelete = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    // CONFIRM END SEASON DIALOG
    if (seasonPendingEnd != null) {
        AlertDialog(
            onDismissRequest = { seasonPendingEnd = null },
            title = { Text(stringResource(R.string.dialog_end_season_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_end_season_body, seasonPendingEnd?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = seasonPendingEnd ?: return@Button
                        scope.launch {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            TennisRepository.endSeason(target.id, todayStr)
                            seasonPendingEnd = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_end_season))
                }
            },
            dismissButton = {
                TextButton(onClick = { seasonPendingEnd = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
fun SeasonListItem(
    season: Season,
    isAdmin: Boolean,
    isEditorOrAdmin: Boolean,
    onEndClick: () -> Unit,
    onReactivateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onManageRosterClick: () -> Unit
) {
    val brandPrimary = MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = season.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (season.isActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (season.isActive) "ACTIVE" else "ENDED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (season.isActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!season.description.isNullOrBlank()) {
                Text(
                    text = season.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Thời gian: ${season.startDate} to ${season.endDate ?: "Present"}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                val formattedMoney = NumberFormat.getIntegerInstance()
                    .format(season.loseMoneyPerLoss ?: 20000)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tiền phạt: ${formattedMoney}đ 1 trận",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Roster button
                TextButton(
                    onClick = onManageRosterClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Danh sách", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Actions
                if (isEditorOrAdmin) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (season.isActive) {
                            OutlinedButton(
                                onClick = onEndClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = null,
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("Kết thúc", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onReactivateClick,
                                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("Bật lại", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isAdmin) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSeasonDialog(
    playersList: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (CreateSeasonRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var endDate by remember { mutableStateOf("") }
    var autoEnd by remember { mutableStateOf(false) }
    var loseMoneyPerLossStr by remember { mutableStateOf("20000") }
    var description by remember { mutableStateOf("") }

    val selectedPlayerIds = remember { mutableStateListOf<Int>() }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Giải Đấu Mới", fontWeight = FontWeight.Bold) },
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

                // Name
                item {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên Giải Đấu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Description
                item {
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả ngắn gọn") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Start Date
                item {
                    TextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Ngày bắt đầu (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // End Date
                item {
                    TextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Ngày kết thúc (Tùy chọn YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Auto end checkbox
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoEnd, onCheckedChange = { autoEnd = it })
                        Text("Tự động kết thúc khi qua ngày hạn", fontSize = 12.sp)
                    }
                }

                // Penalty per Loss
                item {
                    TextField(
                        value = loseMoneyPerLossStr,
                        onValueChange = { loseMoneyPerLossStr = it },
                        label = { Text("Mức phạt tiền (đ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Chọned players roster header
                item {
                    Text("Chọn Danh Sách VĐV Ban Đầu:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Players checkboxes
                items(playersList, key = { it.id }) { p ->
                    val isChecked = selectedPlayerIds.contains(p.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedPlayerIds.removeAll { it == p.id } else selectedPlayerIds.add(p.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (isChecked) selectedPlayerIds.removeAll { it == p.id } else selectedPlayerIds.add(p.id)
                            }
                        )
                        Text(p.name, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val moneyVal = loseMoneyPerLossStr.toIntOrNull()
                    if (name.isBlank()) {
                        validationError = "Please specify a season name."
                        return@Button
                    }
                    if (moneyVal == null) {
                        validationError = "Please specify a valid numeric penalty."
                        return@Button
                    }
                    if (autoEnd && endDate.isBlank()) {
                        validationError = "End Date is required if auto-end is enabled."
                        return@Button
                    }

                    validationError = null
                    onConfirm(
                        CreateSeasonRequest(
                            name = name,
                            startDate = startDate,
                            endDate = if (endDate.isBlank()) null else endDate,
                            autoEnd = autoEnd,
                            loseMoneyPerLoss = moneyVal,
                            playerIds = selectedPlayerIds.toList(),
                            description = if (description.isBlank()) null else description
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Khởi Tạo Giải Đấu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun RosterManagerDialog(
    season: Season,
    allPlayers: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val initData by TennisRepository.initData.collectAsState()
    val isAuthenticated by TennisRepository.isAuthenticated.collectAsState()
    val currentUser by TennisRepository.currentUser.collectAsState()
    val isAdminOrEditor = isAuthenticated && (currentUser?.role == "admin" || currentUser?.role == "editor")

    val scope = rememberCoroutineScope()
    
    // We can fetch from API or look inside initData, wait, initData contains players inside the seasons object or we can filter assigned players.
    // The endpoint is /api/seasons/:id/players. Let's make an in-memory mutable check state
    val chosenPlayerIds = remember { mutableStateListOf<Int>() }
    var assignedPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var isLoadingRoster by remember { mutableStateOf(false) }
    var isSavingRoster by remember { mutableStateOf(false) }

    val brandPrimary = MaterialTheme.colorScheme.primary

    // Query active roster from server for this season
    LaunchedEffect(season.id) {
        isLoadingRoster = true
        val assigned = TennisRepository.getSeasonPlayers(season.id)
        assignedPlayers = assigned
        chosenPlayerIds.clear()
        chosenPlayerIds.addAll(assigned.map { it.id })
        isLoadingRoster = false
    }

    val titleText = if (isAdminOrEditor) "Gán người chơi: ${season.name}" else "Danh sách người chơi: ${season.name}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            if (isLoadingRoster) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = brandPrimary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    if (isAdminOrEditor) {
                        items(allPlayers, key = { it.id }) { player ->
                            val isChecked = chosenPlayerIds.contains(player.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) chosenPlayerIds.removeAll { it == player.id } else chosenPlayerIds.add(player.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) chosenPlayerIds.removeAll { it == player.id } else chosenPlayerIds.add(player.id)
                                    }
                                )
                                Text(player.name, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    } else {
                        if (assignedPlayers.isEmpty()) {
                            item {
                                Text("Không có người chơi nào trong giải đấu này.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(assignedPlayers, key = { it.id }) { player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${player.name}", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAdminOrEditor) {
                Button(
                    onClick = {
                        if (isSavingRoster) return@Button
                        isSavingRoster = true
                        onConfirm(chosenPlayerIds.toList())
                    },
                    enabled = !isSavingRoster,
                    colors = ButtonDefaults.buttonColors(containerColor = brandPrimary)
                ) {
                    if (isSavingRoster) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Lưu DSTĐ")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = brandPrimary)
                ) {
                    Text("Đóng")
                }
            }
        },
        dismissButton = {
            if (isAdminOrEditor) {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}
