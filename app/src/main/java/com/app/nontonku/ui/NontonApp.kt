package com.app.nontonku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.nontonku.R
import com.app.nontonku.theme.Paper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NontonApp(vm: MainViewModel) {
    val stack by vm.stack.collectAsState()
    val notice by vm.notice.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val current = stack.last()

    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice!!)
            vm.clearNotice()
        }
    }
    BackHandler(enabled = stack.size > 1) { vm.back() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (current == Screen.WATCH || current == Screen.CHANNEL) {
                        Text(if (current == Screen.WATCH) "Sedang diputar" else "Channel", fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.size(10.dp))
                            Text("NontonKu", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                },
                navigationIcon = {
                    if (current == Screen.WATCH || current == Screen.CHANNEL) {
                        IconButton(onClick = { vm.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
        bottomBar = {
            if (current == Screen.HOME || current == Screen.SEARCH || current == Screen.LIBRARY) {
                NavigationBar(containerColor = Paper) {
                    NavigationBarItem(
                        selected = current == Screen.HOME,
                        onClick = { vm.goTab(Screen.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Beranda") },
                    )
                    NavigationBarItem(
                        selected = current == Screen.SEARCH,
                        onClick = { vm.goTab(Screen.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Cari") },
                    )
                    NavigationBarItem(
                        selected = current == Screen.LIBRARY,
                        onClick = { vm.goTab(Screen.LIBRARY) },
                        icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        label = { Text("Koleksi") },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Row(Modifier.padding(padding)) {
            when (current) {
                Screen.HOME -> HomeScreen(vm)
                Screen.SEARCH -> SearchScreen(vm)
                Screen.WATCH -> WatchScreen(vm)
                Screen.CHANNEL -> ChannelScreen(vm)
                Screen.LIBRARY -> LibraryScreen(vm)
            }
        }
    }
}
