package com.samyak.simpletube.ui.screens

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.samyak.simpletube.BuildConfig
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.AccountChannelHandleKey
import com.samyak.simpletube.constants.AccountEmailKey
import com.samyak.simpletube.constants.AccountNameKey
import com.samyak.simpletube.constants.AutomaticScannerKey
import com.samyak.simpletube.constants.DarkModeKey
import com.samyak.simpletube.constants.DataSyncIdKey
import com.samyak.simpletube.constants.FirstSetupPassed
import com.samyak.simpletube.constants.InnerTubeCookieKey
import com.samyak.simpletube.constants.LibraryFilter
import com.samyak.simpletube.constants.LibraryFilterKey
import com.samyak.simpletube.constants.LocalLibraryEnableKey
import com.samyak.simpletube.constants.LyricTrimKey
import com.samyak.simpletube.constants.NavigationBarHeight
import com.samyak.simpletube.constants.NewInterfaceKey
import com.samyak.simpletube.constants.PureBlackKey
import com.samyak.simpletube.constants.VisitorDataKey
import com.samyak.simpletube.db.entities.ArtistEntity
import com.samyak.simpletube.db.entities.Song
import com.samyak.simpletube.db.entities.SongEntity
import com.samyak.simpletube.extensions.move
import com.samyak.simpletube.ui.component.ChipsLazyRow
import com.samyak.simpletube.ui.component.EnumListPreference
import com.samyak.simpletube.ui.component.InfoLabel
import com.samyak.simpletube.ui.component.PreferenceEntry
import com.samyak.simpletube.ui.component.ResizableIconButton
import com.samyak.simpletube.ui.component.SongListItem
import com.samyak.simpletube.ui.component.SwitchPreference
import com.samyak.simpletube.ui.component.TextFieldDialog
import com.samyak.simpletube.ui.screens.settings.DarkMode
import com.samyak.simpletube.ui.screens.settings.NavigationTab
import com.samyak.simpletube.utils.decodeTabString
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import com.zionhuang.innertube.utils.parseCookieString
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun SetupWizard(
    navController: NavController,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val uriHandler = LocalUriHandler.current

    val (firstSetupPassed, onFirstSetupPassedChange) = rememberPreference(FirstSetupPassed, defaultValue = false)

    // content prefs
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (newInterfaceStyle, onNewInterfaceStyleChange) = rememberPreference(key = NewInterfaceKey, defaultValue = true)
    var filter by rememberEnumPreference(LibraryFilterKey, LibraryFilter.ALL)


    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (ytmSync, onYtmSyncChange) = rememberPreference(LyricTrimKey, defaultValue = true)

    // local media prefs
    val (localLibEnable, onLocalLibEnableChange) = rememberPreference(LocalLibraryEnableKey, defaultValue = true)
    val (autoScan, onAutoScanChange) = rememberPreference(AutomaticScannerKey, defaultValue = false)

    var position by remember {
        mutableIntStateOf(0)
    }

    val MAX_POS = 4

    if (position > 0) {
        BackHandler {
            position -= 1
        }
    }

    if (firstSetupPassed) {
        navController.navigateUp()
    }

    val navBar = @Composable {
        // nav bar
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (position > 0) {
                        position -= 1
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            ) {
                Text(
                    text = stringResource(R.string.action_back),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                    contentDescription = null
                )
            }

            LinearProgressIndicator(
                progress = { position.toFloat() / MAX_POS },
//                color = ProgressIndicatorDefaults.linearColor,
//                trackColor = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Butt,
                drawStopIndicator = {},
                modifier = Modifier
                    .weight(1f, false)
                    .height(8.dp)  // Height of the progress bar
                    .padding(2.dp),  // Add some padding at the top
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (position == 1) {
                        filter = LibraryFilter.ALL // hax
                    }

                    if (position < MAX_POS) {
                        position += 1
                    }

                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            ) {
                Text(
                    text = stringResource(R.string.action_next),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = null
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (position > 0 && position < MAX_POS) {
                Box(
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        navBar()
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        top = 0.dp,
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                )
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp))

                when (position) {
                    0 -> { // landing page
                        Image(
                            painter = painterResource(R.drawable.launcher_monochrome),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary, BlendMode.SrcIn),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        NavigationBarDefaults.Elevation
                                    )
                                )
                                .clickable { }
                        )
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(R.string.oobe_welcome_message),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicVideo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = stringResource(R.string.oobe_ytm_content_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotInterested,
                                    tint = Color.Red,
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.oobe_ad_free_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = stringResource(R.string.oobe_ytm_sync_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SdCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.oobe_local_playback_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }


                        // maybe add quick restore from backup here
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    navController.navigate("settings/backup_restore")
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.oobe_use_backup),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            TextButton(
                                onClick = {
                                    onFirstSetupPassedChange(true)
                                    navController.navigateUp()
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.action_skip),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // appearance
                    1 -> {
                        val dummySong = Song(
                            artists = listOf(
                                ArtistEntity(
                                    id = "uwu",
                                    name = "Artist",
                                    isLocal = true
                                )
                            ),
                            song = SongEntity(
                                id = "owo",
                                title = "Title",
                                duration = 310,
                                inLibrary = LocalDateTime.now(),
                                isLocal = true,
                                localPath = "/storage"
                            ),
                        )

                        val dummySongs = ArrayList<Song>()
                        for (i in 0..4) {
                            dummySongs.add(dummySong)
                        }

                        Text(
                            text = stringResource(R.string.grp_interface),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        // interface style
                        SwitchPreference(
                            title = { Text(stringResource(R.string.new_interface)) },
                            icon = { Icon(Icons.Rounded.Palette, null) },
                            checked = newInterfaceStyle,
                            onCheckedChange = onNewInterfaceStyleChange
                        )

                        // light/dark theme
                        EnumListPreference(
                            title = { Text(stringResource(R.string.dark_theme)) },
                            icon = { Icon(Icons.Rounded.DarkMode, null) },
                            selectedValue = darkMode,
                            onValueSelected = onDarkModeChange,
                            valueText = {
                                when (it) {
                                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                                }
                            }
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.pure_black)) },
                            icon = { Icon(Icons.Rounded.Contrast, null) },
                            checked = pureBlack,
                            onCheckedChange = onPureBlackChange
                        )

                        Column(
                            Modifier.background(MaterialTheme.colorScheme.secondary.copy(0.2f))
                        ) {
                            Spacer(Modifier.height(16.dp))

                            if (newInterfaceStyle) {
                                // for new layout
                                val filterString = when (filter) {
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.FOLDERS -> stringResource(R.string.folders)
                                    LibraryFilter.ALL -> ""
                                }

                                val defaultFilter: Collection<Pair<LibraryFilter, String>> =
                                    decodeTabString("HSABL").map {
                                        when (it) {
                                            NavigationTab.ALBUM -> LibraryFilter.ALBUMS to stringResource(R.string.albums)
                                            NavigationTab.ARTIST -> LibraryFilter.ARTISTS to stringResource(R.string.artists)
                                            NavigationTab.PLAYLIST -> LibraryFilter.PLAYLISTS to stringResource(R.string.playlists)
                                            NavigationTab.SONG -> LibraryFilter.SONGS to stringResource(R.string.songs)
                                            NavigationTab.FOLDERS -> LibraryFilter.FOLDERS to stringResource(R.string.folders)
                                            else -> LibraryFilter.ALL to stringResource(R.string.home) // there is no all filter, use as null value
                                        }
                                    }.filterNot { it.first == LibraryFilter.ALL }

                                val chips = remember { SnapshotStateList<Pair<LibraryFilter, String>>() }

                                var filterSelected by remember {
                                    mutableStateOf(filter)
                                }

                                LaunchedEffect(Unit) {
                                    if (filter == LibraryFilter.ALL)
                                        chips.addAll(defaultFilter)
                                    else
                                        chips.add(filter to filterString)
                                }

                                val animatorDurationScale = Settings.Global.getFloat(
                                    context.contentResolver,
                                    Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f
                                ).toLong()

                                suspend fun animationBasedDelay(value: Long) {
                                    delay(value * animatorDurationScale)
                                }

                                // Update the filters list in a proper way so that the animations of the LazyRow can work.
                                LaunchedEffect(filter) {
                                    val filterIndex = defaultFilter.indexOf(defaultFilter.find { it.first == filter })
                                    val currentPairIndex = if (chips.size > 0) defaultFilter.indexOf(chips[0]) else -1
                                    val currentPair = if (chips.size > 0) chips[0] else null

                                    if (filter == LibraryFilter.ALL) {
                                        defaultFilter.reversed().fastForEachIndexed { index, it ->
                                            val curFilterIndex = defaultFilter.indexOf(it)
                                            if (!chips.contains(it)) {
                                                chips.add(0, it)
                                                if (currentPairIndex > curFilterIndex) animationBasedDelay(100)
                                                else {
                                                    currentPair?.let {
                                                        animationBasedDelay(2)
                                                        chips.move(chips.indexOf(it), 0)
                                                    }
                                                    animationBasedDelay(80 + (index * 30).toLong())
                                                }
                                            }
                                        }
                                        animationBasedDelay(100)
                                        filterSelected = LibraryFilter.ALL
                                    } else {
                                        filterSelected = filter
                                        chips.filter { it.first != filter }
                                            .onEachIndexed { index, it ->
                                                if (chips.contains(it)) {
                                                    chips.remove(it)
                                                    if (index > filterIndex) animationBasedDelay(150 + 30 * index.toLong())
                                                    else animationBasedDelay(80)
                                                }
                                            }
                                    }
                                }

                                // filter chips
                                Row {
                                    ChipsLazyRow(
                                        chips = chips,
                                        currentValue = filter,
                                        onValueUpdate = {
                                            filter = if (filter == LibraryFilter.ALL)
                                                it
                                            else
                                                LibraryFilter.ALL
                                        },
                                        modifier = Modifier.weight(1f),
                                        selected = { it == filterSelected }
                                    )

                                    if (filter != LibraryFilter.SONGS) {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.List,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                            }

                            // sort header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                SortHeaderDummy()

                                Spacer(Modifier.weight(1f))

                                Text(
                                    text = pluralStringResource(R.plurals.n_song, dummySongs.size, dummySongs.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }


                            // sample UI
                            Column {
                                dummySongs.forEach { song ->
                                    SongListItem(
                                        song = song,
                                        onPlay = {},
                                        onSelectedChange = {},
                                        inSelectMode = null,
                                        isSelected = false,
                                        navController = navController,
                                        enableSwipeToQueue = false,
                                        disableShowMenu = true
                                    )
                                }
                            }


                            val navigationItems =
                                if (!newInterfaceStyle) Screens.getScreens("HSABL") else Screens.MainScreensNew
                            NavigationBar(
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier.height(NavigationBarHeight)
                            ) {
                                navigationItems.fastForEach { screen ->
                                    NavigationBarItem(
                                        selected = false,
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = null
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(screen.titleId),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        }
                                    )
                                }
                            }
                        }

                    }

                    // account
                    2 -> {
                        var showToken: Boolean by remember {
                            mutableStateOf(false)
                        }

                        var showTokenEditor by remember {
                            mutableStateOf(false)
                        }

                        Text(
                            text = stringResource(R.string.account),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )


                        PreferenceEntry(
                            title = { Text(if (isLoggedIn) accountName else stringResource(R.string.login)) },
                            description = if (isLoggedIn) {
                                accountEmail.takeIf { it.isNotEmpty() }
                                    ?: accountChannelHandle.takeIf { it.isNotEmpty() }
                            } else null,
                            icon = { Icon(Icons.Rounded.Person, null) },
                            onClick = { navController.navigate("login") }
                        )
                        if (isLoggedIn) {
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.logout)) },
                                icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) },
                                onClick = {
                                    onInnerTubeCookieChange("")
                                }
                            )
                        }
                        if (showTokenEditor) {
                            TextFieldDialog(
                                modifier = Modifier,
                                initialTextFieldValue = TextFieldValue(innerTubeCookie),
                                onDone = { data ->
                                    data.split("\n").forEach {
                                        if (it.startsWith("***INNERTUBE COOKIE*** =")) {
                                            onInnerTubeCookieChange(it.substringAfter("***INNERTUBE COOKIE*** ="))
                                        } else if (it.startsWith("***VISITOR DATA*** =")) {
                                            onVisitorDataChange(it.substringAfter("***VISITOR DATA*** ="))
                                        } else if (it.startsWith("***DATASYNC ID*** =")) {
                                            onDataSyncIdChange(it.substringAfter("***DATASYNC ID*** ="))
                                        } else if (it.startsWith("***ACCOUNT NAME*** =")) {
                                            onAccountNameChange(it.substringAfter("***ACCOUNT NAME*** ="))
                                        } else if (it.startsWith("***ACCOUNT EMAIL*** =")) {
                                            onAccountEmailChange(it.substringAfter("***ACCOUNT EMAIL*** ="))
                                        } else if (it.startsWith("***ACCOUNT CHANNEL HANDLE*** =")) {
                                            onAccountChannelHandleChange(it.substringAfter("***ACCOUNT CHANNEL HANDLE*** ="))
                                        }
                                    }
                                },
                                onDismiss = { showTokenEditor = false },
                                singleLine = false,
                                maxLines = 20,
                                isInputValid = {
                                    it.isNotEmpty() &&
                                            try {
                                                "SAPISID" in parseCookieString(it)
                                                true
                                            } catch (e: Exception) {
                                                false
                                            }
                                },
                                extraContent = {
                                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                                }
                            )
                        }
                        PreferenceEntry(
                            title = {
                                if (showToken) {
                                    Text(stringResource(R.string.token_shown))
                                    Text(
                                        text = if (isLoggedIn) innerTubeCookie else stringResource(R.string.not_logged_in),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1 // just give a preview so user knows it's at least there
                                    )
                                } else {
                                    Text(stringResource(R.string.token_hidden))
                                }
                            },
                            onClick = {
                                if (showToken == false) {
                                    showToken = true
                                } else {
                                    showTokenEditor = true
                                }
                            },
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.ytm_sync)) },
                            icon = { Icon(Icons.Rounded.Lyrics, null) },
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                            isEnabled = isLoggedIn
                        )

                    }

                    // local media
                    3 -> {
                        Text(
                            text = stringResource(R.string.local_player_settings_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )


                        SwitchPreference(
                            title = { Text(stringResource(R.string.local_library_enable_title)) },
                            description = stringResource(R.string.local_library_enable_description),
                            icon = { Icon(Icons.Rounded.SdCard, null) },
                            checked = localLibEnable,
                            onCheckedChange = onLocalLibEnableChange
                        )

                        // automatic scanner
                        SwitchPreference(
                            title = { Text(stringResource(R.string.auto_scanner_title)) },
                            description = stringResource(R.string.auto_scanner_description),
                            icon = { Icon(Icons.Rounded.Autorenew, null) },
                            checked = autoScan,
                            onCheckedChange = onAutoScanChange,
                            isEnabled = localLibEnable
                        )


                        PreferenceEntry(
                            title = { Text(stringResource(R.string.oobe_local_scan_tooltip)) },
                            icon = { Icon(Icons.Rounded.Backup, null) },
                            onClick = {
                                navController.navigate("settings/local")
                            },
                            isEnabled = localLibEnable
                        )
                    }

                    // exiting
                    4 -> {

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(R.string.oobe_complete),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }



                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = stringResource(R.string.info),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(4.dp))


                            Row {
                                IconButton(
                                    onClick = { uriHandler.openUri("https://github.com/DD3Boh/OuterTune") }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.github),
                                        contentDescription = null
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }


                        Row {
                            Text(
                                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) | ${BuildConfig.FLAVOR}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (position == 0 || position == MAX_POS) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    onClick = {
                        if (position == 0) {
                            position += 1
                        } else {
                            onFirstSetupPassedChange(true)
                            navController.navigateUp()
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}


@Composable
private fun SortHeaderDummy(
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.sort_by_name),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false)
                ) {
                    menuExpanded = !menuExpanded
                }
                .padding(horizontal = 4.dp, vertical = 8.dp)
        )

        val dummyOptions = listOf("Artist", "Name", "Date added", "Date modified", "Date released")
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.widthIn(min = 172.dp)
        ) {

            dummyOptions.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = type,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (type == "Name") Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                    }
                )
            }
        }


        ResizableIconButton(
            icon = if (sortDescending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .padding(8.dp),
            onClick = { sortDescending = !sortDescending }
        )

    }
}