package com.samyak.simpletube.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.samyak.simpletube.LocalDatabase
import com.samyak.simpletube.LocalNetworkConnected
import com.samyak.simpletube.LocalPlayerConnection
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.ArtistSongSortType
import com.samyak.simpletube.db.entities.Artist
import com.samyak.simpletube.models.toMediaMetadata
import com.samyak.simpletube.playback.queues.ListQueue
import com.samyak.simpletube.ui.component.ArtistListItem
import com.samyak.simpletube.ui.component.GridMenu
import com.samyak.simpletube.ui.component.GridMenuItem
import com.zionhuang.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isNetworkConnected = LocalNetworkConnected.current
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    ArtistListItem(
        artist = artist,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null
                )
            }
        }
    )

    HorizontalDivider()

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        if (artist.songCount > 0) {
            GridMenuItem(
                icon = Icons.Rounded.PlayArrow,
                title = R.string.play
            ) {
                coroutineScope.launch {
                    val songs = withContext(Dispatchers.IO) {
                        database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true).first()
                            .map { it.toMediaMetadata() }
                    }

                    val playlistId = withContext(Dispatchers.IO) {
                        YouTube.artist(artist.id).getOrNull()?.artist?.shuffleEndpoint?.playlistId
                    }

                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                            playlistId = playlistId
                        )
                    )
                }
                onDismiss()
            }
            GridMenuItem(
                icon = Icons.Rounded.Shuffle,
                title = R.string.shuffle
            ) {
                coroutineScope.launch {
                    val songs = withContext(Dispatchers.IO) {
                        database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true).first()
                            .map { it.toMediaMetadata() }
                            .shuffled()
                    }

                    val playlistId = withContext(Dispatchers.IO) {
                        YouTube.artist(artist.id).getOrNull()?.artist?.shuffleEndpoint?.playlistId
                    }

                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                            playlistId = playlistId
                        )
                    )
                }
                onDismiss()
            }
        }
        if (artist.artist.isYouTubeArtist) {
            GridMenuItem(
                icon = Icons.Rounded.Share,
                title = R.string.share
            ) {
                onDismiss()
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/channel/${artist.id}")
                }
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
    }
}
