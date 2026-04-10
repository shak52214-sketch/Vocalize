package com.vocalize.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recordingsDirectory get() = File(context.filesDir, Constants.RECORDINGS_DIR).apply { mkdirs() }

    fun getRecordingsDir(): File = recordingsDirectory

    fun getAudioDuration(filePath: String): Long {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) { 0L }
    }

    fun deleteAudioFile(filePath: String): Boolean {
        return File(filePath).let { if (it.exists()) it.delete() else false }
    }

    fun fileExists(filePath: String): Boolean = File(filePath).exists()

    fun importAudioFile(inputStream: InputStream, originalName: String): String? {
        return try {
            val extension = originalName.substringAfterLast('.', "m4a")
            val destFile = File(recordingsDirectory, "${System.currentTimeMillis()}_import.$extension")
            FileOutputStream(destFile).use { out -> inputStream.copyTo(out) }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTotalStorageUsedBytes(): Long {
        return recordingsDirectory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun getStorageUsedMb(): Float = getTotalStorageUsedBytes() / (1024f * 1024f)

    fun getAllRecordingFiles(): List<File> = recordingsDirectory.listFiles()?.toList() ?: emptyList()

    fun clearOrphanFiles(validPaths: Set<String>) {
        getAllRecordingFiles().forEach { file ->
            if (file.absolutePath !in validPaths) file.delete()
        }
    }
}
