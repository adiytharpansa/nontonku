package com.app.nontonku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nontonku.data.NpRepository
import com.app.nontonku.data.VideoItem
import com.app.nontonku.theme.Brand
import com.app.nontonku.theme.Mist
import com.app.nontonku.theme.Slate

@Composable
fun VideoCard(item: VideoItem, onClick: (VideoItem) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable { onClick(item) }.padding(bottom = 18.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Mist)) {
            if (item.thumbnail.isNotBlank()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (item.isLive) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .background(Color(0xFFD81F26), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            } else if (item.durationSec > 0) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text(NpRepository.formatDuration(item.durationSec), color = Color.White, fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
        val meta = buildString {
            append(item.uploader)
            if (item.viewCount >= 0) append(" • ${NpRepository.formatCount(item.viewCount)} x ditonton")
            if (item.uploadDate.isNotBlank()) append(" • ${item.uploadDate}")
        }
        Text(meta, color = Slate, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LoadingBox(text: String = "Memuat…") {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Brand)
        Spacer(Modifier.height(12.dp))
        Text(text, color = Slate)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ada kendala", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Slate, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba lagi") }
    }
}

@Composable
fun EmptyBox(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Slate, fontSize = 14.sp)
    }
}
