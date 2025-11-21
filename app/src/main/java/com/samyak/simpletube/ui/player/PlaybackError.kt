package com.samyak.simpletube.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.DarkModeKey
import com.samyak.simpletube.constants.PlayerBackgroundStyleKey
import com.samyak.simpletube.ui.screens.settings.DarkMode
import com.samyak.simpletube.ui.screens.settings.PlayerBackgroundStyle
import com.samyak.simpletube.utils.rememberEnumPreference

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { retry() }
            )
        }
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = when (error.errorCode) {
                2000 -> error.message ?: "This content requires YouTube sign-in"
                2004 -> "Stream unavailable. Retrying with different source..."
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No internet connection"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection timeout"
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found"
                PlaybackException.ERROR_CODE_REMOTE_ERROR -> error.message ?: "Remote server error"
                else -> error.cause?.cause?.message ?: error.message ?: stringResource(R.string.error_unknown)
            },
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
