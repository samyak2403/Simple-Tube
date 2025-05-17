package com.samyak.simpletube.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.samyak.simpletube.constants.AudioQuality
import com.samyak.simpletube.db.entities.FormatEntity
import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeClient.Companion.IOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.zionhuang.innertube.models.response.PlayerResponse
import com.samyak.simpletube.utils.potoken.PoTokenGenerator
import com.samyak.simpletube.utils.potoken.PoTokenResult
import okhttp3.OkHttpClient
import java.net.SocketTimeoutException
import kotlinx.coroutines.delay

object YTPlayerUtils {

    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.zionhuang.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX.copy(
        clientVersion = "1.20250517.01.00" // Updated client version to latest
    )

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        TVHTML5_SIMPLY_EMBEDDED_PLAYER.copy(
            clientVersion = "2.0",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 5/6.10) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.4 Safari/605.1.15"
        ),
        IOS.copy(clientVersion = "20.20.6"),
        WEB_REMIX.copy(clientVersion = "1.20250510.00.00") // Alternative WEB_REMIX version
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

        // Try to get main player response with retries
        var mainPlayerResponse: PlayerResponse? = null
        var mainException: Exception? = null
        
        for (attempt in 1..2) { // Try up to 2 times
            try {
                mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, webPlayerPot)
                    .getOrThrow()
                break // Success, exit retry loop
            } catch (e: Exception) {
                Log.e(TAG, "[$videoId] Error getting main player response (attempt $attempt): ${e.message}", e)
                mainException = e
                if (attempt < 2) delay(500) // Wait before retry
            }
        }

        var audioConfig = mainPlayerResponse?.playerConfig?.audioConfig
        var videoDetails = mainPlayerResponse?.videoDetails
        var playbackTracking = mainPlayerResponse?.playbackTracking

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null

        var streamPlayerResponse: PlayerResponse? = mainPlayerResponse
        var clientsChecked = 0
        var lastErrorMessage: String? = null
        
        // Try all clients including main client and fallbacks
        val allClients = arrayOf(MAIN_CLIENT) + STREAM_FALLBACK_CLIENTS
        
        for (client in allClients) {
            clientsChecked++
            
            // Skip main client if we already tried it and it failed
            if (client == MAIN_CLIENT && mainPlayerResponse == null) {
                continue
            }
            
            // Skip if client requires login but user is not logged in
            if (client.loginRequired && !isLoggedIn) {
                Log.d(TAG, "[$videoId] Skipping ${client.clientName} as it requires login")
                continue
            }
            
            // Reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            
            try {
                // For main client, use the response we already have
                if (client == MAIN_CLIENT && mainPlayerResponse != null) {
                    streamPlayerResponse = mainPlayerResponse
                } else {
                    // For other clients, make a new request
                    streamPlayerResponse = YouTube.player(videoId, playlistId, client, signatureTimestamp, webPlayerPot)
                        .getOrNull()
                        
                    // If main response failed but this one succeeded, use its metadata
                    if (mainPlayerResponse == null && streamPlayerResponse != null) {
                        audioConfig = streamPlayerResponse.playerConfig?.audioConfig
                        videoDetails = streamPlayerResponse.videoDetails
                        playbackTracking = streamPlayerResponse.playbackTracking
                    }
                }
                
                if (streamPlayerResponse == null) {
                    Log.d(TAG, "[$videoId] Null response from ${client.clientName}")
                    continue
                }
                
                val statusInfo = streamPlayerResponse.playabilityStatus?.let {
                    it.status + (it.reason?.let { " - $it" } ?: "")
                } ?: "null playability status"
                
                Log.d(TAG, "[$videoId] Client: ${client.clientName}, status: $statusInfo")
                
                if (streamPlayerResponse.playabilityStatus?.status == "OK") {
                    // Try to find a suitable audio format
                    format = findFormat(
                        streamPlayerResponse,
                        playedFormat,
                        audioQuality,
                        connectivityManager,
                    )
                    
                    if (format == null) {
                        Log.d(TAG, "[$videoId] [${client.clientName}] No suitable format found")
                        continue
                    }
                    
                    // Try to get stream URL
                    streamUrl = findUrlOrNull(format, videoId)
                    if (streamUrl == null) {
                        Log.d(TAG, "[$videoId] [${client.clientName}] Couldn't extract stream URL")
                        continue
                    }
                    
                    // Get expiration time
                    streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                    if (streamExpiresInSeconds == null) {
                        Log.d(TAG, "[$videoId] [${client.clientName}] Missing expiration time, using default")
                        streamExpiresInSeconds = 14400 // Default to 4 hours (14400 seconds)
                    }
                    
                    // Add pot token if needed
                    if (client.useWebPoTokens && webStreamingPot != null) {
                        streamUrl += "&pot=$webStreamingPot"
                    }
                    
                    // Skip validation for last client to ensure we have at least one option
                    if (client == allClients.last()) {
                        Log.d(TAG, "[$videoId] Using last client ${client.clientName} without validation")
                        break
                    }
                    
                    // Validate URL
                    if (validateStatus(streamUrl)) {
                        Log.d(TAG, "[$videoId] [${client.clientName}] Working stream found")
                        break
                    } else {
                        Log.d(TAG, "[$videoId] [${client.clientName}] URL validation failed")
                    }
                } else {
                    lastErrorMessage = streamPlayerResponse.playabilityStatus?.reason
                    Log.d(TAG, "[$videoId] [${client.clientName}] Playability status not OK: $statusInfo")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "[$videoId] Error with client ${client.clientName}: ${e.message}", e)
            }
        }
        
        // If we couldn't find any working stream
        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response - tried $clientsChecked clients")
        }
        
        if (streamPlayerResponse.playabilityStatus?.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus?.reason ?: "Unknown playability issue",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        
        if (streamExpiresInSeconds == null) {
            Log.w(TAG, "[$videoId] Missing stream expire time, using default")
            streamExpiresInSeconds = 14400 // Default to 4 hours
        }
        
        if (format == null) {
            throw Exception("Could not find format")
        }
        
        if (streamUrl == null) {
            throw Exception("Could not find stream URL")
        }
        
        // At this point, we should have a working stream
        Log.d(TAG, "[$videoId] Stream URL found: $streamUrl")
        
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
        YouTube.player(videoId, playlistId, client = MAIN_CLIENT)

    private fun findFormat(
        playerResponse: PlayerResponse,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats
        
        if (adaptiveFormats.isNullOrEmpty()) {
            Log.d(TAG, "No adaptive formats available")
            return null
        }
        
        return if (playedFormat != null) {
            val matchingFormat = adaptiveFormats.find { it.itag == playedFormat.itag }
            if (matchingFormat == null) {
                Log.d(TAG, "Previously played format not found, using best available format")
                findBestAudioFormat(adaptiveFormats, audioQuality, connectivityManager)
            } else {
                matchingFormat
            }
        } else {
            findBestAudioFormat(adaptiveFormats, audioQuality, connectivityManager)
        }
    }
    
    private fun findBestAudioFormat(
        adaptiveFormats: List<PlayerResponse.StreamingData.Format>,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager
    ): PlayerResponse.StreamingData.Format? {
        val audioFormats = adaptiveFormats.filter { it.isAudio }
        if (audioFormats.isEmpty()) {
            Log.d(TAG, "No audio formats available")
            return null
        }
        
        return audioFormats.maxByOrNull {
            it.bitrate * when (audioQuality) {
                AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                AudioQuality.HIGH -> 1
                AudioQuality.LOW -> -1
            } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
        }
    }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .addHeader("Referer", "https://www.youtube.com/")
                
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Log.d(TAG, "URL validation result: $isSuccessful (${response.code})")
            return isSuccessful
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout validating URL: ${e.message}")
            // On timeout, still return true to try the URL anyway
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating URL: ${e.message}")
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