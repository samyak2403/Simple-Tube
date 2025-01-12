package com.samyak.simpletube.utils.scanners

import com.samyak.simpletube.db.entities.FormatEntity
import com.samyak.simpletube.models.SongTempData
import java.io.File


/**
 * Returns metadata information
 */
interface MetadataScanner {

    /**
     * Given a path to a file, extract necessary metadata.
     *
     * @param path Full file path
     */
    fun getAllMetadataFromPath(path: String): SongTempData


    /**
     * Given a path to a file, extract necessary metadata.
     *
     * @param file Full file path
     */
    fun getAllMetadataFromFile(file: File): SongTempData
}

/**
 * A wrapper containing extra raw metadata that MediaStore fails to read properly
 */
data class ExtraMetadataWrapper(val artists: String?, val genres: String?, val date: String?, var format: FormatEntity?)