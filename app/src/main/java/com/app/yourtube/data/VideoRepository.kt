package com.app.yourtube.data

import android.util.Log
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream

/**
 * Semua akses data YouTube lewat NewPipeExtractor, langsung di perangkat.
 * Fungsi-fungsi ini memblokir — panggil dari Dispatchers.IO.
 */
object VideoRepository {
    private const val TAG = "YT_REPO"
    private const val YOUTUBE_SERVICE_ID = 0
    private const val FALLBACK_QUERY = "musik indonesia terbaru"
    private const val UNTITLED = "(tanpa judul)"

    private var initialized = false

    private var searchExtractor: SearchExtractor? = null
    private var searchPage: Page? = null
    private var searchHasNext = false

    // ---------- Inisialisasi ----------

    @Synchronized
    fun init() {
        if (initialized) return
        NewPipe.init(ExtractorDownloader(), Localization("id", "ID"), ContentCountry("ID"))
        initialized = true
        Log.i(TAG, "NewPipe init ok")
    }

    private val service get() = NewPipe.getService(YOUTUBE_SERVICE_ID)

    // ---------- Pemetaan ----------

    private fun bestImage(images: List<Image>?): String =
        images?.lastOrNull()?.url ?: ""

    private fun videoIdFrom(url: String): String {
        val patterns = listOf(
            "[?&]v=([A-Za-z0-9_-]{11})",
            "youtu\\.be/([A-Za-z0-9_-]{11})",
            "shorts/([A-Za-z0-9_-]{11})",
        )
        for (pattern in patterns) {
            Regex(pattern).find(url)?.let { return it.groupValues[1] }
        }
        return url
    }

    private fun mapItem(item: InfoItem): VideoItem? {
        if (item !is StreamInfoItem) return null
        val url = item.url ?: return null
        return VideoItem(
            id = videoIdFrom(url),
            url = url,
            title = item.name ?: UNTITLED,
            uploader = item.uploaderName ?: "",
            uploaderUrl = item.uploaderUrl ?: "",
            thumbnail = bestImage(item.thumbnails),
            durationSec = item.duration,
            viewCount = item.viewCount,
            uploadDate = item.textualUploadDate ?: "",
            isLive = item.streamType == StreamType.LIVE_STREAM,
        )
    }

    // ---------- Beranda ----------

    fun trending(): List<VideoItem> {
        init()
        try {
            val kiosk = service.kioskList.getDefaultKioskExtractor()
            kiosk.fetchPage()
            val items = kiosk.initialPage.items.mapNotNull { mapItem(it) }
            if (items.isNotEmpty()) {
                Log.i(TAG, "trending ok: ${items.size}")
                return items
            }
        } catch (e: Exception) {
            Log.w(TAG, "kiosk gagal, pakai pencarian cadangan", e)
        }
        return search(FALLBACK_QUERY)
    }

    // ---------- Pencarian ----------

    fun search(query: String): List<VideoItem> {
        init()
        val extractor = service.getSearchExtractor(
            query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "",
        )
        extractor.fetchPage()
        val page = extractor.initialPage
        searchExtractor = extractor
        searchPage = page.nextPage
        searchHasNext = page.hasNextPage()
        return page.items.mapNotNull { mapItem(it) }
    }

    fun searchNext(): List<VideoItem> {
        val extractor = searchExtractor ?: return emptyList()
        val page = searchPage ?: return emptyList()
        if (!searchHasNext) return emptyList()
        val result = extractor.getPage(page)
        searchPage = result.nextPage
        searchHasNext = result.hasNextPage()
        return result.items.mapNotNull { mapItem(it) }
    }

    fun hasNextSearchPage(): Boolean = searchHasNext

    fun suggestions(query: String): List<String> = try {
        init()
        service.suggestionExtractor.suggestionList(query) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    // ---------- Putar ----------

    fun watch(url: String): WatchDetail {
        init()
        val info = StreamInfo.getInfo(url)
        val resolvedUrl = info.url ?: url
        val item = VideoItem(
            id = videoIdFrom(resolvedUrl),
            url = resolvedUrl,
            title = info.name ?: UNTITLED,
            uploader = info.uploaderName ?: "",
            uploaderUrl = info.uploaderUrl ?: "",
            thumbnail = bestImage(info.thumbnails),
            durationSec = info.duration,
            viewCount = info.viewCount,
            uploadDate = info.textualUploadDate ?: "",
            isLive = info.streamType == StreamType.LIVE_STREAM,
        )
        val progressive = info.videoStreams
            ?.filter { !it.isVideoOnly && !it.content.isNullOrBlank() }
            ?.maxByOrNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }
        val (comments, commentsError) = loadComments(url)
        val subs = info.uploaderSubscriberCount
        return WatchDetail(
            item = item,
            uploaderAvatar = bestImage(info.uploaderAvatars),
            subscriberText = subscriberText(subs),
            likeCount = info.likeCount,
            description = try { info.description?.content ?: "" } catch (e: Exception) { "" },
            dashUrl = info.dashMpdUrl?.takeIf { it.isNotBlank() },
            hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() },
            progressiveUrl = progressive?.content,
            progressiveLabel = progressive?.resolution ?: "",
            qualities = if (item.isLive) emptyList() else videoQualities(info),
            audioUrl = if (item.isLive) null else bestAudioUrl(info),
            related = info.relatedItems?.mapNotNull { mapItem(it) } ?: emptyList(),
            comments = comments,
            commentsError = commentsError,
        )
    }

    /** Satu stream video-saja terbaik per resolusi/fps, dari yang tertinggi. */
    private fun videoQualities(info: StreamInfo): List<VideoQuality> =
        info.videoOnlyStreams.orEmpty()
            .filter {
                it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    !it.content.isNullOrBlank() && it.height > 0
            }
            .groupBy { it.height to it.fps }
            .values
            .mapNotNull { group -> group.maxByOrNull { codecScore(it) } }
            .sortedWith(compareByDescending<VideoStream> { it.height }.thenByDescending { it.fps })
            .map { s ->
                VideoQuality(
                    label = "${s.height}p" + if (s.fps > 30) s.fps.toString() else "",
                    height = s.height,
                    url = s.content,
                )
            }

    /** Utamakan MP4/H.264 — paling luas didukung dekoder perangkat. */
    private fun codecScore(s: VideoStream): Int =
        (if (s.format?.suffix == "mp4") 2 else 0) +
            (if (s.codec.orEmpty().startsWith("avc1")) 1 else 0)

    /** Audio terbaik (utamakan M4A, trek asli — bukan dubbing). */
    private fun bestAudioUrl(info: StreamInfo): String? {
        val all = info.audioStreams.orEmpty()
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !it.content.isNullOrBlank() }
        val original = all
            .filter { it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL }
            .ifEmpty { all }
        return original
            .maxByOrNull { (if (it.format?.suffix == "m4a") 1_000_000 else 0) + it.averageBitrate }
            ?.content
    }

    private fun loadComments(url: String): Pair<List<CommentItem>, String?> = try {
        val info = CommentsInfo.getInfo(url)
        val comments = info.relatedItems.map { c ->
            CommentItem(
                author = c.uploaderName ?: "",
                text = c.commentText?.content ?: "",
                avatar = bestImage(c.uploaderAvatars),
                likeCount = c.likeCount.toLong(),
                published = try { c.textualUploadDate ?: "" } catch (e: Exception) { "" },
            )
        }
        comments to if (info.isCommentsDisabled) "Komentar dimatikan untuk video ini." else null
    } catch (e: Exception) {
        Log.w(TAG, "komentar gagal", e)
        emptyList<CommentItem>() to "Komentar tidak tersedia untuk video ini."
    }

    // ---------- Channel ----------

    fun channel(url: String): ChannelDetail {
        init()
        val info = ChannelInfo.getInfo(url)
        // Di extractor v0.26.x, video channel tidak lagi menempel di ChannelInfo —
        // diambil dari tab "videos" lewat ChannelTabInfo.
        val videos = try {
            val tab = info.tabs.orEmpty().firstOrNull { t ->
                t.contentFilters?.contains("videos") == true
            }
            if (tab != null) {
                ChannelTabInfo.getInfo(service, tab).relatedItems.mapNotNull { mapItem(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "tab video channel gagal", e)
            emptyList()
        }
        return ChannelDetail(
            name = info.name ?: "",
            avatar = bestImage(info.avatars),
            subscriberText = subscriberText(info.subscriberCount),
            description = info.description ?: "",
            videos = videos,
        )
    }

    // ---------- Pemformatan ----------

    private fun subscriberText(count: Long): String =
        if (count >= 0) formatCount(count) + " subscriber" else ""

    fun formatCount(n: Long): String = when {
        n < 0 -> ""
        n >= 1_000_000_000 -> trimNum(n / 1_000_000_000.0) + " M"
        n >= 1_000_000 -> trimNum(n / 1_000_000.0) + " jt"
        n >= 1_000 -> trimNum(n / 1_000.0) + " rb"
        else -> n.toString()
    }

    private fun trimNum(d: Double): String =
        if (d % 1.0 == 0.0) d.toInt().toString() else String.format("%.1f", d).replace('.', ',')

    fun formatDuration(sec: Long): String {
        if (sec <= 0) return ""
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
