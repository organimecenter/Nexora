package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ================= ROOM DATA ENTITY =================
@Entity(tableName = "site_storage_metrics")
data class SiteStorageMetrics(
    @PrimaryKey val domain: String, // e.g., "google.com", "youtube.com"
    val title: String = "",
    val totalSize: Long = 0L,         // Total in bytes, combining actual storage & cookie size
    val lastAccessed: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    
    // Storage break-downs
    val cookieCount: Int = 0,
    val cookieSize: Long = 0L,
    
    val localStorageCount: Int = 0,
    val localStorageSize: Long = 0L,
    
    val sessionStorageCount: Int = 0,
    val sessionStorageSize: Long = 0L,
    
    val indexedDbCount: Int = 0,
    val indexedDbSize: Long = 0L,
    
    val cacheStorageCount: Int = 0,
    val cacheStorageSize: Long = 0L,
    
    val serviceWorkerCount: Int = 0,
    
    // Website permissions configuration
    val locationPermission: String = "Allowed", // "Allowed", "Ask first", "Blocked"
    val cameraPermission: String = "Allowed",
    val micPermission: String = "Allowed"
)

// ================= ROOM DATA ACCESS OBJECT =================
@Dao
interface SiteStorageMetricsDao {
    @Query("SELECT * FROM site_storage_metrics WHERE totalSize > 0 OR cookieCount > 0 ORDER BY domain ASC")
    fun getAllSitesFlow(): Flow<List<SiteStorageMetrics>>

    @Query("SELECT * FROM site_storage_metrics WHERE totalSize > 0 OR cookieCount > 0")
    suspend fun getAllSites(): List<SiteStorageMetrics>

    @Query("SELECT * FROM site_storage_metrics WHERE domain = :domain LIMIT 1")
    suspend fun getSiteByDomain(domain: String): SiteStorageMetrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metrics: SiteStorageMetrics)

    @Query("DELETE FROM site_storage_metrics WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM site_storage_metrics")
    suspend fun deleteAll()
}

// ================= ROOM DATABASE =================
@Database(entities = [SiteStorageMetrics::class], version = 1, exportSchema = false)
abstract class SiteDatabase : RoomDatabase() {
    abstract fun siteStorageMetricsDao(): SiteStorageMetricsDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "chrome_site_storage.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
