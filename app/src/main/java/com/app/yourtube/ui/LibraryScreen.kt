package com.app.yourtube.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.yourtube.theme.Slate

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val history by vm.history.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Riwayat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
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
