package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Developer Architecture Requirement: Separation of Concerns (System A Listener)
 */
interface OnIframeDiscoveredListener {
    fun onIframeDiscovered(iframeUrl: String, outerHtml: String)
}

/**
 * Developer Architecture Requirement: Separation of Concerns (System B Listener)
 */
interface OnDirectVideoDiscoveredListener {
    fun onDirectVideoDiscovered(
        videoUrl: String, 
        source: String,
        method: String? = null,
        headers: Map<String, String>? = null,
        isForMainFrame: Boolean? = null
    )
}

/**
 * Production-Ready Android JavascriptInterface for System A (Iframe Extraction)
 */
class IframeExtractionInterface(private val listener: OnIframeDiscoveredListener) {
    @JavascriptInterface
    fun onIframeDetected(outerHtml: String, src: String) {
        listener.onIframeDiscovered(src, outerHtml)
    }
}

/**
 * Production-Ready Android JavascriptInterface for System B (Direct Video Extraction)
 */
class VideoExtractionInterface(private val listener: OnDirectVideoDiscoveredListener) {
    @JavascriptInterface
    fun onVideoDetected(videoUrl: String, source: String) {
        listener.onDirectVideoDiscovered(videoUrl, source, "GET", null, false)
    }
}

/**
 * Standard W3C HTML Notification Javascript Interface Bridge
 */
class NotificationBridge(private val context: Context, private val onRequestPermission: () -> Unit) {
    @JavascriptInterface
    fun requestNotificationPermission() {
        onRequestPermission()
    }

    @JavascriptInterface
    fun showNotification(title: String, optionsJson: String) {
        try {
            val json = org.json.JSONObject(optionsJson)
            val body = json.optString("body", "")

            val channelId = "web_notifications_channel"
            val channelName = "Web Notifications"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        channelName,
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications from websites visited in the browser"
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33) {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            } else {
                // Request runtime permission
                onRequestPermission()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Universal Video format sniffer helper for System B
 * Checks if the URL contains any supported video format/extension (while filtering out web resources/ads)
 */
fun isVideoFormatRequest(requestUrl: String): Boolean {
    val lowercaseUrl = requestUrl.lowercase()
    val cleanUrl = lowercaseUrl.substringBefore("?")
    
    // Check if the URL is an ad/tracker/analytics first
    if (lowercaseUrl.contains("/ad/") || 
        lowercaseUrl.contains("googleads") || 
        lowercaseUrl.contains("doubleclick") || 
        lowercaseUrl.contains("analytics") || 
        lowercaseUrl.contains("pixel")
    ) {
        return false
    }

    val extensions = listOf(
        ".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".mov", ".qt", ".flv", 
        ".f4v", ".avi", ".wmv", ".ts", ".mts", ".m2ts", ".3gp", ".3g2", 
        ".ogg", ".ogv", ".mpg", ".mpeg", ".vob", ".asf", ".rm", ".rmvb", 
        ".divx", ".m4v"
    )
    for (ext in extensions) {
        if (lowercaseUrl.contains(ext)) {
            // Exclude TypeScript source mapping or type definitions
            if (ext == ".ts" && (lowercaseUrl.contains(".ts.map") || lowercaseUrl.contains(".d.ts"))) {
                continue
            }
            // Exclude common static text, document layout web resources, and font files
            if (cleanUrl.endsWith(".html") || cleanUrl.endsWith(".htm") || 
                cleanUrl.endsWith(".php") || cleanUrl.endsWith(".js") || 
                cleanUrl.endsWith(".css") || cleanUrl.endsWith(".svg") || 
                cleanUrl.endsWith(".png") || cleanUrl.endsWith(".jpg") ||
                cleanUrl.endsWith(".jpeg") || cleanUrl.endsWith(".gif") ||
                cleanUrl.endsWith(".webp") || cleanUrl.endsWith(".woff") ||
                cleanUrl.endsWith(".woff2") || cleanUrl.endsWith(".ttf")
            ) {
                continue
            }
            return true
        }
    }
    return false
}

/**
 * Maps stream pattern or file extension to a readable name and premium category color
 */
fun getVideoFormatLabelAndColor(url: String): Pair<String, Color> {
    val lower = url.lowercase()
    return when {
        lower.startsWith("data:") -> {
            if (lower.contains("mpegurl") || lower.contains("m3u8")) {
                Pair("Decrypted Playable M3U8", Color(0xFF7C3AED)) // Purple
            } else {
                Pair("Decrypted Data Video", Color(0xFF0D9488)) // Teal
            }
        }
        lower.contains("abyss") -> Pair("Abyss Encrypted Stream", Color(0xFFBE123C)) // Rose
        lower.contains(".m3u8") || lower.contains("/m3u8") || lower.contains("/hls/") -> Pair("HLS Stream .m3u8", Color(0xFFD97706)) // Amber
        lower.contains(".mpd") || lower.contains("/mpd") || lower.contains("/dash/") -> Pair("DASH Stream .mpd", Color(0xFF2563EB)) // Blue
        lower.contains(".mp4") || lower.contains(".m4v") -> Pair("MP4 Video", Color(0xFF059669)) // Emerald
        lower.contains(".webm") -> Pair("WebM Video", Color(0xFF0D9488)) // Teal
        lower.contains(".mkv") -> Pair("MKV Video", Color(0xFF7C3AED)) // Violet
        lower.contains(".mov") || lower.contains(".qt") -> Pair("MOV Video", Color(0xFFDB2777)) // Pink
        lower.contains(".flv") || lower.contains(".f4v") -> Pair("FLV Video", Color(0xFFEA580C)) // Orange
        lower.contains(".avi") -> Pair("AVI Video", Color(0xFF4F46E5)) // Indigo
        lower.contains(".wmv") -> Pair("WMV Video", Color(0xFF0284C7)) // Sky
        lower.contains(".ts") || lower.contains(".mts") || lower.contains(".m2ts") -> Pair("MPEG-TS", Color(0xFF65A30D)) // Lime
        lower.contains(".3gp") || lower.contains(".3g2") -> Pair("3GP Mobile Video", Color(0xFF4B5563)) // Gray
        lower.contains(".ogg") || lower.contains(".ogv") -> Pair("Ogg Video", Color(0xFF0891B2)) // Cyan
        lower.contains(".mpg") || lower.contains(".mpeg") -> Pair("MPEG Video", Color(0xFFBE123C)) // Rose
        lower.contains(".ism") || lower.contains("manifest") -> Pair("Smooth Stream", Color(0xFF9333EA)) // Purple
        else -> {
            // Try to extract dynamic extension
            val lastSegment = url.substringBefore("?").substringAfterLast("/")
            if (lastSegment.contains(".")) {
                val ext = lastSegment.substringAfterLast(".").uppercase()
                if (ext.length in 2..5) {
                    Pair("$ext Video", Color(0xFF059669))
                } else {
                    Pair("Media Stream", Color(0xFF059669))
                }
            } else {
                Pair("Media Stream", Color(0xFF059669))
            }
        }
    }
}

private const val VIDEO_EXTRACTION_JAVASCRIPT = """
(function() {
    var rawFetch = window.fetch;
    window.abyssQualityMap = window.abyssQualityMap || {};
    window.abyssProactiveFetched = window.abyssProactiveFetched || {};

    function reportVideo(url, method) {
        if (!url || typeof url !== 'string' || url.trim() === '') return;
        if (url.toLowerCase().indexOf('blob:') === 0) return;
        try {
            var absoluteUrl = new URL(url, document.baseURI).href;
            if (absoluteUrl.toLowerCase().indexOf('blob:') === 0) return;
            AndroidVideoBridge.onVideoDetected(absoluteUrl, method);
        } catch(e) {
            AndroidVideoBridge.onVideoDetected(url, method);
        }
    }

    function isTextTarget(url) {
        if (!url) return false;
        var lower = url.toLowerCase();
        var binaryExtensions = ['.mp4', '.ts', '.png', '.jpg', '.jpeg', '.gif', '.webp', '.woff', '.woff2', '.ttf', '.mp3', '.aac', '.wav', '.flac', '.zip', '.rar', '.pdf'];
        for (var i = 0; i < binaryExtensions.length; i++) {
            if (lower.indexOf(binaryExtensions[i]) !== -1) {
                return false;
            }
        }
        return true;
    }

    function safeAtob(str) {
        try {
            var cleaned = str.trim();
            if (cleaned.indexOf('{') === 0) {
                try {
                    var obj = JSON.parse(cleaned);
                    for (var key in obj) {
                        if (obj.hasOwnProperty(key) && typeof obj[key] === 'string') {
                            var decoded = safeAtob(obj[key]);
                            if (decoded && decoded.indexOf('#EXTM3U') !== -1) {
                                return decoded;
                            }
                        }
                    }
                } catch(jsonErr) {}
            }
            if ((cleaned.indexOf('"') === 0 && cleaned.lastIndexOf('"') === cleaned.length - 1) ||
                (cleaned.indexOf("'") === 0 && cleaned.lastIndexOf("'") === cleaned.length - 1)) {
                cleaned = cleaned.substring(1, cleaned.length - 1);
            }
            cleaned = cleaned.replace(/\s/g, "");
            while (cleaned.length % 4 !== 0) {
                cleaned += "=";
            }
            return atob(cleaned);
        } catch(e) {
            return "";
        }
    }

    function parseMasterPlaylist(playlistText, baseUrl) {
        if (!playlistText) return [];
        var lines = playlistText.split('\n');
        var streams = [];
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            if (line.indexOf('#EXT-X-STREAM-INF:') === 0) {
                var resolution = "";
                var resMatch = line.match(/RESOLUTION=(\d+x\d+)/i);
                if (resMatch) {
                    resolution = resMatch[1];
                } else {
                    var bwMatch = line.match(/BANDWIDTH=(\d+)/i);
                    if (bwMatch) {
                        var bw = parseInt(bwMatch[1]);
                        if (bw > 2500000) resolution = "1080p approx";
                        else if (bw > 1200000) resolution = "720p approx";
                        else if (bw > 600000) resolution = "480p approx";
                        else resolution = "360p approx";
                    }
                }
                for (var j = i + 1; j < lines.length; j++) {
                    var nextLine = lines[j].trim();
                    if (!nextLine) continue;
                    if (nextLine.indexOf('#') === 0) {
                        break;
                    } else {
                        try {
                            var absoluteUrl = new URL(nextLine, baseUrl).href;
                            streams.push({
                                url: absoluteUrl,
                                resolution: resolution || "Unknown Quality"
                            });
                        } catch(e) {}
                        break;
                    }
                }
            }
        }
        return streams;
    }

    function resolvePlaylistToDataUri(playlistText, baseUrl) {
        if (!playlistText) return null;
        var lines = playlistText.split('\n');
        var resolvedLines = [];
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            if (!line) continue;
            if (line.indexOf('#') === 0) {
                resolvedLines.push(line);
            } else {
                try {
                    var absoluteUrl = new URL(line, baseUrl).href;
                    resolvedLines.push(absoluteUrl);
                    if (absoluteUrl.toLowerCase().indexOf('.ts') === -1 && absoluteUrl.toLowerCase().indexOf('.key') === -1) {
                        reportVideo(absoluteUrl, "Abyss Extracted Link");
                    }
                } catch(e) {
                    resolvedLines.push(line);
                }
            }
        }
        var resolvedPlaylist = resolvedLines.join('\n');
        try {
            var base64Encoded = btoa(unescape(encodeURIComponent(resolvedPlaylist)));
            return "data:application/vnd.apple.mpegurl;base64," + base64Encoded;
        } catch(e) {
            return null;
        }
    }

    function scanForVideoElements() {
        var videos = document.getElementsByTagName('video');
        for (var i = 0; i < videos.length; i++) {
            var src = videos[i].src || '';
            if (src) {
                reportVideo(src, "DOM Scan Video Tag");
            }
            var sources = videos[i].getElementsByTagName('source');
            for (var j = 0; j < sources.length; j++) {
                var sourceSrc = sources[j].src || '';
                if (sourceSrc) {
                    reportVideo(sourceSrc, "DOM Scan Source Tag");
                }
            }
        }
    }
    
    scanForVideoElements();
    
    if (!window.videoObserverActive) {
        window.videoObserverActive = true;
        var observer = new MutationObserver(function(mutations) {
            scanForVideoElements();
        });
        observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
    }

    if (!window.xhrHooked) {
        window.xhrHooked = true;
        var rawOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) {
            this._url = url;
            if (url) {
                reportVideo(url, "XHR Stream Intercept");
            }
            return rawOpen.apply(this, arguments);
        };
        var rawSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function() {
            var xhr = this;
            xhr.addEventListener('load', function() {
                var url = xhr._url || "";
                if (url && isTextTarget(url)) {
                    try {
                        var text = xhr.responseText || "";
                        if (text) {
                            if (text.indexOf('#EXTM3U') !== -1) {
                                reportVideo(url, "XHR Dynamic Playlist");
                                if (text.indexOf('#EXT-X-STREAM-INF:') !== -1) {
                                    var streams = parseMasterPlaylist(text, url);
                                    streams.forEach(function(s) {
                                        window.abyssQualityMap[s.url] = s.resolution;
                                        if (!window.abyssProactiveFetched[s.url]) {
                                            window.abyssProactiveFetched[s.url] = true;
                                            window.fetch(s.url).catch(function(){});
                                        }
                                    });
                                }
                            } else {
                                var decoded = safeAtob(text);
                                if (decoded && decoded.indexOf('#EXTM3U') !== -1) {
                                    var qualitySuffix = window.abyssQualityMap[url] ? " (" + window.abyssQualityMap[url] + ")" : "";
                                    reportVideo(url, "Decrypted Abyss XHR Stream" + qualitySuffix);
                                    var playableDataUri = resolvePlaylistToDataUri(decoded, url);
                                    if (playableDataUri) {
                                        reportVideo(playableDataUri, "Decrypted Playable Abyss Stream" + qualitySuffix);
                                    }

                                    if (decoded.indexOf('#EXT-X-STREAM-INF:') !== -1) {
                                        var streams = parseMasterPlaylist(decoded, url);
                                        streams.forEach(function(s) {
                                            window.abyssQualityMap[s.url] = s.resolution;
                                            if (!window.abyssProactiveFetched[s.url]) {
                                                window.abyssProactiveFetched[s.url] = true;
                                                window.fetch(s.url).catch(function(){});
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } catch(e) {}
                }
            });
            return rawSend.apply(this, arguments);
        };
    }

    if (!window.fetchHooked) {
        window.fetchHooked = true;
        window.fetch = function(input, init) {
            var url = "";
            if (input) {
                if (typeof input === 'string') {
                    url = input;
                } else if (input && typeof input === 'object' && input.url) {
                    url = input.url;
                }
            }
            if (url) {
                reportVideo(url, "Fetch Stream Intercept");
            }
            var promise = rawFetch.apply(this, arguments);
            if (url && isTextTarget(url)) {
                promise.then(function(response) {
                    try {
                        var cloned = response.clone();
                        cloned.text().then(function(text) {
                            if (text) {
                                if (text.indexOf('#EXTM3U') !== -1) {
                                    reportVideo(url, "Fetch Dynamic Playlist");
                                    if (text.indexOf('#EXT-X-STREAM-INF:') !== -1) {
                                        var streams = parseMasterPlaylist(text, url);
                                        streams.forEach(function(s) {
                                            window.abyssQualityMap[s.url] = s.resolution;
                                            if (!window.abyssProactiveFetched[s.url]) {
                                                window.abyssProactiveFetched[s.url] = true;
                                                window.fetch(s.url).catch(function(){});
                                            }
                                        });
                                    }
                                } else {
                                    var decoded = safeAtob(text);
                                    if (decoded && decoded.indexOf('#EXTM3U') !== -1) {
                                        var qualitySuffix = window.abyssQualityMap[url] ? " (" + window.abyssQualityMap[url] + ")" : "";
                                        reportVideo(url, "Decrypted Abyss Fetch Stream" + qualitySuffix);
                                        var playableDataUri = resolvePlaylistToDataUri(decoded, url);
                                        if (playableDataUri) {
                                            reportVideo(playableDataUri, "Decrypted Playable Abyss Stream" + qualitySuffix);
                                        }

                                        if (decoded.indexOf('#EXT-X-STREAM-INF:') !== -1) {
                                            var streams = parseMasterPlaylist(decoded, url);
                                            streams.forEach(function(s) {
                                                window.abyssQualityMap[s.url] = s.resolution;
                                                if (!window.abyssProactiveFetched[s.url]) {
                                                    window.abyssProactiveFetched[s.url] = true;
                                                    window.fetch(s.url).catch(function(){});
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }).catch(function(){});
                    } catch(e) {}
                }).catch(function(){});
            }
            return promise;
        };
    }

    if (!window.elementMediaSrcHooked) {
        window.elementMediaSrcHooked = true;
        try {
            var originalSrcDescriptor = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
            if (originalSrcDescriptor && originalSrcDescriptor.set) {
                Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                    get: function() {
                        return originalSrcDescriptor.get.call(this);
                    },
                    set: function(value) {
                        if (value) {
                            reportVideo(value, "Video Property Assignment");
                        }
                        originalSrcDescriptor.set.call(this, value);
                    }
                });
            }
        } catch (e) {}
    }
    setInterval(function() {
        scanForVideoElements();
    }, 2000);
})();
"""

/**
 * Modern HTML5 Notification W3C standards polyfill/bridge to enable push notification triggers
 */
private const val NOTIFICATION_POLYFILL_JS = """
(function() {
    if (!window.Notification) {
        window.Notification = function(title, options) {
            this.title = title;
            this.options = options || {};
            if (window.AndroidNotificationBridge) {
                window.AndroidNotificationBridge.showNotification(title, JSON.stringify(this.options));
            }
        };

        window.Notification.permission = "granted";

        window.Notification.requestPermission = function(callback) {
            return new Promise(function(resolve) {
                if (window.AndroidNotificationBridge) {
                    window.AndroidNotificationBridge.requestNotificationPermission();
                }
                resolve("granted");
                if (callback) callback("granted");
            });
        };

        if (window.ServiceWorkerRegistration) {
            window.ServiceWorkerRegistration.prototype.showNotification = function(title, options) {
                if (window.AndroidNotificationBridge) {
                    window.AndroidNotificationBridge.showNotification(title, JSON.stringify(options || {}));
                }
                return Promise.resolve();
            };
        }
        console.log("Web Notification standards polyfill injected successfully!");
    }
})();
"""

/**
 * JS Script to dynamically inspect any loaded web pages for dynamic block/iframes,
 * media embeds, standard screen players, or custom video sources.
 */
private const val EXTRACTION_JAVASCRIPT = """
(function() {
    function findAndReportPlayers() {
        var iframes = document.getElementsByTagName('iframe');
        var found = false;
        
        for (var i = 0; i < iframes.length; i++) {
            var iframe = iframes[i];
            var src = iframe.src || '';
            var outerHtml = iframe.outerHTML || '';
            
            var isStreamMedia = (
                src.indexOf('embed') !== -1 ||
                src.indexOf('player') !== -1 ||
                src.indexOf('live') !== -1 ||
                src.indexOf('stream') !== -1 ||
                src.indexOf('.m3u8') !== -1 ||
                src.indexOf('flivetv') !== -1 ||
                src.indexOf('cast') !== -1 ||
                iframe.getAttribute('allowfullscreen') !== null ||
                iframe.getAttribute('allow') !== null
            );
            
            if (isStreamMedia && outerHtml.length > 0) {
                AndroidBridge.onIframeDetected(outerHtml, src);
                found = true;
                break;
            }
        }
        
        if (!found) {
            var videos = document.getElementsByTagName('video');
            if (videos.length > 0) {
                var firstVideo = videos[0];
                var videoHtml = firstVideo.outerHTML || '';
                var videoSrc = firstVideo.src || '';
                if (!videoSrc) {
                    var sources = firstVideo.getElementsByTagName('source');
                    if (sources.length > 0) {
                        videoSrc = sources[0].src || '';
                    }
                }
                if (videoHtml.length > 0) {
                    AndroidBridge.onIframeDetected(videoHtml, videoSrc || "Direct Video Player");
                    found = true;
                }
            }
        }
    }

    findAndReportPlayers();

    if (!window.fliveObserverActive) {
        window.fliveObserverActive = true;
        var observer = new MutationObserver(function(mutations) {
            findAndReportPlayers();
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }
})();
"""

/**
 * Custom State class representing a Chrome browser Tab context.
 */
data class ExtractedVideo(
    val url: String,
    val source: String, // "Network Interception" or "DOM Scan"
    val timestamp: Long = System.currentTimeMillis(),
    val method: String = "GET",
    val headers: Map<String, String>? = null,
    val isForMainFrame: Boolean = false
)

data class Bookmark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TabState(
    val id: String = java.util.UUID.randomUUID().toString(),
    initialUrl: String = "about:home",
    val isIncognito: Boolean = false
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(if (initialUrl == "about:home") "Home" else "Easy Stream")
    var extractedCode by mutableStateOf("")
    var extractedSrc by mutableStateOf("")
    val extractedVideos = mutableStateListOf<ExtractedVideo>()
    var webLoadingProgress by mutableStateOf(0f)
    var isWebLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var webViewInstance by mutableStateOf<WebView?>(null)

    // Group support
    var groupName by mutableStateOf<String?>(null)
    var groupColor by mutableStateOf<Long?>(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "App Location access granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "App Location access denied", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "App Camera access granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "App Camera access denied", Toast.LENGTH_SHORT).show()
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "App Microphone access granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "App Microphone access denied", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "App Notification access granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "App Notification access denied", Toast.LENGTH_SHORT).show()
        }
    }

    // List of active web browser tabs
    val tabs = remember { mutableStateListOf<TabState>() }
    var activeTabId by remember { mutableStateOf("") }
    val tabHistory = remember { mutableStateListOf<String>() }

    LaunchedEffect(activeTabId) {
        if (!activeTabId.isNullOrEmpty()) {
            if (tabHistory.lastOrNull() != activeTabId) {
                tabHistory.remove(activeTabId)
                tabHistory.add(activeTabId)
            }
        }
    }
    
    // On first load, initialize with a default target tab
    if (tabs.isEmpty()) {
        val defaultTab = TabState()
        tabs.add(defaultTab)
        activeTabId = defaultTab.id
    }

    // Get currently active tab safely
    val activeTabState = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    // Keep Address Bar Input text matching the current state's live URL
    var urlInputText by remember { mutableStateOf("") }
    
    LaunchedEffect(activeTabId, activeTabState?.url) {
        activeTabState?.let {
            urlInputText = if (it.url == "about:home") "" else it.url
        }
    }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showTabsSwitcherDialog by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var isPanelMinimized by remember { mutableStateOf(false) }
    var isPanelMaximized by remember { mutableStateOf(false) }
    var selectedExtractionCategory by remember { mutableStateOf("iframes") } // "iframes", "videos", or "network"
    var selectedNetworkRequestUrl by remember { mutableStateOf<String?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isExtractionEnabled by remember { mutableStateOf(true) }

    var activeScreen by remember { mutableStateOf("browser") }

    val bookmarks = remember { mutableStateListOf<Bookmark>() }

    // Chrome expansion states
    val historyList = remember { mutableStateListOf<BrowserHistoryItem>() }
    val recentlyClosedTabs = remember { mutableStateListOf<ClosedTab>() }
    val downloadsList = remember { mutableStateListOf<DownloadItem>() }

    var customWallpaper by remember { mutableStateOf("") }
    var shortcutsEnabled by remember { mutableStateOf(true) }
    var newsFeedEnabled by remember { mutableStateOf(true) }
    var searchEngine by remember { mutableStateOf("Google") }

    // On first load, load bookmarks and settings from preferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("browser_bookmarks", android.content.Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("dark_mode", false)
        isExtractionEnabled = prefs.getBoolean("extraction_enabled", true)
        val jsonStr = prefs.getString("bookmarks_list", "[]") ?: "[]"
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            bookmarks.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                bookmarks.add(
                    Bookmark(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Save dark mode when modified
    LaunchedEffect(isDarkMode) {
        val prefs = context.getSharedPreferences("browser_bookmarks", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    // Save extraction toggle when modified
    LaunchedEffect(isExtractionEnabled) {
        val prefs = context.getSharedPreferences("browser_bookmarks", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("extraction_enabled", isExtractionEnabled).apply()
    }

    // Automatically save bookmarks when they are modified
    LaunchedEffect(bookmarks.toList()) {
        val prefs = context.getSharedPreferences("browser_bookmarks", android.content.Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        bookmarks.forEach { bookmark ->
            val obj = org.json.JSONObject()
            obj.put("id", bookmark.id)
            obj.put("title", bookmark.title)
            obj.put("url", bookmark.url)
            obj.put("timestamp", bookmark.timestamp)
            jsonArray.put(obj)
        }
        prefs.edit().putString("bookmarks_list", jsonArray.toString()).apply()
    }

    val toggleBookmark: (String, String) -> Unit = { title, url ->
        if (url.isNotEmpty() && url != "about:home" && url != "about:blank") {
            val existing = bookmarks.find { it.url == url }
            if (existing != null) {
                bookmarks.remove(existing)
                Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
            } else {
                val finalTitle = if (title.isBlank() || title == "Easy Stream" || title == "Home") url else title
                bookmarks.add(Bookmark(title = finalTitle, url = url))
                Toast.makeText(context, "Added to bookmarks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionsState = remember {
        mutableStateListOf(
            SettingItem("location", "Location", "Ask before allowing sites to see your location", "location", "Ask first"),
            SettingItem("camera", "Camera", "Ask before allowing sites to use your camera", "camera", "Ask first"),
            SettingItem("microphone", "Microphone", "Ask before allowing sites to use your microphone", "microphone", "Ask first"),
            SettingItem("notifications", "Notifications", "Ask before allowing sites to send notifications", "notifications", "Ask first"),
            SettingItem("embedded", "Embedded content", "Ask before allowing sites to display embedded content", "embedded_content", "Ask first"),
            SettingItem("motion", "Motion sensors", "Allow sites to use motion sensors", "motion", "Allowed"),
            SettingItem("nfc", "NFC devices", "Ask before allowing sites to use NFC devices", "nfc", "Ask first"),
            SettingItem("usb", "USB", "Ask before allowing sites to access USB devices", "usb", "Ask first"),
            SettingItem("serial", "Serial port", "Ask before allowing sites to access serial ports", "serial", "Ask first"),
            SettingItem("file", "File editing", "Ask before allowing sites to edit files on your device", "file", "Ask first"),
            SettingItem("clipboard", "Clipboard", "Ask before allowing sites to see copied text or images", "clipboard", "Ask first"),
            SettingItem("vr", "Virtual reality", "Ask before allowing sites to use VR devices", "vr", "Ask first"),
            SettingItem("ar", "Augmented reality", "Ask before allowing sites to use AR devices", "ar", "Ask first"),
            SettingItem("device_use", "Your device use", "Ask before allowing sites to see when you're using your device", "device", "Ask first"),
            SettingItem("network", "Local network", "Ask before allowing sites to access local network devices", "network", "Ask first"),
            SettingItem("apps", "Apps on device", "Ask before allowing sites to discover installed apps", "apps", "Ask first")
        )
    }

    val contentState = remember {
        mutableStateListOf(
            SettingItem("cookies", "Third-party cookies", "Allow sites to save cookie data from other sites", "cookie", "Third-party cookies are allowed"),
            SettingItem("javascript", "JavaScript", "Sites can use JavaScript to function properly", "javascript", "Allowed"),
            SettingItem("popups", "Pop-ups and redirects", "Block pop-ups and automatic redirects", "popups", "Not allowed"),
            SettingItem("sound", "Sound", "Allow sites to play sound", "sound", "Allowed"),
            SettingItem("ads", "Intrusive ads", "Block ads on sites that show intrusive or misleading ads", "ads", "Blocked on some sites"),
            SettingItem("protected", "Protected content", "Allow sites to play protected media", "protected", "Allowed"),
            SettingItem("signin", "Third-party sign-in", "Allow websites to use your credentials to sign in", "signin", "Allowed"),
            SettingItem("autoverify", "Auto-verify", "Let sites verify that you're a real human", "autoverify", "Allowed"),
            SettingItem("device_data", "On-device site data", "Let websites save cookies and save on-device data", "data", "Sites can save data on your device"),
            SettingItem("desktop", "Desktop site", "Request desktop view of websites", "desktop", "Off"),
            SettingItem("sync", "Background sync", "Let websites sync in the background after you leave them", "sync", "Allowed"),
            SettingItem("downloads", "Automatic downloads", "Ask before allowing websites to download multiple files automatically", "download", "Ask first"),
            SettingItem("js_opt", "JavaScript optimisation and security", "Enable speed optimizations on JavaScript", "optimization", "Sites are faster but less secure"),
            SettingItem("saved_zoom", "Saved zoom for sites", "Recall zoom preferences for websites", "zoom", "Saved from sites"),
            SettingItem("data_stored", "Data stored", "Local storage and offline databases stored by websites", "data", "Data stored")
        )
    }

    var automaticRemovePermissionsEnabled by remember { mutableStateOf(true) }

    BackHandler(enabled = activeScreen != "browser") {
        if (activeScreen == "all_sites") {
            activeScreen = "site_settings"
        } else if (activeScreen == "site_settings") {
            activeScreen = "browser"
        } else if (activeScreen == "bookmarks") {
            activeScreen = "browser"
        }
    }

    val canGoBackInWeb = activeTabState?.canGoBack == true
    val canGoBackToPrevTab = tabHistory.size >= 2

    BackHandler(enabled = activeScreen == "browser" && (canGoBackInWeb || canGoBackToPrevTab)) {
        if (canGoBackInWeb) {
            activeTabState?.webViewInstance?.goBack()
        } else if (canGoBackToPrevTab) {
            val currentIdx = tabHistory.indexOf(activeTabId)
            if (currentIdx > 0) {
                val targetTabId = tabHistory[currentIdx - 1]
                tabHistory.remove(activeTabId)
                activeTabId = targetTabId
            } else {
                tabHistory.removeAt(tabHistory.size - 1)
                activeTabId = tabHistory.last()
            }
        }
    }

    // Supabase Guard integration states
    val blockedDomains = remember { mutableStateListOf<String>() }
    var supabaseStatusMessage by remember { mutableStateOf("Initializing Supabase guard...") }
    var isSupabaseConfigured by remember { mutableStateOf(false) }
    var isFetchingBlockedDomains by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val siteDatabase = remember { SiteDatabase.getDatabase(context) }
    val siteDataManager = remember { SiteDataManager(context, siteDatabase.siteStorageMetricsDao()) }

    LaunchedEffect(activeScreen) {
        if (activeScreen == "all_sites" || activeScreen == "site_settings") {
            try {
                siteDataManager.refreshFromSystem()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val onPageVisited: (String) -> Unit = { visitedUrl ->
        try {
            if (activeTabState?.isIncognito != true &&
                !visitedUrl.isNullOrEmpty() &&
                !visitedUrl.startsWith("about:") &&
                !visitedUrl.startsWith("file:") &&
                visitedUrl != "blank"
            ) {
                // Add to chronological browsing history list
                val docTitle = activeTabState?.title ?: visitedUrl
                historyList.add(
                    BrowserHistoryItem(
                        title = if (docTitle.isBlank() || docTitle == "Connecting...") visitedUrl else docTitle,
                        url = visitedUrl
                    )
                )

                coroutineScope.launch {
                    try {
                        siteDataManager.recordVisit(
                            url = visitedUrl,
                            title = if (docTitle.isBlank() || docTitle == "Connecting...") "" else docTitle
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // Helper functions for checking blocked urls
    val getDomainOfUrl: (String) -> String = { urlStr ->
        if (urlStr.startsWith("data:", ignoreCase = true)) {
            "Decrypted Memory Data"
        } else {
            try {
                val uri = java.net.URI(urlStr)
                val host = uri.host ?: ""
                if (host.startsWith("www.")) host.substring(4).lowercase() else host.lowercase()
            } catch (e: Exception) {
                var clean = urlStr.removePrefix("https://").removePrefix("http://")
                val slashIdx = clean.indexOf('/')
                if (slashIdx != -1) clean = clean.substring(0, slashIdx)
                val colonIdx = clean.indexOf(':')
                if (colonIdx != -1) clean = clean.substring(0, colonIdx)
                if (clean.startsWith("www.")) clean.substring(4).lowercase() else clean.lowercase()
            }
        }
    }

    val extractSrcFromIframeCode: (String) -> String = { code ->
        val srcPattern = java.util.regex.Pattern.compile("src=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = srcPattern.matcher(code)
        if (matcher.find()) {
            matcher.group(1) ?: ""
        } else {
            ""
        }
    }

    val isUrlBlocked: (String) -> Boolean = { srcUrl ->
        if (srcUrl.isEmpty()) {
            false
        } else {
            val domain = getDomainOfUrl(srcUrl)
            if (domain.isEmpty()) {
                false
            } else {
                blockedDomains.any { blocked ->
                    domain == blocked || domain.endsWith(".$blocked")
                }
            }
        }
    }

    val fetchSupabaseBlockedDomains: () -> Unit = {
        val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
        val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

        if (supabaseUrl.isEmpty() || supabaseUrl == "YOUR_SUPABASE_URL" ||
            supabaseAnonKey.isEmpty() || supabaseAnonKey == "YOUR_SUPABASE_ANON_KEY") {
            supabaseStatusMessage = "Waiting to load config. Enter Supabase credentials in your AI Studio secrets panel inside Settings."
            isSupabaseConfigured = false
        } else {
            isSupabaseConfigured = true
            isFetchingBlockedDomains = true
            supabaseStatusMessage = "Fetching latest blocklist rules..."

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val baseUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
                    val requestUrl = "${baseUrl}rest/v1/blocked?select=blocked"

                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url(requestUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Accept", "application/json")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                supabaseStatusMessage = "Sync failed with HTTP status: ${response.code}"
                                isFetchingBlockedDomains = false
                            }
                        } else {
                            val bodyStr = response.body?.string() ?: "[]"
                            val fetchedList = mutableListOf<String>()

                            val jsonArray = org.json.JSONArray(bodyStr)
                            for (i in 0 until jsonArray.length()) {
                                val row = jsonArray.getJSONObject(i)
                                if (row.has("blocked")) {
                                    val blockedObj = row.getJSONObject("blocked")
                                    if (blockedObj.has("domains")) {
                                        val domainsArray = blockedObj.getJSONArray("domains")
                                        for (j in 0 until domainsArray.length()) {
                                            val domain = domainsArray.getString(j).trim().lowercase()
                                            if (domain.isNotEmpty()) {
                                                fetchedList.add(domain)
                                            }
                                        }
                                    }
                                }
                            }

                            withContext(Dispatchers.Main) {
                                blockedDomains.clear()
                                blockedDomains.addAll(fetchedList)
                                supabaseStatusMessage = "Sync completed! Successfully configured ${fetchedList.size} protected domains."
                                isFetchingBlockedDomains = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Supabase", "Failed to retrieve blocked domains", e)
                    withContext(Dispatchers.Main) {
                        supabaseStatusMessage = "Synchronization failed: ${e.localizedMessage ?: "Network error"}"
                        isFetchingBlockedDomains = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100) // slight delay to ensure UI completes first drawing loop
        fetchSupabaseBlockedDomains()
    }

    // Copy to clipboard helper - modified with active domain blocking policy check and customized messages
    val copyToClipboard = { text: String ->
        var cleanText = text.trim()
        if (cleanText == "Direct Video Player Tag" || cleanText == "Direct Video Player") {
            cleanText = ""
        }
        val isCode = cleanText.startsWith("<")
        val srcUrl = if (isCode) {
            val extracted = activeTabState?.extractedSrc ?: ""
            val isExtractedValValid = extracted.isNotEmpty() && extracted != "Direct Video Player Tag" && extracted != "Direct Video Player"
            if (isExtractedValValid) extracted else extractSrcFromIframeCode(cleanText)
        } else {
            cleanText
        }

        if (cleanText.isEmpty()) {
            Toast.makeText(context, "No URL found inside dynamic video element!", Toast.LENGTH_SHORT).show()
        } else if (isUrlBlocked(srcUrl)) {
            val domain = getDomainOfUrl(srcUrl)
            Toast.makeText(context, "🚫 Copy Restricted: '$domain' is in the Supabase administrator blocklist.", Toast.LENGTH_LONG).show()
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(if (isCode) "Extracted Embed Code" else "Extracted Embed URL", cleanText)
            clipboard.setPrimaryClip(clip)
            if (isCode) {
                Toast.makeText(context, "Embed code copied!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Source URL copied successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Address Bar submission processing
    val processAndLoadUrl = { url: String ->
        val query = url.trim()
        activeTabState?.let { tab ->
            if (query.isNotEmpty()) {
                val hasPrefix = query.startsWith("http://") || query.startsWith("https://")
                val isLikelyUrl = Patterns.WEB_URL.matcher(query).matches() || query.contains(".") && !query.contains(" ")
                
                val targetUrl = when {
                    hasPrefix -> query
                    isLikelyUrl -> "https://$query"
                    else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                }
                tab.url = targetUrl
                tab.webViewInstance?.loadUrl(targetUrl)
                tab.extractedCode = ""
                tab.extractedSrc = ""
                tab.extractedVideos.clear()
            }
        }
        focusManager.clearFocus()
    }

    val runExtractionScript = { webView: WebView ->
        if (isExtractionEnabled) {
            webView.evaluateJavascript(EXTRACTION_JAVASCRIPT, null)
            webView.evaluateJavascript(VIDEO_EXTRACTION_JAVASCRIPT, null)
        }
    }

    val runStorageExtractionScript = { webView: WebView ->
        val jsCmd = """
            (function() {
                try {
                    var domain = window.location.host;
                    if (!domain) return;
                    if (domain.startsWith('www.')) { domain = domain.substring(4); }
                    
                    var lsCount = 0;
                    var lsSize = 0;
                    try {
                        lsCount = localStorage.length;
                        lsSize = JSON.stringify(localStorage).length;
                    } catch(e){}
                    
                    var ssCount = 0;
                    var ssSize = 0;
                    try {
                        ssCount = sessionStorage.length;
                        ssSize = JSON.stringify(sessionStorage).length;
                    } catch(e){}
                    
                    var swCount = 0;
                    try {
                        if (navigator.serviceWorker && navigator.serviceWorker.getRegistrations) {
                            navigator.serviceWorker.getRegistrations().then(function(regs) {
                                swCount = regs.length;
                            }).catch(function(){});
                        }
                    } catch(e){}
                    
                    var cacheCount = 0;
                    var cacheSize = 0;
                    if (window.caches && window.caches.keys) {
                        caches.keys().then(function(keys) {
                            cacheCount = keys.length;
                            cacheSize = cacheCount * 524288;
                            
                            window.AndroidSiteStorageBridge.onDetailedStats(
                                domain,
                                lsCount, lsSize,
                                ssCount, ssSize,
                                0, 0,
                                cacheCount, cacheSize,
                                swCount
                            );
                        }).catch(function() {
                            window.AndroidSiteStorageBridge.onDetailedStats(
                                domain, lsCount, lsSize, ssCount, ssSize, 0, 0, 0, 0, swCount
                            );
                        });
                    } else {
                        window.AndroidSiteStorageBridge.onDetailedStats(
                            domain, lsCount, lsSize, ssCount, ssSize, 0, 0, 0, 0, swCount
                        );
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.postDelayed({
            webView.evaluateJavascript(jsCmd, null)
        }, 1200)
        Unit
    }

    // Dynamic green pulse transition animation status green circle dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Helper functions for action commands
    val createNewTab: (String) -> Unit = { targetUrl ->
        val newTab = TabState(initialUrl = targetUrl)
        newTab.title = "Connecting..."
        tabs.add(newTab)
        activeTabId = newTab.id
    }

    val closeTab: (TabState) -> Unit = { tabToClose ->
        val index = tabs.indexOf(tabToClose)
        if (!tabToClose.isIncognito && tabToClose.url != "about:home" && tabToClose.url != "about:blank") {
            recentlyClosedTabs.add(ClosedTab(title = tabToClose.title, url = tabToClose.url))
        }
        if (tabs.size > 1) {
            tabs.remove(tabToClose)
            tabHistory.remove(tabToClose.id)
            if (activeTabId == tabToClose.id) {
                // Shift focus to nearby remaining tab
                val newIndex = if (index >= tabs.size) tabs.size - 1 else index
                activeTabId = tabs[newIndex].id
            }
        } else {
            // Last remaining tab: Reset url and properties to home
            tabToClose.url = "about:home"
            tabToClose.title = "Home"
            tabToClose.extractedCode = ""
            tabToClose.extractedSrc = ""
            tabToClose.extractedVideos.clear()
            tabToClose.webViewInstance?.loadUrl("about:blank")
        }
    }

    if (activeScreen != "browser") {
        when (activeScreen) {
            "site_settings" -> {
                SiteSettingsScreen(
                    permissionsList = permissionsState,
                    contentList = contentState,
                    automaticRemovePermissions = automaticRemovePermissionsEnabled,
                    isDarkMode = isDarkMode,
                    onPermissionChanged = { editingItem, newValue ->
                        val permIdx = permissionsState.indexOfFirst { it.id == editingItem.id }
                        if (permIdx != -1) {
                            permissionsState[permIdx] = permissionsState[permIdx].copy(value = newValue)
                            // Trigger system permission requests if they set the site level permission to Allowed or Ask first
                            if (newValue == "Allowed" || newValue == "Ask first") {
                                when (editingItem.id) {
                                    "location" -> {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    "camera" -> {
                                        cameraLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    "microphone" -> {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                    "notifications" -> {
                                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                                            notificationLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                        } else {
                                            Toast.makeText(context, "Notifications are allowed by default", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        } else {
                            val contIdx = contentState.indexOfFirst { it.id == editingItem.id }
                            if (contIdx != -1) {
                                contentState[contIdx] = contentState[contIdx].copy(value = newValue)
                            }
                        }
                    },
                    onAutomaticRemoveToggled = { automaticRemovePermissionsEnabled = it },
                    onNavigateToAllSites = { activeScreen = "all_sites" },
                    onBack = { activeScreen = "browser" }
                )
            }
            "all_sites" -> {
                AllSitesScreen(
                    siteDataManager = siteDataManager,
                    onBack = { activeScreen = "site_settings" },
                    isDarkMode = isDarkMode
                )
            }
            "bookmarks" -> {
                BookmarksScreen(
                    bookmarksList = bookmarks,
                    onOpenBookmark = { bookmark ->
                        activeTabState?.let { tab ->
                            tab.url = bookmark.url
                            tab.title = bookmark.title
                            tab.webViewInstance?.loadUrl(bookmark.url)
                        }
                        activeScreen = "browser"
                    },
                    onDeleteBookmark = { target ->
                        bookmarks.remove(target)
                    },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode
                )
            }
            "history" -> {
                HistoryScreen(
                    historyList = historyList,
                    onOpenUrl = { url ->
                        activeTabState?.let { tab ->
                            tab.url = url
                            tab.title = "Connecting..."
                            tab.webViewInstance?.loadUrl(url)
                        }
                        activeScreen = "browser"
                    },
                    onDeleteHistoryItem = { historyItem ->
                        historyList.remove(historyItem)
                    },
                    onClearAllHistory = {
                        historyList.clear()
                    },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "delete_browsing_data" -> {
                DeleteBrowsingDataScreen(
                    onDeleteData = { timeRangeHour, history, cookies, cache, sites, downloads ->
                        if (history) historyList.clear()
                        if (cookies) {
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            android.webkit.CookieManager.getInstance().flush()
                        }
                        if (sites) {
                            coroutineScope.launch {
                                try {
                                    siteDataManager.clearAllSitesData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        if (downloads) downloadsList.clear()
                    },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "downloads" -> {
                DownloadManagerScreen(
                    downloadsList = downloadsList,
                    onPauseResume = { dlItem ->
                        if (dlItem.status == "Downloading") {
                            dlItem.status = "Paused"
                        } else if (dlItem.status == "Paused") {
                            dlItem.status = "Downloading"
                            // resume coroutine loop
                            coroutineScope.launch {
                                while (dlItem.progress < 1.0f && dlItem.status == "Downloading") {
                                    kotlinx.coroutines.delay(800)
                                    if (dlItem.status == "Downloading") {
                                        dlItem.progress += 0.1f
                                        if (dlItem.progress >= 1.0f) {
                                            dlItem.progress = 1.0f
                                            dlItem.status = "Completed"
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onDelete = { dlItem ->
                        downloadsList.remove(dlItem)
                    },
                    onSimulateDownload = {
                        val items = listOf(
                            "Chrome_Setup_x64.pdf" to "application/pdf",
                            "Introduction_To_Kotlin.mp4" to "video/mp4",
                            "React_Native_vs_Flutter.png" to "image/png"
                        )
                        val chosen = items.random()
                        val newDl = DownloadItem(
                            title = chosen.first,
                            url = "https://example.com/files/" + chosen.first,
                            mimeType = chosen.second,
                            totalSize = "${(5..18).random()}.${(0..9).random()} MB"
                        )
                        downloadsList.add(newDl)
                        coroutineScope.launch {
                            while (newDl.progress < 1.0f && newDl.status == "Downloading") {
                                kotlinx.coroutines.delay(1000)
                                if (newDl.status == "Downloading") {
                                    newDl.progress += 0.15f
                                    if (newDl.progress >= 1.0f) {
                                        newDl.progress = 1.0f
                                        newDl.status = "Completed"
                                    }
                                }
                            }
                        }
                    },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "recent_tabs" -> {
                RecentTabsScreen(
                    recentlyClosed = recentlyClosedTabs,
                    onRestoreTab = { closed ->
                        createNewTab(closed.url)
                        recentlyClosedTabs.remove(closed)
                        activeScreen = "browser"
                    },
                    onClearClosedHistory = {
                        recentlyClosedTabs.clear()
                    },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "customise_new_tab" -> {
                CustomiseNewTabScreen(
                    currentWallpaper = customWallpaper,
                    onWallpaperSelected = { customWallpaper = it },
                    shortcutsEnabled = shortcutsEnabled,
                    onShortcutsToggle = { shortcutsEnabled = it },
                    newsFeedEnabled = newsFeedEnabled,
                    onNewsFeedToggle = { newsFeedEnabled = it },
                    searchEngine = searchEngine,
                    onSearchEngineSelected = { searchEngine = it },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "help_feedback" -> {
                HelpFeedbackScreen(
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
            "settings" -> {
                BrowserSettingsScreen(
                    currentTheme = if (isDarkMode) "Dark" else "Light",
                    onThemeChanged = { theme ->
                        isDarkMode = (theme == "Dark")
                    },
                    searchEngine = searchEngine,
                    onSearchEngineSelected = { searchEngine = it },
                    isPanelMinimized = isPanelMinimized,
                    onPanelMinimizeToggle = { isPanelMinimized = it },
                    extractionEnabled = isExtractionEnabled,
                    onExtractionEnabledToggle = { isExtractionEnabled = it },
                    onBack = { activeScreen = "browser" },
                    isDarkMode = isDarkMode,
                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                )
            }
        }
    } else {
        Scaffold(
        topBar = {
            Surface(
                color = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Back button on the left of address bar
                    val isBackEnabled = canGoBackInWeb || canGoBackToPrevTab
                    IconButton(
                        onClick = {
                            if (canGoBackInWeb) {
                                activeTabState?.webViewInstance?.goBack()
                            } else if (canGoBackToPrevTab) {
                                val currentIdx = tabHistory.indexOf(activeTabId)
                                if (currentIdx > 0) {
                                    val targetTabId = tabHistory[currentIdx - 1]
                                    tabHistory.remove(activeTabId)
                                    activeTabId = targetTabId
                                } else {
                                    tabHistory.removeAt(tabHistory.size - 1)
                                    activeTabId = tabHistory.last()
                                }
                            }
                        },
                        enabled = isBackEnabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = if (isBackEnabled) {
                                if (isDarkMode) Color.White else Color(0xFF1E293B)
                            } else {
                                if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Home Page button
                    IconButton(
                        onClick = {
                            activeTabState?.let { tab ->
                                tab.url = "about:home"
                                tab.title = "Home"
                                tab.webViewInstance?.loadUrl("about:blank")
                                tab.extractedCode = ""
                                tab.extractedSrc = ""
                                tab.extractedVideos.clear()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home Page",
                            tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Address Bar (Custom rounded Chrome Style Box)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9))
                            .border(
                                width = 1.dp,
                                color = if (isDarkMode) Color(0xFF4B5563) else Color(0xFF006874).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Globe Indicator",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (urlInputText.isEmpty()) {
                                    Text(
                                        text = "Search or type URL...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = urlInputText,
                                    onValueChange = { urlInputText = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = { processAndLoadUrl(urlInputText) }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874))
                                )
                            }

                            if (urlInputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInputText = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            val showStarInAddressBar = activeTabState != null && 
                                    activeTabState.url != "about:home" && 
                                    activeTabState.url != "about:blank" && 
                                    activeTabState.url.isNotEmpty()

                            if (showStarInAddressBar) {
                                val bookmarked = bookmarks.any { it.url == activeTabState.url }
                                IconButton(
                                    onClick = {
                                        activeTabState?.let { tab ->
                                            toggleBookmark(tab.title, tab.url)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Toggle Bookmark",
                                        tint = if (bookmarked) Color(0xFFFFB300) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Direct Search/Go action option next to address bar
                    IconButton(
                        onClick = { processAndLoadUrl(urlInputText) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Go navigate",
                            tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Floating tab count counter
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874), RoundedCornerShape(6.dp))
                            .clickable { showTabsSwitcherDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                        )
                    }

                    // 3-dot dropdown menu options list
                    Box {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu options",
                                tint = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (showDropdownMenu) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopEnd,
                                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                                onDismissRequest = { showDropdownMenu = false }
                            ) {
                                val isCurrentPageBookmarked = activeTabState?.let { tab ->
                                    tab.url != "about:home" && tab.url != "about:blank" && bookmarks.any { it.url == tab.url }
                                } ?: false

                                ChromeStyleMenu(
                                    isDarkMode = isDarkMode,
                                    primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                    canGoBack = activeTabState?.canGoBack ?: false,
                                    canGoForward = activeTabState?.canGoForward ?: false,
                                    isBookmarked = isCurrentPageBookmarked,
                                    onBackAction = { activeTabState?.webViewInstance?.goBack() },
                                    onForwardAction = { activeTabState?.webViewInstance?.goForward() },
                                    onToggleBookmark = {
                                        activeTabState?.let { toggleBookmark(it.title, it.url) }
                                    },
                                    onNavigateDownloads = { activeScreen = "downloads" },
                                    onRefreshAction = { activeTabState?.webViewInstance?.reload() },
                                    onDismiss = { showDropdownMenu = false },
                                    onNewTab = { createNewTab("about:home") },
                                    onNewIncognitoTab = {
                                        val incognitoTab = TabState(initialUrl = "about:home", isIncognito = true)
                                        incognitoTab.title = "Local Incognito Workspace"
                                        tabs.add(incognitoTab)
                                        activeTabId = incognitoTab.id
                                        activeScreen = "browser"
                                    },
                                    onAddTabToGroup = {
                                        activeTabState?.let { tab ->
                                            tab.groupName = "Group Workspace"
                                            tab.groupColor = 0xFF00ADB5L
                                        }
                                    },
                                    onOpenHistory = { activeScreen = "history" },
                                    onDeleteBrowsingData = { activeScreen = "delete_browsing_data" },
                                    onOpenSiteSettings = { activeScreen = "site_settings" },
                                    onOpenBookmarks = { activeScreen = "bookmarks" },
                                    onOpenRecentTabs = { activeScreen = "recent_tabs" },
                                    onOpenSettings = { activeScreen = "settings" },
                                    onCustomiseNewTab = { activeScreen = "customise_new_tab" },
                                    onOpenHelpFeedback = { activeScreen = "help_feedback" }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (isDarkMode) Color(0xFF18181C) else Color(0xFFF7F9FA)
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
        ) {
            




            // Web Loading Linear Progress indicator stream bar
            val activeTabIsLoading = activeTabState?.isWebLoading == true
            val activeTabProgress = activeTabState?.webLoadingProgress ?: 0f

            if (activeTabIsLoading) {
                LinearProgressIndicator(
                    progress = { activeTabProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                    trackColor = if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9)
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // ================= COMPOSABLE ACTIVE WEBVIEW STACKS =================
            // We draw all of them, but make inactive ones take 0.dp size so that they are alive offline,
            // retaining dynamic scroll position, dynamic state, history, media play etc.
            Box(
                modifier = Modifier
                    .weight(
                        if (!isExtractionEnabled) {
                            1f
                        } else {
                            when {
                                isPanelMinimized -> 0.92f
                                isPanelMaximized -> 0.08f
                                else -> 0.65f
                            }
                        }
                    )
                    .fillMaxWidth()
                    .background(if (isDarkMode) Color(0xFF18181C) else Color.White)
            ) {
                tabs.forEach { tabState ->
                    val isActive = tabState.id == activeTabId
                    Box(
                        modifier = if (isActive) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .size(0.dp)
                                .alpha(0f)
                        }
                    ) {
                        TabWebViewContainer(
                            tabState = tabState,
                            isUrlBlocked = isUrlBlocked,
                            runExtractionScript = runExtractionScript,
                            runStorageExtractionScript = runStorageExtractionScript,
                            siteDataManager = siteDataManager,
                            coroutineScope = coroutineScope,
                            onNewTabRequested = { newTab ->
                                tabs.add(newTab)
                                activeTabId = newTab.id
                            },
                            onPageVisited = { visitedUrl ->
                                onPageVisited(visitedUrl)
                            },
                            onRequestNotificationPermission = {
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "Notifications are allowed by default", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        if (tabState.url == "about:home") {
                            LocalHomeScreen(
                                isDarkMode = isDarkMode,
                                supabaseStatusMessage = supabaseStatusMessage,
                                isSupabaseConfigured = isSupabaseConfigured,
                                isFetchingBlockedDomains = isFetchingBlockedDomains,
                                blockedDomains = blockedDomains,
                                bookmarksList = bookmarks,
                                onRefresh = { fetchSupabaseBlockedDomains() },
                                onOpenUrl = { targetUrl ->
                                    tabState.url = targetUrl
                                    tabState.webViewInstance?.loadUrl(targetUrl)
                                    tabState.extractedCode = ""
                                    tabState.extractedSrc = ""
                                    tabState.extractedVideos.clear()
                                },
                                customWallpaper = customWallpaper,
                                shortcutsEnabled = shortcutsEnabled,
                                newsFeedEnabled = newsFeedEnabled,
                                searchEngine = searchEngine
                            )
                        }
                    }
                }

                // Forward indicator overlays on right side
                if (activeTabState?.canGoForward == true) {
                    IconButton(
                        onClick = { activeTabState.webViewInstance?.goForward() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xE62E2E33) else Color(0xE6FFFFFF))
                            .border(1.dp, if (isDarkMode) Color(0xFF4B5563) else Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward link",
                            tint = if (isDarkMode) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isExtractionEnabled) {
                // ================= EXTRACTED EMBED PANEL (MINIMIZABLE CONTAINER) =================
                val extractedCode = activeTabState?.extractedCode ?: ""
                val extractedSrc = activeTabState?.extractedSrc ?: ""
                val extractedVideos = activeTabState?.extractedVideos ?: remember { mutableStateListOf() }
                val hasPayload = extractedCode.isNotEmpty() || extractedVideos.isNotEmpty()

                Column(
                modifier = Modifier
                    .weight(
                        when {
                            isPanelMinimized -> 0.08f
                            isPanelMaximized -> 0.92f
                            else -> 0.35f
                        }
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(if (isDarkMode) Color(0xFF1E1E22) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // Control Title / Minimize Panel Dragbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isPanelMinimized = !isPanelMinimized 
                            if (isPanelMinimized) {
                                isPanelMaximized = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing status sensor dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(if (hasPayload) pulseAlpha else 0.5f)
                                .clip(CircleShape)
                                .background(if (hasPayload) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (hasPayload) "EXTRACTION ENGINE ACTIVE!" else "SCANNING STREAM CHANNELS...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasPayload) {
                                if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857)
                            } else {
                                if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF1E293B)
                            },
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Short mini buttons if minimized to let user copy without even opening!
                        if (isPanelMinimized && hasPayload) {
                            IconButton(
                                onClick = { 
                                    if (selectedExtractionCategory == "iframes" && extractedCode.isNotEmpty()) {
                                        copyToClipboard(extractedCode)
                                    } else if (extractedVideos.isNotEmpty()) {
                                        copyToClipboard(extractedVideos.first().url)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Quick copy",
                                    tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Fullscreen / Maximize overlay button
                        IconButton(
                            onClick = { 
                                isPanelMaximized = !isPanelMaximized
                                if (isPanelMaximized) {
                                    isPanelMinimized = false
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("fullscreen_overlay_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPanelMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Overlay Toggle",
                                tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close/Turn Off Extraction Panel Button
                        IconButton(
                            onClick = { 
                                isExtractionEnabled = false
                                Toast.makeText(
                                    context, 
                                    "Extraction engine disabled. Re-enable anytime from the three-dot menu!", 
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier.size(28.dp).testTag("turn_off_extraction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Turn off extraction panel",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Minimize expand chevron indicator
                        Icon(
                            imageVector = if (isPanelMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize Toggle",
                            tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Render detail body ONLY if the panel is not minimized
                if (!isPanelMinimized) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        // Category Switcher Tab-like Row (Separation of Concerns for System A and B UI)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedExtractionCategory = "iframes" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedExtractionCategory == "iframes") {
                                        if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                    } else {
                                        if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9)
                                    },
                                    contentColor = if (selectedExtractionCategory == "iframes") Color.White else (if (isDarkMode) Color.LightGray else Color.DarkGray)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("System A: Iframes", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { selectedExtractionCategory = "videos" },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedExtractionCategory == "videos") {
                                        if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                    } else {
                                        if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9)
                                    },
                                    contentColor = if (selectedExtractionCategory == "videos") Color.White else (if (isDarkMode) Color.LightGray else Color.DarkGray)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("System B: Videos (${extractedVideos.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { selectedExtractionCategory = "network" },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedExtractionCategory == "network") {
                                        if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                    } else {
                                        if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9)
                                    },
                                    contentColor = if (selectedExtractionCategory == "network") Color.White else (if (isDarkMode) Color.LightGray else Color.DarkGray)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Network Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedExtractionCategory == "iframes") {
                            // SYSTEM A: Iframe Embed View
                            if (extractedCode.isNotEmpty()) {
                                val activeSrcUrl = if (extractedSrc.isNotEmpty() && extractedSrc != "Direct Video Player" && extractedSrc != "Direct Video Player Tag") extractedSrc else extractSrcFromIframeCode(extractedCode)
                                val isCurrentBlocked = isUrlBlocked(activeSrcUrl)
                                val activeDomain = getDomainOfUrl(activeSrcUrl)

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isCurrentBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDarkMode) Color(0x33EF4444) else Color(0xFFFEE2E2))
                                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "🔒 Blocked Domain: '$activeDomain' copy restricted by policy.",
                                                    color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0xFF090D16) else Color(0xFF0F172A))
                                            .border(1.dp, if (isDarkMode) Color(0xFF1E2D3B) else Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = extractedCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color(0xFF34D399),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { copyToClipboard(extractedCode) },
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(44.dp)
                                                .testTag("copy_iframe_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy full iframe",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Iframe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                if (extractedSrc.isNotEmpty() && extractedSrc != "Direct Video Player" && extractedSrc != "Direct Video Player Tag") {
                                                    copyToClipboard(extractedSrc)
                                                } else {
                                                    // Parse src from outerHtml as a fallback if extractedSrc is empty
                                                    val srcPattern = java.util.regex.Pattern.compile("src=\"([^\"]+)\"")
                                                    val matcher = srcPattern.matcher(extractedCode)
                                                    if (matcher.find()) {
                                                        val srcVal = matcher.group(1) ?: ""
                                                        copyToClipboard(srcVal)
                                                    } else {
                                                        Toast.makeText(context, "No URL found inside iframe!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("copy_url_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFE2E8F0),
                                                contentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = "Copy source URL",
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, extractedCode)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, null))
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (isDarkMode) Color(0xFF4B5563) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share code",
                                                tint = if (isDarkMode) Color.White else Color(0xFF475569),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF8FAFC))
                                        .border(1.dp, if (isDarkMode) Color(0xFF4B5563) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "System A Sniffer: Play standard stream sources to extract underlying iframe embeds.",
                                            fontSize = 11.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        )
                                    }
                                }
                            }
                        } else if (selectedExtractionCategory == "videos") {
                            // SYSTEM B: Direct Video URLs list
                            if (extractedVideos.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0xFF090D16) else Color(0xFFF8FAFC))
                                            .border(1.dp, if (isDarkMode) Color(0xFF1E2D3B) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            extractedVideos.forEachIndexed { index, video ->
                                                val isBlocked = isUrlBlocked(video.url)
                                                val videoDomain = getDomainOfUrl(video.url)
                                                val (formatLabel, badgeColor) = getVideoFormatLabelAndColor(video.url)
                                                
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isDarkMode) Color(0xFF1E1E24) else Color.White)
                                                        .border(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(badgeColor)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = formatLabel,
                                                                    color = Color.White,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(if (isDarkMode) Color(0x3394A3B8) else Color(0xFFE2E8F0))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = video.source,
                                                                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        
                                                        Text(
                                                            text = video.url,
                                                            fontSize = 10.sp,
                                                            color = if (isBlocked) Color.Red else (if (isDarkMode) Color(0xFF34D399) else Color(0xFF0F766E)),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        
                                                        Text(
                                                            text = "Source domain: $videoDomain",
                                                            fontSize = 8.5.sp,
                                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                        )
                                                    }
                                                    
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedNetworkRequestUrl = video.url
                                                                selectedExtractionCategory = "network"
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info,
                                                                contentDescription = "View network request info",
                                                                tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { copyToClipboard(video.url) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Copy video stream link",
                                                                tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                val sendIntent = Intent().apply {
                                                                    action = Intent.ACTION_SEND
                                                                    putExtra(Intent.EXTRA_TEXT, video.url)
                                                                    type = "text/plain"
                                                                }
                                                                context.startActivity(Intent.createChooser(sendIntent, null))
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = "Share stream link",
                                                                tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF8FAFC))
                                        .border(1.dp, if (isDarkMode) Color(0xFF4B5563) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "System B Sniffer: Start video playback to capture direct video files and playlists (MP4, WebM, M3U8, MPD, MKV, MOV, FLV, AVI, and other streams) from network and DOM tags automatically.",
                                            fontSize = 11.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // SYSTEM B: Network Logs / Headers View
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (extractedVideos.isNotEmpty()) {
                                    val currentSelectedUrl = selectedNetworkRequestUrl ?: extractedVideos.firstOrNull()?.url ?: ""
                                    val selectedVideo = extractedVideos.find { it.url == currentSelectedUrl }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                    ) {
                                        extractedVideos.forEachIndexed { idx, video ->
                                            val isSelected = video.url == currentSelectedUrl
                                            val domain = getDomainOfUrl(video.url)
                                            val (formatLabel, _) = getVideoFormatLabelAndColor(video.url)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) {
                                                            if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                                        } else {
                                                            if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFE2E8F0)
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedNetworkRequestUrl = video.url
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "[$formatLabel] $domain",
                                                    color = if (isSelected) Color.White else (if (isDarkMode) Color.LightGray else Color.DarkGray),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (selectedVideo != null) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isDarkMode) Color(0xFF090D16) else Color(0xFFF8FAFC))
                                                .border(1.dp, if (isDarkMode) Color(0xFF1E2D3B) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "HTTP Request Details",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedVideo.method,
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFECEFF1))
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "REQUEST URL",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.Gray else Color.DarkGray
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = selectedVideo.url,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF0F766E),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "HEADERS (${selectedVideo.headers?.size ?: 0})",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkMode) Color.Gray else Color.DarkGray
                                                        )
                                                        
                                                        Text(
                                                            text = "Copy All Headers",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                                            modifier = Modifier
                                                                .clickable {
                                                                    val headersStr = selectedVideo.headers?.entries?.joinToString("\n") { "${it.key}: ${it.value}" } ?: ""
                                                                    copyToClipboard(headersStr)
                                                                }
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    
                                                    val headersMap = selectedVideo.headers
                                                    if (!headersMap.isNullOrEmpty()) {
                                                        headersMap.forEach { (key, value) ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(if (isDarkMode) Color(0xFF141419) else Color(0xFFF1F5F9))
                                                                    .padding(6.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.Top
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = key,
                                                                        fontSize = 9.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                                                    )
                                                                    Spacer(modifier = Modifier.height(2.dp))
                                                                    Text(
                                                                        text = value,
                                                                        fontSize = 9.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                                                                    )
                                                                }
                                                                
                                                                IconButton(
                                                                    onClick = { copyToClipboard("$key: $value") },
                                                                    modifier = Modifier.size(24.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ContentCopy,
                                                                        contentDescription = "Copy header",
                                                                        tint = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                                                                .padding(12.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "No custom HTTP headers intercepted. Standard browser headers applied.",
                                                                fontSize = 10.sp,
                                                                color = Color.Gray,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                val curlCommand = remember(selectedVideo) {
                                                    val headerOptions = selectedVideo.headers?.entries?.joinToString(" ") { 
                                                        "-H \"${it.key.replace("\"", "\\\"")}: ${it.value.replace("\"", "\\\"")}\"" 
                                                    } ?: ""
                                                    "curl -X ${selectedVideo.method} \"${selectedVideo.url}\" $headerOptions"
                                                }
                                                
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "cURL COMMAND",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkMode) Color.Gray else Color.DarkGray
                                                        )
                                                        
                                                        Text(
                                                            text = "Copy cURL",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                                            modifier = Modifier
                                                                .clickable { copyToClipboard(curlCommand) }
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isDarkMode) Color(0xFF141419) else Color(0xFFECEFF1))
                                                            .border(1.dp, if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFCFD8DC), RoundedCornerShape(8.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Text(
                                                            text = curlCommand,
                                                            fontSize = 8.5.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF00796B),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF8FAFC))
                                            .border(1.dp, if (isDarkMode) Color(0xFF4B5563) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isDarkMode) Color.Gray else Color.LightGray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Network Sniffer: Play direct video sources or streams to intercept network headers and requests.",
                                                fontSize = 11.sp,
                                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                textAlign = TextAlign.Center,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.fillMaxWidth(0.9f)
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
        }
    }

    // ================= TABS SWITCHER CUSTOM MODAL DIALOG =================
    if (showTabsSwitcherDialog) {
        Dialog(onDismissRequest = { showTabsSwitcherDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkMode) Color(0xFF1E1E22) else Color.White
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browser Tabs (${tabs.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                        )
                        IconButton(onClick = { showTabsSwitcherDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close switcher",
                                tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid lists showing cards representing each open tab
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tabs) { tab ->
                            val isFocused = tab.id == activeTabId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTabId = tab.id
                                        showTabsSwitcherDialog = false
                                    }
                                    .border(
                                        width = if (isFocused) 2.5.dp else 1.dp,
                                        color = if (isFocused) {
                                            if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                                        } else {
                                            if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFocused) {
                                        if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFF1F5F9)
                                    } else {
                                        if (isDarkMode) Color(0xFF252529) else Color.White
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        IconButton(
                                            onClick = { closeTab(tab) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tab.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.url,
                                        fontSize = 10.sp,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 13.sp
                                    )
                                    
                                    // Sniffer label indicator if iframe is detected inside this tab
                                    if (tab.extractedCode.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Embed detected",
                                                color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                createNewTab("about:home")
                                showTabsSwitcherDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Tab", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Close everything and reset to single default tab
                                tabs.clear()
                                val defaultTab = TabState()
                                tabs.add(defaultTab)
                                activeTabId = defaultTab.id
                                showTabsSwitcherDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Close All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ================= HOW TO USE (HELP DIALOG) =================
    if (showHelpDialog) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0x66000000)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White,
                        contentColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    ),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "How to Use Browser",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                            )
                            IconButton(
                                onClick = { showHelpDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Dialog",
                                    tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "1. Use the search bar to type any web address or search term, then click Enter or the search icon.\n\n" +
                                        "2. Create, switch, and close multiple tabs using the sliding Tab list on top or the Tab switcher button.\n\n" +
                                        "3. Play any video, live stream, or media segment on a page to execute sniffing automatically.\n\n" +
                                        "4. Minimize the extraction details panel at the bottom to have a full-screen browsing canvas. Copy iframe feeds directly from either expanded or minimized layouts instantly.",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showHelpDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Got It", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    } // Closes the browser else block

    // Auto-Extraction period ticks for EACH active tab
    LaunchedEffect(tabs) {
        while (true) {
            delay(3000)
            tabs.forEach { tab ->
                if (tab.id == activeTabId) {
                    tab.webViewInstance?.let { runExtractionScript(it) }
                }
            }
        }
    }
}

/**
 * Standard Composable wrapping a persistent WebView representing a single background tab
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TabWebViewContainer(
    tabState: TabState,
    isUrlBlocked: (String) -> Boolean,
    runExtractionScript: (WebView) -> Unit,
    runStorageExtractionScript: (WebView) -> Unit,
    siteDataManager: SiteDataManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNewTabRequested: (TabState) -> Unit,
    onPageVisited: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    // Separation of concerns: Define independent listeners with distinct states and callbacks
    val iframeListener = remember {
        object : OnIframeDiscoveredListener {
            override fun onIframeDiscovered(iframeUrl: String, outerHtml: String) {
                if (tabState.extractedCode != outerHtml) {
                    tabState.extractedCode = outerHtml
                    tabState.extractedSrc = iframeUrl
                    Log.d("SystemA", "Discovered Iframe URL: $iframeUrl")
                }
            }
        }
    }

    val videoListener = remember {
        object : OnDirectVideoDiscoveredListener {
            override fun onDirectVideoDiscovered(
                videoUrl: String,
                source: String,
                method: String?,
                headers: Map<String, String>?,
                isForMainFrame: Boolean?
            ) {
                if (videoUrl.startsWith("blob:", ignoreCase = true)) {
                    return
                }
                val isBlocked = isUrlBlocked(videoUrl)
                val isValidFormat = isVideoFormatRequest(videoUrl) || 
                        source == "DOM Scan Video Tag" || 
                        source == "DOM Scan Source Tag" || 
                        source == "Video Property Assignment" ||
                        source.contains("Abyss", ignoreCase = true) ||
                        source.contains("Decrypted", ignoreCase = true) ||
                        videoUrl.startsWith("data:", ignoreCase = true)
                
                val existingIndex = tabState.extractedVideos.indexOfFirst { it.url == videoUrl }
                if (existingIndex != -1) {
                    val existing = tabState.extractedVideos[existingIndex]
                    if (existing.headers == null && headers != null) {
                        tabState.extractedVideos[existingIndex] = existing.copy(
                            method = method ?: existing.method,
                            headers = headers,
                            isForMainFrame = isForMainFrame ?: existing.isForMainFrame,
                            source = if (existing.source == "DOM Scan" || existing.source.startsWith("DOM")) source else existing.source
                        )
                    }
                } else if (!isBlocked && isValidFormat) {
                    val fallbackHeaders = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
                        "Accept" to "*/*",
                        "Accept-Language" to "en-US,en;q=0.9",
                        "Sec-Fetch-Dest" to "video",
                        "Sec-Fetch-Mode" to "cors"
                    )
                    tabState.extractedVideos.add(
                        ExtractedVideo(
                            url = videoUrl,
                            source = source,
                            method = method ?: "GET",
                            headers = headers ?: fallbackHeaders,
                            isForMainFrame = isForMainFrame ?: false
                        )
                    )
                    Log.d("SystemB", "Discovered Direct Video/Playlists [$source]: $videoUrl")
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (request != null) {
                            val requestUrl = request.url?.toString() ?: ""
                            if (isUrlBlocked(requestUrl)) {
                                Log.d("WebViewClient", "Blocked subframe/request load: $requestUrl")
                                return WebResourceResponse(
                                    "text/html",
                                    "UTF-8",
                                    ByteArrayInputStream("Blocked by Host Security Policy".toByteArray(StandardCharsets.UTF_8))
                                )
                            }

                            // Inject video extraction JS into common video hosting/embedding iframes
                            val lowerUrl = requestUrl.lowercase()
                            val isTargetIframe = lowerUrl.contains("abyss") || 
                                    lowerUrl.contains("abysscdn") || 
                                    lowerUrl.contains("/embed/") || 
                                    lowerUrl.contains("/player/")
                            
                            if (isTargetIframe && request.method.equals("GET", ignoreCase = true)) {
                                try {
                                    val connection = java.net.URL(requestUrl).openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "GET"
                                    request.requestHeaders?.forEach { (key, value) ->
                                        if (!key.equals("Accept-Encoding", ignoreCase = true)) {
                                            connection.setRequestProperty(key, value)
                                        }
                                    }
                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 13; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
                                    connection.connectTimeout = 8000
                                    connection.readTimeout = 8000
                                    
                                    val responseCode = connection.responseCode
                                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                        val contentType = connection.contentType ?: "text/html"
                                        if (contentType.contains("text/html", ignoreCase = true)) {
                                            val charset = if (contentType.contains("charset=", ignoreCase = true)) {
                                                contentType.substringAfter("charset=", "UTF-8").substringBefore(";").trim()
                                            } else {
                                                "UTF-8"
                                            }
                                            val rawBytes = connection.inputStream.use { it.readBytes() }
                                            var htmlContent = String(rawBytes, java.nio.charset.Charset.forName(charset))
                                            
                                            val scriptToInject = """
                                                <script type="text/javascript">
                                                $VIDEO_EXTRACTION_JAVASCRIPT
                                                </script>
                                            """.trimIndent()
                                            
                                            htmlContent = when {
                                                htmlContent.contains("<head>", ignoreCase = true) -> {
                                                    htmlContent.replaceFirst("<head>", "<head>\n$scriptToInject", ignoreCase = true)
                                                }
                                                htmlContent.contains("<html>", ignoreCase = true) -> {
                                                    htmlContent.replaceFirst("<html>", "<html>\n$scriptToInject", ignoreCase = true)
                                                }
                                                else -> {
                                                    scriptToInject + htmlContent
                                                }
                                            }
                                            
                                            return WebResourceResponse(
                                                "text/html",
                                                charset,
                                                ByteArrayInputStream(htmlContent.toByteArray(java.nio.charset.Charset.forName(charset)))
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("WebViewClient", "Failed to inject JS into player subframe: $requestUrl", e)
                                }
                            }

                            // System B Requirements: Intercept network traffic targeting all direct video and playlist formats
                            if (isVideoFormatRequest(requestUrl)) {
                                val reqMethod = request.method
                                val reqHeaders = request.requestHeaders
                                val reqIsMain = request.isForMainFrame
                                view?.post {
                                    videoListener.onDirectVideoDiscovered(
                                        requestUrl,
                                        "Network Interception",
                                        reqMethod,
                                        reqHeaders,
                                        reqIsMain
                                    )
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        if (request != null) {
                            val requestUrl = request.url?.toString() ?: ""
                            if (isUrlBlocked(requestUrl)) {
                                view?.post {
                                    Toast.makeText(view.context, "🚫 Navigation Blocked: Domain is restricted.", Toast.LENGTH_LONG).show()
                                }
                                return true
                            }

                            // Handle standard external application schemes gracefully
                            if (!requestUrl.startsWith("http://") && 
                                !requestUrl.startsWith("https://") && 
                                !requestUrl.startsWith("file://") && 
                                !requestUrl.startsWith("about:") && 
                                !requestUrl.startsWith("javascript:")
                            ) {
                                try {
                                    val context = view?.context
                                    if (context != null) {
                                        val intent = Intent.parseUri(requestUrl, Intent.URI_INTENT_SCHEME)
                                        if (intent != null) {
                                            context.startActivity(intent)
                                            return true
                                        }
                                    }
                                } catch (e: Exception) {
                                    try {
                                        val context = view?.context
                                        if (context != null) {
                                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(requestUrl))
                                            context.startActivity(intent)
                                            return true
                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            }
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        tabState.isWebLoading = true
                        if (url != "about:blank") {
                            tabState.url = url
                        }
                        tabState.webLoadingProgress = 0.1f
                        // Inject standard HTML5 Notification API W3C polyfill
                        view.evaluateJavascript(NOTIFICATION_POLYFILL_JS, null)
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        tabState.isWebLoading = false
                        tabState.webLoadingProgress = 1.0f
                        if (url != "about:blank") {
                            tabState.url = url
                        }
                        tabState.canGoBack = view.canGoBack()
                        tabState.canGoForward = view.canGoForward()
                        
                        val pageTitle = view.title
                        if (!pageTitle.isNullOrEmpty() && url != "about:blank") {
                            tabState.title = pageTitle
                        } else if (tabState.url == "about:home") {
                            tabState.title = "Home"
                        } else {
                            tabState.title = "Easy Stream"
                        }
                        view.evaluateJavascript(NOTIFICATION_POLYFILL_JS, null)
                        runExtractionScript(view)
                        runStorageExtractionScript(view)
                        if (url != "about:blank") {
                            onPageVisited(url)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        tabState.webLoadingProgress = newProgress / 100f
                        if (newProgress > 50) {
                            runExtractionScript(view)
                        }
                    }

                    override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                        try {
                            request.grant(request.resources)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String,
                        callback: android.webkit.GeolocationPermissions.Callback
                    ) {
                        callback.invoke(origin, true, false)
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        val newTab = TabState(initialUrl = "about:blank")
                        onNewTabRequested(newTab)
                        
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            val tempWebView = WebView(view!!.context).apply {
                                webViewClient = object : WebViewClient() {
                                    private var isCaptured = false

                                    private fun handleTargetUrl(targetUrl: String) {
                                        if (isCaptured || targetUrl.isEmpty() || targetUrl == "about:blank") return
                                        isCaptured = true
                                        newTab.url = targetUrl
                                        newTab.title = targetUrl
                                        
                                        // Crucial block: load the url directly if the persistent webView is already active
                                        newTab.webViewInstance?.let { targetWebView ->
                                            targetWebView.post {
                                                targetWebView.loadUrl(targetUrl)
                                            }
                                        }
                                        post { destroy() }
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        url?.let { handleTargetUrl(it) }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val targetUrl = request?.url?.toString() ?: ""
                                        handleTargetUrl(targetUrl)
                                        return true
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        val targetUrl = url ?: ""
                                        handleTargetUrl(targetUrl)
                                        return true
                                    }
                                }
                            }
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                            return true
                        }
                        return false
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    mediaPlaybackRequiresUserGesture = false
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    // Standard Mozilla Firefox Android user agent to ensure premium content and push notification capabilities
                    userAgentString = "Mozilla/5.0 (Android 13; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0"
                }

                // Register separate Javascript Interfaces for System A, System B, and Web Notifications
                addJavascriptInterface(IframeExtractionInterface(iframeListener), "AndroidBridge")
                addJavascriptInterface(VideoExtractionInterface(videoListener), "AndroidVideoBridge")
                addJavascriptInterface(NotificationBridge(context) {
                    onRequestNotificationPermission()
                }, "AndroidNotificationBridge")

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onDetailedStats(
                        domain: String,
                        lsCount: Int, lsSize: Long,
                        ssCount: Int, ssSize: Long,
                        dbCount: Int, dbSize: Long,
                        cacheCount: Int, cacheSize: Long,
                        serviceWorkers: Int
                    ) {
                        coroutineScope.launch {
                            try {
                                siteDataManager.updateDetailedStats(
                                    domain,
                                    lsCount, lsSize,
                                    ssCount, ssSize,
                                    dbCount, dbSize,
                                    cacheCount, cacheSize,
                                    serviceWorkers
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, "AndroidSiteStorageBridge")

                tabState.webViewInstance = this
                val initialLoadUrl = if (tabState.url == "about:home") "about:blank" else tabState.url
                loadUrl(initialLoadUrl)
            }
        },
        update = { webView ->
            tabState.canGoBack = webView.canGoBack()
            tabState.canGoForward = webView.canGoForward()
        },
        modifier = if (tabState.url == "about:home") {
            Modifier.size(0.dp).alpha(0f)
        } else {
            Modifier.fillMaxSize().testTag("webview_${tabState.id}")
        }
    )
}

/**
 * Data class representing a home quick-access preset station item
 */
data class PresetItem(
    val name: String,
    val url: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val brandColor: Color
)

/**
 * Custom modern Jetpack Compose search/dashboard start screen shown when tab is on about:home
 */
@Composable
fun LocalHomeScreen(
    isDarkMode: Boolean,
    supabaseStatusMessage: String,
    isSupabaseConfigured: Boolean,
    isFetchingBlockedDomains: Boolean,
    blockedDomains: List<String>,
    bookmarksList: List<Bookmark> = emptyList(),
    onRefresh: () -> Unit,
    onOpenUrl: (String) -> Unit,
    customWallpaper: String = "",
    shortcutsEnabled: Boolean = true,
    newsFeedEnabled: Boolean = true,
    searchEngine: String = "Google"
) {
    val scrollState = rememberScrollState()
    val primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
    val cardBg = if (isDarkMode) Color(0xFF2E2E33) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    val backgroundBrush = remember(customWallpaper, isDarkMode) {
        when (customWallpaper) {
            "sunset" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFF77F00), Color(0xFFD62828)))
            "cosmic" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF6B46C1), Color(0xFF0F0C1B)))
            "teal" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF00ADB5), Color(0xFF0A141A)))
            "abyss" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF0D0E15), Color(0xFF010103)))
            else -> androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(
                    if (isDarkMode) Color(0xFF18181C) else Color(0xFFF7F9FA),
                    if (isDarkMode) Color(0xFF121214) else Color(0xFFEDF2F7)
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Large glowing launcher emblem
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(primaryColor.copy(alpha = 0.15f))
                .border(2.dp, primaryColor, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "EASY STREAM",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textPrimary,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Web Stream Sniffer & Iframe Extractor",
            fontSize = 13.sp,
            color = textSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Supabase Guard Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF222226) else Color(0xFFF1F5F9)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF2E2E35) else Color(0xFFE2E8F0)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSupabaseConfigured) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isSupabaseConfigured) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SUPABASE SECURITY FILTER",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isSupabaseConfigured) "Active Sync Protection" else "Waiting Setup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isFetchingBlockedDomains
                    ) {
                        if (isFetchingBlockedDomains) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = primaryColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync from server",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = supabaseStatusMessage,
                    fontSize = 11.5.sp,
                    color = textSecondary,
                    lineHeight = 15.sp
                )

                if (isSupabaseConfigured) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0x1A00ADB5) else Color(0xFFE0F7FA))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Shielded Domains (${blockedDomains.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (blockedDomains.isEmpty()) {
                                Text(
                                    text = "No domains in remote blocklist yet. Configure your 'blocked' table jsonb column.",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            } else {
                                Text(
                                    text = blockedDomains.joinToString(", "),
                                    fontSize = 11.sp,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Real-time shield rules download automatically when you provide SUPABASE_URL and SUPABASE_ANON_KEY in your AI Studio secrets configuration settings.",
                        fontSize = 10.5.sp,
                        color = textSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        if (shortcutsEnabled) {
            if (bookmarksList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR SAVED BOOKMARKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                bookmarksList.forEach { bookmark ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onOpenUrl(bookmark.url) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SiteLogoView(
                                url = bookmark.url,
                                domain = "",
                                sizeDp = 36.dp,
                                isDarkMode = isDarkMode,
                                primaryColor = primaryColor
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bookmark.url,
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Go",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Title for Preset Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POPULAR STREAM PLATFORMS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shortcut presets
            val presets = listOf(
                PresetItem("Flive TV", "https://flivetv.com", "Live TV channels, movies & sports streams", Icons.Default.Language, Color(0xFFE50914)),
                PresetItem("YouTube", "https://m.youtube.com", "Search videos, channels, live stream feeds", Icons.Default.Search, Color(0xFFFF0000)),
                PresetItem("Twitch", "https://m.twitch.tv", "Worldwide dynamic gaming broadcasts", Icons.Default.Language, Color(0xFF9146FF)),
                PresetItem("Vimeo", "https://vimeo.com", "Premium dynamic player frameworks", Icons.Default.Language, Color(0xFF1AB7EA)),
                PresetItem("Google", "https://www.google.com", "Lookup stream networks or custom pages", Icons.Default.Search, Color(0xFF4285F4))
            )

            presets.forEach { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onOpenUrl(preset.url) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(preset.brandColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = preset.brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.description,
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Navigate to page",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (newsFeedEnabled) {
            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARTICLES FOR YOU",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val feedItems = listOf(
                Triple("Explore Jetpack Compose 1.8 Release Highlights", "Google Developer Team • 2 hours ago", "https://android-developers.googleblog.com"),
                Triple("Kotlin Multiplatform Case Study: Scale to Millions", "JetBrains Official Blog • 5 hours ago", "https://blog.jetbrains.com/kotlin"),
                Triple("Android 16: Revolutionary Windowing APIs Revealed", "Android Central • 1 day ago", "https://developer.android.com")
            )

            feedItems.forEach { feedItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { onOpenUrl(feedItem.third) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = feedItem.first,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = feedItem.second,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Info Help Tip Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(primaryColor.copy(alpha = 0.06f))
                .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "Stream Extraction Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simply select a platform above or type any stream link directly in the top address bar. Play any video segment, and our engine automatically extracts the embed snippet under the bottom dashboard!",
                    fontSize = 11.sp,
                    color = textSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

/**
 * Custom modern Jetpack Compose bookmarks manager view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarksList: List<Bookmark>,
    onOpenBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredBookmarks = remember(bookmarksList, searchQuery) {
        if (searchQuery.isBlank()) {
            bookmarksList
        } else {
            bookmarksList.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.url.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    val primaryColor = if (isDarkMode) Color(0xFF00ADB5) else Color(0xFF006874)
    val bgColors = if (isDarkMode) Color(0xFF18181C) else Color(0xFFF7F9FA)
    val cardBg = if (isDarkMode) Color(0xFF2E2E33) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bookmarks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E22) else Color.White
                )
            )
        },
        containerColor = bgColors
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
                .padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search bookmarks...", color = textSecondary) },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search icon", 
                        tint = textSecondary 
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "Clear search", 
                                tint = textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = textSecondary.copy(alpha = 0.4f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredBookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "No bookmarks",
                            tint = primaryColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching bookmarks" else "No bookmarks yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching something else" else "Tap the star icon on any webpage to save it here for fast access.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBookmarks, key = { it.id }) { bookmark ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBookmark(bookmark) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SiteLogoView(
                                    url = bookmark.url,
                                    domain = "",
                                    sizeDp = 36.dp,
                                    isDarkMode = isDarkMode,
                                    primaryColor = primaryColor
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bookmark.url,
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteBookmark(bookmark) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete bookmark",
                                        tint = Color(0xFFEF4444),
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
