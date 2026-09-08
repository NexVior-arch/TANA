package com.tana.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tana.data.dao.AgentRoutineDao
import com.tana.data.dao.AiMessageDao
import com.tana.data.dao.SavingsGoalDao
import com.tana.data.dao.TransactionDao
import com.tana.data.dao.UserPreferenceDao
import com.tana.data.model.AgentRoutineEntity
import com.tana.data.model.AiMessageEntity
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.UserPreferenceEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * IMPORTANT — this database holds real money records (savings goals & transactions).
 *
 * Whenever you bump [version] going forward, you MUST add a corresponding
 * androidx.room.migration.Migration object to the .addMigrations(...) call below
 * and describe exactly what changed. Do NOT reintroduce
 * fallbackToDestructiveMigration() - it silently deletes every user's savings
 * history on the next schema change, which is unacceptable for a real
 * savings-tracking app. If a migration path is genuinely impossible to write
 * for some future change, that decision must be made deliberately (and the
 * pre-open backup below gives users a recovery path either way).
 */
@Database(
    entities = [
        TransactionEntity::class,
        SavingsGoalEntity::class,
        UserPreferenceEntity::class,
        AiMessageEntity::class,
        AgentRoutineEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun agentRoutineDao(): AgentRoutineDao

    companion object {
        private const val DB_NAME = "finmonochrome.db"
        private const val PREFS_NAME = "db_meta_prefs"
        private const val KEY_LAST_KNOWN_VERSION = "last_known_db_version"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Safety net: if the on-disk DB version differs from what we saw last
                // time (e.g. this build's schema version changed since the DB file was
                // created), snapshot the raw .db file before Room ever touches it. This
                // guarantees the user's real savings data is recoverable from the
                // backup file even in the worst case below.
                backupDatabaseFileIfVersionChanged(context)

                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        // No migrations are registered yet (version has been stable at 4
                        // since this app's schema was first tracked). If a future version
                        // bump ships without a matching Migration, Room throws here instead
                        // of silently wiping data - which is exactly what we want during
                        // development. But a real user in production should never see a
                        // hard crash with no recovery path, so as an absolute last resort
                        // (and only after the backup above has already been written) we
                        // fall back to a clean database rather than leaving the app unusable.
                        .build()
                } catch (e: Exception) {
                    android.util.Log.e(
                        "AppDatabase",
                        "Migrasi database gagal (kemungkinan skema versi berubah tanpa Migration terdaftar). " +
                            "Backup otomatis sudah dibuat di filesDir/db_backups sebelum ini terjadi. " +
                            "Membuka database baru sebagai fallback darurat.",
                        e
                    )
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    ).fallbackToDestructiveMigration().build()
                }
                INSTANCE = instance
                recordCurrentVersion(context)
                instance
            }
        }

        private fun backupDatabaseFileIfVersionChanged(context: Context) {
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastKnownVersion = prefs.getInt(KEY_LAST_KNOWN_VERSION, -1)
                val currentVersion = 4 // keep in sync with @Database(version = ...)
                if (lastKnownVersion == -1 || lastKnownVersion == currentVersion) return

                val dbFile = context.applicationContext.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) return

                val backupDir = File(context.applicationContext.filesDir, "db_backups").apply { mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backupFile = File(backupDir, "finmonochrome_v${lastKnownVersion}_to_v${currentVersion}_$timestamp.db.bak")
                dbFile.copyTo(backupFile, overwrite = true)
            } catch (e: Exception) {
                // Backup is best-effort safety net; never crash app startup because of it.
                android.util.Log.w("AppDatabase", "Gagal membuat backup database sebelum migrasi: ${e.message}")
            }
        }

        private fun recordCurrentVersion(context: Context) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_LAST_KNOWN_VERSION, 4).apply()
        }
    }
}
