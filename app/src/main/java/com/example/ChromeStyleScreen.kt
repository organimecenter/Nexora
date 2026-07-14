package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.widget.Toast

// ==========================================
// 1. CHROME STYLE HISTORY SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<BrowserHistoryItem>,
    onOpenUrl: (String) -> Unit,
    onDeleteHistoryItem: (BrowserHistoryItem) -> Unit,
    onClearAllHistory: () -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredHistory = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = onClearAllHistory) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search browsing history") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    focusedContainerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            )

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Your history is empty" else "No matches found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredHistory.reversed(), key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUrl(item.url) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SiteLogoView(
                                    url = item.url,
                                    domain = "",
                                    sizeDp = 40.dp,
                                    isDarkMode = isDarkMode,
                                    primaryColor = primaryColor
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { "Untitled" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.url,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }

                                IconButton(onClick = { onDeleteHistoryItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete from history",
                                        tint = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. DELETE BROWSING DATA SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBrowsingDataScreen(
    onDeleteData: (
        timeRangeHour: String,
        history: Boolean,
        cookies: Boolean,
        cache: Boolean,
        sites: Boolean,
        downloads: Boolean
    ) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    var timeRange by remember { mutableStateOf("All time") }
    var clearHistory by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearSites by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }

    var expandedDropdown by remember { mutableStateOf(false) }
    val timeOptions = listOf("Last hour", "Last 24 hours", "Last 7 days", "Last 4 weeks", "All time")

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete browsing data", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time range",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(timeRange, color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = primaryColor)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            timeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        timeRange = option
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select what to clear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Checking rows
                    CheckboxItem(
                        title = "Browsing history",
                        desc = "Clears history from active sessions",
                        checked = clearHistory,
                        onCheckedChange = { clearHistory = it },
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                    Divider()
                    CheckboxItem(
                        title = "Cookies and site data",
                        desc = "Signs you out of most sites",
                        checked = clearCookies,
                        onCheckedChange = { clearCookies = it },
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                    Divider()
                    CheckboxItem(
                        title = "Cached images and files",
                        desc = "Frees up web rendering cache space",
                        checked = clearCache,
                        onCheckedChange = { clearCache = it },
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                    Divider()
                    CheckboxItem(
                        title = "Saved site permissions",
                        desc = "Site level lists and parameters",
                        checked = clearSites,
                        onCheckedChange = { clearSites = it },
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                    Divider()
                    CheckboxItem(
                        title = "Downloads history list",
                        desc = "Clears list of current downloads",
                        checked = clearDownloads,
                        onCheckedChange = { clearDownloads = it },
                        isDarkMode = isDarkMode,
                        primaryColor = primaryColor
                    )
                }
            }

            Button(
                onClick = {
                    onDeleteData(timeRange, clearHistory, clearCookies, clearCache, clearSites, clearDownloads)
                    Toast.makeText(context, "Selected browsing data deleted", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text("Delete data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CheckboxItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )
            Text(
                desc,
                fontSize = 11.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = primaryColor
            )
        )
    }
}


// ==========================================
// 3. DOWNLOAD MANAGER SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    downloadsList: List<DownloadItem>,
    onPauseResume: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onSimulateDownload: () -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSimulateDownload) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = "Simulate New Download", tint = primaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Button(
                onClick = onSimulateDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate PDF/Video Download demo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (downloadsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No downloads yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(downloadsList.reversed(), key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = when (item.mimeType) {
                                            "video/mp4" -> Icons.Default.VideoFile
                                            "image/png" -> Icons.Default.Image
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = item.totalSize + " • " + item.status,
                                            fontSize = 11.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    Row {
                                        if (item.status != "Completed") {
                                            IconButton(onClick = { onPauseResume(item) }) {
                                                Icon(
                                                    imageVector = if (item.status == "Paused") Icons.Default.PlayArrow else Icons.Default.Pause,
                                                    contentDescription = "Pause/Resume",
                                                    tint = primaryColor
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onDelete(item) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }

                                if (item.status != "Completed") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = primaryColor,
                                        trackColor = primaryColor.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. RECENT TABS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentTabsScreen(
    recentlyClosed: List<ClosedTab>,
    onRestoreTab: (ClosedTab) -> Unit,
    onClearClosedHistory: () -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently closed tabs", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recentlyClosed.isNotEmpty()) {
                        IconButton(onClick = onClearClosedHistory) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (recentlyClosed.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Tab,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No recently closed tabs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(recentlyClosed.reversed(), key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRestoreTab(item) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tab,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { "Untitled" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = item.url,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }

                                Button(
                                    onClick = { onRestoreTab(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.15f), contentColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Reopen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. CUSTOMISE NEW TAB PAGE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomiseNewTabScreen(
    currentWallpaper: String,
    onWallpaperSelected: (String) -> Unit,
    shortcutsEnabled: Boolean,
    onShortcutsToggle: (Boolean) -> Unit,
    newsFeedEnabled: Boolean,
    onNewsFeedToggle: (Boolean) -> Unit,
    searchEngine: String,
    onSearchEngineSelected: (String) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    val wallpapers = listOf(
        "Default" to "",
        "Sunset Glow" to "sunset",
        "Cosmic Cloud" to "cosmic",
        "Teal Aurora" to "teal",
        "Deep Abyss" to "abyss"
    )

    val engines = listOf("Google", "DuckDuckGo", "Bing", "Yahoo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customise New Tab", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Wallpaper & Canvas Theme",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )

            // Horizontal wallpaper selections
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                wallpapers.forEach { (name, tag) ->
                    val isSelected = currentWallpaper == tag
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .height(95.dp)
                            .clickable { onWallpaperSelected(tag) }
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) primaryColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (tag) {
                                "sunset" -> Color(0xFFFF8C00)
                                "cosmic" -> Color(0xFF6B46C1)
                                "teal" -> Color(0xFF00ADB5)
                                "abyss" -> Color(0xFF0D0E15)
                                else -> if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Options",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShortcutsToggle(!shortcutsEnabled) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Shortcuts row", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Fast entries for popular sites", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = shortcutsEnabled,
                            onCheckedChange = onShortcutsToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNewsFeedToggle(!newsFeedEnabled) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show recommendations feed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Latest news cards on home display", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = newsFeedEnabled,
                            onCheckedChange = onNewsFeedToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Default Search Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    engines.forEach { engine ->
                        val selected = searchEngine.equals(engine, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchEngineSelected(engine) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                engine,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (selected) primaryColor else (if (isDarkMode) Color.White else Color(0xFF1E293B))
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { onSearchEngineSelected(engine) },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                        }
                        if (engine != engines.last()) Divider()
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. HELP & FEEDBACK SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFeedbackScreen(
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    val faqs = listOf(
        "Is this browser secure?" to "Yes, this browser features a Secure Media Extraction Shield that blocks malicious and background script domains automatically.",
        "How do I use Incognito Mode?" to "Tap the 3-dot menu and select 'New Incognito tab'. Private mode is marked with dark colors and does not capture browsing history, cookies, or tracking parameters.",
        "How do I clear cached site files?" to "Tap the 3-dot menu, select 'Delete browsing data', verify that cookies and cache options are ticked, and click 'Delete Data'.",
        "What is tab grouping?" to "Tab grouping allows adding multiple tabs together in a single workspace. You can choose to add tab to custom color codes to organize your bookmarks."
    )

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & feedback", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Frequently Asked Questions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )

            // FAQs cards
            faqs.forEachIndexed { idx, (q, a) ->
                val expanded = expandedFaqIndex == idx
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { expandedFaqIndex = if (expanded) null else idx }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                q,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                modifier = Modifier.weight(1f),
                                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    a,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Feedback and Support",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Contact Support via peaks@gmail.com", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Contact customer support", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider()

                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Issue reports filed securely", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Report a browsing bug", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Browser Version label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Chrome Style Secure Web Browser • Version 1.4.2",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// ==========================================
// 7. BROWSER THEME & SETTINGS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSettingsScreen(
    currentTheme: String, // "Light", "Dark", "System"
    onThemeChanged: (String) -> Unit,
    searchEngine: String,
    onSearchEngineSelected: (String) -> Unit,
    isPanelMinimized: Boolean,
    onPanelMinimizeToggle: (Boolean) -> Unit,
    extractionEnabled: Boolean,
    onExtractionEnabledToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Theme Mode Selection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    listOf("Light", "Dark", "System").forEach { theme ->
                        val selected = if (theme == "Light") !isDarkMode else if (theme == "Dark") isDarkMode else false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (theme == "Light") onThemeChanged("Light") else onThemeChanged("Dark")
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(theme, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            RadioButton(
                                selected = selected,
                                onClick = { if (theme == "Light") onThemeChanged("Light") else onThemeChanged("Dark") },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                        }
                        if (theme != "System") Divider()
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "General Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExtractionEnabledToggle(!extractionEnabled) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Media Interception Shield", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Detections of inline videos & scripts", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = extractionEnabled,
                            onCheckedChange = onExtractionEnabledToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPanelMinimizeToggle(!isPanelMinimized) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Minimized Sidebar Panels", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Compact layouts for tablet sidebar viewports", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isPanelMinimized,
                            onCheckedChange = onPanelMinimizeToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. CHROME STYLE 3-DOT OVERFLOW POPUP MENU
// ==========================================
@Composable
fun ChromeStyleMenu(
    isDarkMode: Boolean,
    primaryColor: Color,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    onBackAction: () -> Unit,
    onForwardAction: () -> Unit,
    onToggleBookmark: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onRefreshAction: () -> Unit,
    onDismiss: () -> Unit,
    // Section Items
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onAddTabToGroup: () -> Unit,
    onOpenHistory: () -> Unit,
    onDeleteBrowsingData: () -> Unit,
    onOpenSiteSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenRecentTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onCustomiseNewTab: () -> Unit,
    onOpenHelpFeedback: () -> Unit
) {
    val menuBg = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF1F3F4)
    val cardColor = if (isDarkMode) Color(0xFF2E2E33) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .widthIn(max = 340.dp)
            .padding(top = 8.dp, end = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(menuBg)
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF3E3E45) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(32.dp)
            )
            .clickable(enabled = false) {} // block click leakage
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // === Quick Action Row (Top) ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Back
                QuickActionCircleButton(
                    icon = Icons.Default.ArrowBack,
                    enabled = canGoBack,
                    onClick = {
                        onBackAction()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                // 2. Forward
                QuickActionCircleButton(
                    icon = Icons.Default.ArrowForward,
                    enabled = canGoForward,
                    onClick = {
                        onForwardAction()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                // 3. Bookmark
                QuickActionCircleButton(
                    icon = if (isBookmarked) Icons.Default.Star else Icons.Default.StarOutline,
                    enabled = true,
                    onClick = {
                        onToggleBookmark()
                        onDismiss()
                    },
                    iconTint = if (isBookmarked) Color(0xFFFFB300) else null,
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                // 4. Downloads
                QuickActionCircleButton(
                    icon = Icons.Default.Download,
                    enabled = true,
                    onClick = {
                        onNavigateDownloads()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                // 5. Refresh
                QuickActionCircleButton(
                    icon = Icons.Default.Refresh,
                    enabled = true,
                    onClick = {
                        onRefreshAction()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = if (isDarkMode) Color(0xFF3A3A40) else Color(0xFFE2E8F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable Menu Items Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // --- SECTION 1 ---
                ChromeMenuItem(
                    icon = Icons.Default.Add,
                    text = "New tab",
                    onClick = {
                        onNewTab()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.VisibilityOff,
                    text = "New Incognito tab",
                    onClick = {
                        onNewIncognitoTab()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.GroupWork,
                    text = "Add tab to new group",
                    onClick = {
                        onAddTabToGroup()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                Divider(color = if (isDarkMode) Color(0xFF333339) else Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                // --- SECTION 2 ---
                ChromeMenuItem(
                    icon = Icons.Default.History,
                    text = "History",
                    onClick = {
                        onOpenHistory()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.DeleteForever,
                    text = "Delete browsing data",
                    onClick = {
                        onDeleteBrowsingData()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.Lock,
                    text = "Site Settings",
                    onClick = {
                        onOpenSiteSettings()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                Divider(color = if (isDarkMode) Color(0xFF333339) else Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                // --- SECTION 3 ---
                ChromeMenuItem(
                    icon = Icons.Default.Download,
                    text = "Downloads",
                    onClick = {
                        onNavigateDownloads()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.Star,
                    text = "Bookmarks",
                    onClick = {
                        onOpenBookmarks()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.Restore,
                    text = "Recent tabs",
                    onClick = {
                        onOpenRecentTabs()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )

                Divider(color = if (isDarkMode) Color(0xFF333339) else Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                // --- SECTION 4 ---
                ChromeMenuItem(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = {
                        onOpenSettings()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.Palette,
                    text = "Customise new tab page",
                    onClick = {
                        onCustomiseNewTab()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
                ChromeMenuItem(
                    icon = Icons.Default.Help,
                    text = "Help and feedback",
                    onClick = {
                        onOpenHelpFeedback()
                        onDismiss()
                    },
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor
                )
            }
        }
    }
}

@Composable
fun QuickActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    iconTint: Color? = null,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val buttonBg = if (isDarkMode) Color(0xFF2E2E33) else Color.White
    val contentAlpha = if (enabled) 1.0f else 0.35f

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(buttonBg.copy(alpha = contentAlpha))
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: (if (isDarkMode) Color.White else Color(0xFF1E293B)),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun ChromeMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color(0xFF1E293B)
        )
    }
}
