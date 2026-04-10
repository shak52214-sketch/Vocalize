package com.vocalize.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Vocalize").build()
    }

    suspend fun backup(dbFile: File, recordingsDir: File, onProgress: (String) -> Unit): Boolean {
        return try {
            val driveService = getDriveService() ?: return false
            onProgress("Compressing files...")

            val zipBytes = ByteArrayOutputStream()
            ZipOutputStream(zipBytes).use { zip ->
                // Add database
                if (dbFile.exists()) {
                    zip.putNextEntry(ZipEntry("db/${dbFile.name}"))
                    dbFile.inputStream().copyTo(zip)
                    zip.closeEntry()
                }
                // Add recordings
                recordingsDir.listFiles()?.forEach { file ->
                    zip.putNextEntry(ZipEntry("recordings/${file.name}"))
                    file.inputStream().copyTo(zip)
                    zip.closeEntry()
                }
            }

            onProgress("Uploading to Drive...")

            val timestamp = System.currentTimeMillis()
            val fileMetadata = DriveFile().apply {
                name = "vocalize_backup_$timestamp.zip"
                parents = listOf("appDataFolder")
            }

            val content = com.google.api.client.http.ByteArrayContent(
                "application/zip",
                zipBytes.toByteArray()
            )

            driveService.files().create(fileMetadata, content)
                .setFields("id")
                .execute()

            // Keep only last 3 backups
            cleanOldBackups(driveService)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restore(recordingsDir: File, onProgress: (String) -> Unit): Boolean {
        return try {
            val driveService = getDriveService() ?: return false
            onProgress("Finding latest backup...")

            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime)")
                .setOrderBy("createdTime desc")
                .setPageSize(1)
                .execute()

            val latestFile = result.files.firstOrNull() ?: return false
            onProgress("Downloading backup...")

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(latestFile.id)
                .executeMediaAndDownloadTo(outputStream)

            onProgress("Restoring files...")
            recordingsDir.mkdirs()

            ZipInputStream(outputStream.toByteArray().inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name.startsWith("recordings/") -> {
                            val fileName = entry.name.removePrefix("recordings/")
                            val destFile = File(recordingsDir, fileName)
                            FileOutputStream(destFile).use { zip.copyTo(it) }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun cleanOldBackups(driveService: Drive) {
        val files = driveService.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id, name, createdTime)")
            .setOrderBy("createdTime desc")
            .execute().files
        if (files.size > 3) {
            files.drop(3).forEach { driveService.files().delete(it.id).execute() }
        }
    }

    fun getLastBackupTime(): Long {
        val prefs = context.getSharedPreferences("vocalize_prefs", Context.MODE_PRIVATE)
        return prefs.getLong(Constants.PREFS_LAST_BACKUP, 0L)
    }

    fun saveLastBackupTime(time: Long) {
        context.getSharedPreferences("vocalize_prefs", Context.MODE_PRIVATE)
            .edit().putLong(Constants.PREFS_LAST_BACKUP, time).apply()
    }
}
