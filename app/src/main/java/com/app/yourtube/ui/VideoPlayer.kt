package com.app.yourtube.ui

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.app.yourtube.data.VideoQuality
import com.app.yourtube.data.WatchDetail
import com.app.yourtube.player.PlayerController
import kotlinx.coroutines.delay

/** Membuat [PlayerController] per video; dijeda saat aplikasi ke latar dan dilepas saat layar ditinggalkan. */
@Composable
fun rememberPlayerController(detail: WatchDetail): PlayerController {
    val context = LocalContext.current.applicationContext
    val controller = remember(detail.item.id) { PlayerController(context, detail) }
    DisposableEffect(controller) { onDispose { controller.release() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}

private data class SeekHint(val forward: Boolean, val tick: Int)

/**
 * Pemutar dengan: ketuk sekali = tampil/sembunyi kontrol, ketuk dua kali di sisi kiri/kanan =
 * mundur/maju 10 detik, pilihan kualitas, dan tombol layar penuh.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun VideoPlayerView(
    controller: PlayerController,
    qualities: List<VideoQuality>,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var seekHint by remember { mutableStateOf<SeekHint?>(null) }
    var qualityMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(700)
            seekHint = null
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    val view = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player = controller.player
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        },
                    )
                    val detector = GestureDetector(
                        ctx,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onDown(e: MotionEvent): Boolean = true

                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                if (view.isControllerFullyVisible) view.hideController() else view.showController()
                                return true
                            }

                            override fun onDoubleTap(e: MotionEvent): Boolean {
                                val forward = e.x >= view.width / 2f
                                if (forward) controller.player.seekForward() else controller.player.seekBack()
                                seekHint = SeekHint(forward, (seekHint?.tick ?: 0) + 1)
                                return true
                            }
                        },
                    )
                    setOnTouchListener { _, event -> detector.onTouchEvent(event) }
                }
            },
            update = { it.player = controller.player },
            modifier = Modifier.fillMaxSize(),
        )

        seekHint?.let { hint ->
            Row(
                Modifier
                    .align(if (hint.forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 40.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (hint.forward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("${PlayerController.SEEK_STEP_SECONDS} dtk", color = Color.White, fontSize = 13.sp)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible || qualityMenuOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (qualities.isNotEmpty()) {
                    QualityMenu(
                        controller = controller,
                        qualities = qualities,
                        open = qualityMenuOpen,
                        onOpenChange = { qualityMenuOpen = it },
                    )
                    Spacer(Modifier.size(4.dp))
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (fullscreen) "Keluar layar penuh" else "Layar penuh",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityMenu(
    controller: PlayerController,
    qualities: List<VideoQuality>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
) {
    val selected = controller.selected
    Box {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onOpenChange(true) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(selected?.label ?: "Otomatis", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = open, onDismissRequest = { onOpenChange(false) }) {
            DropdownMenuItem(
                text = { Text("Otomatis") },
                onClick = { controller.selectQuality(null); onOpenChange(false) },
                trailingIcon = { if (selected == null) Icon(Icons.Filled.Check, contentDescription = null) },
            )
            qualities.forEach { q ->
                DropdownMenuItem(
                    text = { Text(q.label) },
                    onClick = { controller.selectQuality(q); onOpenChange(false) },
                    trailingIcon = { if (selected == q) Icon(Icons.Filled.Check, contentDescription = null) },
                )
            }
        }
    }
}
