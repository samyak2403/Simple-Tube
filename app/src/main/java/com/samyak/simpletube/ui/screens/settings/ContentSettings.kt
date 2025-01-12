package com.samyak.simpletube.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.AccountChannelHandleKey
import com.samyak.simpletube.constants.AccountEmailKey
import com.samyak.simpletube.constants.AccountNameKey
import com.samyak.simpletube.constants.ContentCountryKey
import com.samyak.simpletube.constants.ContentLanguageKey
import com.samyak.simpletube.constants.CountryCodeToName
import com.samyak.simpletube.constants.InnerTubeCookieKey
import com.samyak.simpletube.constants.LanguageCodeToName
import com.samyak.simpletube.constants.LikedAutoDownloadKey
import com.samyak.simpletube.constants.LikedAutodownloadMode
import com.samyak.simpletube.constants.ProxyEnabledKey
import com.samyak.simpletube.constants.ProxyTypeKey
import com.samyak.simpletube.constants.ProxyUrlKey
import com.samyak.simpletube.constants.SYSTEM_DEFAULT
import com.samyak.simpletube.constants.VisitorDataKey
import com.samyak.simpletube.constants.YtmSyncKey
import com.samyak.simpletube.ui.component.EditTextPreference
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.InfoLabel
import com.samyak.simpletube.ui.component.ListPreference
import com.samyak.simpletube.ui.component.PreferenceEntry
import com.samyak.simpletube.ui.component.PreferenceGroupTitle
import com.samyak.simpletube.ui.component.SwitchPreference
import com.samyak.simpletube.ui.component.TextFieldDialog
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.dataStore
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import com.zionhuang.innertube.utils.parseCookieString
import kotlinx.coroutines.runBlocking
import java.net.Proxy

@SuppressLint("PrivateResource")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)
    val (likedAutoDownload, onLikedAutoDownload) = rememberEnumPreference(LikedAutoDownloadKey, LikedAutodownloadMode.OFF)
    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")

    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")


    // temp vars
    var showToken: Boolean by remember {
        mutableStateOf(false)
    }

    var showTokenEditor by remember {
        mutableStateOf(false)
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(
            title = "ACCOUNT"
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
                    runBlocking {
                        context.dataStore.edit { settings ->
                            settings.remove(InnerTubeCookieKey)
                            settings.remove(VisitorDataKey)
                        }
                    }
                }
            )
        }

        if (showTokenEditor) {
            TextFieldDialog(
                modifier = Modifier,
                initialTextFieldValue = TextFieldValue(innerTubeCookie),
                onDone = { onInnerTubeCookieChange(it) },
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
            icon = { Icon(Icons.Rounded.Sync, null) },
            checked = ytmSync,
            onCheckedChange = onYtmSyncChange,
            isEnabled = isLoggedIn
        )
        ListPreference(
            title = { Text(stringResource(R.string.like_autodownload)) },
            icon = { Icon(Icons.Rounded.Favorite, null) },
            values = listOf(LikedAutodownloadMode.OFF, LikedAutodownloadMode.ON, LikedAutodownloadMode.WIFI_ONLY),
            selectedValue = likedAutoDownload,
            valueText = { when (it) {
                LikedAutodownloadMode.OFF -> stringResource(androidx.compose.ui.R.string.state_off)
                LikedAutodownloadMode.ON -> stringResource(androidx.compose.ui.R.string.state_on)
                LikedAutodownloadMode.WIFI_ONLY -> stringResource(R.string.wifi_only)
            } },
            onValueSelected = onLikedAutoDownload
        )

        PreferenceGroupTitle(
            title = "LOCALIZATION"
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(Icons.Rounded.Language, null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentLanguageChange
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(Icons.Rounded.LocationOn, null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentCountryChange
        )

        PreferenceGroupTitle(
            title = "PROXY"
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange
        )

        if (proxyEnabled) {
            ListPreference(
                title = { Text(stringResource(R.string.proxy_type)) },
                selectedValue = proxyType,
                values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                valueText = { it.name },
                onValueSelected = onProxyTypeChange
            )
            EditTextPreference(
                title = { Text(stringResource(R.string.proxy_url)) },
                value = proxyUrl,
                onValueChange = onProxyUrlChange
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
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
