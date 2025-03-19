package com.samyak.simpletube.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.CONTENT_TYPE_HEADER
import com.samyak.simpletube.constants.CONTENT_TYPE_PLAYLIST
import com.samyak.simpletube.constants.GridThumbnailHeight
import com.samyak.simpletube.constants.LibraryViewType
import com.samyak.simpletube.constants.LibraryViewTypeKey
import com.samyak.simpletube.constants.PlaylistFilter
import com.samyak.simpletube.constants.PlaylistFilterKey
import com.samyak.simpletube.constants.PlaylistSortDescendingKey
import com.samyak.simpletube.constants.PlaylistSortType
import com.samyak.simpletube.constants.PlaylistSortTypeKey
import com.samyak.simpletube.constants.PlaylistViewTypeKey
import com.samyak.simpletube.constants.ShowLikedAndDownloadedPlaylist
import com.samyak.simpletube.db.entities.PlaylistEntity
import com.samyak.simpletube.extensions.isSyncEnabled
import com.samyak.simpletube.ui.component.CreatePlaylistDialog
import com.samyak.simpletube.ui.component.AutoPlaylistGridItem
import com.samyak.simpletube.ui.component.AutoPlaylistListItem
import com.samyak.simpletube.ui.component.ChipsRow
import com.samyak.simpletube.ui.component.EmptyPlaceholder
import com.samyak.simpletube.ui.component.HideOnScrollFAB
import com.samyak.simpletube.ui.component.LibraryPlaylistGridItem
import com.samyak.simpletube.ui.component.LibraryPlaylistListItem
import com.samyak.simpletube.ui.component.LocalMenuState
import com.samyak.simpletube.ui.component.SortHeader
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import com.samyak.simpletube.viewmodels.LibraryPlaylistsViewModel

@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    libraryFilterContent: @Composable() (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    var filter by rememberEnumPreference(PlaylistFilterKey, PlaylistFilter.LIBRARY)
    libraryFilterContent?.let { filter = PlaylistFilter.LIBRARY }

    var playlistViewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val libraryViewType by rememberEnumPreference(LibraryViewTypeKey, LibraryViewType.GRID)
    val viewType = if (libraryFilterContent != null) libraryViewType else playlistViewType

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)
    val (showLikedAndDownloadedPlaylist) = rememberPreference(ShowLikedAndDownloadedPlaylist, true)

    val playlists by viewModel.allPlaylists.collectAsState()
    val isSyncingRemotePlaylists by viewModel.isSyncingRemotePlaylists.collectAsState()

    val likedPlaylist = PlaylistEntity(id = "liked", name = stringResource(id = R.string.liked_songs))
    val downloadedPlaylist = PlaylistEntity(id = "downloaded", name = stringResource(id = R.string.downloaded_songs))

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (context.isSyncEnabled()) viewModel.syncPlaylists() }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    val filterContent = @Composable {
        ChipsRow(
            chips = listOf(
                PlaylistFilter.LIBRARY to stringResource(R.string.filter_library),
                PlaylistFilter.DOWNLOADED to stringResource(R.string.filter_downloaded)
            ),
            currentValue = filter,
            onValueUpdate = {
                filter = it
                if (context.isSyncEnabled()) {
                    if (it == PlaylistFilter.LIBRARY) viewModel.syncPlaylists()
                }
            },
            isLoading = { filter ->
                filter == PlaylistFilter.LIBRARY && isSyncingRemotePlaylists
            }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            playlists?.let { playlists ->
                Text(
                    text = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (libraryFilterContent == null) {
                IconButton(
                    onClick = {
                        playlistViewType = playlistViewType.toggle()
                    },
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp)
                ) {
                    Icon(
                        imageVector =
                        when (playlistViewType) {
                            LibraryViewType.LIST -> Icons.AutoMirrored.Rounded.List
                            LibraryViewType.GRID -> Icons.Rounded.GridView
                        },
                        contentDescription = null
                    )
                }
            } else {
                Spacer(Modifier.size(16.dp))
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        libraryFilterContent?.let { it() } ?: filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLikedAndDownloadedPlaylist) {
                        item(
                            key = likedPlaylist.id,
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            AutoPlaylistListItem(
                                playlist = likedPlaylist,
                                thumbnail = Icons.Rounded.Favorite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/${likedPlaylist.id}")
                                    }
                                    .animateItem()
                            )
                        }

                        item(
                            key = downloadedPlaylist.id,
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            AutoPlaylistListItem(
                                playlist = downloadedPlaylist,
                                thumbnail = Icons.Rounded.CloudDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/${downloadedPlaylist.id}")
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    playlists?.let { playlists ->
                        if (playlists.isEmpty() && !showLikedAndDownloadedPlaylist) {
                            item {
                                EmptyPlaceholder(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    text = stringResource(R.string.library_playlist_empty),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = Icons.Rounded.Add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        libraryFilterContent?.let { it() } ?: filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLikedAndDownloadedPlaylist) {
                        item(
                            key = likedPlaylist.id,
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            AutoPlaylistGridItem(
                                playlist = likedPlaylist,
                                thumbnail = Icons.Rounded.Favorite,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/${likedPlaylist.id}")
                                    }
                                    .animateItem()
                            )
                        }

                        item(
                            key = downloadedPlaylist.id,
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            AutoPlaylistGridItem(
                                playlist = downloadedPlaylist,
                                thumbnail = Icons.Rounded.CloudDownload,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/${downloadedPlaylist.id}")
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    playlists?.let { playlists ->
                        if (playlists.isEmpty() && !showLikedAndDownloadedPlaylist) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.queue_music,
                                    text = stringResource(R.string.library_playlist_empty),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            LibraryPlaylistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = Icons.Rounded.Add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                )
            }
        }

    }
}
