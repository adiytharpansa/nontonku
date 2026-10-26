package com.app.yourtube.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.yourtube.data.ChannelDetail
import com.app.yourtube.data.LocalStore
import com.app.yourtube.data.VideoRepository
import com.app.yourtube.data.VideoItem
import com.app.yourtube.data.WatchDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { HOME, SEARCH, WATCH, CHANNEL, LIBRARY }

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Content<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)

    private val _stack = MutableStateFlow(listOf(Screen.HOME))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()
    val current: Screen get() = _stack.value.last()

    // Beranda
    private val _home = MutableStateFlow<LoadState<List<VideoItem>>>(LoadState.Loading)
    val home: StateFlow<LoadState<List<VideoItem>>> = _home.asStateFlow()
    private val _topic = MutableStateFlow("Semua")
    val topic: StateFlow<String> = _topic.asStateFlow()

    // Pencarian
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    private val _results = MutableStateFlow<LoadState<List<VideoItem>>>(LoadState.Content(emptyList()))
    val results: StateFlow<LoadState<List<VideoItem>>> = _results.asStateFlow()
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()
    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    private var suggestionJob: Job? = null

    // Putar
    private val _watch = MutableStateFlow<LoadState<WatchDetail>>(LoadState.Loading)
    val watch: StateFlow<LoadState<WatchDetail>> = _watch.asStateFlow()
    private val _watchItem = MutableStateFlow<VideoItem?>(null)
    val watchItem: StateFlow<VideoItem?> = _watchItem.asStateFlow()
    private val _fullscreen = MutableStateFlow(false)
    val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()
    private val _bookmarked = MutableStateFlow(false)
    val bookmarked: StateFlow<Boolean> = _bookmarked.asStateFlow()

    // Channel
    private val _channel = MutableStateFlow<LoadState<ChannelDetail>>(LoadState.Loading)
    val channel: StateFlow<LoadState<ChannelDetail>> = _channel.asStateFlow()

    // Koleksi
    private val _history = MutableStateFlow(store.history())
    val history: StateFlow<List<VideoItem>> = _history.asStateFlow()
    private val _bookmarks = MutableStateFlow(store.bookmarks())
    val bookmarks: StateFlow<List<VideoItem>> = _bookmarks.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var autoFirstPending = false
    private var lastChannelUrl = ""

    init { loadHome("Semua") }

    fun applyAutoFirst(enabled: Boolean) {
        if (enabled) autoFirstPending = true
        maybeAutoFirst()
    }

    private fun maybeAutoFirst() {
        if (!autoFirstPending) return
        val state = _home.value
        if (state is LoadState.Content && state.data.isNotEmpty()) {
            autoFirstPending = false
            openWatch(state.data.first())
        }
    }

    // ---------- Navigasi ----------
    fun goTab(screen: Screen) {
        _fullscreen.value = false
        _stack.value = listOf(screen)
        if (screen == Screen.LIBRARY) refreshLibrary()
    }

    private fun push(screen: Screen) { _stack.value = _stack.value + screen }

    fun back(): Boolean {
        if (_fullscreen.value) {
            _fullscreen.value = false
            return true
        }
        if (_stack.value.size > 1) {
            _stack.value = _stack.value.dropLast(1)
            return true
        }
        return false
    }

    // ---------- Beranda ----------
    fun loadHome(topicLabel: String) {
        _topic.value = topicLabel
        _home.value = LoadState.Loading
        val query = TOPICS[topicLabel]
        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    if (query == null) VideoRepository.trending() else VideoRepository.search(query)
                }
                _home.value = LoadState.Content(items)
                Log.i(TAG, "home[$topicLabel] ok: ${items.size}")
                maybeAutoFirst()
            } catch (e: Exception) {
                Log.w(TAG, "home gagal", e)
                _home.value = LoadState.Error(friendlyError(e))
            }
        }
    }

    // ---------- Pencarian ----------
    fun setQuery(q: String) {
        _query.value = q
        suggestionJob?.cancel()
        if (q.isBlank()) { _suggestions.value = emptyList(); return }
        suggestionJob = viewModelScope.launch {
            delay(300)
            val list = withContext(Dispatchers.IO) { VideoRepository.suggestions(q) }
            if (_query.value == q) _suggestions.value = list
        }
    }

    fun submitSearch(raw: String? = null) {
        val q = (raw ?: _query.value).trim()
        if (q.isBlank()) return
        _query.value = q
        _suggestions.value = emptyList()
        _results.value = LoadState.Loading
        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) { VideoRepository.search(q) }
                _results.value = LoadState.Content(items)
                _hasMore.value = withContext(Dispatchers.IO) { VideoRepository.hasNextSearchPage() }
            } catch (e: Exception) {
                _results.value = LoadState.Error(friendlyError(e))
            }
        }
    }

    fun loadMoreSearch() {
        if (_loadingMore.value || !_hasMore.value) return
        _loadingMore.value = true
        viewModelScope.launch {
            try {
                val more = withContext(Dispatchers.IO) { VideoRepository.searchNext() }
                val currentItems = (_results.value as? LoadState.Content)?.data ?: emptyList()
                _results.value = LoadState.Content(currentItems + more)
                _hasMore.value = withContext(Dispatchers.IO) { VideoRepository.hasNextSearchPage() }
            } catch (e: Exception) {
                Log.w(TAG, "load more gagal", e)
            } finally {
                _loadingMore.value = false
            }
        }
    }

    // ---------- Putar ----------
    fun openWatch(item: VideoItem) {
        _watchItem.value = item
        _bookmarked.value = store.isBookmarked(item.id)
        _watch.value = LoadState.Loading
        // Dari video terkait: ganti halaman video, jangan menumpuk — "kembali" langsung ke layar asal.
        if (current != Screen.WATCH) push(Screen.WATCH)
        loadWatch(item.url)
    }

    fun retryWatch() {
        val item = _watchItem.value ?: return
        _watch.value = LoadState.Loading
        loadWatch(item.url)
    }

    private fun loadWatch(url: String) {
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { VideoRepository.watch(url) }
                _watch.value = LoadState.Content(detail)
                store.addHistory(detail.item)
                _history.value = store.history()
                Log.i(TAG, "watch ok: ${detail.item.title} dash=${detail.dashUrl != null} prog=${detail.progressiveUrl != null}")
            } catch (e: Exception) {
                Log.w(TAG, "watch gagal", e)
                _watch.value = LoadState.Error(friendlyError(e, isWatch = true))
            }
        }
    }

    fun toggleFullscreen() { _fullscreen.value = !_fullscreen.value }

    fun toggleBookmark() {
        val item = _watchItem.value ?: return
        val now = store.toggleBookmark(item)
        _bookmarked.value = now
        _bookmarks.value = store.bookmarks()
        _notice.value = if (now) "Ditambahkan ke Tandai" else "Dihapus dari Tandai"
    }

    // ---------- Channel ----------
    fun openChannel(uploaderUrl: String) {
        if (uploaderUrl.isBlank()) return
        push(Screen.CHANNEL)
        loadChannel(uploaderUrl)
    }

    fun retryChannel() {
        if (lastChannelUrl.isNotBlank()) loadChannel(lastChannelUrl)
    }

    private fun loadChannel(url: String) {
        lastChannelUrl = url
        _channel.value = LoadState.Loading
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { VideoRepository.channel(url) }
                _channel.value = LoadState.Content(detail)
            } catch (e: Exception) {
                _channel.value = LoadState.Error(friendlyError(e))
            }
        }
    }

    // ---------- Koleksi ----------
    private fun refreshLibrary() {
        _history.value = store.history()
        _bookmarks.value = store.bookmarks()
    }

    fun clearHistory() {
        store.clearHistory()
        _history.value = store.history()
        _notice.value = "Riwayat dihapus"
    }

    fun clearNotice() { _notice.value = null }

    companion object {
        private const val TAG = "YT_REPO"

        val TOPICS: Map<String, String?> = linkedMapOf(
            "Semua" to null,
            "Musik" to "musik indonesia terbaru",
            "Game" to "gaming indonesia",
            "Film" to "film indonesia terbaru",
            "Berita" to "berita hari ini indonesia",
            "Olahraga" to "olahraga highlight",
            "Komedi" to "komedi lucu indonesia",
            "Kuliner" to "kuliner indonesia",
        )

        fun friendlyError(e: Exception, isWatch: Boolean = false): String {
            val name = e.javaClass.simpleName
            val msg = e.message ?: ""
            return when {
                name.contains("SignInConfirmNotBot") || name.contains("LoginRequired") || msg.contains("LOGIN_REQUIRED") ->
                    "YouTube sedang menolak jaringan ini (mengira robot). Coba lagi nanti, atau ganti jaringan — di HP dengan jaringan biasa umumnya lolos."
                name.contains("ReCaptcha") || msg.contains("429") ->
                    "Terlalu banyak permintaan ke YouTube. Tunggu sebentar lalu coba lagi."
                name.contains("UnknownHost") || name.contains("ConnectException") || msg.contains("Unable to resolve host") ->
                    "Tidak ada koneksi internet. Periksa jaringan lalu coba lagi."
                name.contains("SocketTimeout") || msg.contains("timeout") ->
                    "Koneksi ke YouTube kehabisan waktu. Coba lagi."
                isWatch -> "Gagal memuat video ini: ${msg.ifBlank { name }}"
                else -> "Gagal memuat: ${msg.ifBlank { name }}"
            }
        }
    }
}
