package com.samyak.simpletube.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.samyak.simpletube.LocalDatabase
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.LocalPlayerConnection
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.HistorySource
import com.samyak.simpletube.constants.InnerTubeCookieKey
import com.samyak.simpletube.db.entities.EventWithSong
import com.samyak.simpletube.extensions.toMediaItem
import com.samyak.simpletube.extensions.togglePlayPause
import com.samyak.simpletube.models.toMediaMetadata
import com.samyak.simpletube.playback.queues.ListQueue
import com.samyak.simpletube.ui.component.ChipsRow
import com.samyak.simpletube.ui.component.HideOnScrollFAB
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.LocalMenuState
import com.samyak.simpletube.ui.component.NavigationTitle
import com.samyak.simpletube.ui.component.SelectHeader
import com.samyak.simpletube.ui.component.SongListItem
import com.samyak.simpletube.ui.component.SwipeToQueueBox
import com.samyak.simpletube.ui.component.YouTubeListItem
import com.samyak.simpletube.ui.menu.YouTubeSongMenu
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.rememberPreference
import com.samyak.simpletube.viewmodels.DateAgo
import com.samyak.simpletube.viewmodels.HistoryViewModel
import com.zionhuang.innertube.utils.parseCookieString
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val historySource by viewModel.historySource.collectAsState()
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }


    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Long>, Long>(
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

    // no multiselect for remote hisory (yet)
    val historyPage by viewModel.historyPage

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    fun dateAgoToString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> context.getString(R.string.today)
            DateAgo.Yesterday -> context.getString(R.string.yesterday)
            DateAgo.ThisWeek -> context.getString(R.string.this_week)
            DateAgo.LastWeek -> context.getString(R.string.last_week)
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    val eventsMap by viewModel.events.collectAsState()
    val filteredEventsMap = remember(eventsMap, query) {
        if (query.text.isEmpty()) eventsMap
        else eventsMap
            .mapValues { (_, songs) ->
                songs.filter { song ->
                    song.song.title.contains(query.text, ignoreCase = true) ||
                            song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
                }
            }
            .filterValues { it.isNotEmpty() }
    }
    val filteredEventIndex: Map<Long, EventWithSong> by remember(filteredEventsMap) {
        derivedStateOf {
            filteredEventsMap.flatMap { it.value }.associateBy { it.event.id }
        }
    }
    LaunchedEffect(filteredEventsMap) {
        selection.fastForEachReversed { eventId ->
            if (filteredEventIndex[eventId] == null) {
                selection.remove(eventId)
            }
        }
    }

    val lazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .union(WindowInsets.ime)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Top)
            )
        ) {
            stickyHeader(
                key = "searchbar"
            ) {
                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        IconButton(
                            onClick = { isSearching = true }
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null
                            )
                        }
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                }
                Spacer(Modifier.height(1.dp)) // for Compose ui 1.8
            }

            item {
                ChipsRow(
                    chips = if (isLoggedIn) listOf(
                        HistorySource.LOCAL to stringResource(R.string.local_history),
                        HistorySource.REMOTE to stringResource(R.string.remote_history),
                    ) else {
                        listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
                    },
                    currentValue = historySource,
                    onValueUpdate = {
                        viewModel.historySource.value = it
                        if (it == HistorySource.REMOTE) {
                            viewModel.fetchRemoteHistory()
                        }
                    }
                )
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                historyPage?.sections?.forEach { section ->
                    stickyHeader {
                        NavigationTitle(
                            title = section.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    items(
                        items = section.songs,
                        key = { it.id }
                    ) { song ->
                        val content: @Composable () -> Unit = {
                            YouTubeListItem(
                                item = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = context.getString(R.string.queue_remote_history),
                                                        items = section.songs.map { it.toMediaMetadata() }
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {

                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }

                                        }
                                    )
                                    .animateItem()
                            )
                        }



                        SwipeToQueueBox(
                            item = song.toMediaItem(),
                            content = { content() },
                            snackbarHostState = snackbarHostState
                        )

                    }
                }
            } else {
                filteredEventsMap.forEach { (dateAgo, eventsGroup) ->
                    stickyHeader {
                        NavigationTitle(
                            title = dateAgoToString(dateAgo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Spacer(modifier = Modifier.width(16.dp)) // why compose no margin...

                            if (inSelectMode) {
                                SelectHeader(
                                    selectedItems = eventsMap.flatMap { group ->
                                        group.value.filter { it.event.id in selection }
                                    }.map { it.song.toMediaMetadata() },
                                    totalItemCount = eventsMap.flatMap { group -> group.value.map { it.song } }.size,
                                    onSelectAll = {
                                        selection.clear()
                                        selection.addAll(eventsMap.flatMap { group ->
                                            group.value.map { it.event.id }
                                        })
                                    },
                                    onDeselectAll = { selection.clear() },
                                    menuState = menuState,
                                    onDismiss = onExitSelectionMode,
                                    onRemoveFromHistory = {
                                        val sel = selection.mapNotNull { eventId ->
                                            filteredEventIndex[eventId]?.event
                                        }
                                        database.query {
                                            sel.forEach {
                                                delete(it)
                                            }
                                        }
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }

                    itemsIndexed(
                        items = eventsGroup,
                    ) { index, event ->
                        SongListItem(
                            song = event.song,
                            onPlay = {
                                if (event.song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "${context.getString(R.string.queue_local_history)}: ${
                                                dateAgoToString(
                                                    dateAgo
                                                )
                                            }",
                                            items = eventsGroup.map { it.song.toMediaMetadata() },
                                            startIndex = index
                                        )
                                    )
                                }
                            },
                            onSelectedChange = {
                                inSelectMode = true
                                if (it) {
                                    selection.add(event.event.id)
                                } else {
                                    selection.remove(event.event.id)
                                }
                            },
                            inSelectMode = inSelectMode,
                            isSelected = selection.contains(event.event.id),
                            navController = navController,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        )
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = filteredEventsMap.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.history),
                        items = filteredEventIndex.values.map { it.song.toMediaMetadata() },
                        startShuffled = true,
                    )
                )
            }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.history)) },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        isSearching = false
                        query = TextFieldValue()
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (!isSearching) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true }
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
