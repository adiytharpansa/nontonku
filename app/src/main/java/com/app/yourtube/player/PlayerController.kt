package com.app.yourtube.player

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.app.yourtube.data.VideoQuality
import com.app.yourtube.data.WatchDetail
import com.app.yourtube.data.userAgentForStream

/**
 * Pemegang ExoPlayer untuk satu video.
 *
 * - Kualitas "Otomatis": DASH → HLS (adaptif) → video+audio terpisah ≤720p → progresif.
 * - Kualitas manual: video-saja + audio digabung ([MergingMediaSource]), lalu rantai di atas.
 * - Bila satu sumber gagal, otomatis lanjut ke sumber berikutnya di posisi yang sama.
 */
@Stable
class PlayerController(context: Context, private val detail: WatchDetail) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setSeekBackIncrementMs(SEEK_STEP_MS)
        .setSeekForwardIncrementMs(SEEK_STEP_MS)
        .build()

    /** Kualitas pilihan pengguna; null = Otomatis. */
    var selected by mutableStateOf<VideoQuality?>(null)
        private set

    /** Pesan galat bila semua sumber gagal; null bila baik-baik saja. */
    var error by mutableStateOf<String?>(null)
        private set

    private class Candidate(val name: String, val create: () -> MediaSource)

    private var candidates: List<Candidate> = emptyList()
    private var index = 0

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val failed = candidates.getOrNull(index)?.name
            Log.w(TAG, "sumber '$failed' gagal: ${error.errorCodeName}", error)
            index++
            if (index < candidates.size) {
                load(player.currentPosition)
            } else {
                this@PlayerController.error =
                    "Putar gagal di perangkat ini (${error.errorCodeName}). " +
                        "Coba ganti kualitas atau jaringan, lalu ketuk Coba lagi."
            }
        }
    }

    init {
        player.addListener(listener)
        player.playWhenReady = true
        candidates = buildCandidates(null)
        load(0)
    }

    fun selectQuality(quality: VideoQuality?) {
        if (quality == selected) return
        selected = quality
        candidates = buildCandidates(quality)
        index = 0
        error = null
        load(player.currentPosition)
    }

    fun retry() {
        error = null
        index = 0
        load(player.currentPosition)
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }

    // ---------- Internal ----------

    private fun load(startPositionMs: Long) {
        val candidate = candidates.getOrNull(index)
        if (candidate == null) {
            error = "Tidak ada stream yang bisa diputar untuk video ini."
            return
        }
        try {
            val source = candidate.create()
            if (startPositionMs > 0 && !detail.item.isLive) {
                player.setMediaSource(source, startPositionMs)
            } else {
                player.setMediaSource(source)
            }
            player.prepare()
            player.playWhenReady = true
            Log.i(TAG, "memutar via '${candidate.name}'")
        } catch (e: Exception) {
            Log.w(TAG, "gagal menyiapkan '${candidate.name}'", e)
            index++
            load(startPositionMs)
        }
    }

    private fun buildCandidates(quality: VideoQuality?): List<Candidate> {
        val list = mutableListOf<Candidate>()
        if (quality != null) mergedCandidate(quality)?.let { list += it }
        detail.dashUrl?.let { url ->
            list += Candidate("dash") {
                DashMediaSource.Factory(httpFactory(url)).createMediaSource(MediaItem.fromUri(url))
            }
        }
        detail.hlsUrl?.let { url ->
            list += Candidate("hls") {
                HlsMediaSource.Factory(httpFactory(url)).createMediaSource(MediaItem.fromUri(url))
            }
        }
        if (quality == null) {
            val auto = detail.qualities.firstOrNull { it.height <= AUTO_MAX_HEIGHT }
                ?: detail.qualities.lastOrNull()
            auto?.let { q -> mergedCandidate(q)?.let { list += it } }
        }
        detail.progressiveUrl?.let { url ->
            list += Candidate("progresif") { progressive(url) }
        }
        return list
    }

    private fun mergedCandidate(quality: VideoQuality): Candidate? {
        val audioUrl = detail.audioUrl ?: return null
        return Candidate("gabungan ${quality.label}") {
            MergingMediaSource(progressive(quality.url), progressive(audioUrl))
        }
    }

    private fun progressive(url: String): MediaSource =
        ProgressiveMediaSource.Factory(httpFactory(url)).createMediaSource(MediaItem.fromUri(url))

    private fun httpFactory(url: String) = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgentForStream(url))
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)

    companion object {
        private const val TAG = "YT_PLAYER"
        private const val SEEK_STEP_MS = 10_000L
        private const val AUTO_MAX_HEIGHT = 720
        const val SEEK_STEP_SECONDS = 10
    }
}
