package com.example

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URI
import kotlin.coroutines.resume

class SiteDataManager(
    private val context: Context,
    private val dao: SiteStorageMetricsDao
) {
    val allSitesFlow: Flow<List<SiteStorageMetrics>> = dao.getAllSitesFlow()

    /**
     * Periodically or dynamically triggers synchronization of Room records
     * with WebView's native cookie and storage states.
     */
    suspend fun refreshFromSystem() = withContext(Dispatchers.IO) {
        try {
            val webStorageOrigins = suspendCancellableCoroutine<Map<String, WebStorage.Origin>?> { continuation ->
                WebStorage.getInstance().getOrigins { origins ->
                    if (continuation.isActive) {
                        @Suppress("UNCHECKED_CAST")
                        continuation.resume(origins as? Map<String, WebStorage.Origin>)
                    }
                }
            }

            if (webStorageOrigins != null) {
                for ((originStr, origin) in webStorageOrigins) {
                    if (originStr == null || origin == null) continue
                    
                    val uri = try { URI(originStr) } catch (e: Exception) { null }
                    val domain = uri?.host ?: originStr
                    if (domain.isBlank()) continue

                    val realUsage = origin.usage // bytes

                    // Query CookieManager under both protocols as fallback
                    val cookieManager = CookieManager.getInstance()
                    val cookieStrHttps = cookieManager.getCookie("https://$domain") ?: ""
                    val cookieStrHttp = cookieManager.getCookie("http://$domain") ?: ""
                    val cookieStr = if (cookieStrHttps.isNotBlank()) cookieStrHttps else cookieStrHttp
                    
                    val cookieCount = if (cookieStr.isNotBlank()) cookieStr.split(";").size else 0
                    val cookieSize = cookieStr.toByteArray(Charsets.UTF_8).size.toLong()

                    val existing = dao.getSiteByDomain(domain)
                    val updated = if (existing != null) {
                        existing.copy(
                            totalSize = if (realUsage > 0) realUsage else existing.totalSize,
                            cookieCount = cookieCount,
                            cookieSize = cookieSize,
                            lastModified = System.currentTimeMillis()
                        )
                    } else {
                        SiteStorageMetrics(
                            domain = domain,
                            title = domain,
                            totalSize = realUsage,
                            cookieCount = cookieCount,
                            cookieSize = cookieSize,
                            lastAccessed = System.currentTimeMillis(),
                            lastModified = System.currentTimeMillis()
                        )
                    }
                    dao.insertOrUpdate(updated)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Automatically called whenever a new webpage load completes.
     * Upserts basic host visits with real-time Cookie count and sizes.
     */
    suspend fun recordVisit(url: String, title: String) = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank() || url.startsWith("about:") || url.startsWith("file:")) return@withContext
            val uri = try { URI(url) } catch (e: Exception) { null }
            val domain = uri?.host ?: return@withContext
            if (domain.isBlank()) return@withContext

            val cookieStr = CookieManager.getInstance().getCookie(url) ?: ""
            val cookieCount = if (cookieStr.isNotBlank()) cookieStr.split(";").size else 0
            val cookieSize = cookieStr.toByteArray(Charsets.UTF_8).size.toLong()

            val existing = dao.getSiteByDomain(domain)
            val updated = if (existing != null) {
                existing.copy(
                    title = if (title.isNotBlank() && title != "Connecting..." && title != "Easy Stream") title else existing.title,
                    lastAccessed = System.currentTimeMillis(),
                    cookieCount = cookieCount,
                    cookieSize = cookieSize
                )
            } else {
                SiteStorageMetrics(
                    domain = domain,
                    title = if (title.isNotBlank() && title != "Connecting..." && title != "Easy Stream") title else domain,
                    lastAccessed = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    cookieCount = cookieCount,
                    cookieSize = cookieSize,
                    totalSize = if (cookieSize > 0) cookieSize else 0L
                )
            }
            dao.insertOrUpdate(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Automatically registers advanced stats provided from script execution
     * in active WebViews.
     */
    suspend fun updateDetailedStats(
        domain: String,
        localStorageCount: Int, localStorageSize: Long,
        sessionStorageCount: Int, sessionStorageSize: Long,
        indexedDbCount: Int, indexedDbSize: Long,
        cacheCount: Int, cacheSize: Long,
        serviceWorkers: Int
    ) = withContext(Dispatchers.IO) {
        try {
            if (domain.isBlank()) return@withContext
            val existing = dao.getSiteByDomain(domain)
            val baseSize = existing?.totalSize ?: 0L
            val jsRecordedSize = localStorageSize + sessionStorageSize + indexedDbSize + cacheSize
            val finalTotal = maxOf(baseSize, jsRecordedSize)

            val updated = if (existing != null) {
                existing.copy(
                    localStorageCount = localStorageCount,
                    localStorageSize = localStorageSize,
                    sessionStorageCount = sessionStorageCount,
                    sessionStorageSize = sessionStorageSize,
                    indexedDbCount = indexedDbCount,
                    indexedDbSize = indexedDbSize,
                    cacheStorageCount = cacheCount,
                    cacheStorageSize = cacheSize,
                    serviceWorkerCount = serviceWorkers,
                    totalSize = finalTotal,
                    lastModified = System.currentTimeMillis()
                )
            } else {
                SiteStorageMetrics(
                    domain = domain,
                    title = domain,
                    totalSize = finalTotal,
                    localStorageCount = localStorageCount,
                    localStorageSize = localStorageSize,
                    sessionStorageCount = sessionStorageCount,
                    sessionStorageSize = sessionStorageSize,
                    indexedDbCount = indexedDbCount,
                    indexedDbSize = indexedDbSize,
                    cacheStorageCount = cacheCount,
                    cacheStorageSize = cacheSize,
                    serviceWorkerCount = serviceWorkers,
                    lastAccessed = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
            }
            dao.insertOrUpdate(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Wipes all native WebView storage footprints along with Room records
     * for a requested host domain.
     */
    suspend fun clearSiteData(domain: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Wipe cookies
            val cookieManager = CookieManager.getInstance()
            val urlHttps = "https://$domain"
            val urlHttp = "http://$domain"
            
            cookieManager.setCookie(urlHttps, "")
            cookieManager.setCookie(urlHttp, "")
            cookieManager.flush()

            // 2. Clear native HTML5 databases, local storage via WebStorage API
            val webStorage = WebStorage.getInstance()
            webStorage.deleteOrigin(urlHttps)
            webStorage.deleteOrigin(urlHttp)
            webStorage.deleteOrigin("https://$domain/")
            webStorage.deleteOrigin("http://$domain/")

            // 3. Purge Room local tracking
            dao.deleteByDomain(domain)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Wipes cookies only but preserves other local storage
     */
    suspend fun clearSiteCookiesOnly(domain: String) = withContext(Dispatchers.IO) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie("https://$domain", "")
            cookieManager.setCookie("http://$domain", "")
            cookieManager.flush()

            val existing = dao.getSiteByDomain(domain)
            if (existing != null) {
                val updated = existing.copy(
                    cookieCount = 0,
                    cookieSize = 0L,
                    totalSize = maxOf(0L, existing.totalSize - existing.cookieSize)
                )
                dao.insertOrUpdate(updated)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resets permission rules to default ("Allowed" or system default)
     */
    suspend fun resetSitePermissions(domain: String) = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getSiteByDomain(domain)
            if (existing != null) {
                val updated = existing.copy(
                    locationPermission = "Allowed",
                    cameraPermission = "Allowed",
                    micPermission = "Allowed"
                )
                dao.insertOrUpdate(updated)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears all sites in database and system WebView.
     */
    suspend fun clearAllSitesData() = withContext(Dispatchers.IO) {
        try {
            WebStorage.getInstance().deleteAllData()
            
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            dao.deleteAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
