package com.samyak.simpletube.extensions

import com.samyak.simpletube.db.entities.Song

fun List<Song>.getAvailableSongs(isInternetConnected: Boolean): List<Song> {
    if (isInternetConnected) {
        return this
    }
    return filter { it.song.isAvailableOffline() }
}