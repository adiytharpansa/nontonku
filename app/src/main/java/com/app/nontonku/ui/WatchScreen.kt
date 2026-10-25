package com.app.nontonku.ui

import android.content.Intent
import android.view.ViewGroup
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.app.nontonku.data.NpRepository
import com.app.nontonku.data.WatchDetail
import com.app.nontonku.theme.Mist
import com.app.nontonku.theme.Slate

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
    val context = LocalContext.current
    val item = detail.item
    val bookmarked by vm.bookmarked.collectAsState()
    var playerError by remember(detail.item.id) { mutableStateOf<String?>(null) }
    var descExpanded by remember { mutableStateOf(false) }

    val sourceKey = detail.dashUrl ?: detail.hlsUrl ?: detail.progressiveUrl ?: ""
    val player = remember(detail.item.id, sourceKey) {
        val exo = ExoPlayer.Builder(context).build()
        val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
        try {
            when {
                detail.dashUrl != null -> {
                    val src = DashMediaSource.Factory(dataSource)
                        .createMediaSource(MediaItem.fromUri(detail.dashUrl))
                    exo.setMediaSource(src)
                }
                detail.hlsUrl != null -> {
                    val src = HlsMediaSource.Factory(dataSource)
                        .createMediaSource(MediaItem.fromUri(detail.hlsUrl))
                    exo.setMediaSource(src)
                }
                detail.progressiveUrl != null -> {
                    exo.setMediaItem(MediaItem.fromUri(detail.progressiveUrl))
                }
            }
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = "Putar gagal di perangkat ini (${error.errorCodeName}). Di HP dengan jaringan biasa, putar memakai alamat stream langsung dari YouTube."
                }
            })
            exo.playWhenReady = true
            exo.prepare()
        } catch (e: Exception) {
            playerError = "Putar gagal disiapkan: ${e.message}"
        }
        exo
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            if (sourceKey.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    "Tidak ada stream yang bisa diputar untuk video ini.",
                    color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            if (playerError != null) {
                Text(playerError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            val meta = buildString {
                if (item.viewCount >= 0) append("${NpRepository.formatCount(item.viewCount)} x ditonton")
                if (item.uploadDate.isNotBlank()) append(" • ${item.uploadDate}")
                if (detail.likeCount >= 0) append(" • ${NpRepository.formatCount(detail.likeCount)} suka")
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
                            model = detail.uploaderAvatar, contentDescription = item.uploader,
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

            if (detail.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth().background(Mist, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .clickable { descExpanded = !descExpanded }.padding(12.dp),
                ) {
                    Text("Deskripsi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        detail.description,
                        fontSize = 13.sp, color = Slate,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (detail.related.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Berikutnya", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                detail.related.take(12).forEach { rel -> VideoCard(rel) { vm.openWatch(it) } }
            }

            Spacer(Modifier.height(8.dp))
            Text("Komentar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (detail.commentsError != null) {
                Text(detail.commentsError, color = Slate, fontSize = 13.sp)
            } else if (detail.comments.isEmpty()) {
                Text("Belum ada komentar yang termuat.", color = Slate, fontSize = 13.sp)
            } else {
                detail.comments.take(30).forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        if (c.avatar.isNotBlank()) {
                            AsyncImage(
                                model = c.avatar, contentDescription = c.author,
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
                                    Text(NpRepository.formatCount(c.likeCount), color = Slate, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
