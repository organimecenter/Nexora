package com.example

import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ================= PRIVATE DATA MODELS =================

data class SettingItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val value: String,
    val choices: List<String> = listOf("Allowed", "Ask first", "Blocked")
)

// ================= CUSTOM CANVAS ICONS =================
// Bulletproof, high-fidelity native drawing of Chrome settings icons
@Composable
fun ChromeSettingIcon(type: String, tint: Color = Color(0xFF5F6368), modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        when (type) {
            "all_sites" -> {
                // Globe outline
                drawCircle(color = tint, radius = w * 0.4f, style = Stroke(width = 2f.dp.toPx()))
                drawLine(color = tint, start = Offset(cx - w * 0.4f, cy), end = Offset(cx + w * 0.4f, cy), strokeWidth = 2f.dp.toPx())
                drawLine(color = tint, start = Offset(cx, cy - h * 0.4f), end = Offset(cx, cy + h * 0.4f), strokeWidth = 2f.dp.toPx())
                // Elipses inside globe
                drawOval(color = tint, topLeft = Offset(cx - w * 0.2f, cy - h * 0.4f), size = Size(w * 0.4f, h * 0.8f), style = Stroke(width = 1.5f.dp.toPx()))
            }
            "location" -> {
                // Map Pin
                val path = Path().apply {
                    moveTo(cx, cy - h * 0.35f)
                    cubicTo(cx - w * 0.25f, cy - h * 0.35f, cx - w * 0.25f, cy + h * 0.1f, cx, cy + h * 0.4f)
                    cubicTo(cx + w * 0.25f, cy + h * 0.1f, cx + w * 0.25f, cy - h * 0.35f, cx, cy - h * 0.35f)
                    close()
                }
                drawPath(path = path, color = tint)
                drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(cx, cy - h * 0.1f))
            }
            "camera" -> {
                // Camera outline with lens
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.35f, cy - h * 0.2f),
                    size = Size(w * 0.7f, h * 0.45f),
                    cornerRadius = CornerRadius(4f.dp.toPx(), 4f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawCircle(color = tint, radius = w * 0.12f, center = Offset(cx, cy + h * 0.02f))
                drawRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.15f, cy - h * 0.3f),
                    size = Size(w * 0.3f, h * 0.12f)
                )
            }
            "microphone" -> {
                // Mic container and stand
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.15f, cy - h * 0.3f),
                    size = Size(w * 0.3f, h * 0.45f),
                    cornerRadius = CornerRadius(6f.dp.toPx(), 6f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                // Cradle and stand
                drawCircle(color = tint, radius = w * 0.22f, center = Offset(cx, cy + h * 0.06f), style = Stroke(width = 2f.dp.toPx()))
                drawLine(color = tint, start = Offset(cx, cy + h * 0.2f), end = Offset(cx, cy + h * 0.35f), strokeWidth = 2f.dp.toPx())
                drawLine(color = tint, start = Offset(cx - w * 0.2f, cy + h * 0.35f), end = Offset(cx + w * 0.2f, cy + h * 0.35f), strokeWidth = 2f.dp.toPx())
            }
            "notifications" -> {
                // Bell shape
                val path = Path().apply {
                    moveTo(cx, cy - h * 0.35f)
                    cubicTo(cx - w * 0.25f, cy - h * 0.2f, cx - w * 0.25f, cy + h * 0.15f, cx - w * 0.35f, cy + h * 0.25f)
                    lineTo(cx + w * 0.35f, cy + h * 0.25f)
                    cubicTo(cx + w * 0.25f, cy + h * 0.15f, cx + w * 0.25f, cy - h * 0.2f, cx, cy - h * 0.35f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = 2f.dp.toPx()))
                drawCircle(color = tint, radius = w * 0.1f, center = Offset(cx, cy + h * 0.3f))
            }
            "embedded_content" -> {
                // Nested boxes representing layout embedded contents
                drawRect(color = tint, topLeft = Offset(cx - w * 0.35f, cy - h * 0.3f), size = Size(w * 0.7f, h * 0.6f), style = Stroke(width = 2f.dp.toPx()))
                drawRect(color = tint, topLeft = Offset(cx - w * 0.2f, cy - h * 0.15f), size = Size(w * 0.4f, h * 0.35f))
            }
            "motion" -> {
                // Oscillating sensor arcs
                drawCircle(color = tint, radius = w * 0.1f, center = Offset(cx, cy))
                drawArc(color = tint, startAngle = -60f, sweepAngle = 120f, useCenter = false, topLeft = Offset(cx - w * 0.25f, cy - h * 0.25f), size = Size(w * 0.5f, h * 0.5f), style = Stroke(width = 2f.dp.toPx()))
                drawArc(color = tint, startAngle = 120f, sweepAngle = 120f, useCenter = false, topLeft = Offset(cx - w * 0.25f, cy - h * 0.25f), size = Size(w * 0.5f, h * 0.5f), style = Stroke(width = 2f.dp.toPx()))
            }
            "nfc" -> {
                // Stacked chip-wave card rectangle
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.2f, cy - h * 0.35f),
                    size = Size(w * 0.4f, h * 0.7f),
                    cornerRadius = CornerRadius(4f.dp.toPx(), 4f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawCircle(color = tint, radius = w * 0.08f, center = Offset(cx, cy))
            }
            "usb" -> {
                // USB Pitchfork core
                drawLine(color = tint, start = Offset(cx, cy - h * 0.3f), end = Offset(cx, cy + h * 0.35f), strokeWidth = 2f.dp.toPx())
                drawLine(color = tint, start = Offset(cx, cy - h * 0.05f), end = Offset(cx - w * 0.2f, cy - h * 0.2f), strokeWidth = 2f.dp.toPx())
                drawLine(color = tint, start = Offset(cx, cy - h * 0.05f), end = Offset(cx + w * 0.2f, cy - h * 0.2f), strokeWidth = 2f.dp.toPx())
                // Top tip arrow
                drawCircle(color = tint, radius = w * 0.06f, center = Offset(cx, cy - h * 0.32f))
                drawRect(color = tint, topLeft = Offset(cx - w * 0.24f, cy - h * 0.24f), size = Size(w * 0.08f, h * 0.08f))
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(cx + w * 0.2f, cy - h * 0.2f))
            }
            "serial" -> {
                // DB9 connector layout
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.35f, cy - h * 0.25f),
                    size = Size(w * 0.7f, h * 0.5f),
                    cornerRadius = CornerRadius(6f.dp.toPx(), 6f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                for (i in -1..1) {
                    drawCircle(color = tint, radius = 2f.dp.toPx(), center = Offset(cx + i * 8f.dp.toPx(), cy - 3f.dp.toPx()))
                    drawCircle(color = tint, radius = 2f.dp.toPx(), center = Offset(cx + i * 8f.dp.toPx(), cy + 3f.dp.toPx()))
                }
            }
            "file" -> {
                // Page outline and lines
                val path = Path().apply {
                    moveTo(cx - w * 0.25f, cy - h * 0.35f)
                    lineTo(cx + w * 0.1f, cy - h * 0.35f)
                    lineTo(cx + w * 0.25f, cy - h * 0.2f)
                    lineTo(cx + w * 0.25f, cy + h * 0.35f)
                    lineTo(cx - w * 0.25f, cy + h * 0.35f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = 2f.dp.toPx()))
                drawLine(color = tint, start = Offset(cx - w * 0.15f, cy), end = Offset(cx + w * 0.15f, cy), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = tint, start = Offset(cx - w * 0.15f, cy + h * 0.15f), end = Offset(cx + w * 0.1f, cy + h * 0.15f), strokeWidth = 1.5f.dp.toPx())
            }
            "clipboard" -> {
                // Clipboard with paper notes
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.25f, cy - h * 0.22f),
                    size = Size(w * 0.5f, h * 0.55f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawRect(color = tint, topLeft = Offset(cx - w * 0.12f, cy - h * 0.32f), size = Size(w * 0.24f, h * 0.12f))
            }
            "vr" -> {
                // VR mask frame
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.35f, cy - h * 0.2f),
                    size = Size(w * 0.7f, h * 0.4f),
                    cornerRadius = CornerRadius(8f.dp.toPx(), 8f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawPath(
                    path = Path().apply {
                        moveTo(cx - w * 0.1f, cy + h * 0.2f)
                        quadraticTo(cx, cy + h * 0.05f, cx + w * 0.1f, cy + h * 0.2f)
                    },
                    color = tint,
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(cx - w * 0.18f, cy))
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(cx + w * 0.18f, cy))
            }
            "ar" -> {
                // Coordinate bracket frame
                drawPath(
                    path = Path().apply {
                        moveTo(cx - w * 0.2f, cy - h * 0.3f)
                        lineTo(cx - w * 0.35f, cy - h * 0.3f)
                        lineTo(cx - w * 0.35f, cy - h * 0.15f)
                        
                        moveTo(cx + w * 0.2f, cy - h * 0.3f)
                        lineTo(cx + w * 0.35f, cy - h * 0.3f)
                        lineTo(cx + w * 0.35f, cy - h * 0.15f)
                        
                        moveTo(cx - w * 0.2f, cy + h * 0.3f)
                        lineTo(cx - w * 0.35f, cy + h * 0.3f)
                        lineTo(cx - w * 0.35f, cy + h * 0.15f)
                        
                        moveTo(cx + w * 0.2f, cy + h * 0.3f)
                        lineTo(cx + w * 0.35f, cy + h * 0.3f)
                        lineTo(cx + w * 0.35f, cy + h * 0.15f)
                    },
                    color = tint,
                    style = Stroke(width = 2f.dp.toPx())
                )
                // Cube outline inside
                drawRect(color = tint, topLeft = Offset(cx - w * 0.12f, cy - h * 0.12f), size = Size(w * 0.24f, h * 0.24f), style = Stroke(width = 1.5f.dp.toPx()))
            }
            "device" -> {
                // Smartphone outline
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.18f, cy - h * 0.32f),
                    size = Size(w * 0.36f, h * 0.64f),
                    cornerRadius = CornerRadius(4f.dp.toPx(), 4f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawLine(color = tint, start = Offset(cx - w * 0.05f, cy - h * 0.26f), end = Offset(cx + w * 0.05f, cy - h * 0.26f), strokeWidth = 2f.dp.toPx())
                drawCircle(color = tint, radius = w * 0.03f, center = Offset(cx, cy + h * 0.24f))
            }
            "network" -> {
                // Wi-Fi signal waves
                drawArc(color = tint, startAngle = -135f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - w * 0.35f, cy - h * 0.35f), size = Size(w * 0.7f, h * 0.7f), style = Stroke(width = 2f.dp.toPx()))
                drawArc(color = tint, startAngle = -135f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - w * 0.2f, cy - h * 0.2f), size = Size(w * 0.4f, h * 0.4f), style = Stroke(width = 2f.dp.toPx()))
                drawCircle(color = tint, radius = w * 0.06f, center = Offset(cx, cy + h * 0.18f))
            }
            "apps" -> {
                // 3x3 Dots grid
                for (i in -1..1) {
                    for (j in -1..1) {
                        drawCircle(color = tint, radius = 2.5f.dp.toPx(), center = Offset(cx + i * 8f.dp.toPx(), cy + j * 8f.dp.toPx()))
                    }
                }
            }
            "cookie" -> {
                // Cookie outline, chips and bite
                val path = Path().apply {
                    moveTo(cx + w * 0.25f, cy - h * 0.25f)
                    cubicTo(cx + w * 0.2f, cy - h * 0.1f, cx + w * 0.35f, cy, cx + w * 0.2f, cy + h * 0.15f)
                    cubicTo(cx + w * 0.15f, cy + h * 0.35f, cx - w * 0.2f, cy + h * 0.35f, cx - w * 0.35f, cy)
                    cubicTo(cx - w * 0.35f, cy - h * 0.2f, cx - w * 0.15f, cy - h * 0.35f, cx + w * 0.15f, cy - h * 0.35f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = 2f.dp.toPx()))
                // small chocolate chip spots
                drawCircle(color = tint, radius = 2f.dp.toPx(), center = Offset(cx - w * 0.15f, cy - h * 0.1f))
                drawCircle(color = tint, radius = 1.5f.dp.toPx(), center = Offset(cx + w * 0.02f, cy + h * 0.15f))
                drawCircle(color = tint, radius = 2.2f.dp.toPx(), center = Offset(cx - w * 0.08f, cy + h * 0.08f))
                drawCircle(color = tint, radius = 1.8f.dp.toPx(), center = Offset(cx + w * 0.05f, cy - h * 0.18f))
            }
            "javascript" -> {
                // `< \>` Tags representing coding engine
                val path = Path().apply {
                    // `<`
                    moveTo(cx - 8f.dp.toPx(), cy - 7f.dp.toPx())
                    lineTo(cx - 18f.dp.toPx(), cy)
                    lineTo(cx - 8f.dp.toPx(), cy + 7f.dp.toPx())
                    // `>`
                    moveTo(cx + 8f.dp.toPx(), cy - 7f.dp.toPx())
                    lineTo(cx + 18f.dp.toPx(), cy)
                    lineTo(cx + 8f.dp.toPx(), cy + 7f.dp.toPx())
                    // `/` slash
                    moveTo(cx + 3f.dp.toPx(), cy - 10f.dp.toPx())
                    lineTo(cx - 3f.dp.toPx(), cy + 10f.dp.toPx())
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = 2f.dp.toPx())
                )
            }
            "popups" -> {
                // Secondary popout screens
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.3f, cy - h * 0.12f),
                    size = Size(w * 0.45f, h * 0.42f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawRect(color = Color.White, topLeft = Offset(cx - w * 0.1f, cy - h * 0.32f), size = Size(w * 0.45f, h * 0.42f))
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.1f, cy - h * 0.32f),
                    size = Size(w * 0.45f, h * 0.42f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
            }
            "sound" -> {
                // Speaker megaphone with soundwave
                val path = Path().apply {
                    moveTo(cx - w * 0.3f, cy - h * 0.12f)
                    lineTo(cx - w * 0.15f, cy - h * 0.12f)
                    lineTo(cx + w * 0.05f, cy - h * 0.28f)
                    lineTo(cx + w * 0.05f, cy + h * 0.28f)
                    lineTo(cx - w * 0.15f, cy + h * 0.12f)
                    lineTo(cx - w * 0.3f, cy + h * 0.12f)
                    close()
                }
                drawPath(path = path, color = tint)
                drawArc(color = tint, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - w * 0.15f, cy - h * 0.25f), size = Size(w * 0.4f, h * 0.5f), style = Stroke(width = 2f.dp.toPx()))
            }
            "ads" -> {
                // Prohibited square element layout
                drawRect(color = tint, topLeft = Offset(cx - w * 0.35f, cy - h * 0.3f), size = Size(w * 0.7f, h * 0.6f), style = Stroke(width = 2f.dp.toPx()))
                drawLine(color = tint, start = Offset(cx - w * 0.35f, cy - h * 0.3f), end = Offset(cx + w * 0.35f, cy + h * 0.3f), strokeWidth = 2f.dp.toPx())
            }
            "protected" -> {
                // Shield outline
                val path = Path().apply {
                    moveTo(cx, cy - h * 0.35f)
                    lineTo(cx + w * 0.28f, cy - h * 0.25f)
                    cubicTo(cx + w * 0.28f, cy, cx + w * 0.15f, cy + h * 0.25f, cx, cy + h * 0.38f)
                    cubicTo(cx - w * 0.15f, cy + h * 0.25f, cx - w * 0.28f, cy, cx - w * 0.28f, cy - h * 0.25f)
                    close()
                }
                drawPath(path = path, color = tint, style = Stroke(width = 2f.dp.toPx()))
            }
            "signin" -> {
                // Person contact lock entry
                drawCircle(color = tint, radius = w * 0.12f, center = Offset(cx, cy - h * 0.12f), style = Stroke(width = 2f.dp.toPx()))
                drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(cx - w * 0.25f, cy), size = Size(w * 0.5f, h * 0.3f), style = Stroke(width = 2f.dp.toPx()))
            }
            "autoverify" -> {
                // Circular seal badge
                drawCircle(color = tint, radius = w * 0.3f, style = Stroke(width = 2f.dp.toPx()))
                // inner checkmark
                val path = Path().apply {
                    moveTo(cx - w * 0.12f, cy)
                    lineTo(cx - w * 0.02f, cy + h * 0.1f)
                    lineTo(cx + w * 0.12f, cy - h * 0.1f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = 2f.dp.toPx())
                )
            }
            "data" -> {
                // Database server stack
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.32f, cy - h * 0.32f),
                    size = Size(w * 0.64f, h * 0.2f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.32f, cy - h * 0.05f),
                    size = Size(w * 0.64f, h * 0.2f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.32f, cy + h * 0.22f),
                    size = Size(w * 0.64f, h * 0.2f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
            }
            "desktop" -> {
                // Monitor framework
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.35f, cy - h * 0.28f),
                    size = Size(w * 0.7f, h * 0.45f),
                    cornerRadius = CornerRadius(3f.dp.toPx(), 3f.dp.toPx()),
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawLine(color = tint, start = Offset(cx, cy + h * 0.17f), end = Offset(cx, cy + h * 0.32f), strokeWidth = 3f.dp.toPx())
                drawLine(color = tint, start = Offset(cx - w * 0.18f, cy + h * 0.32f), end = Offset(cx + w * 0.18f, cy + h * 0.32f), strokeWidth = 3f.dp.toPx())
            }
            "sync" -> {
                // Clockwise sync vectors
                drawCircle(color = tint, radius = w * 0.22f, style = Stroke(width = 2f.dp.toPx()))
                drawRect(color = Color.White, topLeft = Offset(cx - w * 0.3f, cy - h * 0.08f), size = Size(w * 0.18f, h * 0.16f))
                drawRect(color = Color.White, topLeft = Offset(cx + w * 0.12f, cy - h * 0.08f), size = Size(w * 0.18f, h * 0.16f))
                
                // Small arrow head overlays
                val arrowRight = Path().apply {
                    moveTo(cx + w * 0.15f, cy - h * 0.15f)
                    lineTo(cx + w * 0.3f, cy)
                    lineTo(cx + w * 0.02f, cy)
                    close()
                }
                drawPath(path = arrowRight, color = tint)
                val arrowLeft = Path().apply {
                    moveTo(cx - w * 0.15f, cy + h * 0.15f)
                    lineTo(cx - w * 0.3f, cy)
                    lineTo(cx - w * 0.02f, cy)
                    close()
                }
                drawPath(path = arrowLeft, color = tint)
            }
            "download" -> {
                // Download symbol
                drawLine(color = tint, start = Offset(cx, cy - h * 0.3f), end = Offset(cx, cy + h * 0.18f), strokeWidth = 2f.dp.toPx())
                val path = Path().apply {
                    moveTo(cx - w * 0.15f, cy + h * 0.03f)
                    lineTo(cx, cy + h * 0.2f)
                    lineTo(cx + w * 0.15f, cy + h * 0.03f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = 2f.dp.toPx())
                )
                drawLine(color = tint, start = Offset(cx - w * 0.28f, cy + h * 0.3f), end = Offset(cx + w * 0.28f, cy + h * 0.3f), strokeWidth = 2f.dp.toPx())
            }
            "optimization" -> {
                // Lightning speed bolt
                val path = Path().apply {
                    moveTo(cx + w * 0.1f, cy - h * 0.35f)
                    lineTo(cx - w * 0.22f, cy + h * 0.05f)
                    lineTo(cx - w * 0.02f, cy + h * 0.05f)
                    lineTo(cx - w * 0.1f, cy + h * 0.35f)
                    lineTo(cx + w * 0.22f, cy - h * 0.05f)
                    lineTo(cx + w * 0.02f, cy - h * 0.05f)
                    close()
                }
                drawPath(path = path, color = tint)
            }
            "zoom" -> {
                // Magnifier glass
                drawCircle(color = tint, radius = w * 0.15f, center = Offset(cx - w * 0.08f, cy - h * 0.08f), style = Stroke(width = 2f.dp.toPx()))
                drawLine(color = tint, start = Offset(cx + w * 0.02f, cy + h * 0.02f), end = Offset(cx + w * 0.25f, cy + h * 0.25f), strokeWidth = 3f.dp.toPx())
            }
            else -> {
                // Fallback circular target marker
                drawCircle(color = tint, radius = w * 0.25f, style = Stroke(width = 2f.dp.toPx()))
                drawCircle(color = tint, radius = w * 0.08f)
            }
        }
    }
}

// ================= REUSABLE CONFIG WIDGETS =================

@Composable
fun SettingsSection(
    title: String,
    isDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1E293B), // Use beautiful accent blue or dark charcoal
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String? = null,
    iconType: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    isDarkMode: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChromeSettingIcon(
                type = iconType,
                tint = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568),
                modifier = Modifier
                    .size(24.dp)
                    .testTag("setting_icon_$iconType")
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.8.dp,
                color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)
            )
        }
    }
}

// ================= PRIMARY SITE SETTINGS PAGE =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSettingsScreen(
    permissionsList: List<SettingItem>,
    contentList: List<SettingItem>,
    automaticRemovePermissions: Boolean,
    onPermissionChanged: (SettingItem, String) -> Unit,
    onAutomaticRemoveToggled: (Boolean) -> Unit,
    onNavigateToAllSites: () -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean = false
) {
    var activeEditingItem by remember { mutableStateOf<SettingItem?>(null) }
    
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Site settings", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back to browser",
                            tint = if (isDarkMode) Color.White else Color(0xFF4A5568)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Chrome-based browser Site settings configuration panel", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "About Site Settings",
                            tint = if (isDarkMode) Color.White else Color(0xFF4A5568)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF1F3F4)
                )
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF1F3F4)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = 0.dp
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            // "All sites" Card Link Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAllSites() }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChromeSettingIcon(
                        type = "all_sites",
                        tint = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "All sites",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Stored sites list",
                        tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Permissions Section
            SettingsSection(title = "Permissions", isDarkMode = isDarkMode) {
                permissionsList.forEachIndexed { index, item ->
                    val systemPermissionText = when (item.id) {
                        "location" -> {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) " • App Permission: Granted" else " • App Permission: Denied"
                        }
                        "camera" -> {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) " • App Permission: Granted" else " • App Permission: Denied"
                        }
                        "microphone" -> {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) " • App Permission: Granted" else " • App Permission: Denied"
                        }
                        "notifications" -> {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                val granted = ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
                                if (granted) " • App Permission: Granted" else " • App Permission: Denied"
                            } else {
                                " • App Permission: Granted"
                            }
                        }
                        else -> ""
                    }
                    SettingsTile(
                        title = item.title,
                        subtitle = "${item.value}$systemPermissionText",
                        iconType = item.type,
                        showDivider = index < permissionsList.lastIndex,
                        isDarkMode = isDarkMode,
                        onClick = { activeEditingItem = item }
                    )
                }
            }

            // Content Section
            SettingsSection(title = "Content", isDarkMode = isDarkMode) {
                contentList.forEachIndexed { index, item ->
                    SettingsTile(
                        title = item.title,
                        subtitle = item.value,
                        iconType = item.type,
                        showDivider = index < contentList.lastIndex,
                        isDarkMode = isDarkMode,
                        onClick = { activeEditingItem = item }
                    )
                }
            }

            // Automatic Remove Permissions Card (M3 Switch row)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatically remove permissions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To protect your data, let Chrome remove permissions from sites you haven't visited recently",
                            fontSize = 12.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = automaticRemovePermissions,
                        onCheckedChange = { onAutomaticRemoveToggled(it) },
                        modifier = Modifier.testTag("auto_remove_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8) // Chrome Blue accent
                        )
                    )
                }
            }
        }
    }

    // Interactive Option Selection Dialog for Setting Items
    if (activeEditingItem != null) {
        val editingItem = activeEditingItem!!
        Dialog(onDismissRequest = { activeEditingItem = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = editingItem.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = editingItem.description,
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    editingItem.choices.forEach { choice ->
                        val isSelected = editingItem.value == choice
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onPermissionChanged(editingItem, choice)
                                    activeEditingItem = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onPermissionChanged(editingItem, choice)
                                    activeEditingItem = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = choice,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { activeEditingItem = null }
                        ) {
                            Text("Cancel", color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                        }
                    }
                }
            }
        }
    }
}

// ================= ALL SITES PAGE =================

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSitesScreen(
    siteDataManager: SiteDataManager,
    onBack: () -> Unit,
    isDarkMode: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val sitesList by siteDataManager.allSitesFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var currentSortOrder by remember { mutableStateOf("most_storage") } 

    var selectedSiteForDetails by remember { mutableStateOf<SiteStorageMetrics?>(null) }

    val liveDetailsTarget = selectedSiteForDetails?.let { detail ->
        sitesList.find { it.domain == detail.domain } ?: detail
    }

    val filteredSites = remember(sitesList, searchQuery, currentSortOrder) {
        val list = if (searchQuery.isBlank()) {
            sitesList
        } else {
            sitesList.filter { 
                it.domain.contains(searchQuery.trim(), ignoreCase = true) ||
                it.title.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
        
        when (currentSortOrder) {
            "most_storage" -> list.sortedByDescending { it.totalSize }
            "least_storage" -> list.sortedBy { it.totalSize }
            "most_recent" -> list.sortedByDescending { it.lastAccessed }
            else -> list.sortedBy { it.domain.lowercase() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = liveDetailsTarget != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (liveDetailsTarget != null) {
                SiteDetailsSubScreen(
                    site = liveDetailsTarget,
                    isDarkMode = isDarkMode,
                    onBack = { selectedSiteForDetails = null },
                    onClearAll = {
                        coroutineScope.launch {
                            siteDataManager.clearSiteData(liveDetailsTarget.domain)
                            selectedSiteForDetails = null
                        }
                    },
                    onClearCookies = {
                        coroutineScope.launch {
                            siteDataManager.clearSiteCookiesOnly(liveDetailsTarget.domain)
                        }
                    },
                    onResetPermissions = {
                        coroutineScope.launch {
                            siteDataManager.resetSitePermissions(liveDetailsTarget.domain)
                        }
                    }
                )
            }
        }

        if (liveDetailsTarget == null) {
            Scaffold(
                topBar = {
                    if (isSearchActive) {
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search by domain or title...", color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8), fontSize = 15.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("site_search_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkMode) Color.White else Color(0xFF1E293B)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search", tint = if (isDarkMode) Color.White else Color(0xFF4A5568))
                                }
                            },
                            actions = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear query", tint = if (isDarkMode) Color.White else Color(0xFF4A5568))
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White)
                        )
                    } else {
                        TopAppBar(
                            title = { 
                                Text(
                                    "All sites", 
                                    fontSize = 20.sp, 
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                ) 
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("allsites_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                        contentDescription = "Back to settings",
                                        tint = if (isDarkMode) Color.White else Color(0xFF4A5568)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search sites", tint = if (isDarkMode) Color.White else Color(0xFF4A5568))
                                }
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Sort choices", tint = if (isDarkMode) Color.White else Color(0xFF4A5568))
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Sort by most storage") },
                                            leadingIcon = { if (currentSortOrder == "most_storage") Icon(Icons.Default.Check, contentDescription = null) },
                                            onClick = {
                                                currentSortOrder = "most_storage"
                                                showSortMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort by least storage") },
                                            leadingIcon = { if (currentSortOrder == "least_storage") Icon(Icons.Default.Check, contentDescription = null) },
                                            onClick = {
                                                currentSortOrder = "least_storage"
                                                showSortMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort by most recently used") },
                                            leadingIcon = { if (currentSortOrder == "most_recent") Icon(Icons.Default.Check, contentDescription = null) },
                                            onClick = {
                                                currentSortOrder = "most_recent"
                                                showSortMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort by site name (A-Z)") },
                                            leadingIcon = { if (currentSortOrder == "alphabetical") Icon(Icons.Default.Check, contentDescription = null) },
                                            onClick = {
                                                currentSortOrder = "alphabetical"
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF1F3F4)
                            )
                        )
                    }
                },
                containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF1F3F4)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = 0.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clickable { showDeleteAllConfirmDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF8AB4F8).copy(alpha = 0.45f) else Color(0xFF1A73E8).copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Delete browsing data...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredSites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ChromeSettingIcon(
                                    type = "all_sites",
                                    tint = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No sites match search queries" else "No stored site data",
                                    fontSize = 14.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(filteredSites, key = { it.domain }) { site ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedSiteForDetails = site }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SiteLogoView(
                                        url = "",
                                        domain = site.domain,
                                        sizeDp = 38.dp,
                                        isDarkMode = isDarkMode,
                                        primaryColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(14.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.domain,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                            lineHeight = 18.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        val sizeStr = formatBytes(site.totalSize)
                                        val cookiesStr = if (site.cookieCount > 0) {
                                            "• ${site.cookieCount} cookie" + (if (site.cookieCount > 1) "s" else "")
                                        } else ""
                                        
                                        Text(
                                            text = "$sizeStr $cookiesStr".trim(),
                                            fontSize = 12.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Details",
                                        tint = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(start = 52.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("Clear all site data?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
            text = { 
                Text(
                    "This will delete ALL local cookies, offline client-side storage, local databases, and cached data for all listed websites. You will be signed out of those websites.",
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            siteDataManager.clearAllSitesData()
                            showDeleteAllConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFFE57373) else Color(0xFFD32F2F))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllConfirmDialog = false }
                ) {
                    Text("Cancel", color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailsSubScreen(
    site: SiteStorageMetrics,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onClearCookies: () -> Unit,
    onResetPermissions: () -> Unit
) {
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showClearCookiesConfirm by remember { mutableStateOf(false) }
    var showResetPermissionsConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Site settings", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back to All Sites",
                            tint = if (isDarkMode) Color.White else Color(0xFF4A5568)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF1F3F4))
            )
        },
        containerColor = if (isDarkMode) Color(0xFF121214) else Color(0xFFF1F3F4)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SiteLogoView(
                    url = "",
                    domain = site.domain,
                    sizeDp = 48.dp,
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = site.domain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    if (site.title.isNotEmpty() && site.title != site.domain) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = site.title,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "Usage",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChromeSettingIcon(type = "data", tint = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total storage used", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                            Text(formatBytes(site.totalSize), fontSize = 12.sp, color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0).copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 8.dp))

                    BreakdownItem(
                        iconType = "cookie",
                        label = "Cookies",
                        subtitle = "${site.cookieCount} cookies • ${formatBytes(site.cookieSize)}",
                        isDarkMode = isDarkMode
                    )

                    BreakdownItem(
                        iconType = "all_sites",
                        label = "LocalStorage",
                        subtitle = "${site.localStorageCount} entries • ${formatBytes(site.localStorageSize)}",
                        isDarkMode = isDarkMode
                    )

                    BreakdownItem(
                        iconType = "optimization",
                        label = "SessionStorage",
                        subtitle = "${site.sessionStorageCount} entries • ${formatBytes(site.sessionStorageSize)}",
                        isDarkMode = isDarkMode
                    )

                    BreakdownItem(
                        iconType = "data",
                        label = "IndexedDB",
                        subtitle = "${site.indexedDbCount} databases • ${formatBytes(site.indexedDbSize)}",
                        isDarkMode = isDarkMode
                    )

                    BreakdownItem(
                        iconType = "download",
                        label = "Cache Storage",
                        subtitle = "${site.cacheStorageCount} persistent caches • ${formatBytes(site.cacheStorageSize)}",
                        isDarkMode = isDarkMode
                    )

                    BreakdownItem(
                        iconType = "sync",
                        label = "Service Workers",
                        subtitle = if (site.serviceWorkerCount > 0) "${site.serviceWorkerCount} script workers registered" else "None registered",
                        isDarkMode = isDarkMode
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Permissions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionRow(iconType = "location", name = "Location access", value = site.locationPermission, isDarkMode = isDarkMode)
                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    PermissionRow(iconType = "camera", name = "Camera access", value = site.cameraPermission, isDarkMode = isDarkMode)
                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    PermissionRow(iconType = "microphone", name = "Microphone access", value = site.micPermission, isDarkMode = isDarkMode)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF5C2D2D) else Color(0xFFFFCDD2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ButtonRowAction(
                        label = "Clear cookies",
                        color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1E293B),
                        onClick = { showClearCookiesConfirm = true }
                    )
                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0))
                    
                    ButtonRowAction(
                        label = "Reset permissions",
                        color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1E293B),
                        onClick = { showResetPermissionsConfirm = true }
                    )
                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0))

                    ButtonRowAction(
                        label = "Clear & reset all data",
                        color = if (isDarkMode) Color(0xFFEF9A9A) else Color(0xFFD32F2F), 
                        onClick = { showClearAllConfirm = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear all site data?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
            text = { Text("Wipe all local cookie, local storage, DB, cached APIs, and permission overrides for ${site.domain}? This sign-out action cannot be undone.", fontSize = 14.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFFE57373) else Color(0xFFD32F2F))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showClearCookiesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCookiesConfirm = false },
            title = { Text("Clear cookies?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
            text = { Text("This will sign you out of ${site.domain} but will keep offline storage assets intact.", fontSize = 14.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCookies()
                        showClearCookiesConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                ) {
                    Text("Clear Cookies", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesConfirm = false }) {
                    Text("Cancel", color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showResetPermissionsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetPermissionsConfirm = false },
            title = { Text("Reset permissions?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
            text = { Text("Reset location, camera, and microphone prompt responses back to 'Allowed' or original settings?", fontSize = 14.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetPermissions()
                        showResetPermissionsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPermissionsConfirm = false }) {
                    Text("Cancel", color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun BreakdownItem(
    iconType: String,
    label: String,
    subtitle: String,
    isDarkMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChromeSettingIcon(type = iconType, tint = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF64748B), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isDarkMode) Color.White else Color(0xFF1E293B))
            Text(subtitle, fontSize = 11.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
        }
    }
}

@Composable
fun PermissionRow(
    iconType: String,
    name: String,
    value: String,
    isDarkMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChromeSettingIcon(type = iconType, tint = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF4A5568), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isDarkMode) Color.White else Color(0xFF1E293B))
            Text(value, fontSize = 11.sp, color = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1A73E8))
        }
    }
}

@Composable
fun ButtonRowAction(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}

