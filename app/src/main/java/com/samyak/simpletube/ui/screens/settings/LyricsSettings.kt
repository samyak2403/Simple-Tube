package com.samyak.simpletube.ui.screens.settings


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.EnableKugouKey
import com.samyak.simpletube.constants.EnableLrcLibKey
import com.samyak.simpletube.constants.LyricFontSizeKey
import com.samyak.simpletube.constants.LyricSourcePrefKey
import com.samyak.simpletube.constants.LyricTrimKey
import com.samyak.simpletube.constants.LyricsTextPositionKey
import com.samyak.simpletube.constants.MultilineLrcKey
import com.samyak.simpletube.ui.component.CounterDialog
import com.samyak.simpletube.ui.component.EnumListPreference
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.PreferenceEntry
import com.samyak.simpletube.ui.component.PreferenceGroupTitle
import com.samyak.simpletube.ui.component.SwitchPreference
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    // state variables and such
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrcLib, onEnableLrcLibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (multilineLrc, onMultilineLrcChange) = rememberPreference(MultilineLrcKey, defaultValue = true)
    val (lyricTrim, onLyricTrimChange) = rememberPreference(LyricTrimKey, defaultValue = false)
    val (lyricFontSize, onLyricFontSizeChange) = rememberPreference(LyricFontSizeKey, defaultValue = 20)


    val (preferLocalLyric, onPreferLocalLyric) = rememberPreference(LyricSourcePrefKey, defaultValue = true)
    var showFontSizeDialog by remember {
        mutableStateOf(false)
    }

    // lyrics font size
    if (showFontSizeDialog) {
        CounterDialog(
            title = stringResource(R.string.lyrics_font_Size),
            initialValue = lyricFontSize,
            upperBound = 32,
            lowerBound = 8,
            unitDisplay = " pt",
            onDismiss = { showFontSizeDialog = false },
            onConfirm = {
                onLyricFontSizeChange(it)
                showFontSizeDialog = false
            },
            onReset = { onLyricFontSizeChange(20) },
            onCancel = { showFontSizeDialog = false }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.grp_lyrics_source)
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            checked = enableLrcLib,
            onCheckedChange = onEnableLrcLibChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange
        )
        // prioritize local lyric files over all cloud providers
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_prefer_local)) },
            description = stringResource(R.string.lyrics_prefer_local_description),
            icon = { Icon(Icons.Rounded.ContentCut, null) },
            checked = preferLocalLyric,
            onCheckedChange = onPreferLocalLyric
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.grp_lyrics_parser)
        )
        // multiline lyrics
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_multiline_title)) },
            description = stringResource(R.string.lyrics_multiline_description),
            icon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
            checked = multilineLrc,
            onCheckedChange = onMultilineLrcChange
        )

        // trim (remove spaces around) lyrics
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_trim_title)) },
            icon = { Icon(Icons.Rounded.ContentCut, null) },
            checked = lyricTrim,
            onCheckedChange = onLyricTrimChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.grp_lyrics_format)
        )
        // lyrics position
        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            selectedValue = lyricsPosition,
            onValueSelected = onLyricsPositionChange,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_font_Size)) },
            description = "$lyricFontSize sp",
            icon = { Icon(Icons.Rounded.TextFields, null) },
            onClick = { showFontSizeDialog = true }
        )
    }


    TopAppBar(
        title = { Text(stringResource(R.string.lyrics_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
