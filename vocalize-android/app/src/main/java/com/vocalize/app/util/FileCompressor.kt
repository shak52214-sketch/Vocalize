package com.vocalize.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun zipDirectory(sourceDir: File, outputZip: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entryName = sourceDir.toURI().relativize(file.toURI()).path
                        val entry = ZipEntry(entryName)
                        zos.putNextEntry(entry)
                        BufferedInputStream(FileInputStream(file)).use { bis ->
                            bis.copyTo(zos, bufferSize = BUFFER_SIZE)
                        }
                        zos.closeEntry()
                    }
                }
            }
            true
        }.getOrElse { it.printStackTrace(); false }
    }

    suspend fun zipFiles(files: List<File>, outputZip: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
                files.filter { it.exists() && it.isFile }.forEach { file ->
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    BufferedInputStream(FileInputStream(file)).use { bis ->
                        bis.copyTo(zos, bufferSize = BUFFER_SIZE)
                    }
                    zos.closeEntry()
                }
            }
            true
        }.getOrElse { it.printStackTrace(); false }
    }

    suspend fun unzip(zipFile: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            destinationDir.mkdirs()
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            zis.copyTo(bos, bufferSize = BUFFER_SIZE)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        }.getOrElse { it.printStackTrace(); false }
    }

    fun getZipSize(zipFile: File): Long = zipFile.length()

    fun getTempZipFile(name: String): File {
        val cacheDir = context.cacheDir
        cacheDir.mkdirs()
        return File(cacheDir, "$name.zip")
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}
