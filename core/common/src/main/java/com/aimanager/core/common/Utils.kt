package com.aimanager.core.common

import java.security.MessageDigest

object TokenEstimator {
    // Rough estimation: ~4 chars per token for English, ~2 for CJK
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        val cjkCount = text.count { it.code in 0x4E00..0x9FFF || it.code in 0x3040..0x309F }
        val otherCount = text.length - cjkCount
        return (otherCount / 4) + (cjkCount / 2) + 1
    }
}

object HashUtil {
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object TimeUtil {
    fun formatDuration(ms: Long): String = when {
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> "%.1fs".format(ms / 1000.0)
        ms < 3600_000 -> "${ms / 60_000}m ${ms % 60_000 / 1000}s"
        else -> "${ms / 3600_000}h ${ms % 3600_000 / 60_000}m"
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun isToday(timestamp: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        val today = cal.get(java.util.Calendar.DAY_OF_YEAR)
        cal.timeInMillis = timestamp
        val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
        return today == day
    }
}
