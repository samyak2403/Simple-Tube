package com.samyak.simpletube.ui.screens.artist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.LocalPlayerConnection
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.ArtistSongSortDescendingKey
import com.samyak.simpletube.constants.ArtistSongSortType
import com.samyak.simpletube.constants.ArtistSongSortTypeKey
import com.samyak.simpletube.constants.CONTENT_TYPE_HEADER
import com.samyak.simpletube.models.toMediaMetadata
import com.samyak.simpletube.playback.queues.ListQueue
import com.samyak.simpletube.ui.component.HideOnScrollFAB
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.LocalMenuState
import com.samyak.simpletube.ui.component.SelectHeader
import com.samyak.simpletube.ui.component.SongListItem
import com.samyak.simpletube.ui.component.SortHeader
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import com.samyak.simpletube.viewmodels.ArtistSongsViewModel
import com.zionhuang.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistSongsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSongSortTypeKey, ArtistSongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSongSortDescendingKey, true)

    val artist by viewModel.artist.collectAsState()
    val songs by viewModel.songs.collectAsState()

    val lazyListState = rememberLazyListState()

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (inSelectMode) {
                        SelectHeader(
                            selectedItems = selection.mapNotNull { songId ->
                                songs.find { it.id == songId }
                            }.map { it.toMediaMetadata() },
                            totalItemCount = songs.size,
                            onSelectAll = {
                                selection.clear()
                                selection.addAll(songs.map { it.id })
                            },
                            onDeselectAll = { selection.clear() },
                            menuState = menuState,
                            onDismiss = onExitSelectionMode
                        )
                    } else {
                        SortHeader(
                            sortType = sortType,
                            sortDescending = sortDescending,
                            onSortTypeChange = onSortTypeChange,
                            onSortDescendingChange = onSortDescendingChange,
                            sortTypeText = { sortType ->
                                when (sortType) {
                                    ArtistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                    ArtistSongSortType.NAME -> R.string.sort_by_name
                                    ArtistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                }
                            }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, item -> item.id }
            ) { index, song ->
                SongListItem(
                    song = song,
                    onPlay = {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val playlistId = YouTube.artist(artist?.id!!).getOrNull()
                                ?.artist?.shuffleEndpoint?.playlistId

                            // for some reason this get called on the wrong thread and crashes, use main
                            CoroutineScope(Dispatchers.Main).launch {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = artist?.artist?.name,
                                        items = songs.map { it.toMediaMetadata() },
                                        startIndex = index,
                                        playlistId = playlistId
                                    )
                                )
                            }
                        }
                    },
                    onSelectedChange = {
                        inSelectMode = true
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    },
                    inSelectMode = inSelectMode,
                    isSelected = selection.contains(song.id),
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                )
            }
        }

        TopAppBar(
            title = { Text(artist?.artist?.name.orEmpty()) },
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

        HideOnScrollFAB(
            lazyListState = lazyListState,
            icon = Icons.Rounded.Shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = artist?.artist?.name,
                        items = songs.map { it.toMediaMetadata() },
                        startShuffled = true,
                        playlistId = null,
                    )
                )
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }
}