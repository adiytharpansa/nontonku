package com.app.nontonku.data

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
    val related: List<VideoItem> = emptyList(),
    val comments: List<CommentItem> = emptyList(),
    val commentsError: String? = null,
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
