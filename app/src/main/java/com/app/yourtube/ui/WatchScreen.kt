package com.app.yourtube.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.yourtube.data.CommentItem
import com.app.yourtube.data.VideoRepository
import com.app.yourtube.data.WatchDetail
import com.app.yourtube.theme.Mist
import com.app.yourtube.theme.Slate

private const val MAX_RELATED = 12
private const val MAX_COMMENTS = 30

@Composable
fun WatchScreen(vm: MainViewModel) {
    val watch by vm.watch.collectAsState()
    when (val state = watch) {
        is LoadState.Loading -> LoadingBox("Memuat video…")
        is LoadState.Error -> ErrorBox(state.message) { vm.retryWatch() }
        is LoadState.Content -> WatchContent(vm, state.data)
    }
}

@Composable
private fun WatchContent(vm: MainViewModel, detail: WatchDetail) {
    val controller = rememberPlayerController(detail)
    val fullscreen by vm.fullscreen.collectAsState()
    FullscreenEffect(fullscreen)

    Column(Modifier.fillMaxSize()) {
        val playerBox = if (fullscreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        }
        Box(playerBox.background(Color.Black)) {
            if (detail.hasPlayableStream) {
                VideoPlayerView(
                    controller = controller,
                    qualities = detail.qualities,
                    fullscreen = fullscreen,
                    onToggleFullscreen = { vm.toggleFullscreen() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    "Tidak ada stream yang bisa diputar untuk video ini.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            }
        }
        if (!fullscreen) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                controller.error?.let { message ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { controller.retry() }) { Text("Coba lagi") }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                VideoHeader(vm, detail)
                if (detail.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    DescriptionBox(detail.description)
                }
                if (detail.related.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("Berikutnya", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    detail.related.take(MAX_RELATED).forEach { rel -> VideoCard(rel) { vm.openWatch(it) } }
                }
                Spacer(Modifier.height(8.dp))
                CommentSection(detail)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun VideoHeader(vm: MainViewModel, detail: WatchDetail) {
    val context = LocalContext.current
    val item = detail.item
    val bookmarked by vm.bookmarked.collectAsState()

    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    val meta = buildString {
        if (item.viewCount >= 0) append("${VideoRepository.formatCount(item.viewCount)} x ditonton")
        if (item.uploadDate.isNotBlank()) append(" • ${item.uploadDate}")
        if (detail.likeCount >= 0) append(" • ${VideoRepository.formatCount(detail.likeCount)} suka")
    }
    Text(meta, color = Slate, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clickable { vm.openChannel(item.uploaderUrl) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (detail.uploaderAvatar.isNotBlank()) {
                AsyncImage(
                    model = detail.uploaderAvatar,
                    contentDescription = item.uploader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Mist),
                )
                Spacer(Modifier.size(10.dp))
            }
            Column {
                Text(item.uploader, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (detail.subscriberText.isNotBlank()) {
                    Text(detail.subscriberText, color = Slate, fontSize = 12.sp)
                }
            }
        }
        IconButton(onClick = { vm.toggleBookmark() }) {
            Icon(
                if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "Tandai",
            )
        }
        IconButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.url}")
            }
            context.startActivity(Intent.createChooser(send, "Bagikan video"))
        }) {
            Icon(Icons.Filled.Share, contentDescription = "Bagikan")
        }
    }
}

@Composable
private fun DescriptionBox(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(Mist, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp),
    ) {
        Text("Deskripsi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            fontSize = 13.sp, color = Slate,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CommentSection(detail: WatchDetail) {
    Text("Komentar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    when {
        detail.commentsError != null -> Text(detail.commentsError, color = Slate, fontSize = 13.sp)
        detail.comments.isEmpty() -> Text("Belum ada komentar yang termuat.", color = Slate, fontSize = 13.sp)
        else -> detail.comments.take(MAX_COMMENTS).forEach { CommentRow(it) }
    }
}

@Composable
private fun CommentRow(c: CommentItem) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        if (c.avatar.isNotBlank()) {
            AsyncImage(
                model = c.avatar,
                contentDescription = c.author,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Mist),
            )
            Spacer(Modifier.size(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.author, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c.published.isNotBlank()) {
                    Spacer(Modifier.size(8.dp))
                    Text(c.published, color = Slate, fontSize = 11.sp)
                }
            }
            Text(c.text, fontSize = 13.sp)
            if (c.likeCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Slate, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(VideoRepository.formatCount(c.likeCount), color = Slate, fontSize = 11.sp)
                }
            }
        }
    }
}
