package com.snowsnooks.core.domain

data class NowPlayingResponse(
    val status: Boolean,
    val nowplaying: String,
    val coverart: String,
    val bitrate: String,
    val format: String
) {
    // Extract theme index from track like "TP03" or "TPX"
    fun getThemeIndex(): Int? {
        val regex = """TP(\d{2}|X)""".toRegex()
        val match = regex.find(nowplaying) ?: return null
        val trackNum = match.groupValues[1]

        val trackNumber = when (trackNum) {
            "X" -> 10 // TPX = TP10
            else -> trackNum.toIntOrNull() ?: return null
        }

        // Map track numbers to theme indices (0-4)
        return when (trackNumber) {
            in 1..4 -> 0    // TP01-04 = Theme 1
            in 5..10 -> 1   // TP05-10 = Theme 2
            in 11..15 -> 2  // TP11-15 = Theme 3
            in 16..20 -> 3  // TP16-20 = Theme 4
            in 21..26 -> 4  // TP21-26 = Theme 5
            else -> null
        }
    }
}