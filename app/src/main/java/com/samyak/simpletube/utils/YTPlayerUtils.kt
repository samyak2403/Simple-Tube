/*
 * Copyright (C) 2025-2026 SimpleTune Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.samyak.simpletube.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.samyak.simpletube.constants.AudioQuality
import com.samyak.simpletube.db.entities.FormatEntity
import com.samyak.simpletube.utils.potoken.PoTokenGenerator
import com.samyak.simpletube.utils.potoken.PoTokenResult
import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeClient.Companion.ANDROID
import com.zionhuang.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.zionhuang.innertube.models.YouTubeClient.Companion.IOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.zionhuang.innertube.models.response.PlayerResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

object YTPlayerUtils {

    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * Create an OkHttpClient configured with the appropriate User-Agent and headers
     * for YouTube / googlevideo media stream requests.
     */
    fun createPlaybackOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url
                val host = url.host
                if (host.contains("googlevideo.com") || host.contains("youtube.com")) {
                    val clientParam = url.queryParameter("c")
                    val userAgent = when (clientParam) {
                        "IOS" -> YouTubeClient.IOS.userAgent
                        "VISIONOS" -> "com.google.ios.youtube/19.29.1 (Apple Vision Pro; visionOS 1.2; gzip)"
                        "ANDROID_VR" -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
                        "ANDROID" -> YouTubeClient.ANDROID.userAgent
                        else -> YouTubeClient.USER_AGENT_WEB
                    }
                    val newRequestBuilder = request.newBuilder()
                        .header("User-Agent", userAgent)
                    if (clientParam != "IOS" && clientParam != "VISIONOS" && clientParam != "ANDROID" && clientParam != "ANDROID_VR") {
                        newRequestBuilder
                            .header("Referer", "https://www.youtube.com/")
                            .header("Origin", "https://www.youtube.com")
                    }
                    chain.proceed(newRequestBuilder.build())
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }

    /**
     * The main client is used for metadata and initial streams.
     * IOS currently provides working direct audio streams without bot detection.
     */
    val MAIN_CLIENT: YouTubeClient = IOS

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID,
        ANDROID_VR_NO_AUTH,
        WEB_REMIX,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Log.d(TAG, "Playback info requested: $videoId")

        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }

        Log.d(TAG, "[$videoId] signatureTimestamp: $signatureTimestamp, isLoggedIn: $isLoggedIn")

        val (webPlayerPot, webStreamingPot) = getWebClientPoTokenOrNull(videoId, sessionId)?.let {
            Pair(it.playerRequestPoToken, it.streamingDataPoToken)
        } ?: Pair(null, null).also {
            Log.w(TAG, "[$videoId] No po token")
        }

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, webPlayerPot)
                .getOrNull()

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null

        var streamPlayerResponse: PlayerResponse? = null
        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                Log.d(TAG, "Trying client: ${MAIN_CLIENT.clientName}")
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                Log.d(TAG, "Trying fallback client: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]

                if (client.loginRequired && !isLoggedIn) {
                    // skip client if it requires login but user is not logged in
                    continue
                }

                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp, webPlayerPot)
                        .getOrNull()
            }

            Log.d(TAG, "[$videoId] stream client: ${client.clientName}, " +
                    "playabilityStatus: ${streamPlayerResponse?.playabilityStatus?.let {
                        it.status + (it.reason?.let { " - $it" } ?: "")
                    }}")

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format =
                    findFormat(
                        streamPlayerResponse,
                        playedFormat,
                        audioQuality,
                        connectivityManager,
                    ) ?: continue
                streamUrl = findUrlOrNull(format, videoId) ?: continue
                streamExpiresInSeconds =
                    streamPlayerResponse.streamingData?.expiresInSeconds ?: 14400

                if (client.useWebPoTokens && webStreamingPot != null) {
                    streamUrl += "&pot=$webStreamingPot"
                }

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    break
                }
                if (validateStatus(streamUrl)) {
                    // working stream found
                    Log.i(TAG, "[$videoId] [${client.clientName}] found working stream")
                    break
                } else {
                    Log.w(TAG, "[$videoId] [${client.clientName}] got bad http status code")
                }
            }
        }
        
        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus.reason ?: "Unknown playback error",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            Log.w(TAG, "[$videoId] Missing stream expire time, using default")
            streamExpiresInSeconds = 14400
        }
        if (format == null) {
            throw Exception("Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("Could not find stream url")
        }

        Log.d(TAG, "[$videoId] stream url: $streamUrl")

        val audioConfig = mainPlayerResponse?.playerConfig?.audioConfig
            ?: streamPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse?.videoDetails
            ?: streamPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse?.playbackTracking
            ?: streamPlayerResponse.playbackTracking

        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history

    private fun findFormat(
        playerResponse: PlayerResponse,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats
        
        if (adaptiveFormats.isNullOrEmpty()) {
            return null
        }
        
        return if (playedFormat != null) {
            val matchingFormat = adaptiveFormats.find { it.itag == playedFormat.itag }
            matchingFormat ?: findBestAudioFormat(adaptiveFormats, audioQuality, connectivityManager)
        } else {
            findBestAudioFormat(adaptiveFormats, audioQuality, connectivityManager)
        }
    }
    
    private fun findBestAudioFormat(
        adaptiveFormats: List<PlayerResponse.StreamingData.Format>,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager
    ): PlayerResponse.StreamingData.Format? =
        adaptiveFormats
            .filter { it.isAudio }
            .maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    fun validateStatus(url: String): Boolean {
        try {
            val httpUrl = url.toHttpUrlOrNull()
            val clientParam = httpUrl?.queryParameter("c")
            val userAgent = when (clientParam) {
                "IOS" -> YouTubeClient.IOS.userAgent
                "VISIONOS" -> "com.google.ios.youtube/19.29.1 (Apple Vision Pro; visionOS 1.2; gzip)"
                "ANDROID_VR" -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
                "ANDROID" -> YouTubeClient.ANDROID.userAgent
                else -> YouTubeClient.USER_AGENT_WEB
            }
            val requestBuilder = okhttp3.Request.Builder()
                .get()
                .url(url)
                .addHeader("Range", "bytes=0-1024")
                .addHeader("User-Agent", userAgent)
            if (clientParam != "IOS" && clientParam != "VISIONOS" && clientParam != "ANDROID" && clientParam != "ANDROID_VR") {
                requestBuilder
                    .addHeader("Referer", "https://www.youtube.com/")
                    .addHeader("Origin", "https://www.youtube.com")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccess = response.isSuccessful || response.code == 206
            response.close()
            Log.d(TAG, "URL validation result: $isSuccess (${response.code})")
            return isSuccess
        } catch (e: Exception) {
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [PoTokenGenerator.getWebClientPoToken] function which reports exceptions
     */
    private fun getWebClientPoTokenOrNull(videoId: String, sessionId: String?): PoTokenResult? {
        if (sessionId == null) {
            Log.d(TAG, "[$videoId] Session identifier is null")
            return null
        }
        try {
            return poTokenGenerator.getWebClientPoToken(videoId, sessionId)
        } catch (e: Exception) {
            reportException(e)
        }
        return null
    }
}