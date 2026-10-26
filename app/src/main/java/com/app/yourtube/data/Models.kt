package com.app.yourtube.data

import kotlinx.serialization.Serializable

@Serializable
data class VideoItem(
    val id: String,
    val url: String,
    val title: String,
    val uploader: String,
    val uploaderUrl: String = "",
    val thumbnail: String = "",
    val durationSec: Long = 0,
    val viewCount: Long = -1,
    val uploadDate: String = "",
    val isLive: Boolean = false,
)

data class WatchDetail(
    val item: VideoItem,
    val uploaderAvatar: String = "",
    val subscriberText: String = "",
    val likeCount: Long = -1,
    val description: String = "",
    val dashUrl: String? = null,
    val hlsUrl: String? = null,
    val progressiveUrl: String? = null,
    val progressiveLabel: String = "",
    /** Pilihan kualitas video-saja (diurutkan dari tertinggi), digabung dengan [audioUrl] saat diputar. */
    val qualities: List<VideoQuality> = emptyList(),
    val audioUrl: String? = null,
    val related: List<VideoItem> = emptyList(),
    val comments: List<CommentItem> = emptyList(),
    val commentsError: String? = null,
) {
    val hasPlayableStream: Boolean
        get() = dashUrl != null || hlsUrl != null || progressiveUrl != null ||
            (qualities.isNotEmpty() && audioUrl != null)
}

/** Satu pilihan kualitas video-saja, mis. "1080p" atau "720p60". */
data class VideoQuality(
    val label: String,
    val height: Int,
    val url: String,
)

data class CommentItem(
    val author: String,
    val text: String,
    val avatar: String = "",
    val likeCount: Long = -1,
    val published: String = "",
)

data class ChannelDetail(
    val name: String,
    val avatar: String = "",
    val subscriberText: String = "",
    val description: String = "",
    val videos: List<VideoItem> = emptyList(),
)
