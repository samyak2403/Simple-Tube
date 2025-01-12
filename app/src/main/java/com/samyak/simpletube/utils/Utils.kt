package com.samyak.simpletube.utils

import android.content.pm.PackageManager
import com.samyak.simpletube.db.entities.Artist
import com.samyak.simpletube.ui.screens.settings.NavigationTab

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

/**
 * Converts the enable tabs list (string) to NavigationTab
 *
 * @param str Encoded string
 */
fun decodeTabString(str: String): List<NavigationTab> {
    return str.toCharArray().map {
        when (it) {
            'H' -> NavigationTab.HOME
            'S' -> NavigationTab.SONG
            'F' -> NavigationTab.FOLDERS
            'A' -> NavigationTab.ARTIST
            'B' -> NavigationTab.ALBUM
            'L' -> NavigationTab.PLAYLIST
            else -> {
                NavigationTab.NULL // this case should never happen. Just shut the compiler up
            }
        }
    }
}

/**
 * Converts the NavigationTab tabs list to string
 *
 * @param list Decoded NavigationTab list
 */
fun encodeTabString(list: List<NavigationTab>): String {
    var encoded = ""
    list.subList(0, list.indexOf(NavigationTab.NULL)).forEach {
        encoded += when (it) {
            NavigationTab.HOME -> "H"
            NavigationTab.SONG -> "S"
            NavigationTab.FOLDERS -> "F"
            NavigationTab.ARTIST -> "A"
            NavigationTab.ALBUM -> "B"
            NavigationTab.PLAYLIST -> "L"
            else -> { "" }
        }
    }

    return encoded
}

/**
 * Find the matching string, if not found the closest super string
 */
fun closestMatch(query: String, stringList: List<Artist>): Artist? {
    // Check for exact match first

    val exactMatch = stringList.find { query.lowercase() == it.artist.name.lowercase() }
    if (exactMatch != null) {
        return exactMatch
    }

    // Check for query as substring in any of the strings
    val substringMatches = stringList.filter { it.artist.name.contains(query) }
    if (substringMatches.isNotEmpty()) {
        return substringMatches.minByOrNull { it.artist.name.length }
    }

    return null
}

/**
 * Convert a number to a string representation
 *
 * The value of the number is denoted with characters from A through J. A being 0, and J being 9. This is prefixed by
 * number of digits the number has (always 2 digits, in the same representation) and succeeded with a null terminator "0"
 * In format:
 * <digit tens><digit ones><value in string form>0
 *
 *
 * For example:
 * 100          -> ADBAA0 ("AD" is "03", which represents this is a AD 3 digit number, "BAA" is "100")
 * 101          -> ADBAB0
 * 1013         -> AEBABD0
 * 9            -> ABJ0
 * 111222333444 -> BCBBBCCCDDDEEE0
 */
fun numberToAlpha(l: Long): String {
    val alphabetMap = ('A'..'J').toList()
    val weh = l.toString()
    val lengthStr = if (weh.length.toInt() < 10) {
        "0" + weh.length.toInt()
    } else {
        weh.length.toInt().toString()
    }

    return (lengthStr + weh + "\u0000").map {
        if (it == '\u0000') {
            "0"
        } else {
            alphabetMap[it.digitToInt()]
        }
    }.joinToString("")
}

/**
 * Check if a package with the specified package name is installed
 */
fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}