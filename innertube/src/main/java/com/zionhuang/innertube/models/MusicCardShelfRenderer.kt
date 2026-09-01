package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicCardShelfRenderer(
    val title: Runs? = null,
    val subtitle: Runs? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val header: Header? = null,
    val contents: List<Content>? = null,
    val buttons: List<Button> = emptyList(),
    val onTap: NavigationEndpoint? = null,
    val subtitleBadges: List<Badges>? = null,
) {
    @Serializable
    data class Header(
        val musicCardShelfHeaderBasicRenderer: MusicCardShelfHeaderBasicRenderer? = null,
    ) {
        @Serializable
        data class MusicCardShelfHeaderBasicRenderer(
            val title: Runs? = null,
        )
    }

    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
    )
}
