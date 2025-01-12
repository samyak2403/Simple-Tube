package com.samyak.simpletube.models

import com.samyak.simpletube.db.entities.FormatEntity
import com.samyak.simpletube.db.entities.Song

/**
 * For passing along song metadata
 */
data class SongTempData(val song: Song, val format: FormatEntity?)