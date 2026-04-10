package com.vocalize.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object Utils {

    fun generateUniqueId(): String = UUID.randomUUID().toString()

    fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val shortId = generateUniqueId().take(8)
        return "${timestamp}_${shortId}.m4a"
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs

        return when {
            diffMs < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diffMs < TimeUnit.HOURS.toMillis(1) -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
                "${mins}m ago"
            }
            diffMs < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
                "${hours}h ago"
            }
            diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diffMs < TimeUnit.DAYS.toMillis(7) -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestampMs))
            }
            else -> {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestampMs))
            }
        }
    }

    fun formatDateFull(timestampMs: Long): String =
        SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(timestampMs))

    fun formatDateShort(timestampMs: Long): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))

    fun formatTimeOnly(timestampMs: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMs))

    fun formatReminderTime(timestampMs: Long): String =
        SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault()).format(Date(timestampMs))

    fun formatTimeUntil(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = timestampMs - now
        if (diffMs <= 0) return "Due now"

        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

        return when {
            days > 0 -> {
                if (hours > 0) "${days}d ${hours}h" else "${days}d"
            }
            hours > 0 -> {
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
            minutes > 0 -> "${minutes}m"
            else -> "Less than a minute"
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun speedLabel(speed: Float): String = when (speed) {
        0.5f -> "0.5×"
        0.75f -> "0.75×"
        1.0f -> "1×"
        1.25f -> "1.25×"
        1.5f -> "1.5×"
        1.75f -> "1.75×"
        2.0f -> "2×"
        else -> "${speed}×"
    }

    val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    fun dayOfWeekLabel(index: Int): String = when (index) {
        0 -> "Sun"
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        else -> ""
    }

    fun parseDayBitset(days: String?): Set<Int> {
        if (days.isNullOrBlank()) return emptySet()
        return days.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun encodeDayBitset(days: Set<Int>): String = days.sorted().joinToString(",")

    fun autoTitle(durationMs: Long): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return "Voice Memo $time"
    }

    fun mimeTypeForExtension(ext: String): String = when (ext.lowercase()) {
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        else -> "audio/*"
    }
}
