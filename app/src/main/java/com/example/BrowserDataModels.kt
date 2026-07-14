package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.UUID

data class BrowserHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ClosedTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val mimeType: String,
    val totalSize: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    var progress by mutableStateOf(0.0f)
    var status by mutableStateOf("Downloading") // "Downloading", "Paused", "Completed"
}
