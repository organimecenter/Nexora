package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale

@Composable
fun SiteLogoView(
    url: String,
    domain: String,
    sizeDp: Dp,
    isDarkMode: Boolean,
    primaryColor: Color
) {
    val cleanUrlDomain = remember(url, domain) {
        val d = if (domain.isNotEmpty()) {
            domain
        } else {
            try {
                val uri = java.net.URI(url)
                val host = uri.host
                if (host != null) {
                    if (host.startsWith("www.")) host.substring(4) else host
                } else {
                    var u = url.replace("https://", "").replace("http://", "")
                    val idx = u.indexOf('/')
                    if (idx != -1) u = u.substring(0, idx)
                    if (u.startsWith("www.")) u.substring(4) else u
                }
            } catch (e: Exception) {
                var u = url.replace("https://", "").replace("http://", "")
                val idx = u.indexOf('/')
                if (idx != -1) u = u.substring(0, idx)
                if (u.startsWith("www.")) u.substring(4) else u
            }
        }
        if (d.startsWith("www.")) d.substring(4) else d
    }

    val faviconUrl = remember(cleanUrlDomain) {
        if (cleanUrlDomain.isEmpty()) "" else "https://www.google.com/s2/favicons?sz=128&domain=$cleanUrlDomain"
    }

    val letter = remember(cleanUrlDomain) {
        cleanUrlDomain.replaceFirst("www.", "").firstOrNull()?.toString()?.uppercase() ?: "S"
    }

    val letterColor = if (isDarkMode) Color.White else Color(0xFF4A5568)
    val fallbackBg = if (isDarkMode) primaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(fallbackBg),
        contentAlignment = Alignment.Center
    ) {
        if (faviconUrl.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(faviconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "favicon for $cleanUrlDomain",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentScale = ContentScale.Fit,
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = (sizeDp.value * 0.45).sp,
                            fontWeight = FontWeight.Bold,
                            color = letterColor
                        )
                    }
                },
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = (sizeDp.value * 0.45).sp,
                            fontWeight = FontWeight.Bold,
                            color = letterColor
                        )
                    }
                }
            )
        } else {
            Text(
                text = letter,
                fontSize = (sizeDp.value * 0.45).sp,
                fontWeight = FontWeight.Bold,
                color = letterColor
            )
        }
    }
}
