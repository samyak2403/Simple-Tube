package com.samyak.simpletube.ui.utils

import android.Manifest
import android.os.Build
import com.samyak.simpletube.models.DirectoryTree
import com.samyak.simpletube.utils.LmImageCacheMgr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

const val TAG = "LocalMediaUtils"


const val SCANNER_CRASH_AT_FIRST_ERROR = false // crash at first FFmpeg scanner error. Currently not implemented
const val SYNC_SCANNER = false // true will not use multithreading for scanner
const val MAX_CONCURRENT_JOBS = 4
const val SCANNER_DEBUG = false

const val EXTRACTOR_DEBUG = false
const val DEBUG_SAVE_OUTPUT = false // ignored (will be false) when EXTRACTOR_DEBUG IS false
const val EXTRACTOR_TAG = "MetadataExtractor"

@OptIn(ExperimentalCoroutinesApi::class)
val scannerSession = Dispatchers.IO.limitedParallelism(MAX_CONCURRENT_JOBS)

// stuff to make this work
val MEDIA_PERMISSION_LEVEL =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
const val STORAGE_ROOT = "/storage/"
const val DEFAULT_SCAN_PATH = "/tree/primary:Music\n"
val ARTIST_SEPARATORS = Regex("\\s*;\\s*|\\s*ft\\.\\s*|\\s*feat\\.\\s*|\\s*&\\s*|\\s*,\\s*", RegexOption.IGNORE_CASE)
val uninitializedDirectoryTree = DirectoryTree(STORAGE_ROOT)
private var cachedDirectoryTree: MutableStateFlow<DirectoryTree?> = MutableStateFlow(uninitializedDirectoryTree)

var imageCache: LmImageCacheMgr = LmImageCacheMgr()



fun getDirectoryTree(): MutableStateFlow<DirectoryTree?> {
    return cachedDirectoryTree
}

fun cacheDirectoryTree(new: DirectoryTree?) {
    cachedDirectoryTree.value = new?: uninitializedDirectoryTree
}