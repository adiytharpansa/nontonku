package com.app.nontonku.data

import android.util.Log
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.search.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Semua akses data YouTube lewat NewPipeExtractor, langsung di perangkat.
 * Fungsi-fungsi ini memblokir — panggil dari Dispatchers.IO.
 */
object NpRepository {
    private const val TAG = "NK_REPO"
    private var initialized = false

    private var searchExtractor: SearchExtractor? = null
    private var searchPage: Page? = null
    private var searchHasNext = false

    @Synchronized
    fun init() {
        if (initialized) return
        NewPipe.init(NpDownloader(), Localization("id", "ID"), ContentCountry("ID"))
        initialized = true
        Log.i(TAG, "NewPipe init ok")
    }

    private fun bestImage(images: List<Image>?): String =
        images?.lastOrNull()?.url ?: ""

    private fun videoIdFrom(url: String): String {
        val m = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(url)
        if (m != null) return m.groupValues[1]
        val s = Regex("youtu\\.be/([A-Za-z0-9_-]{11})").find(url)
        if (s != null) return s.groupValues[1]
        val sh = Regex("shorts/([A-Za-z0-9_-]{11})").find(url)
        if (sh != null) return sh.groupValues[1]
        return url
    }

    private fun mapItem(item: InfoItem): VideoItem? {
        if (item !is StreamInfoItem) return null
        val url = item.url ?: return null
        return VideoItem(
            id = videoIdFrom(url),
            url = url,
            title = item.name ?: "(tanpa judul)",
            uploader = item.uploaderName ?: "",
            uploaderUrl = item.uploaderUrl ?: "",
            thumbnail = bestImage(item.thumbnails),
            durationSec = item.duration,
            viewCount = item.viewCount,
            uploadDate = item.textualUploadDate ?: "",
            isLive = item.streamType == StreamType.LIVE_STREAM,
        )
    }

    fun trending(): List<VideoItem> {
        init()
        try {
            val service = NewPipe.getService(0)
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
        return searchFirst("musik indonesia terbaru")
    }

    private fun searchFirst(query: String): List<VideoItem> {
        init()
        val service = NewPipe.getService(0)
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

    fun search(query: String): List<VideoItem> = searchFirst(query)

    fun searchNext(): List<VideoItem> {
        val extractor = searchExtractor ?: return emptyList()
        val page = searchPage ?: return emptyList()
        if (!searchHasNext) return emptyList()
        val result = extractor.getNextPage(page)
        searchPage = result.nextPage
        searchHasNext = result.hasNextPage()
        return result.items.mapNotNull { mapItem(it) }
    }

    fun hasNextSearchPage(): Boolean = searchHasNext

    fun suggestions(query: String): List<String> = try {
        init()
        val service = NewPipe.getService(0)
        service.suggestionExtractor.getSuggestionsList(query) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun watch(url: String): WatchDetail {
        init()
        val info = StreamInfo.getInfo(url)
        val item = VideoItem(
            id = videoIdFrom(info.url ?: url),
            url = info.url ?: url,
            title = info.name ?: "(tanpa judul)",
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
        var comments: List<CommentItem> = emptyList()
        var commentsError: String? = null
        try {
            val cInfo = CommentsInfo.getInfo(url)
            comments = cInfo.comments.map { c ->
                CommentItem(
                    author = c.author ?: "",
                    text = c.commentText ?: "",
                    avatar = bestImage(c.authorAvatars),
                    likeCount = c.likeCount,
                    published = try { c.textualPublishedDate ?: "" } catch (e: Exception) { "" },
                )
            }
        } catch (e: Exception) {
            commentsError = "Komentar tidak tersedia untuk video ini."
            Log.w(TAG, "komentar gagal", e)
        }
        val subs = info.uploaderSubscriberCount
        return WatchDetail(
            item = item,
            uploaderAvatar = bestImage(info.uploaderAvatars),
            subscriberText = if (subs >= 0) formatCount(subs) + " subscriber" else "",
            likeCount = info.likeCount,
            description = try { info.description?.content ?: "" } catch (e: Exception) { "" },
            dashUrl = info.dashMpdUrl?.takeIf { it.isNotBlank() },
            hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() },
            progressiveUrl = progressive?.content,
            progressiveLabel = progressive?.resolution ?: "",
            related = info.relatedItems?.mapNotNull { mapItem(it) } ?: emptyList(),
            comments = comments,
            commentsError = commentsError,
        )
    }

    fun channel(url: String): ChannelDetail {
        init()
        val info = ChannelInfo.getInfo(url)
        val subs = info.subscriberCount
        return ChannelDetail(
            name = info.name ?: "",
            avatar = bestImage(info.avatars),
            subscriberText = if (subs >= 0) formatCount(subs) + " subscriber" else "",
            description = info.description ?: "",
            videos = info.relatedItems?.mapNotNull { mapItem(it) } ?: emptyList(),
        )
    }

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
