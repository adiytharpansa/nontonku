package com.app.yourtube.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.yourtube.theme.Slate

@Composable
fun ChannelScreen(vm: MainViewModel) {
    val channel by vm.channel.collectAsState()
    when (val state = channel) {
        is LoadState.Loading -> LoadingBox("Memuat channel…")
        is LoadState.Error -> ErrorBox(state.message) { vm.retryChannel() }
        is LoadState.Content -> {
            val ch = state.data
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (ch.avatar.isNotBlank()) {
                            AsyncImage(
                                model = ch.avatar,
                                contentDescription = ch.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(84.dp).clip(CircleShape),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(ch.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (ch.subscriberText.isNotBlank()) {
                            Text(ch.subscriberText, color = Slate, fontSize = 13.sp)
                        }
                        if (ch.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                ch.description.take(400),
                                color = Slate, fontSize = 13.sp,
                                maxLines = 6, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Video", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(ch.videos, key = { it.id + it.url }) { item ->
                    VideoCard(item) { vm.openWatch(it) }
                }
            }
        }
    }
}
