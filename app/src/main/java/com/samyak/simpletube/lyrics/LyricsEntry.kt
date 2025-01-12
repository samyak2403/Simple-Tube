package com.samyak.simpletube.lyrics

data class LyricsEntry(
    val timeStamp: Long,
    val content: String,
    var isTranslation: Boolean = false
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (timeStamp - other.timeStamp).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}