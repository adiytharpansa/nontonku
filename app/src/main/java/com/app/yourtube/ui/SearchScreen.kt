package com.app.yourtube.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.yourtube.theme.Slate

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
            SuggestionList(suggestions) { vm.submitSearch(it) }
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
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
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
private fun SuggestionList(suggestions: List<String>, onPick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(suggestions) { s ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Slate, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(12.dp))
                Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
