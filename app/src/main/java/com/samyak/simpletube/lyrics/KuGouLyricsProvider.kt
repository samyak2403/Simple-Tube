package com.samyak.simpletube.lyrics

import android.content.Context
import com.zionhuang.kugou.KuGou
import com.samyak.simpletube.constants.EnableKugouKey
import com.samyak.simpletube.utils.dataStore
import com.samyak.simpletube.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(id: String, title: String, artist: String, duration: Int, callback: (String) -> Unit) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
