package com.app.yourtube.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
