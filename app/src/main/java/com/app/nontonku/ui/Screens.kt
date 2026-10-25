package com.app.nontonku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nontonku.data.NpRepository
import com.app.nontonku.theme.Slate

@Composable
fun HomeScreen(vm: MainViewModel) {
    val home by vm.home.collectAsState()
    val topic by vm.topic.collectAsState()
    Column(Modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            items(MainViewModel.TOPICS.keys.toList()) { label ->
                FilterChip(
                    selected = topic == label,
                    onClick = { vm.loadHome(label) },
                    label = { Text(label) },
                )
                Spacer(Modifier.size(8.dp))
            }
        }
        when (val state = home) {
            is LoadState.Loading -> LoadingBox("Memuat video…")
            is LoadState.Error -> ErrorBox(state.message) { vm.loadHome(topic) }
            is LoadState.Content -> {
                if (state.data.isEmpty()) {
                    EmptyBox("Belum ada video", "Coba topik lain atau periksa koneksi.")
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(state.data, key = { it.id + it.url }) { item ->
                            VideoCard(item) { vm.openWatch(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(vm: MainViewModel) {
    val query by vm.query.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val results by vm.results.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            label = { Text("Cari video") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submitSearch() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (suggestions.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(suggestions) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.submitSearch(s) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Slate, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            return@Column
        }
        when (val state = results) {
            is LoadState.Loading -> LoadingBox("Mencari…")
            is LoadState.Error -> ErrorBox(state.message) { vm.submitSearch() }
            is LoadState.Content -> {
                if (state.data.isEmpty()) {
                    EmptyBox("Cari sesuatu", "Ketik judul, artis, atau topik lalu tekan cari.")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.data, key = { it.id + it.url }) { item ->
                            VideoCard(item) { vm.openWatch(it) }
                        }
                        if (hasMore) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (loadingMore) {
                                        CircularProgressIndicator()
                                    } else {
                                        Button(onClick = { vm.loadMoreSearch() }) { Text("Muat lebih banyak") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelScreen(vm: MainViewModel) {
    val channel by vm.channel.collectAsState()
    when (val state = channel) {
        is LoadState.Loading -> LoadingBox("Memuat channel…")
        is LoadState.Error -> ErrorBox(state.message) { }
        is LoadState.Content -> {
            val ch = state.data
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (ch.avatar.isNotBlank()) {
                            AsyncImage(
                                model = ch.avatar, contentDescription = ch.name,
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
                            Text(ch.description.take(400), color = Slate, fontSize = 13.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
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

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val history by vm.history.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Riwayat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { vm.clearHistory() }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Hapus")
                    }
                }
            }
            if (history.isEmpty()) {
                Text("Belum ada riwayat tontonan.", color = Slate, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        items(history, key = { "h" + it.id }) { item -> VideoCard(item) { vm.openWatch(it) } }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Ditandai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (bookmarks.isEmpty()) {
                Text("Belum ada video yang ditandai. Tekan ikon Tandai di halaman video.", color = Slate, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        items(bookmarks, key = { "b" + it.id }) { item -> VideoCard(item) { vm.openWatch(it) } }
    }
}
