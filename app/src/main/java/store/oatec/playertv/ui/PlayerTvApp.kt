package store.oatec.playertv.ui

import store.oatec.playertv.BuildConfig

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions
import io.github.yuroyami.libmpvkt.view.PreparedCore
import io.github.yuroyami.libmpvkt.view.MpvView
import coil.compose.AsyncImage
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import store.oatec.playertv.data.PlayerPreferences
import store.oatec.playertv.data.OatecAboutRepository
import store.oatec.playertv.data.SecureSourceStore
import store.oatec.playertv.data.SourceRepository
import store.oatec.playertv.data.MediaCatalogStore
import store.oatec.playertv.data.OfflineDownloadStore
import store.oatec.playertv.data.OfflineItem
import store.oatec.playertv.data.EpgGuideClient
import store.oatec.playertv.data.EpgProgram
import store.oatec.playertv.data.EpgRepository
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind
import store.oatec.playertv.model.CatalogSection
import store.oatec.playertv.model.PlayerSource
import store.oatec.playertv.model.SourceCatalog
import store.oatec.playertv.model.SourceType

private val Ink = Color(0xFF05070A)
private val Panel = Color(0xFF0C1016)
private val Raised = Color(0xFF111722)
private val Lime = Color(0xFFE8B83E)
private val GoldLight = Color(0xFFFFDA72)
private val TextMain = Color(0xFFF7F7F5)
private val TextMuted = Color(0xFF9DA5B1)
private val ErrorRed = Color(0xFFFF8177)
private const val CHANNEL_INFO_DURATION_MS = 5_000L
private const val CATALOG_PAGE_SIZE = 500
private const val PLAYER_API_BASE_URL = "https://admin.oatec.store/api/player"

private fun activateOatecCode(context: Context, code: String): PlayerSource {
    val prefs = context.getSharedPreferences("oatec_activation", Context.MODE_PRIVATE)
    val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device_id", it).apply()
    }
    val payload = JSONObject()
        .put("code", code)
        .put("device_id", deviceId)
        .put("device_name", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(255))
        .put("platform", "android")
        .put("app_version", "1.0.0")
    val connection = (URL(PLAYER_API_BASE_URL + "/activate-by-code").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 12000
        readTimeout = 15000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }
    return try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val response = runCatching { JSONObject(body) }.getOrNull()
        if (status !in 200..299 || response?.optBoolean("ok") != true) {
            val serverError = response?.optString("error").orEmpty()
            throw IOException(when {
                status == 401 -> "كود التفعيل غير صحيح أو سالات الصلاحية ديالو."
                status == 403 -> "الاشتراك ديالك ماشي فعّال أو سالات المدة ديالو."
                status == 409 -> "هاد الكود مربوط بجهاز آخر."
                serverError.isNotBlank() -> serverError
                else -> "تعذر التحقق من كود التفعيل (HTTP " + status + ")."
            })
        }
        val primary = response.optJSONObject("config")?.optJSONObject("primary")
            ?: throw IOException("السيرفر ما رجّعش إعدادات المصدر للحساب.")
        val sourceType = primary.optString("type").trim().lowercase()
        val server = primary.optString("server").trim().trimEnd('/')
        val username = primary.optString("username").trim()
        val password = primary.optString("password")
        val m3uUrl = primary.optString("url").trim()
        val xtreamUri = runCatching { Uri.parse(server) }.getOrNull()
        val validXtream = !xtreamUri?.host.isNullOrBlank() &&
            xtreamUri?.scheme in setOf("http", "https") &&
            username.isNotBlank() && password.isNotBlank()
        val m3uUri = runCatching { Uri.parse(m3uUrl) }.getOrNull()
        val validM3u = !m3uUri?.host.isNullOrBlank() && m3uUri?.scheme in setOf("http", "https")
        // Activation APIs can label an Xtream get.php playlist as M3U.
        // Convert only this recognizable endpoint; keep normal M3U URLs as M3U.
        val isXtreamGetPhp = m3uUri?.lastPathSegment.equals("get.php", ignoreCase = true)
        val urlUsername = m3uUri?.getQueryParameter("username").orEmpty().ifBlank { username }
        val urlPassword = m3uUri?.getQueryParameter("password").orEmpty().ifBlank { password }
        val validGetPhpXtream = isXtreamGetPhp && validM3u &&
            urlUsername.isNotBlank() && urlPassword.isNotBlank()
        val getPhpBase = if (isXtreamGetPhp) m3uUrl.substringBefore('?').substringBeforeLast('/') else ""
        when {
            sourceType == "xtream" && validXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = server,
                username = username,
                password = password,
            )
            validGetPhpXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = getPhpBase,
                username = urlUsername,
                password = urlPassword,
            )
            sourceType == "m3u" && validM3u -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.M3U,
                serverUrl = m3uUrl,
            )
            sourceType.isBlank() && validXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = server,
                username = username,
                password = password,
            )
            sourceType.isBlank() && validM3u -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.M3U,
                serverUrl = m3uUrl,
            )
            else -> throw IOException(
                "كود التفعيل صحيح، ولكن نوع المصدر أو بياناته فلوحة الأدمن غير متطابقة. تأكد أن الحساب مضبوط على M3U مع رابط صالح، أو Xtream مع Server وUsername وPassword."
            )
        }
    } finally { connection.disconnect() }
}
private enum class Section(val label: String) {
    HOME("Home"), LIVE("Live TV"), MOVIES("Movies"), SERIES("Series"), DOWNLOADS("Downloads"), SOURCES("Sources"), SETTINGS("Settings"),
}

@Composable
fun PlayerTvApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val compactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp < 900
    val repository = remember { SourceRepository() }
    val mediaCatalogStore = remember { MediaCatalogStore(context) }
    val sourceStore = remember { SecureSourceStore(context) }
    val preferences = remember { PlayerPreferences(context) }
    val aboutRepository = remember { OatecAboutRepository() }
    val offlineDownloadStore = remember { OfflineDownloadStore(context) }
    var premiumCode by remember { mutableStateOf(runCatching { sourceStore.loadPremiumCode() }.getOrNull().orEmpty()) }
    var kidsModeActive by remember { mutableStateOf(preferences.kidsModeEnabled()) }
    val catalogLoadMutex = remember { Mutex() }
    val sources = remember {
        mutableStateListOf<PlayerSource>().apply {
            addAll(runCatching { sourceStore.load() }.getOrDefault(emptyList()))
        }
    }
    var activeSourceId by rememberSaveable { mutableStateOf(sources.firstOrNull()?.id.orEmpty()) }
    val activeSource = sources.firstOrNull { it.id == activeSourceId }
    var catalog by remember { mutableStateOf(SourceCatalog()) }
        var loadedCatalogs by remember { mutableStateOf<Set<String>>(emptySet()) }
    // DB pagination offset must count raw rows, before Kids Mode filtering.
    var catalogPageOffsets by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // Open directly on the live channel list; users can switch straight to Movies, Series, or Settings.
    var section by remember { mutableStateOf(Section.LIVE.name) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var liveCategoryNames by remember(activeSourceId) { mutableStateOf<List<String>>(emptyList()) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<MediaEntry?>(null) }
    var selectedDetails by remember { mutableStateOf<MediaEntry?>(null) }
    var organizingChannel by remember { mutableStateOf<MediaEntry?>(null) }
    var pendingSeries by remember { mutableStateOf<MediaEntry?>(null) }
    var episodes by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var episodesBusy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var activeLoadCount by remember { mutableStateOf(0) }
    var catalogProgress by remember { mutableStateOf(0f) }
    var catalogLoadedItems by remember { mutableStateOf(0) }
    var loadingMore by remember { mutableStateOf(false) }
    var preferenceRevision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var showAddSource by remember { mutableStateOf(false) }
    var selectedKidsYoutubeId by rememberSaveable { mutableStateOf("") }
    var showNumberPad by remember { mutableStateOf(false) }
    var showKidsExitDialog by remember { mutableStateOf(false) }
    var kidsExitPinInput by remember { mutableStateOf("") }
    var kidsExitPinError by remember { mutableStateOf("") }
    var numberInput by rememberSaveable { mutableStateOf("") }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    var epgPrograms by remember(activeSourceId) { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var epgStatus by remember(activeSourceId) { mutableStateOf("") }
    var epgClock by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val currentSection = Section.entries.firstOrNull { it.name == section } ?: Section.LIVE
    val sectionHistory = remember { mutableStateListOf<Section>() }

    fun navigateTo(target: Section) {
        val current = Section.entries.firstOrNull { it.name == section } ?: Section.LIVE
        if (current != target) {
            sectionHistory.add(current)
            if (sectionHistory.size > 20) sectionHistory.removeAt(0)
        }
        section = target.name
        selectedCategory = "All"
        search = ""
        selectedMedia = null
        selectedDetails = null
    }

    fun goBack() {
        when {
            selectedDetails != null -> selectedDetails = null
            selectedMedia != null -> selectedMedia = null
            pendingSeries != null -> { pendingSeries = null; episodes = emptyList(); episodesBusy = false }
            sectionHistory.isNotEmpty() -> {
                val previous = sectionHistory.removeAt(sectionHistory.lastIndex)
                section = previous.name
                selectedCategory = "All"
                search = ""
                selectedMedia = null
                selectedDetails = null
            }
            currentSection != Section.LIVE -> section = Section.LIVE.name
            // Back from the root LIVE/channels screen must never close the app accidentally.
            // Return to Home instead; Android system Home/Recents remain available as usual.
            else -> {
                section = Section.HOME.name
                selectedCategory = "All"
                search = ""
                selectedMedia = null
                selectedDetails = null
            }
        }
    }

    // Keep Home navigation lightweight: clear transient detail/player state immediately,
    // rather than leaving episode lists and selection state attached to the previous screen.
    fun goHome() {
        section = Section.HOME.name
        selectedCategory = "All"
        search = ""
        selectedMedia = null
        selectedDetails = null
        pendingSeries = null
        episodes = emptyList()
        episodesBusy = false
        message = ""
    }

    // Keep children inside Kids Mode when Back is pressed, including from LIVE.
    // A normal app cannot block Android Home/Recents without device kiosk/lock-task setup.
    BackHandler(enabled = true) {
        if (kidsModeActive) {
            section = Section.HOME.name
            selectedCategory = "All"
            search = ""
            selectedMedia = null
            selectedDetails = null
        } else goBack()
    }

    fun refresh(source: PlayerSource, requestedSection: Section = currentSection, force: Boolean = false) {
        val catalogSection = when (requestedSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return
        }
        val loadKey = "${source.id}:${catalogSection.name}"
        if (!force && loadKey in loadedCatalogs) return
        message = ""
        catalogProgress = 0f
        catalogLoadedItems = 0
        activeLoadCount++
        busy = true
        scope.launch {
            try {
                // Show the last successfully saved catalogue immediately while a large
                // Xtream response is being downloaded. Read it in bounded DB pages.
                if (source.id == activeSourceId) {
                    val cachedState = withContext(Dispatchers.IO) {
                        mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0) to
                            mediaCatalogStore.count(source.id, catalogSection.name)
                    }
                    val cached = cachedState.first
                    val cachedCount = cachedState.second
                    catalogPageOffsets = catalogPageOffsets + (loadKey to cached.size)
                    if (cached.isNotEmpty() && source.id == activeSourceId) {
                        val safeCached = applyKidsModeFilter(cached, preferences)
                        catalog = when (catalogSection) {
                            CatalogSection.LIVE -> catalog.copy(liveChannels = safeCached)
                            CatalogSection.MOVIES -> catalog.copy(movies = safeCached)
                            CatalogSection.SERIES -> catalog.copy(series = safeCached)
                        }
                        message = "Loaded saved catalogue (" + cachedCount + " items)."
                    }
                    // A populated local catalogue is the default on app startup and
                    // section changes. Avoid downloading the full provider catalogue
                    // every launch; refresh remains available through the explicit
                    // refresh action (force=true).
                    if (cachedCount > 0L && !force && source.id == activeSourceId) {
                        loadedCatalogs = loadedCatalogs + loadKey
                        return@launch
                    }
                }
                catalogLoadMutex.withLock {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            // Stream bounded batches directly into one SQLite transaction.
                            // A failed response rolls back, preserving the last good catalogue.
                            val result = mediaCatalogStore.beginImport(source.id, catalogSection.name).use { session ->
                                val loaded = repository.loadCatalogBatched(source, catalogSection) { batch ->
                                    session.insertBatch(batch)
                                    catalogLoadedItems += batch.size
                                    // Providers often do not expose a reliable total in advance.
                                    // Progress fills gradually, with the final 8% reserved for completion.
                                    val count = catalogLoadedItems
                                    catalogProgress = (1f - kotlin.math.exp(-count / 9000f).toFloat()).coerceAtMost(0.92f)
                                }
                                session.commit()
                                catalogProgress = 1f
                                kotlinx.coroutines.delay(450)
                                loaded
                            }
                            val firstPage = mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0)
                            val totalCount = mediaCatalogStore.count(source.id, catalogSection.name)
                            Triple(result, firstPage, totalCount)
                        }
                    }
                        .onSuccess { (result, firstPage, totalCount) ->
                            if (source.id == activeSourceId) {
                                catalogPageOffsets = catalogPageOffsets + (loadKey to firstPage.size)
                                catalog = when (catalogSection) {
                                    CatalogSection.LIVE -> catalog.copy(liveChannels = applyKidsModeFilter(firstPage, preferences))
                                    CatalogSection.MOVIES -> catalog.copy(movies = applyKidsModeFilter(firstPage, preferences))
                                    CatalogSection.SERIES -> catalog.copy(series = applyKidsModeFilter(firstPage, preferences))
                                }
                                if (catalogSection == CatalogSection.LIVE) selectedCategory = "All"
                                loadedCatalogs = loadedCatalogs + loadKey
                                message = "$totalCount ${catalogSection.name.lowercase()} items saved; loaded into the local catalogue and displayed without a 500-item cap."
                            }
                        }
                        .onFailure { if (source.id == activeSourceId) message = it.message ?: "Could not load this source." }
                    }
            } finally {
                activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
                busy = activeLoadCount > 0
            }
        }
    }

    fun loadMore(requestedSection: Section) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return
        val catalogSection = when (requestedSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return
        }
        if (loadingMore) return
        val loadKey = "${source.id}:${catalogSection.name}"
        val currentSize = catalogPageOffsets[loadKey] ?: when (catalogSection) {
            CatalogSection.LIVE -> catalog.liveChannels.size
            CatalogSection.MOVIES -> catalog.movies.size
            CatalogSection.SERIES -> catalog.series.size
        }
        val requestedSearch = search
        loadingMore = true
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    // Use the database count and the current displayed page as the
                    // continuation offset. The query uses the same search predicate,
                    // so search results continue from the correct row rather than
                    // restarting or skipping items.
                    val categoryFilter = selectedCategory.takeIf {
                        catalogSection == CatalogSection.LIVE && it in liveCategoryNames
                    }
                    val total = mediaCatalogStore.count(
                        source.id, catalogSection.name, search = requestedSearch, category = categoryFilter,
                    )
                    if (currentSize.toLong() >= total) emptyList()
                    else mediaCatalogStore.page(
                        source.id, catalogSection.name,
                        limit = CATALOG_PAGE_SIZE, offset = currentSize,
                        search = requestedSearch, category = categoryFilter,
                    )
                }
                if (source.id == activeSourceId && requestedSearch == search && page.isNotEmpty()) {
                    catalogPageOffsets = catalogPageOffsets + (loadKey to (currentSize + page.size))
                    val combined = when (catalogSection) {
                        CatalogSection.LIVE -> catalog.liveChannels + page
                        CatalogSection.MOVIES -> catalog.movies + page
                        CatalogSection.SERIES -> catalog.series + page
                    }
                    val filteredCombined = applyKidsModeFilter(combined, preferences)
                    catalog = when (catalogSection) {
                        CatalogSection.LIVE -> catalog.copy(liveChannels = filteredCombined)
                        CatalogSection.MOVIES -> catalog.copy(movies = filteredCombined)
                        CatalogSection.SERIES -> catalog.copy(series = filteredCombined)
                    }
                }
            } catch (error: Exception) {
                if (source.id == activeSourceId) message = error.message ?: "Could not load the next catalogue page."
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(activeSourceId, activeSource?.epgUrl) {
        epgPrograms = emptyList()
        epgStatus = ""
        val guideUrl = activeSource?.epgUrl?.trim().orEmpty()
        if (guideUrl.isNotEmpty()) {
            runCatching { withContext(Dispatchers.IO) { EpgGuideClient().fetch(guideUrl) } }
                .onSuccess { programs -> epgPrograms = programs; epgStatus = if (programs.isEmpty()) "No programmes found in EPG guide." else "TV guide loaded: ${programs.size} programmes." }
                .onFailure { error -> epgStatus = "TV guide unavailable: ${error.message ?: "download failed"}" }
        }
    }

    LaunchedEffect(Unit) {
        while (true) { delay(60_000); epgClock = System.currentTimeMillis() }
    }

    LaunchedEffect(activeSourceId) {
        catalog = SourceCatalog()
        loadedCatalogs = emptySet()
        episodes = emptyList()
        episodesBusy = false
        pendingSeries = null
    }

    // Load only the section the user is viewing. Each section reads its saved
    // SQLite catalogue first, so returning to it is fast; uncached sections are
    // imported in the background only when opened instead of downloading all
    // 36k+ items at startup.
    LaunchedEffect(activeSourceId, section, loadedCatalogs) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        val requestedSection = when (currentSection) {
            Section.LIVE -> Section.LIVE
            Section.MOVIES -> Section.MOVIES
            Section.SERIES -> Section.SERIES
            else -> return@LaunchedEffect
        }
        val loadKey = when (requestedSection) {
            Section.LIVE -> "${source.id}:${CatalogSection.LIVE.name}"
            Section.MOVIES -> "${source.id}:${CatalogSection.MOVIES.name}"
            Section.SERIES -> "${source.id}:${CatalogSection.SERIES.name}"
            else -> return@LaunchedEffect
        }
        if (loadKey !in loadedCatalogs) refresh(source, requestedSection)
    }

    LaunchedEffect(activeSourceId, section, search, loadedCatalogs, selectedCategory, preferenceRevision) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        val catalogSection = when (currentSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return@LaunchedEffect
        }
        val loadKey = "${source.id}:${catalogSection.name}"
        if (loadKey !in loadedCatalogs) return@LaunchedEffect
        val requestedSearch = search
        val requestedCategory = selectedCategory
        val categoryFilter = requestedCategory.takeIf {
            catalogSection == CatalogSection.LIVE && it in liveCategoryNames
        }
        val filteredPage = withContext(Dispatchers.IO) {
            mediaCatalogStore.page(
                source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0,
                search = requestedSearch, category = categoryFilter,
            )
        }
        if (source.id != activeSourceId || requestedSearch != search || currentSection.name != section ||
            selectedCategory != requestedCategory
        ) return@LaunchedEffect
        catalog = when (catalogSection) {
            CatalogSection.LIVE -> catalog.copy(liveChannels = applyKidsModeFilter(filteredPage, preferences))
            CatalogSection.MOVIES -> catalog.copy(movies = applyKidsModeFilter(filteredPage, preferences))
            CatalogSection.SERIES -> catalog.copy(series = applyKidsModeFilter(filteredPage, preferences))
        }
    }

    LaunchedEffect(activeSourceId, loadedCatalogs) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        if ("${source.id}:LIVE" !in loadedCatalogs) return@LaunchedEffect
        val categories = withContext(Dispatchers.IO) {
            mediaCatalogStore.categories(source.id, CatalogSection.LIVE.name)
        }
        if (source.id == activeSourceId) {
            // Provider categories can be used as SQL filters; custom categories are applied locally.
            liveCategoryNames = categories.distinct().sortedBy { it.lowercase() }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { importLocalPlaylist(context, uri) }
                }.onSuccess { source ->
                    sources += source
                    sourceStore.save(sources)
                    activeSourceId = source.id
                    section = Section.HOME.name
                    message = "Playlist imported"
                }.onFailure { message = it.message ?: "Could not import playlist." }
                busy = false
            }
        }
    }

    val needsInitialSetup = sources.isEmpty()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            onPrimary = Ink,
            secondary = GoldLight,
            onSecondary = Ink,
            tertiary = Color(0xFF43D19E),
            background = Ink,
            onBackground = TextMain,
            surface = Panel,
            onSurface = TextMain,
            surfaceVariant = Raised,
            onSurfaceVariant = TextMuted,
            outline = Color(0xFF303743),
            error = ErrorRed,
            onError = Ink,
        ),
    ) {
        if (selectedKidsYoutubeId.isNotBlank()) {
            KidsYouTubePlayerDialog(videoId = selectedKidsYoutubeId, onDismiss = { selectedKidsYoutubeId = "" })
        }
        if (needsInitialSetup) {
            InitialSetupScreen(
                savedPremiumCode = premiumCode,
                onComplete = { code, source ->
                    sourceStore.savePremiumCode(code)
                    premiumCode = code
                    sources.removeAll { it.name == "OATEC Subscription" }
                    sources += source
                    sourceStore.save(sources)
                    activeSourceId = source.id
                    section = Section.HOME.name
                },
            )
        } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x1AE8B83E), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.1f),
                            radius = size.maxDimension * 0.75f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x12375B91), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.25f),
                            radius = size.maxDimension * 0.7f,
                        ),
                    )
                }
                .onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    when (key.keyCode) {
                        AndroidKeyEvent.KEYCODE_MENU -> {
                            goHome()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_BACK -> { goBack(); true }
                        AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                            if (currentSection == Section.LIVE && catalog.liveChannels.isNotEmpty()) {
                                val channels = visibleChannels(catalog, selectedCategory, search, preferences)
                                val current = channels.indexOfFirst { it.id == selectedChannelId }
                                val index = if (current <= 0) channels.lastIndex else current - 1
                                channels.getOrNull(index)?.let { selectedChannelId = it.id; selectedMedia = it }
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (currentSection == Section.LIVE && catalog.liveChannels.isNotEmpty()) {
                                val channels = visibleChannels(catalog, selectedCategory, search, preferences)
                                val current = channels.indexOfFirst { it.id == selectedChannelId }
                                val index = if (current < 0 || current == channels.lastIndex) 0 else current + 1
                                channels.getOrNull(index)?.let { selectedChannelId = it.id; selectedMedia = it }
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_0,
                        AndroidKeyEvent.KEYCODE_1,
                        AndroidKeyEvent.KEYCODE_2,
                        AndroidKeyEvent.KEYCODE_3,
                        AndroidKeyEvent.KEYCODE_4,
                        AndroidKeyEvent.KEYCODE_5,
                        AndroidKeyEvent.KEYCODE_6,
                        AndroidKeyEvent.KEYCODE_7,
                        AndroidKeyEvent.KEYCODE_8,
                        AndroidKeyEvent.KEYCODE_9 -> {
                            numberInput = (numberInput + (key.keyCode - AndroidKeyEvent.KEYCODE_0)).takeLast(4)
                            true
                        }
                        in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> {
                            numberInput = (numberInput + (key.keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0)).takeLast(4)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            numberInput.toIntOrNull()?.let { number ->
                                val match = numberedChannels(catalog.liveChannels, preferences)
                                    .firstOrNull { (channel, channelNumber) -> channelNumber == number }
                                match?.first?.let { selectedChannelId = it.id; selectedMedia = it }
                            }
                            numberInput = ""
                            true
                        }
                        else -> false
                    }
                },
        ) {
            if (kidsModeActive) {
                KidsDashboard(
                    compact = compactLandscape,
                    section = currentSection,
                    movies = applyKidsModeFilter(catalog.movies, preferences),
                    series = applyKidsModeFilter(catalog.series, preferences),
                    channels = applyKidsModeFilter(catalog.liveChannels, preferences),
                    search = search,
                    onSearch = { search = it },
                    onSection = { next -> navigateTo(next); pendingSeries = null },
                    onPlay = { selectedMedia = it },
                    onPlayYoutube = { videoId -> selectedKidsYoutubeId = videoId },
                    onMovieDetails = { selectedDetails = it },
                    onExit = { kidsExitPinInput = ""; kidsExitPinError = ""; showKidsExitDialog = true },
                    onLoadMore = { loadMore(currentSection) },
                )
            } else if (currentSection == Section.HOME) {
                HomeDashboard(
                    compact = compactLandscape,
                    sourceName = activeSource?.name,
                    onSelect = { navigateTo(it) },
                    onExit = { (context as? Activity)?.finishAndRemoveTask() },
                )
            } else {
            Column(Modifier.fillMaxSize()) {
                // LIVE TV is a clean, full-screen channel list: no navigation strip,
                // title/search bar, or divider above the channels.
                if (currentSection !in setOf(Section.LIVE, Section.MOVIES, Section.SERIES, Section.SETTINGS)) {
                    NavigationRail(
                        selected = if (currentSection == Section.SOURCES) Section.SETTINGS else currentSection,
                        sourceName = activeSource?.name,
                        compact = compactLandscape,
                        onSelect = { navigateTo(it) },
                        onHome = { goBack() },
                        onExit = { (context as? Activity)?.finishAndRemoveTask() },
                    )
                }
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    if (currentSection !in setOf(Section.LIVE, Section.MOVIES, Section.SERIES, Section.SETTINGS)) {
                        TopBar(
                            title = currentSection.label,
                            sourceName = activeSource?.name,
                            search = search,
                            onSearch = { search = it },
                            busy = busy,
                            onRefresh = { activeSource?.let { refresh(it, currentSection, force = true) } },
                            showRefresh = currentSection in setOf(Section.LIVE, Section.MOVIES, Section.SERIES),
                            compact = compactLandscape,
                            onAddSource = { showAddSource = true },
                            onNumberPad = { showNumberPad = true },
                            numberInput = numberInput,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                    when (currentSection) {
                        Section.HOME -> Unit // HomeDashboard is rendered above this content switch.
                        Section.LIVE -> LiveScreen(
                            catalog = catalog,
                            epgPrograms = epgPrograms,
                            epgClock = epgClock,
                            epgStatus = epgStatus,
                            category = selectedCategory,
                            availableCategories = liveCategoryNames,
                            search = search,
                            preferences = preferences,
                            preferenceRevision = preferenceRevision,
                            onSearch = { search = it },
                            onHome = { goBack() },
                            message = message,
                            busy = busy,
                            catalogProgress = catalogProgress,
                            catalogLoadedItems = catalogLoadedItems,
                            onCategory = {
                                if (selectedCategory != it) {
                                    selectedCategory = it
                                    // Clear the previous category while its first page loads.
                                    catalog = catalog.copy(liveChannels = emptyList())
                                }
                            },
                            onLoadMore = { loadMore(Section.LIVE) },
                            onPlay = {
                                selectedChannelId = it.id
                                selectedMedia = it.copy(title = preferences.channelTitle(it.id, it.title))
                            },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                            onOrganize = { organizingChannel = it },
                            onHide = { item -> preferences.setHidden(item.id, item.id !in preferences.hiddenChannels()); preferenceRevision++ },
                        )
                        Section.MOVIES -> MediaScreen(
                            title = "Movies",
                            onHome = { goBack() },
                            onSearch = { search = it },
                            entries = catalog.movies,
                            search = search,
                            busy = busy,
                            message = message,
                            catalogProgress = catalogProgress,
                            catalogLoadedItems = catalogLoadedItems,
                            preferences = preferences,
                            recentEntries = preferences.recentlyWatched(catalog.movies + episodes),
                            onLoadMore = { loadMore(Section.MOVIES) },
                            onPlay = { selectedDetails = it },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.SERIES -> SeriesScreen(
                            onHome = { goBack() },
                            onSearch = { search = it },
                            entries = catalog.series,
                            episodes = episodes,
                            selectedSeries = pendingSeries,
                            search = search,
                            busy = episodesBusy,
                            message = message,
                            preferences = preferences,
                            onLoadMore = { loadMore(Section.SERIES) },
                            onSelect = { series ->
                                val source = activeSource
                                if (source == null || source.type != SourceType.XTREAM) {
                                    message = "Series episodes require an Xtream source."
                                } else {
                                    pendingSeries = series
                                    episodes = emptyList()
                                    episodesBusy = true
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                repository.loadSeriesEpisodes(source, series.id.substringAfterLast(':'))
                                            }
                                        }.onSuccess { episodes = it }
                                            .onFailure { message = it.message ?: "Could not load episodes." }
                                        episodesBusy = false
                                    }
                                }
                            },
                            onPlay = { selectedMedia = it },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.DOWNLOADS -> OfflineDownloadsScreen(store = offlineDownloadStore, onHome = { goBack() }, onPlay = { selectedMedia = it }, onMessage = { message = it })
                        Section.SOURCES -> SourcesScreen(
                            sources = sources,
                            activeId = activeSourceId,
                            busy = busy,
                            onSelect = { source -> activeSourceId = source.id; navigateTo(Section.LIVE) },
                            onRefresh = { source -> refresh(source, Section.LIVE, force = true) },
                            onDelete = { source ->
                                sources.remove(source)
                                sourceStore.save(sources)
                                if (activeSourceId == source.id) activeSourceId = sources.firstOrNull()?.id.orEmpty()
                            },
                            onAdd = { showAddSource = true },
                            onImport = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "application/octet-stream")) },
                        )
                        Section.SETTINGS -> SettingsScreen(
                            onHome = { goBack() },
                            preferences = preferences,
                            onKidsModeChanged = { enabled ->
                                kidsModeActive = enabled
                                if (enabled) section = Section.HOME.name
                            },
                            aboutRepository = aboutRepository,
                            onManageSources = { navigateTo(Section.SOURCES) },
                            premiumCode = premiumCode,
                            onSavePremiumCode = { code ->
                                sourceStore.savePremiumCode(code)
                                premiumCode = code
                            },
                            onExit = { (context as? Activity)?.finishAndRemoveTask() },
                        )
                    }
                }
            }
            }
            if (showKidsExitDialog) {
                AlertDialog(
                    onDismissRequest = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" },
                    title = { Text("Parent PIN required") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enter the parent PIN to leave Kids Mode.", color = TextMuted, fontSize = 12.sp)
                            OutlinedTextField(
                                value = kidsExitPinInput,
                                onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsExitPinInput = value },
                                label = { Text("Parent PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            )
                            if (kidsExitPinError.isNotBlank()) Text(kidsExitPinError, color = ErrorRed, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (preferences.verifyKidsPin(kidsExitPinInput)) {
                                preferences.setKidsModeEnabled(false)
                                kidsModeActive = false
                                section = Section.HOME.name
                                search = ""
                                kidsExitPinInput = ""
                                kidsExitPinError = ""
                                showKidsExitDialog = false
                            } else kidsExitPinError = "Incorrect PIN."
                        }) { Text("Exit Kids Mode") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" }) { Text("Cancel") }
                    },
                )
            }
            if (showAddSource) {
                AddSourceDialog(
                    onDismiss = { showAddSource = false },
                    onSave = { source ->
                        sources.removeAll { it.id == source.id }
                        sources += source
                        sourceStore.save(sources)
                        activeSourceId = source.id
                        showAddSource = false
                    },
                )
            }
            if (showNumberPad) {
                NumberPadDialog(
                    channels = catalog.liveChannels,
                    preferences = preferences,
                    onDismiss = { showNumberPad = false },
                    onSelect = { channel ->
                        selectedChannelId = channel.id
                        selectedMedia = channel
                        showNumberPad = false
                    },
                )
            }
            organizingChannel?.let { channel ->
                OrganizeChannelDialog(
                    channel = channel,
                    channels = catalog.liveChannels,
                    preferences = preferences,
                    onDismiss = { organizingChannel = null },
                    onSave = { title, category, number ->
                        preferences.setChannelTitle(channel.id, title)
                        preferences.moveToCategory(channel.id, category)
                        preferences.moveChannelToNumber(channel.id, number, catalog.liveChannels.map { it.id })
                        preferenceRevision++
                        organizingChannel = null
                    },
                )
            }
            selectedMedia?.let { media ->
                PlaybackDialog(
                    entry = media,
                    channels = numberedChannels(catalog.liveChannels, preferences).map { (channel, _) ->
                        channel.copy(title = preferences.channelTitle(channel.id, channel.title))
                    },
                    preferences = preferences,
                    onDismiss = { selectedMedia = null },
                    onPlay = { selectedChannelId = it.id; selectedMedia = it },
                )
            }
            selectedDetails?.let { media ->
                MediaDetailsDialog(
                    entry = media,
                    onDismiss = { selectedDetails = null },
                    onPlay = {
                        selectedDetails = null
                        selectedMedia = media
                    },
                    onBrowseEpisodes = { selectedDetails = null },
                    onDownload = {
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { offlineDownloadStore.enqueue(media) } }
                                .onSuccess { message = "Download started: " + media.title }
                                .onFailure { message = it.message ?: "Could not start download." }
                        }
                    },
                )
            }
        }
        }
    }
}

@Composable
private fun InitialSetupScreen(
    savedPremiumCode: String,
    onComplete: (String, PlayerSource) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val compactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp < 900
    var entryScreen by rememberSaveable { mutableStateOf("welcome") }
    var premiumCode by rememberSaveable { mutableStateOf(savedPremiumCode) }
    var sourceType by rememberSaveable { mutableStateOf(SourceType.XTREAM.name) }
    var sourceName by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var activationBusy by rememberSaveable { mutableStateOf(false) }
    val activationScope = rememberCoroutineScope()
    val isXtream = sourceType == SourceType.XTREAM.name
    val accent = Color(0xFFE50920)
    val panel = Color(0xFF111111)
    val spacing = if (compactLandscape) 8.dp else 14.dp
    val horizontalPadding = if (compactLandscape) 20.dp else 28.dp

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF26070A), Color(0xFF090909), Color.Black),
                center = Offset(0.92f, 0.08f),
                radius = 1100f,
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (entryScreen == "welcome") {
            Column(
                Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = if (compactLandscape) 10.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TVI PLAYER", color = Color.White, fontSize = if (compactLandscape) 22.sp else 28.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("ENTERTAINMENT, REFINED", color = accent, fontSize = if (compactLandscape) 9.sp else 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(color = accent, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(if (compactLandscape) 42.dp else 56.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("شاشتك.\nتجربتك المميزة.", color = Color.White, fontSize = if (compactLandscape) 23.sp else 32.sp, fontWeight = FontWeight.Black, lineHeight = if (compactLandscape) 27.sp else 38.sp)
                    Text("وجهتك الأنيقة للتلفاز المباشر والأفلام والمسلسلات.", color = Color(0xFFBDBDBD), fontSize = if (compactLandscape) 10.sp else 14.sp)
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = panel,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF393939)),
                        modifier = Modifier.weight(1f).fillMaxHeight(0.88f)
                            .clickable { entryScreen = "standard" },
                    ) {
                        Box(Modifier.fillMaxSize().padding(if (compactLandscape) 14.dp else 24.dp)) {
                            Text("◉", color = accent, fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd))
                            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                Text("STANDARD PLAYER", color = Color.White, fontSize = if (compactLandscape) 18.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("M3U  •  XTREAM CODES", color = Color(0xFFAAAAAA), fontSize = if (compactLandscape) 9.sp else 11.sp, letterSpacing = 1.sp)
                            }
                            Text("→", color = accent, fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }
                    Surface(
                        color = Color(0xFF1B090B),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF512126)),
                        modifier = Modifier.weight(1.25f).fillMaxHeight(0.88f)
                            .clickable { entryScreen = "premium" },
                    ) {
                        Box(Modifier.fillMaxSize().padding(if (compactLandscape) 14.dp else 24.dp)) {
                            Text("✦", color = accent, fontSize = 34.sp, modifier = Modifier.align(Alignment.CenterStart))
                            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                Surface(color = Color(0xFF310D12), shape = RoundedCornerShape(50.dp)) {
                                    Text("PREMIUM", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("OATEC PREMIUM CODE", color = Color.White, fontSize = if (compactLandscape) 17.sp else 25.sp, fontWeight = FontWeight.Black)
                                Text("SECURE ACTIVATION", color = Color(0xFFAAAAAA), fontSize = 9.sp, letterSpacing = 1.4.sp)
                            }
                        }
                    }
                }
                Surface(
                    color = Color(0xFF090909),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/")))
                    },
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compactLandscape) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("→", color = accent, fontSize = 20.sp)
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الإعدادات والمساعدة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("الملف الشخصي واللغة والتشغيل", color = Color(0xFFAAAAAA), fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("⚙", color = Color(0xFFBDBDBD), fontSize = 23.sp)
                    }
                }
            }
        } else if (entryScreen == "premium") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text("OATEC PREMIUM CODE", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("دخل كود التفعيل اللي توصّلتي به من OATEC باش نربطو الحساب ديالك أوتوماتيكياً.", color = Color(0xFFBDBDBD), fontSize = 13.sp)
                OutlinedTextField(
                    value = premiumCode,
                    onValueChange = { premiumCode = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Premium code") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { entryScreen = "welcome"; error = "" }, modifier = Modifier.weight(1f), enabled = !activationBusy) { Text("رجوع") }
                    Button(
                        onClick = {
                            val normalizedCode = premiumCode.trim().uppercase()
                            if (normalizedCode.isBlank()) error = "دخل كود التفعيل ديالك."
                            else {
                                activationBusy = true
                                error = ""
                                activationScope.launch {
                                    try {
                                        val source = withContext(Dispatchers.IO) { activateOatecCode(context, normalizedCode) }
                                        premiumCode = normalizedCode
                                        onComplete(normalizedCode, source)
                                    } catch (e: Exception) {
                                        error = e.message ?: "تعذر التفعيل. تأكد من الكود والاتصال بالإنترنت."
                                    } finally { activationBusy = false }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !activationBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) {
                        if (activationBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("تفعيل وربط الحساب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = if (compactLandscape) 8.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 6.dp else 12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { entryScreen = "welcome"; error = "" }) { Text("← رجوع") }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("STANDARD PLAYER", color = Color.White, fontSize = if (compactLandscape) 18.sp else 25.sp, fontWeight = FontWeight.Black)
                        Text("اختار طريقة الاتصال بمصدر القنوات", color = Color(0xFFBDBDBD), fontSize = if (compactLandscape) 9.sp else 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPill("Xtream Codes", isXtream) { sourceType = SourceType.XTREAM.name; error = "" }
                    CategoryPill("M3U URL", !isXtream) { sourceType = SourceType.M3U.name; error = "" }
                }
                if (compactLandscape) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupField(serverUrl, { serverUrl = it; error = "" }, if (isXtream) "Server URL" else "M3U playlist URL", Modifier.weight(1.6f), uri = true)
                        SetupField(sourceName, { sourceName = it }, "Source name (optional)", Modifier.weight(1f))
                    }
                    if (isXtream) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetupField(username, { username = it; error = "" }, "Username", Modifier.weight(1f))
                            SetupField(password, { password = it; error = "" }, "Password", Modifier.weight(1f), password = true)
                        }
                    }
                } else {
                    SetupField(sourceName, { sourceName = it }, "Source name (optional)", Modifier.fillMaxWidth())
                    SetupField(serverUrl, { serverUrl = it; error = "" }, if (isXtream) "Server URL" else "M3U playlist URL", Modifier.fillMaxWidth(), uri = true)
                    if (isXtream) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetupField(username, { username = it; error = "" }, "Username", Modifier.weight(1f))
                            SetupField(password, { password = it; error = "" }, "Password", Modifier.weight(1f), password = true)
                        }
                    }
                }
                if (serverUrl.startsWith("http://", true)) {
                    Text("HTTP غير مشفر؛ استعمل HTTPS إلا كان متاح.", color = ErrorRed, fontSize = 9.sp)
                }
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f, fill = !compactLandscape))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/"))) },
                        modifier = Modifier.weight(1f).height(if (compactLandscape) 42.dp else 48.dp),
                    ) { Text("OATEC website", fontSize = 11.sp) }
                    Button(
                        onClick = {
                            val rawUrl = serverUrl.trim()
                            val parsed = Uri.parse(rawUrl)
                            val queryUser = parsed.getQueryParameter("username").orEmpty().trim()
                            val queryPassword = parsed.getQueryParameter("password").orEmpty()
                            val isXtreamPlaylistUrl =
                                parsed.lastPathSegment.equals("get.php", ignoreCase = true) &&
                                queryUser.isNotBlank() && queryPassword.isNotBlank()
                            val resolvedSource = if (isXtreamPlaylistUrl) {
                                val baseUrl = rawUrl.substringBefore('?').substringBeforeLast('/')
                                PlayerSource(
                                    id = UUID.randomUUID().toString(),
                                    name = sourceName.trim().ifBlank { "Xtream source" },
                                    type = SourceType.XTREAM,
                                    serverUrl = baseUrl,
                                    username = queryUser,
                                    password = queryPassword,
                                )
                            } else {
                                PlayerSource(
                                    id = UUID.randomUUID().toString(),
                                    name = sourceName.trim().ifBlank { if (isXtream) "Xtream source" else "M3U playlist" },
                                    type = if (isXtream) SourceType.XTREAM else SourceType.M3U,
                                    serverUrl = rawUrl,
                                    username = username.trim(),
                                    password = password,
                                )
                            }
                            when {
                                parsed.host.isNullOrBlank() || parsed.scheme !in setOf("http", "https") -> error = "دخل رابط HTTP أو HTTPS صحيح."
                                isXtream && (username.isBlank() || password.isBlank()) -> error = "دخل Username وPassword ديال Xtream."
                                isXtreamPlaylistUrl && (resolvedSource.serverUrl.isBlank() || resolvedSource.username.isBlank() || resolvedSource.password.isBlank()) -> error = "رابط Xtream M3U ناقص معلومات الدخول."
                                else -> onComplete(premiumCode.trim(), resolvedSource)
                            }
                        },
                        modifier = Modifier.weight(1f).height(if (compactLandscape) 42.dp else 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) { Text("اتصال وبدء الاستخدام", fontWeight = FontWeight.Bold, fontSize = if (compactLandscape) 11.sp else 14.sp) }
                }
            }
        }
    }
}

@Composable
private fun SetupField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    uri: Boolean = false,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 48.dp),
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(keyboardType = if (uri) KeyboardType.Uri else KeyboardType.Text),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun SettingsScreen(
    onHome: () -> Unit,
    preferences: PlayerPreferences,
    onKidsModeChanged: (Boolean) -> Unit,
    aboutRepository: OatecAboutRepository,
    onManageSources: () -> Unit,
    premiumCode: String,
    onSavePremiumCode: (String) -> Unit,
    onExit: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var resumePlayback by remember { mutableStateOf(preferences.resumePlayback()) }
    var about by remember { mutableStateOf<store.oatec.playertv.data.OatecAbout?>(null) }
    var loadingAbout by remember { mutableStateOf(false) }
    var aboutError by remember { mutableStateOf("") }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var premiumCodeInput by remember(premiumCode) { mutableStateOf(premiumCode) }
    var premiumCodeSaved by remember { mutableStateOf(false) }
    var kidsEnabled by remember { mutableStateOf(preferences.kidsModeEnabled()) }
    var showKidsPinDialog by remember { mutableStateOf(false) }
    var kidsPinInput by remember { mutableStateOf("") }
    var kidsCurrentPinInput by remember { mutableStateOf("") }
    var showClearKidsPinDialog by remember { mutableStateOf(false) }
    var showKidsExitDialog by remember { mutableStateOf(false) }
    var kidsExitPinInput by remember { mutableStateOf("") }
    var kidsExitPinError by remember { mutableStateOf("") }
    var clearKidsPinInput by remember { mutableStateOf("") }
    var clearKidsPinError by remember { mutableStateOf("") }
    var kidsPinError by remember { mutableStateOf("") }
    var kidsPinSaved by remember { mutableStateOf(false) }
    var showKidsRulesDialog by remember { mutableStateOf(false) }
    var kidsRuleItemId by remember { mutableStateOf("") }
    var kidsRuleMessage by remember { mutableStateOf("") }

    fun loadAbout() {
        loadingAbout = true
        aboutError = ""
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { aboutRepository.fetch() } }
                .onSuccess { about = it }
                .onFailure { aboutError = it.message ?: "Could not load OATEC information." }
            loadingAbout = false
        }
    }

    LaunchedEffect(Unit) { loadAbout() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
            ) { Text("← Back", fontSize = 12.sp) }
            Column(Modifier.weight(1f)) {
                Text("SETTINGS", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Playback, sources and OATEC preferences", color = TextMuted, fontSize = 12.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sources & playlists", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Manage existing providers or import a playlist.", color = TextMuted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onManageSources) { Text("Manage sources") }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PLAYBACK", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Resume VOD playback", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Continue movies and episodes from the saved position.", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = resumePlayback,
                        onCheckedChange = {
                            resumePlayback = it
                            preferences.setResumePlayback(it)
                        },
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                var playbackEngine by remember { mutableStateOf(preferences.playbackEngine()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playback engine", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Choose the playback engine for Live TV, movies and series.", color = TextMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("exoplayer" to "Media3 / ExoPlayer", "mpv" to "MPV").forEach { (value, label) ->
                            if (playbackEngine == value) {
                                Button(onClick = { playbackEngine = value; preferences.setPlaybackEngine(value) }) { Text(label) }
                            } else {
                                OutlinedButton(onClick = { playbackEngine = value; preferences.setPlaybackEngine(value) }) { Text(label) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Watch history", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("History is stored on this device.", color = TextMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { confirmClearHistory = true }) { Text("Clear history") }
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("KIDS MODE", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Parental PIN protection", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (preferences.hasKidsPin()) "PIN protected. Unknown content is hidden unless classified as child-safe or allowed manually."
                            else "Set a 4–8 digit PIN before enabling Kids Mode.",
                            color = TextMuted, fontSize = 11.sp,
                        )
                    }
                    Switch(checked = kidsEnabled, onCheckedChange = { enabled ->
                        if (enabled && !preferences.hasKidsPin()) {
                            kidsPinInput = ""
                            kidsCurrentPinInput = ""
                            kidsPinError = ""
                            showKidsPinDialog = true
                        } else if (!enabled && preferences.hasKidsPin()) {
                            // Exiting Kids Mode is parent-protected too.
                            kidsExitPinInput = ""
                            kidsExitPinError = ""
                            showKidsExitDialog = true
                        } else {
                            kidsEnabled = preferences.setKidsModeEnabled(enabled)
                            onKidsModeChanged(kidsEnabled)
                        }
                    })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showKidsRulesDialog = true; kidsRuleItemId = ""; kidsRuleMessage = "" }) { Text("Manage content rules") }
                    OutlinedButton(onClick = {
                        kidsPinInput = ""
                        kidsCurrentPinInput = ""
                        kidsPinError = ""
                        kidsPinSaved = false
                        showKidsPinDialog = true
                    }) { Text(if (preferences.hasKidsPin()) "Set / change PIN" else "Create PIN") }
                    if (preferences.hasKidsPin()) OutlinedButton(onClick = { clearKidsPinInput = ""; clearKidsPinError = ""; showClearKidsPinDialog = true }) { Text("Remove PIN") }
                    if (kidsPinSaved) Text("PIN saved", color = GoldLight, fontSize = 11.sp)
                }
                if (showClearKidsPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearKidsPinDialog = false; clearKidsPinInput = ""; clearKidsPinError = "" },
                        title = { Text("Remove Kids Mode PIN?") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("You must enter the current PIN. Removing it will also turn Kids Mode off.", color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = clearKidsPinInput,
                                    onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) clearKidsPinInput = value },
                                    label = { Text("Current PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (clearKidsPinError.isNotBlank()) Text(clearKidsPinError, color = Color(0xFFFF7777), fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (preferences.clearKidsPin(clearKidsPinInput)) {
                                    kidsEnabled = false
                                    onKidsModeChanged(false)
                                    showClearKidsPinDialog = false
                                    clearKidsPinInput = ""
                                    clearKidsPinError = ""
                                } else clearKidsPinError = "Incorrect PIN."
                            }) { Text("Remove PIN") }
                        },
                        dismissButton = { TextButton(onClick = { showClearKidsPinDialog = false; clearKidsPinInput = ""; clearKidsPinError = "" }) { Text("Cancel") } },
                    )
                }
                if (showKidsRulesDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsRulesDialog = false },
                        title = { Text("Kids Mode content rules") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Enter the exact catalogue item ID to allow or block it. These rules are saved on this device.", color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(value = kidsRuleItemId, onValueChange = { kidsRuleItemId = it.trim() }, label = { Text("Catalogue item ID") }, singleLine = true)
                                Text("Allowed: ${preferences.kidsAllowedItemIds().size} · Blocked: ${preferences.kidsBlockedItemIds().size}", color = TextMuted, fontSize = 11.sp)
                                if (kidsRuleMessage.isNotBlank()) Text(kidsRuleMessage, color = GoldLight, fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else { preferences.setKidsItemAllowed(kidsRuleItemId, true); kidsRuleMessage = "Item allowed on this device." }
                                }) { Text("Allow") }
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else { preferences.setKidsItemAllowed(kidsRuleItemId, false); kidsRuleMessage = "Item blocked on this device." }
                                }) { Text("Block") }
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else kidsRuleMessage = if (preferences.clearKidsItemRule(kidsRuleItemId)) "Override cleared; automatic classification applies." else "No override exists for this item."
                                }) { Text("Clear rule") }
                            }
                        },
                        dismissButton = { TextButton(onClick = { showKidsRulesDialog = false }) { Text("Close") } },
                    )
                }
                if (showKidsExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" },
                        title = { Text("Parent PIN required") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Enter the parent PIN to leave Kids Mode.", color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = kidsExitPinInput,
                                    onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsExitPinInput = value },
                                    label = { Text("Parent PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (kidsExitPinError.isNotBlank()) Text(kidsExitPinError, color = ErrorRed, fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (preferences.verifyKidsPin(kidsExitPinInput)) {
                                    kidsEnabled = preferences.setKidsModeEnabled(false)
                                    showKidsExitDialog = false
                                    kidsExitPinInput = ""
                                    kidsExitPinError = ""
                                } else kidsExitPinError = "Incorrect PIN."
                            }) { Text("Exit Kids Mode") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" }) { Text("Cancel") }
                        },
                    )
                }
                if (showKidsPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsPinDialog = false; kidsPinInput = ""; kidsCurrentPinInput = ""; kidsPinError = "" },
                        title = { Text(if (preferences.hasKidsPin()) "Set a new Kids Mode PIN" else "Create Kids Mode PIN") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (preferences.hasKidsPin()) {
                                    OutlinedTextField(
                                        value = kidsCurrentPinInput,
                                        onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsCurrentPinInput = value },
                                        label = { Text("Current PIN") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    )
                                }
                                OutlinedTextField(
                                    value = kidsPinInput,
                                    onValueChange = { value ->
                                        if (value.length <= 8 && value.all(Char::isDigit)) kidsPinInput = value
                                        kidsPinError = ""
                                    },
                                    label = { Text("4–8 digit PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (kidsPinError.isNotBlank()) Text(kidsPinError, color = Color(0xFFFF7777), fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val saved = if (preferences.hasKidsPin()) preferences.changeKidsPin(kidsCurrentPinInput, kidsPinInput) else preferences.setKidsPin(kidsPinInput)
                                if (saved) {
                                    kidsEnabled = true
                                    onKidsModeChanged(true)
                                    kidsPinSaved = true
                                    showKidsPinDialog = false
                                    kidsPinInput = ""
                                    kidsCurrentPinInput = ""
                                    kidsPinError = ""
                                } else kidsPinError = if (preferences.hasKidsPin()) "Check the current PIN and enter a new 4–8 digit PIN." else "Enter 4 to 8 digits."
                            }) { Text("Save PIN") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKidsPinDialog = false; kidsPinInput = ""; kidsCurrentPinInput = ""; kidsPinError = "" }) { Text("Cancel") }
                        },
                    )
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("OATEC PREMIUM", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                OutlinedTextField(
                    value = premiumCodeInput,
                    onValueChange = { premiumCodeInput = it; premiumCodeSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Premium code (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text("The code is stored securely. Online verification is not available until OATEC provides an activation API.", color = TextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onSavePremiumCode(premiumCodeInput.trim()); premiumCodeSaved = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text("Save code") }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/"))) },
                    ) { Text("Open OATEC website") }
                    if (premiumCodeSaved) Text("Saved", color = GoldLight, fontSize = 11.sp)
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ABOUT OATEC", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                when {
                    loadingAbout && about == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Lime, strokeWidth = 2.dp)
                        Text("Loading information from oatec.store", color = TextMuted, fontSize = 12.sp)
                    }
                    about != null -> {
                        Text(about!!.title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(about!!.description, color = TextMuted, fontSize = 13.sp)
                        Text(about!!.website, color = Lime, fontSize = 12.sp)
                    }
                    aboutError.isNotBlank() -> Text(aboutError, color = ErrorRed, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::loadAbout, enabled = !loadingAbout) { Text("Refresh info") }
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(about?.website ?: "https://oatec.store/")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text("Visit website") }
                    OutlinedButton(onClick = onExit) { Text("Exit app") }
                }
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear watch history?") },
            text = { Text("Saved playback positions will also be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { preferences.clearWatchHistory(); confirmClearHistory = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HomeDashboard(
    compact: Boolean,
    sourceName: String?,
    onSelect: (Section) -> Unit,
    onExit: () -> Unit,
) {
    var selectedHomeCard by remember { mutableStateOf(Section.LIVE) }
    Box(
        Modifier.fillMaxSize()
            .background(Ink)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x665A350A), Color(0x1AE8B83E), Color.Transparent),
                        center = Offset(size.width * 0.52f, size.height * 0.08f),
                        radius = size.maxDimension * 0.88f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x44331D08), Color.Transparent),
                        center = Offset(size.width * 0.08f, size.height * 0.92f),
                        radius = size.maxDimension * 0.62f,
                    ),
                )
            },
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 34.dp, vertical = if (compact) 12.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(if (compact) 62.dp else 104.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
            ) {
                Surface(
                    color = Color(0xFF090B0E),
                    shape = RoundedCornerShape(if (compact) 12.dp else 18.dp),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(GoldLight, Lime, Color(0xFF8B5812)))),
                    modifier = Modifier.size(if (compact) 48.dp else 76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("▶", color = Lime, fontSize = if (compact) 16.sp else 25.sp, fontWeight = FontWeight.Black)
                            Text("TVI", color = GoldLight, fontSize = if (compact) 9.sp else 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("TVI", color = Lime, fontSize = if (compact) 20.sp else 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("PLAYER", color = TextMain, fontSize = if (compact) 20.sp else 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("Your entertainment, your way", color = TextMuted, fontSize = if (compact) 9.sp else 15.sp, maxLines = 1)
                }
                Column(
                    Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛒", color = Lime, fontSize = if (compact) 17.sp else 27.sp)
                        Text("OATEC", color = GoldLight, fontSize = if (compact) 17.sp else 30.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("STORE", color = TextMain.copy(alpha = 0.86f), fontSize = if (compact) 11.sp else 18.sp, fontWeight = FontWeight.Light, maxLines = 1)
                    }
                    Text("VOTRE BOUTIQUE EN LIGNE", color = Lime.copy(alpha = 0.8f), fontSize = if (compact) 7.sp else 10.sp, letterSpacing = if (compact) 1.sp else 2.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("◉  WIFI", color = TextMain.copy(alpha = 0.8f), fontSize = if (compact) 9.sp else 13.sp)
                    OutlinedButton(
                        onClick = { onSelect(Section.SETTINGS) },
                        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 13.dp, vertical = 2.dp),
                    ) { Text("⚙  ${if (compact) "Settings" else "SETTINGS"}", color = GoldLight, fontSize = if (compact) 9.sp else 12.sp) }
                }
            }

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cards = listOf(
                    HomeCardData(Section.LIVE, "TV LIVE", "Chaînes en direct", "▣", "LIVE TV", listOf(Color(0xFF193348), Color(0xFF07121C))),
                    HomeCardData(Section.MOVIES, "FILM", "Films à la demande", "▰", "CINÉMA", listOf(Color(0xFF4A351D), Color(0xFF100D0A))),
                    HomeCardData(Section.SERIES, "SIRRIES", "Séries & épisodes", "▤", "SIRRIES", listOf(Color(0xFF263746), Color(0xFF0A0D12))),
                    HomeCardData(Section.DOWNLOADS, "DOWNLOADS", "Regarder hors ligne", "⬇", "OFFLINE", listOf(Color(0xFF244536), Color(0xFF08140F))),
                    HomeCardData(Section.SETTINGS, "SITTING", "Préférences & sources", "⚙", "OATEC", listOf(Color(0xFF3C2B13), Color(0xFF080A0D))),
                )
                cards.forEachIndexed { index, card ->
                    val active = card.section == selectedHomeCard
                    Surface(
                        color = Color(0xCC05080C),
                        shape = RoundedCornerShape(if (compact) 12.dp else 20.dp),
                        border = BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) Brush.linearGradient(listOf(GoldLight, Lime, Color(0xFFB87816)))
                            else Brush.linearGradient(listOf(Color(0xFF41434A), Color(0xFF20242B))),
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .onFocusChanged { if (it.isFocused) selectedHomeCard = card.section }
                            .focusable()
                            .clickable {
                                selectedHomeCard = card.section
                                onSelect(card.section)
                            }
                            .drawBehind {
                                if (active) {
                                    drawRect(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0x44E8B83E), Color.Transparent),
                                            center = Offset(size.width * 0.5f, size.height * 0.28f),
                                            radius = size.maxDimension * 0.65f,
                                        ),
                                    )
                                }
                            },
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = if (compact) 7.dp else 14.dp, vertical = if (compact) 8.dp else 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier.fillMaxWidth().weight(1f)
                                    .background(Brush.verticalGradient(card.artColors), RoundedCornerShape(if (compact) 8.dp else 14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    card.artLabel,
                                    color = TextMain.copy(alpha = 0.10f),
                                    fontSize = if (compact) 26.sp else 48.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(card.symbol, color = Lime, fontSize = if (compact) 37.sp else 62.sp, fontWeight = FontWeight.Bold)
                                    Box(Modifier.width(if (compact) 35.dp else 54.dp).height(3.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Lime, GoldLight, Color.Transparent))))
                                }
                            }
                            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
                            Text(card.title, color = if (active) GoldLight else TextMain, fontSize = if (compact) 12.sp else 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Text(card.subtitle, color = TextMuted, fontSize = if (compact) 8.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.fillMaxWidth().padding(top = if (compact) 4.dp else 10.dp), horizontalArrangement = Arrangement.End) {
                                Text("›", color = if (active) Lime else TextMuted, fontSize = if (compact) 23.sp else 31.sp, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(if (compact) 28.dp else 42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(if (compact) 22.dp else 34.dp).height(2.dp).background(Lime))
                    Text("OATEC", color = Lime, fontSize = if (compact) 9.sp else 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text("STORE", color = TextMain, fontSize = if (compact) 9.sp else 13.sp, letterSpacing = 3.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("♢  Service fiable", color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                    Text("ϟ  Accès rapide", color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                    Text("◉  Support 24/7", color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                }
                Text("Votre satisfaction est notre priorité", color = GoldLight, fontSize = if (compact) 8.sp else 13.sp, maxLines = 1)
            }
        }
    }
}

private data class HomeCardData(
    val section: Section,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val artLabel: String,
    val artColors: List<Color>,
)

@Composable
private fun NavigationRail(
    selected: Section,
    sourceName: String?,
    compact: Boolean,
    onSelect: (Section) -> Unit,
    onHome: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF171D28), Color(0xFF0B0E14))))
            .padding(horizontal = if (compact) 8.dp else 22.dp, vertical = if (compact) 7.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = Color(0xFF0D1117),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(Lime, GoldLight))),
                modifier = Modifier.size(if (compact) 32.dp else 40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("TVI", color = Lime, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("TVI PLAYER", color = TextMain, fontSize = if (compact) 12.sp else 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (!sourceName.isNullOrBlank()) Text(sourceName, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("← Back", fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("Exit", fontSize = 10.sp)
            }
        }
        // Compact destinations replace the removed oversized top strip. Each opens content directly.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Section.LIVE to "LIVE TV",
                Section.MOVIES to "Movies",
                Section.SERIES to "Series",
                Section.SETTINGS to "Settings",
                Section.SOURCES to "Sources",
            ).forEach { (destination, label) ->
                OutlinedButton(
                    onClick = { onSelect(destination) },
                    modifier = Modifier.heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = if (compact) 9.dp else 13.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, if (selected == destination) Lime else Color.White.copy(alpha = 0.18f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selected == destination) Lime else TextMain,
                    ),
                ) {
                    Text(label, fontSize = if (compact) 10.sp else 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    sourceName: String?,
    search: String,
    onSearch: (String) -> Unit,
    busy: Boolean,
    onRefresh: () -> Unit,
    showRefresh: Boolean,
    compact: Boolean,
    onAddSource: () -> Unit,
    onNumberPad: () -> Unit,
    numberInput: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 28.dp, vertical = if (compact) 8.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = if (compact) 17.sp else 27.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(sourceName ?: "Add an IPTV source to begin", color = GoldLight.copy(alpha = 0.88f), fontSize = if (compact) 9.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (numberInput.isNotEmpty()) {
            Text("CH $numberInput", color = Lime, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (title == Section.LIVE.label) {
            OutlinedButton(onClick = onNumberPad, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("CH #", fontSize = 10.sp) }
        }
        SearchField(search, onSearch, Modifier.width(if (compact) 120.dp else 220.dp))
        if (showRefresh) {
            OutlinedButton(onClick = onRefresh, enabled = sourceName != null && !busy, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (compact) "Load" else "Refresh", fontSize = if (compact) 10.sp else 14.sp)
            }
        }
        Button(
            onClick = onAddSource,
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
            contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 16.dp, vertical = 4.dp),
        ) {
            Text(if (compact) "Add" else "Add source", fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 14.sp)
        }
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.height(54.dp),
        singleLine = true,
        placeholder = { Text("Search", color = TextMuted, fontSize = 13.sp) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun LiveScreen(
    catalog: SourceCatalog,
    epgPrograms: List<EpgProgram>,
    epgClock: Long,
    epgStatus: String,
    category: String,
    availableCategories: List<String>,
    search: String,
    preferences: PlayerPreferences,
    preferenceRevision: Int,
    message: String,
    busy: Boolean,
    catalogProgress: Float,
    catalogLoadedItems: Int,
    onCategory: (String) -> Unit,
    onSearch: (String) -> Unit,
    onHome: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
    onOrganize: (MediaEntry) -> Unit,
    onHide: (MediaEntry) -> Unit,
) {
    val channels = remember(catalog.liveChannels, category, search, preferenceRevision) {
        visibleChannels(catalog, category, search, preferences)
    }
    var selectedChannel by remember(channels) { mutableStateOf(channels.firstOrNull()) }
    val categories = remember(availableCategories, catalog.liveChannels, preferenceRevision) {
        (listOf("All", "Favorites", "Hidden") + availableCategories +
            catalog.liveChannels.map { preferences.categoryFor(it.id, it.category) } +
            preferences.customCategories()
        ).filter(String::isNotBlank).distinct()
    }
    val listState = rememberLazyGridState()
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, channels.size) {
        if (channels.isNotEmpty() && lastVisible >= channels.size - 10) onLoadMore()
    }
    Row(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.weight(1.6f).fillMaxHeight().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE TV", color = Lime, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("← Back", fontSize = 11.sp) }
            }
            OutlinedTextField(
                value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                singleLine = true, placeholder = { Text("Search channels…", color = TextMuted) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain), shape = RoundedCornerShape(10.dp),
            )
            LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories, key = { it }) { item -> CategoryPill(item, category == item) { onCategory(item) } }
            }
            when {
                busy && catalog.liveChannels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState("Loading channels", catalogProgress, catalogLoadedItems) }
                channels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("No channels available", message.ifBlank { "Add or refresh an IPTV source to load channels." }) }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp), state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    gridItems(channels, key = { it.id }) { item ->
                        val selected = selectedChannel?.id == item.id
                        Surface(
                            color = if (selected) Raised else Panel, shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Lime else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth().height(86.dp).combinedClickable(
                                onClick = { selectedChannel = item },
                                onDoubleClick = { onPlay(item.copy(title = preferences.channelTitle(item.id, item.title))) },
                            ),
                        ) {
                            Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Box(Modifier.size(40.dp).background(Color.Black, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                    if (item.logoUrl.isNotBlank()) AsyncImage(item.logoUrl, contentDescription = item.title, modifier = Modifier.fillMaxSize().padding(2.dp))
                                    else Text(channelNumber(item, catalog.liveChannels, preferences).toString(), color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(channelNumber(item, catalog.liveChannels, preferences).toString().padStart(3, '0'), color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(preferences.channelTitle(item.id, item.title), color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, end = 14.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CHANNEL PREVIEW", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Surface(Modifier.fillMaxWidth().weight(1f), color = Panel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Lime.copy(alpha = 0.35f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        val preview = selectedChannel
                        if (preview != null) {
                            ChannelMiniPreview(entry = preview)
                        } else {
                            Text("TV", color = Lime, fontSize = 42.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(selectedChannel?.let { preferences.channelTitle(it.id, it.title) } ?: "Select a channel", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    selectedChannel?.let { item ->
                        Text("CH " + channelNumber(item, catalog.liveChannels, preferences).toString().padStart(3, '0') + " • " + preferences.categoryFor(item.id, item.category), color = TextMuted, fontSize = 10.sp)
                        val current = EpgRepository.currentProgram(epgPrograms, item.epgChannelId, epgClock)
                        val next = EpgRepository.nextProgram(epgPrograms, item.epgChannelId, epgClock)
                        Text(current?.title?.let { "NOW  " + it } ?: next?.title?.let { "NEXT  " + it } ?: "Programme information unavailable", color = GoldLight, fontSize = 11.sp, maxLines = 2)
                        Button(onClick = { onPlay(item.copy(title = preferences.channelTitle(item.id, item.title))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Watch full screen") }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onFavorite(item) }) { Text(if (item.id in preferences.favorites()) "★ Saved" else "☆ Favorite", color = Lime) }
                            TextButton(onClick = { onOrganize(item) }) { Text("Edit", color = TextMuted) }
                            TextButton(onClick = { onHide(item) }) { Text(if (item.id in preferences.hiddenChannels()) "Restore" else "Hide", color = TextMuted) }
                        }
                    }
                }
            }
            if (epgStatus.isNotBlank()) Text(epgStatus, color = TextMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable
private fun ChannelMiniPreview(entry: MediaEntry) {
    // Keep browsing lightweight: don't create a second ExoPlayer just for a silent preview.
    // The full decoder is created only when the user explicitly starts playback.
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (entry.logoUrl.isNotBlank()) {
            AsyncImage(
                model = entry.logoUrl,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(entry.title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Lime else Panel,
        contentColor = if (selected) Ink else TextMain,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.focusable().clickable(onClick = onClick),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun MediaScreen(
    title: String,
    entries: List<MediaEntry>,
    search: String,
    busy: Boolean,
    message: String,
    catalogProgress: Float,
    catalogLoadedItems: Int,
    preferences: PlayerPreferences,
    recentEntries: List<MediaEntry>,
    onHome: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    var selectedShelf by rememberSaveable(title) { mutableStateOf("All") }
    val shelves = remember(entries, title) {
        val providerCategories = entries.map { it.category.trim() }.filter { it.isNotBlank() && !it.equals("Other", true) }.distinct().sortedBy { it.lowercase() }
        val curated = listOf("Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western")
        val knownGenres = entries.flatMap { entry -> (entry.genre + "," + entry.title).split(",", "|", "/", "·").map { it.trim() } }
            .mapNotNull { raw -> curated.firstOrNull { it.equals(raw, true) } }.distinct()
        listOf("All") + (curated + providerCategories + knownGenres).distinct().filterNot { it.equals("All", true) }
    }
    val searched = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) || it.genre.contains(search, true) }
    val filtered = if (selectedShelf == "All") searched else if (selectedShelf in listOf("4K", "UHD", "1080p", "HD")) {
        searched.filter { qualityLabel(it).equals(selectedShelf, true) || (selectedShelf == "HD" && qualityLabel(it) == "1080p") }
    } else searched.filter { entry ->
        entry.category.equals(selectedShelf, true) ||
            entry.genre.split(",", "|", "/", "·").any { it.trim().equals(selectedShelf, true) } ||
            entry.title.contains(selectedShelf, true)
    }
    val listState = rememberLazyListState()
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, entries.size, selectedShelf, search, title) {
        if (entries.isNotEmpty() && selectedShelf == "All" && lastVisible >= listState.layoutInfo.totalItemsCount - 3) onLoadMore()
    }
    val isMovies = title.equals("Movies", true)
    val accent = if (isMovies) Color(0xFFE50914) else Color(0xFF8B5CF6)
    val rowShelves = remember(filtered, selectedShelf, search, title) {
        if (selectedShelf != "All" || search.isNotBlank()) listOf("Search results" to filtered)
        else {
            val grouped = linkedMapOf<String, List<MediaEntry>>()
            if (recentEntries.isNotEmpty()) grouped["Continue watching"] = recentEntries.take(12)
            val preferred = listOf("Action", "Adventure", "Animation", "Comedy", "Drama", "Family", "Fantasy", "Horror", "Sci-Fi", "Thriller")
            preferred.forEach { genre ->
                val matches = filtered.filter { entry ->
                    entry.category.equals(genre, true) || entry.genre.split(",", "|", "/", "·").any { it.trim().equals(genre, true) }
                }
                if (matches.isNotEmpty()) grouped[genre] = matches
            }
            filtered.groupBy { it.category.trim().ifBlank { "More to explore" } }
                .entries.sortedByDescending { it.value.size }.take(8).forEach { (name, items) ->
                    if (grouped.keys.none { it.equals(name, true) }) grouped[name] = items
                }
            if (grouped.isEmpty() || grouped.values.sumOf { it.size } < filtered.size / 2) grouped["Explore all titles"] = filtered
            grouped.entries.map { it.key to it.value }
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF09090B)).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text("← Back", fontSize = 11.sp) }
            Column(Modifier.weight(1f)) {
                Text(if (isMovies) "FILMS" else title.uppercase(), color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${filtered.size} titles · Discover your next favourite", color = TextMuted, fontSize = 11.sp)
            }
            SearchField(search, onSearch, Modifier.width(190.dp))
        }
        if (message.isNotBlank()) Text(message, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (shelves.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(shelves, key = { "shelf:$it" }) { shelf ->
                    CategoryPill(shelf, selectedShelf == shelf) { selectedShelf = shelf }
                }
            }
        }
        if (isMovies && search.isBlank() && selectedShelf == "All") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                listOf("4K", "UHD", "1080p", "HD").forEach { quality ->
                    val count = entries.count { qualityLabel(it).equals(quality, true) || (quality == "HD" && qualityLabel(it) == "1080p") }
                    if (count > 0) CategoryPill("$quality · $count", false) { selectedShelf = quality }
                }
            }
        }
        when {
            busy && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState("Loading library", catalogProgress, catalogLoadedItems) }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("Nothing to watch yet", message.ifBlank { "No titles match this category. Categories depend on the metadata supplied by your provider." }) }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (selectedShelf == "All" && search.isBlank()) {
                    val featured = filtered.firstOrNull { it.logoUrl.isNotBlank() } ?: filtered.first()
                    item(key = "featured-title") {
                        Box(Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 245.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxSize().clickable { onPlay(featured) },
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF202027),
                            ) {
                                Box {
                                    if (featured.logoUrl.isNotBlank()) AsyncImage(
                                        model = featured.logoUrl,
                                        contentDescription = featured.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Box(Modifier.fillMaxSize().background(
                                        Brush.horizontalGradient(listOf(Color(0xEE09090B), Color(0x9909090B), Color.Transparent))
                                    ))
                                    Column(
                                        Modifier.align(Alignment.BottomStart).fillMaxWidth(0.8f).padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(if (isMovies) "FEATURED MOVIE" else "FEATURED SERIES", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                                        Text(featured.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(listOf(featured.year, featured.genre, featured.rating.takeIf { it.isNotBlank() }?.let { "★ $it" }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                                            color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Button(onClick = { onPlay(featured) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                                            Text("▶  Watch now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                rowShelves.forEachIndexed { index, (rowTitle, rowEntries) ->
                    if (rowEntries.isNotEmpty()) {
                        item(key = "shelf-heading:$index:$rowTitle") {
                            Text(rowTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        item(key = "shelf-row:$index:$rowTitle") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
                                items(rowEntries.distinctBy { it.id }, key = { "poster:${rowTitle}:${it.id}" }) { entry ->
                                    PosterCard(entry = entry, favorite = entry.id in preferences.favorites(), onClick = { onPlay(entry) }, onFavorite = { onFavorite(entry) }, accent = accent, progressFraction = preferences.watchProgressFraction(entry.id))
                                }
                            }
                        }
                    }
                }
                if (busy) item(key = "catalog-loading") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { LoadingState("Loading more titles", catalogProgress, catalogLoadedItems) }
                }
            }
        }
    }
}

private fun qualityLabel(entry: MediaEntry): String {
    val text = "${entry.title} ${entry.category} ${entry.streamUrl}".lowercase()
    return when {
        listOf("2160p", "4k", "uhd", "hevc 4k").any { it in text } -> "4K"
        listOf("1080p", "fhd", "full hd").any { it in text } -> "1080p"
        listOf("720p", "hd").any { it in text } -> "HD"
        else -> ""
    }
}

private fun streamQualityRank(entry: MediaEntry): Int = when (qualityLabel(entry)) {
    "4K" -> 3
    "1080p" -> 2
    "HD" -> 1
    else -> 0
}

private fun sameChannelIdentity(first: MediaEntry, second: MediaEntry): Boolean {
    fun identity(entry: MediaEntry): String = entry.title.lowercase()
        .replace(Regex("\\b(4k|uhd|2160p|1080p|720p|fhd|hd|hevc|h265|h\\.265)\\b"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val a = identity(first)
    val b = identity(second)
    return a.isNotBlank() && a == b
}

@Composable
private fun PosterCard(entry: MediaEntry, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit, accent: Color = Lime, progressFraction: Float = 0f) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(142.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.68f).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        ) {
            Surface(
                color = Raised,
                shape = RoundedCornerShape(10.dp),
                border = if (focused) BorderStroke(2.dp, accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (entry.logoUrl.isNotBlank()) AsyncImage(
                    model = entry.logoUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) else Box(contentAlignment = Alignment.Center) {
                    Text(entry.title.take(1).uppercase(), color = accent, fontSize = 35.sp, fontWeight = FontWeight.Black)
                }
            }
            val quality = qualityLabel(entry)
            if (quality.isNotBlank()) {
                Surface(color = accent, shape = RoundedCornerShape(bottomEnd = 8.dp), modifier = Modifier.align(Alignment.TopStart)) {
                    Text(quality, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            TextButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(if (favorite) "♥" else "+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (progressFraction > 0f) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp),
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.28f),
                )
            }
        }
        Text(entry.title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        val subtitle = listOf(entry.year, entry.genre.ifBlank { entry.category }).firstOrNull { it.isNotBlank() }
        if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun OfflineDownloadsScreen(
    store: OfflineDownloadStore,
    onHome: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onMessage: (String) -> Unit,
) {
    var downloads by remember { mutableStateOf<List<OfflineItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            downloads = withContext(Dispatchers.IO) { store.list() }
            delay(1200)
        }
    }
    Column(Modifier.fillMaxSize().background(Ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                }) { Text("← Back") }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("DOWNLOADS", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("المحتوى المحمّل محفوظ فذاكرة الجهاز", color = TextMuted, fontSize = 12.sp)
            }
        }
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ما كاين حتى تحميل دابا. اختار فيلم واضغط Download offline.", color = TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads, key = { it.downloadId }) { item ->
                    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (item.logoUrl.isNotBlank()) AsyncImage(model = item.logoUrl, contentDescription = item.title, modifier = Modifier.size(50.dp, 68.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(item.title, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val label = when (item.status) {
                                    android.app.DownloadManager.STATUS_SUCCESSFUL -> if (item.isComplete) "جاهز للمشاهدة بلا إنترنت" else "الملف غير موجود"
                                    android.app.DownloadManager.STATUS_FAILED -> "فشل التحميل"
                                    android.app.DownloadManager.STATUS_PAUSED -> "التحميل متوقف مؤقتاً"
                                    else -> "كيتم التحميل… ${item.progress}%"
                                }
                                Text(label, color = if (item.isComplete) Color(0xFF70D6A2) else TextMuted, fontSize = 11.sp)
                                if (!item.isComplete && item.status != android.app.DownloadManager.STATUS_FAILED) {
                                    androidx.compose.material3.LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = Lime)
                                }
                            }
                            if (item.isComplete) Button(onClick = { onPlay(item.asMediaEntry()) }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Play") }
                            TextButton(onClick = {
                                store.delete(item.downloadId)
                                downloads = downloads.filterNot { it.downloadId == item.downloadId }
                                onMessage("تحيد التحميل ديال " + item.title)
                            }) { Text("حذف", color = ErrorRed) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsDialog(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onBrowseEpisodes: () -> Unit,
    onDownload: () -> Unit,
) {
    val metadata = listOf(
        "Category" to entry.category,
        "Genre" to entry.genre,
        "Year" to entry.year.ifBlank { entry.releaseDate.take(4) },
        "Rating" to entry.rating,
        "Release date" to entry.releaseDate,
        "Director" to entry.director,
        "Cast" to entry.cast,
    ).filter { it.second.isNotBlank() }
    val isSeries = entry.kind == MediaKind.SERIES

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 960.dp).heightIn(max = 720.dp),
            color = Panel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Lime.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        modifier = Modifier.weight(1f),
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = onDismiss) { Text("Close", color = GoldLight) }
                }
                HorizontalDivider(color = TextMain.copy(alpha = 0.1f))
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (entry.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = entry.logoUrl,
                            contentDescription = entry.title,
                            modifier = Modifier.width(160.dp).heightIn(max = 250.dp),
                        )
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (metadata.isNotEmpty()) {
                            metadata.forEach { (label, value) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("$label:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(value, color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                        Text("Synopsis", color = Lime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            entry.description.ifBlank { "No synopsis or additional details are available from this source." },
                            color = TextMain,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                        if (isSeries) {
                            Text("Open the episodes list to browse available seasons and episodes.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(color = TextMain.copy(alpha = 0.1f))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    if (!isSeries && entry.kind != MediaKind.LIVE) {
                        OutlinedButton(onClick = onDownload) { Text("⬇ Download offline") }
                    }
                    Button(
                        onClick = if (isSeries) onBrowseEpisodes else onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text(if (isSeries) "Episodes" else "Play") }
                }
            }
        }
    }
}

@Composable
private fun SeriesScreen(
    entries: List<MediaEntry>,
    episodes: List<MediaEntry>,
    selectedSeries: MediaEntry?,
    search: String,
    busy: Boolean,
    message: String,
    preferences: PlayerPreferences,
    onHome: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSelect: (MediaEntry) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    var selectedShelf by rememberSaveable { mutableStateOf("All") }
    val shelves = remember(entries) {
        val providerCategories = entries.map { it.category.trim() }.filter { it.isNotBlank() && !it.equals("Other", true) }.distinct().sortedBy { it.lowercase() }
        val curated = listOf("Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Mystery", "Reality", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western")
        val genres = entries.flatMap { it.genre.split(",", "|", "/", "·").map(String::trim) }
            .mapNotNull { raw -> curated.firstOrNull { it.equals(raw, true) } }.distinct()
        // Show standard genres even when the source has incomplete metadata.
        listOf("All") + (curated + providerCategories + genres).distinct().filterNot { it.equals("All", true) }
    }
    val searched = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) || it.genre.contains(search, true) }
    val filtered = if (selectedShelf == "All") searched else searched.filter { entry ->
        entry.category.equals(selectedShelf, true) ||
            entry.genre.split(",", "|", "/", "·").any { it.trim().equals(selectedShelf, true) } ||
            entry.title.contains(selectedShelf, true)
    }
    val gridState = rememberLazyGridState()
    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, entries.size, selectedShelf, search) {
        if (entries.isNotEmpty() && selectedShelf == "All" &&
            lastVisible >= gridState.layoutInfo.totalItemsCount - 6
        ) onLoadMore()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text("← Back", fontSize = 11.sp) }
            Column(Modifier.weight(1f)) {
                Text("SERIES", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Seasons and episodes • ${filtered.size} titles", color = TextMuted, fontSize = 11.sp)
            }
            SearchField(search, onSearch, Modifier.width(190.dp))
        }
        if (message.isNotBlank()) Text(message, color = ErrorRed, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (shelves.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(shelves, key = { "series-shelf:$it" }) { shelf ->
                    CategoryPill(shelf, selectedShelf == shelf) { selectedShelf = shelf }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.05f).fillMaxHeight()) {
                when {
                    filtered.isEmpty() -> EmptyState("No series available", message.ifBlank { "Series metadata is available from supported Xtream sources." })
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        gridItems(filtered, key = { it.id }) { entry ->
                            val selected = entry.id == selectedSeries?.id
                            Column(Modifier.fillMaxWidth().clickable { onSelect(entry) }) {
                                Surface(
                                    color = Raised,
                                    border = if (selected) BorderStroke(2.dp, Lime) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
                                ) {
                                    if (entry.logoUrl.isNotBlank()) AsyncImage(model = entry.logoUrl, contentDescription = entry.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Box(contentAlignment = Alignment.Center) { Text(entry.title.take(1).uppercase(), color = Lime, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                                }
                                if (preferences.watchProgressFraction(entry.id) > 0f) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { preferences.watchProgressFraction(entry.id) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = Color(0xFFE50914),
                                        trackColor = Color.White.copy(alpha = 0.28f),
                                    )
                                }
                                Text(entry.title, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                                TextButton(onClick = { onFavorite(entry) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                                    Text(if (entry.id in preferences.favorites()) "★ Saved" else "+ Favorite", color = Lime, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(0.95f).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text(selectedSeries?.title ?: "SEASONS & EPISODES", color = Lime, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                selectedSeries?.let { series ->
                    if (series.logoUrl.isNotBlank()) AsyncImage(series.logoUrl, contentDescription = series.title, modifier = Modifier.size(58.dp, 76.dp))
                    val details = listOfNotNull(
                        series.year.takeIf(String::isNotBlank),
                        series.rating.takeIf(String::isNotBlank)?.let { "Rating $it" },
                        series.genre.takeIf(String::isNotBlank),
                    )
                    if (details.isNotEmpty()) Text(details.joinToString(" | "), color = GoldLight, fontSize = 10.sp)
                    if (series.description.isNotBlank()) Text(series.description, color = TextMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                }
                when {
                    busy -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { LoadingState("Loading episodes") }
                    selectedSeries == null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { EmptyState("Select a series", "Choose a poster to load its seasons and episodes.") }
                    episodes.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { EmptyState("No episode data", "This provider did not return playable episodes.") }
                    else -> LazyColumn(Modifier.weight(1f)) {
                        episodes.groupBy { it.season }.forEach { (season, seasonEpisodes) ->
                            item(key = "season:$season") {
                                Text(season, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(seasonEpisodes, key = { it.id }) { episode ->
                                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    OutlinedButton(onClick = { onPlay(episode) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("${episode.episodeNumber.padStart(2, '0')}  ${episode.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    val episodeProgress = preferences.watchProgressFraction(episode.id)
                                    if (episodeProgress > 0f) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { episodeProgress },
                                            modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 4.dp),
                                            color = Color(0xFFE50914),
                                            trackColor = Color.White.copy(alpha = 0.22f),
                                        )
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
private fun SourcesScreen(
    sources: List<PlayerSource>,
    activeId: String,
    busy: Boolean,
    onSelect: (PlayerSource) -> Unit,
    onRefresh: (PlayerSource) -> Unit,
    onDelete: (PlayerSource) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your IPTV sources", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Source access is provided by your IPTV provider.", color = TextMuted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onImport) { Text("Import M3U file") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Add source") }
        }
        Spacer(Modifier.height(20.dp))
        if (sources.isEmpty()) EmptyState("No sources added", "Add an M3U playlist or an Xtream-compatible account.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sources, key = { it.id }) { source ->
                var focused by remember { mutableStateOf(false) }
                Surface(
                    color = if (source.id == activeId) Raised else Panel,
                    shape = RoundedCornerShape(9.dp),
                    border = if (focused || source.id == activeId) BorderStroke(1.dp, Lime.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.focusable().clickable { onSelect(source) },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(source.name, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${source.type.name.replace('_', ' ')} | ${source.serverUrl}", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (source.serverUrl.startsWith("http://", true)) {
                                Text("Unencrypted HTTP source", color = ErrorRed, fontSize = 10.sp)
                            }
                        }
                        OutlinedButton(onClick = { onRefresh(source) }, enabled = !busy) { Text("Refresh") }
                        TextButton(onClick = { onDelete(source) }) { Text("Remove", color = ErrorRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onSave: (PlayerSource) -> Unit) {
    var type by rememberSaveable { mutableStateOf(SourceType.M3U.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var epgUrl by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add IPTV source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SourceType.M3U, SourceType.XTREAM).forEach { option ->
                        CategoryPill(option.name, type == option.name) { type = option.name }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Source name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text(if (type == SourceType.M3U.name) "M3U URL" else "Server URL") }, singleLine = true)
                OutlinedTextField(epgUrl, { epgUrl = it }, label = { Text("EPG XMLTV URL (optional)") }, singleLine = true)
                if (type == SourceType.XTREAM.name || type == SourceType.M3U.name) {
                    OutlinedTextField(username, { username = it }, label = { Text("Username (optional for M3U)") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text("Password (optional for M3U)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (type == SourceType.M3U.name) {
                        Text("If you enter a server address instead of a full playlist URL, the app can build the get.php M3U link using these credentials.", color = TextMuted, fontSize = 10.sp)
                    }
                }
                if (url.startsWith("http://", true)) Text("HTTP is unencrypted. Prefer HTTPS when your provider supports it.", color = ErrorRed, fontSize = 11.sp)
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = Uri.parse(url.trim())
                if (name.isBlank()) error = "Enter a source name."
                else if (parsed.host.isNullOrBlank() || parsed.scheme !in setOf("http", "https")) error = "Enter a valid HTTP or HTTPS URL."
                else if (type == SourceType.XTREAM.name && (username.isBlank() || password.isBlank())) error = "Enter the provider username and password."
                else if (type == SourceType.M3U.name && (parsed.lastPathSegment.equals("get.php", true) || parsed.lastPathSegment.equals("player_api.php", true)) && (username.isBlank() || password.isBlank()) && parsed.getQueryParameter("username").isNullOrBlank()) error = "Enter the provider username and password for this playlist URL."
                else if (epgUrl.isNotBlank() && (Uri.parse(epgUrl.trim()).host.isNullOrBlank() || Uri.parse(epgUrl.trim()).scheme !in setOf("http", "https"))) error = "Enter a valid HTTP or HTTPS EPG URL."
                else {
                    val enteredUrl = url.trim()
                    val queryUser = parsed.getQueryParameter("username").orEmpty().ifBlank { username.trim() }
                    val queryPassword = parsed.getQueryParameter("password").orEmpty().ifBlank { password }
                    val finalUrl = if (type == SourceType.M3U.name &&
                        parsed.path.orEmpty().trim('/').isBlank() &&
                        queryUser.isNotBlank() && queryPassword.isNotBlank()
                    ) {
                        enteredUrl.trimEnd('/') + "/get.php?username=" +
                            Uri.encode(queryUser) + "&password=" + Uri.encode(queryPassword) +
                            "&type=m3u_plus&output=ts"
                    } else enteredUrl
                    onSave(
                        PlayerSource(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            type = SourceType.valueOf(type),
                            serverUrl = finalUrl,
                            username = username.trim(),
                            password = password,
                            epgUrl = epgUrl.trim(),
                        ),
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberPadDialog(
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onSelect: (MediaEntry) -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    val channel = digits.toIntOrNull()?.let { number ->
        numberedChannels(channels, preferences).firstOrNull { it.second == number }?.first
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tune a channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(digits.ifBlank { "Enter a channel number" }, color = if (digits.isBlank()) TextMuted else Lime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Clear", "0", "Back")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            OutlinedButton(
                                onClick = {
                                    digits = when (key) {
                                        "Clear" -> ""
                                        "Back" -> digits.dropLast(1)
                                        else -> (digits + key).takeLast(4)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(key) }
                        }
                    }
                }
                Text(channel?.title ?: if (digits.isBlank()) "" else "No channel with that number", color = if (channel == null) TextMuted else TextMain, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { channel?.let(onSelect) }, enabled = channel != null) { Text("Tune") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlaybackDialog(
    entry: MediaEntry,
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val trackSelector = remember(context) {
        DefaultTrackSelector(context).apply {
            // Adapt among provider HLS/DASH variants using measured bandwidth and supported device formats.
            parameters = buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setForceHighestSupportedBitrate(false)
                .build()
        }
    }
    val renderersFactory = remember(context) { DefaultRenderersFactory(context).setEnableDecoderFallback(true) }
    val player = remember(context) {
        ExoPlayer.Builder(context, renderersFactory).setTrackSelector(trackSelector).build().apply {
            val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
            setAudioAttributes(attrs, true)
            volume = 1f
        }
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    val qualityOptions = remember(entry.id, channels) {
        if (entry.kind != MediaKind.LIVE) emptyList()
        else channels.filter { it.kind == MediaKind.LIVE && sameChannelIdentity(it, entry) }
            .distinctBy { it.id }
            .sortedByDescending { streamQualityRank(it) }
    }
    var activeEngine by remember(entry.id) { mutableStateOf(preferences.playbackEngine()) }
    val mpvCaBundle = remember(context) {
        File(context.filesDir, "mpv-cacert.pem").apply {
            if (!exists()) {
                context.assets.open("cacert.pem").use { input ->
                    outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
    var mpvAttempt by remember(entry.id) { mutableStateOf(0) }
    var mpvView by remember(entry.id, mpvAttempt) { mutableStateOf<MpvView?>(null) }
    val channelIndex = channels.indexOfFirst { it.id == entry.id }
    val latestEntry by rememberUpdatedState(entry)
    val latestPreferences by rememberUpdatedState(preferences)
    val latestEngine by rememberUpdatedState(activeEngine)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                loading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_ENDED && latestEntry.kind != MediaKind.LIVE && latestEngine == "exoplayer") {
                    latestPreferences.saveProgress(latestEntry.id, 0L, player.duration.coerceAtLeast(0L))
                }
            }
            override fun onPlayerError(playbackException: PlaybackException) {
                loading = false
                error = playbackException.localizedMessage ?: "ExoPlayer couldn't play this stream. Check the URL and stream format."
                // Separate 4K/HD playlist links cannot switch by bandwidth measurement alone,
                // but retry a clearly named lower-quality sibling after a live playback error.
                if (latestEntry.kind == MediaKind.LIVE) {
                    val currentRank = streamQualityRank(latestEntry)
                    val sibling = channels
                        .filter { it.id != latestEntry.id && it.kind == MediaKind.LIVE }
                        .filter { sameChannelIdentity(it, latestEntry) && streamQualityRank(it) in 1 until currentRank }
                        .maxByOrNull { streamQualityRank(it) }
                    if (sibling != null) {
                        onPlay(sibling)
                        return
                    }
                }
                if (latestEngine == "exoplayer" && latestEntry.kind != MediaKind.LIVE && latestPreferences.resumePlayback()) {
                    latestPreferences.saveProgress(latestEntry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (latestEntry.kind != MediaKind.LIVE && latestPreferences.resumePlayback()) {
                if (latestEngine == "exoplayer") {
                    latestPreferences.saveProgress(latestEntry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                } else if (latestEngine == "mpv") {
                    runCatching {
                        val positionMs = ((mpvView?.timePos ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        val durationMs = ((mpvView?.duration ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        if (positionMs > 0L) latestPreferences.saveProgress(latestEntry.id, positionMs, durationMs)
                    }
                }
            }
            player.stop()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, entry.id, activeEngine) {
        while (true) {
            delay(5_000)
            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                if (activeEngine == "exoplayer" && player.isPlaying) {
                    preferences.saveProgress(entry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                } else if (activeEngine == "mpv") {
                    runCatching {
                        val positionMs = ((mpvView?.timePos ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        val durationMs = ((mpvView?.duration ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        if (positionMs > 0L) preferences.saveProgress(entry.id, positionMs, durationMs)
                    }
                }
            }
        }
    }

    LaunchedEffect(entry.id, activeEngine, mpvAttempt, mpvView) {
        error = ""
        loading = true
        if (entry.kind != MediaKind.LIVE) preferences.recordWatched(entry.id)
        if (activeEngine == "exoplayer") {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(entry.streamUrl))
            player.prepare()
            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                val position = preferences.progress(entry.id)
                if (position > 5_000L) player.seekTo(position)
            }
            player.playWhenReady = true
        } else {
            // Do not leave Media3 decoding audio/video in the background when MPV is selected.
            player.stop()
            player.clearMediaItems()
            val view = mpvView ?: return@LaunchedEffect
            runCatching {
                // MPV initialization is expensive on some TV boxes, so prepare its native core off the UI thread.
                val prepared: PreparedCore = withContext(Dispatchers.IO) {
                    view.prepare(
                        MpvOptions(
                            tlsCaFile = mpvCaBundle,
                            // Prefer Arabic subtitles, then English/French when the stream provides them.
                            subtitleLanguages = listOf("ar", "ara", "en", "eng", "fr", "fre"),
                            // Automatically discover adjacent external subtitle files where supported.
                            extra = mapOf("sub-auto" to "all"),
                        ),
                    )
                }
                view.attach(prepared)
                view.playFile(entry.streamUrl).getOrThrow()
                if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                    val resumePosition = preferences.progress(entry.id)
                    if (resumePosition > 5_000L) {
                        // MPV reports positions in seconds; the shared progress store uses milliseconds.
                        view.timePos = resumePosition / 1_000.0
                    }
                }
            }.onSuccess {
                loading = false
            }.onFailure { failure ->
                loading = false
                error = failure.localizedMessage ?: "MPV could not start this stream."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Ink).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (key.keyCode) {
                    AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_ESCAPE -> {
                        if (showList) { showList = false; true } else false
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (channels.isEmpty()) false else {
                            onPlay(channels[if (channelIndex <= 0) channels.lastIndex else channelIndex - 1]); true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (channels.isEmpty()) false else {
                            onPlay(channels[if (channelIndex < 0 || channelIndex == channels.lastIndex) 0 else channelIndex + 1]); true
                        }
                    }
                    else -> false
                }
            },
        ) {
            if (activeEngine == "mpv") {
                key(mpvAttempt) {
                    AndroidView(
                        factory = { viewContext ->
                            MpvView(viewContext).apply {
                                keepScreenOn = true
                            }.also { mpvView = it }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { it.destroy() },
                    )
                }
            } else {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            keepScreenOn = true
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (entry.kind == MediaKind.LIVE) {
                Column(
                    Modifier.align(Alignment.TopStart).padding(24.dp)
                        .background(Ink.copy(alpha = 0.82f), RoundedCornerShape(8.dp)).padding(14.dp),
                ) {
                    Text("${(channelIndex + 1).coerceAtLeast(1).toString().padStart(3, '0')}  |  LIVE", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(entry.title, color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(entry.category, color = TextMuted, fontSize = 12.sp)
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.kind == MediaKind.LIVE && qualityOptions.size > 1) {
                    OutlinedButton(onClick = { showQualityDialog = true }) { Text("Choose quality") }
                }
                if (entry.kind == MediaKind.LIVE && channels.isNotEmpty()) {
                    OutlinedButton(onClick = { showList = !showList }) { Text(if (showList) "Hide channels" else "Channels") }
                }
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Lime)
            if (error.isNotBlank()) {
                Column(
                    Modifier.align(Alignment.Center).background(Ink.copy(alpha = 0.94f), RoundedCornerShape(10.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Playback unavailable", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(error, color = TextMuted, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            error = ""
                            loading = true
                            if (activeEngine == "mpv") {
                                mpvAttempt++
                            } else {
                                player.prepare()
                                player.play()
                            }
                        }) { Text("Retry") }
                    }
                }
            }
            if (showList) {
                LazyColumn(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(330.dp)
                        .background(Ink.copy(alpha = 0.96f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                        Surface(
                            color = if (channel.id == entry.id) Raised else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().focusable().clickable { showList = false; onPlay(channel) },
                        ) {
                            Text(
                                "${(index + 1).toString().padStart(3, '0')}  ${channel.title}",
                                color = if (channel.id == entry.id) Lime else TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Choose stream quality") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(qualityOptions, key = { it.id }) { option ->
                        Surface(
                            color = if (option.id == entry.id) Raised else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showQualityDialog = false
                                onPlay(option)
                            },
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(option.title, color = if (option.id == entry.id) Lime else TextMain, fontWeight = FontWeight.SemiBold)
                                Text(qualityLabel(option).ifBlank { "Quality not identified" }, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQualityDialog = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun OrganizeChannelDialog(
    channel: MediaEntry,
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit,
) {
    var title by rememberSaveable(channel.id) { mutableStateOf(preferences.channelTitle(channel.id, channel.title)) }
    var category by rememberSaveable(channel.id) { mutableStateOf(preferences.categoryFor(channel.id, channel.category)) }
    var channelNumber by rememberSaveable(channel.id) { mutableStateOf(channelNumber(channel, channels, preferences).toString()) }
    var newCategory by rememberSaveable(channel.id) { mutableStateOf("") }
    var error by rememberSaveable(channel.id) { mutableStateOf("") }
    val categories = (channels.map { it.category } + preferences.customCategories()).distinct().sorted()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit channel") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Channel name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = channelNumber,
                    onValueChange = { channelNumber = it.filter(Char::isDigit).take(5); error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Channel number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("Move to category", color = TextMuted, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(categories, key = { it }) { option ->
                        CategoryPill(option, category == option) { category = option }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        newCategory,
                        { newCategory = it; error = "" },
                        label = { Text("New category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        if (!preferences.addCategory(newCategory)) error = "Enter a new category name."
                        else {
                            category = newCategory.trim()
                            newCategory = ""
                        }
                    }) { Text("Create") }
                }
                Text("If the number is in use, the two channels will swap numbers.", color = TextMuted, fontSize = 10.sp)
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val number = channelNumber.toIntOrNull()
                when {
                    title.isBlank() -> error = "Enter a channel name."
                    category.isBlank() -> error = "Choose a category."
                    number == null || number < 1 -> error = "Enter a channel number greater than zero."
                    else -> onSave(title.trim(), category, number)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LoadingState(label: String, progress: Float? = null, loadedItems: Int = 0) {
    if (progress == null) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Lime)
            Text(label, color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(350), label = "catalog-cup-fill")
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.width(108.dp).height(142.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp)
                .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 8.dp, topEnd = 8.dp))
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 5.dp, topEnd = 5.dp))) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(animatedProgress).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(GoldLight, Lime, Color(0xFFB77D17)))))
            }
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent,
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 8.dp, topEnd = 8.dp),
                border = BorderStroke(3.dp, GoldLight.copy(alpha = 0.9f))) {}
            Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.TopCenter).padding(horizontal = 1.dp).background(GoldLight, RoundedCornerShape(50)))
        }
        Spacer(Modifier.height(22.dp))
        Text(label, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("${(animatedProgress * 100).toInt()}%", color = GoldLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (loadedItems > 0) Text("$loadedItems items loaded", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text("Please wait until the catalogue is ready", color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = TextMuted, fontSize = 12.sp)
    }
}

private fun visibleChannels(
    catalog: SourceCatalog,
    category: String,
    search: String,
    preferences: PlayerPreferences,
): List<MediaEntry> = numberedChannels(catalog.liveChannels, preferences).map { it.first }.filter { channel ->
    val hidden = channel.id in preferences.hiddenChannels()
    (when (category) {
        "All" -> !hidden
        "Favorites" -> !hidden && channel.id in preferences.favorites()
        "Hidden" -> hidden
        else -> !hidden && preferences.categoryFor(channel.id, channel.category) == category
    }) &&
        (search.isBlank() || channel.title.contains(search, true) || channel.category.contains(search, true))
}

@Composable
private fun KidsDashboard(
    compact: Boolean,
    section: Section,
    movies: List<MediaEntry>,
    series: List<MediaEntry>,
    channels: List<MediaEntry>,
    search: String,
    onSearch: (String) -> Unit,
    onSection: (Section) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onPlayYoutube: (String) -> Unit,
    onMovieDetails: (MediaEntry) -> Unit,
    onExit: () -> Unit,
    onLoadMore: () -> Unit,
) {
    var showYoutubeKidsSearch by rememberSaveable { mutableStateOf(false) }
    var youtubeResults by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var youtubeLoading by remember { mutableStateOf(false) }
    var youtubeError by remember { mutableStateOf("") }
    var selectedKidCategory by remember(section) { mutableStateOf("الكل") }
    LaunchedEffect(search) {
        val query = search.trim()
        if (query.length < 2) {
            youtubeResults = emptyList()
            youtubeError = ""
            youtubeLoading = false
        } else {
            delay(450)
            youtubeLoading = true
            youtubeError = ""
            runCatching { withContext(Dispatchers.IO) { searchYouTubeKids(query) } }
                .onSuccess { youtubeResults = it }
                .onFailure { youtubeResults = emptyList(); youtubeError = it.message ?: "تعذر تحميل نتائج YouTube" }
            youtubeLoading = false
        }
    }
    val allVisible = remember(section, movies, series, channels, search, youtubeResults) {
        val source = when (section) {
            Section.LIVE -> channels
            Section.SERIES -> series
            else -> movies
        }
        val catalogueResults = source.filter { search.isBlank() || it.title.contains(search, ignoreCase = true) || it.category.contains(search, ignoreCase = true) }
        if (search.isBlank()) catalogueResults else (catalogueResults + youtubeResults).distinctBy { it.id }
    }
    val kidCategories = remember(allVisible) {
        allVisible.map { it.category.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }.take(18)
    }
    val visible = remember(allVisible, selectedKidCategory) {
        if (selectedKidCategory == "الكل") allVisible
        else allVisible.filter { it.category.equals(selectedKidCategory, ignoreCase = true) }
    }
    val kidShelves = remember(visible, section, search) {
        if (search.isNotBlank()) listOf("نتائج البحث 🔎" to visible)
        else {
            val result = linkedMapOf<String, List<MediaEntry>>()
            if (visible.isNotEmpty()) result["✨ اختياراتنا ليك"] = visible.take(14)
            listOf("Animation", "Kids", "Children", "Educational", "Family", "Cartoon", "Anime").forEach { category ->
                val matches = visible.filter { entry ->
                    entry.category.contains(category, true) || entry.genre.contains(category, true) || entry.title.contains(category, true)
                }
                if (matches.isNotEmpty() && result.keys.none { it.contains(category, true) }) result[when(category) {
                    "Educational" -> "🧠 تعلم والعب"
                    "Family" -> "👨‍👩‍👧‍👦 وقت العائلة"
                    "Animation", "Cartoon" -> "🎨 عالم الكرتون"
                    "Anime" -> "🌟 مغامرات الأنيمي"
                    else -> "🧸 عالم الصغار"
                }] = matches
            }
            val hasCategoryShelves = result.keys.any { it != "✨ اختياراتنا ليك" }
            if (!hasCategoryShelves) {
                result.clear()
                result["🌈 اكتشف المحتوى"] = visible
            }
            result.entries.map { it.key to it.value }
        }
    }
    val kidAccent = when (section) {
        Section.SERIES -> Color(0xFFB99AFF)
        Section.LIVE -> Color(0xFF73E0FF)
        else -> Color(0xFFFFC857)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF102E50), Color(0xFF151B3B), Color(0xFF301D4D))))) {
        KidsBackgroundDoodles(compact = compact)
        Column(
            Modifier.fillMaxSize().padding(if (compact) 8.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("🌈 TVI KIDS 🦄", color = Color(0xFFFFD76A), fontSize = if (compact) 23.sp else 34.sp, fontWeight = FontWeight.Black)
                    Text("مغامرات، كرتون وتعلّم فبلاصة وحدة!", color = Color(0xFFE5F4FF), fontSize = if (compact) 11.sp else 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showYoutubeKidsSearch = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("▶ YouTube", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = onExit, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("👨 Parent / PIN", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Section.LIVE to "📺 قنوات", Section.MOVIES to "🎨 كرتون وأفلام", Section.SERIES to "🦸 مسلسلات").forEach { (target, label) ->
                    Button(
                        onClick = { onSection(target) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (section == target) Color(0xFF7657D7) else Color(0xFF24456B),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = if (compact) 3.dp else 5.dp),
                    ) { Text(label, fontSize = if (compact) 10.sp else 13.sp, maxLines = 1) }
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth().heightIn(max = 58.dp),
                singleLine = true,
                label = { Text("🔎 قلب على الكرتون أو البرنامج", fontSize = 11.sp) },
            )
            if (kidCategories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    item(key = "kids-category-all") {
                        OutlinedButton(
                            onClick = { selectedKidCategory = "الكل" },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                            border = BorderStroke(1.dp, if (selectedKidCategory == "الكل") Color(0xFFFFD76A) else Color.White.copy(alpha = 0.35f)),
                        ) { Text("الكل", color = Color.White, fontSize = 11.sp) }
                    }
                    items(kidCategories, key = { "kids-category:$it" }) { category ->
                        OutlinedButton(
                            onClick = { selectedKidCategory = category },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                            border = BorderStroke(1.dp, if (selectedKidCategory == category) Color(0xFFFFD76A) else Color.White.copy(alpha = 0.35f)),
                        ) { Text(category, color = Color.White, fontSize = 11.sp, maxLines = 1) }
                    }
                }
            }
            if (visible.isEmpty()) {
                LaunchedEffect(section, movies.size, series.size, channels.size, search) {
                    // Continue paging when a raw catalogue page contains no child-safe items.
                    onLoadMore()
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🧸 🌟 🚀", fontSize = if (compact) 30.sp else 44.sp)
                        Text(when {
                            youtubeLoading -> "كنقلبو ليك على فيديوهات مناسبة... 🔎"
                            search.isNotBlank() && youtubeError.isNotBlank() -> "ما لقيناش نتائج محلية، وYouTube ما تجاوبش دابا."
                            search.isBlank() -> "مازال ما كاين حتى محتوى أطفال مصنّف هنا."
                            else -> "ما لقيناش هاد الشي، جرّب كلمة أخرى 😊"
                        }, color = Color(0xFFE5F4FF))
                    }
                }
            } else {
                val kidsListState = rememberLazyListState()
                LaunchedEffect(kidsListState, section, visible.size) {
                    snapshotFlow { kidsListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .distinctUntilChanged()
                        .collectLatest { lastVisible ->
                            val totalItems = kidsListState.layoutInfo.totalItemsCount
                            if (totalItems > 0 && lastVisible >= totalItems - 3) onLoadMore()
                        }
                }
                LazyColumn(
                    state = kidsListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "kids-welcome-banner") {
                        Surface(
                            color = Color(0xFF6546B8),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD76A).copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = if (compact) 5.dp else 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text("أهلاً بالأبطال الصغار! ⭐", color = Color.White, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                    Text("اختار مغامرتك الجاية وتعلّم 🎉", color = Color(0xFFFFEAB0), fontSize = if (compact) 9.sp else 11.sp, maxLines = 1)
                                }
                                Text("🦄 🚀 🐻", fontSize = if (compact) 16.sp else 20.sp, maxLines = 1)
                            }
                        }
                    }
                    kidShelves.forEachIndexed { shelfIndex, (shelfTitle, itemsForShelf) ->
                        if (itemsForShelf.isNotEmpty()) {
                            item(key = "kids-heading:$shelfIndex") {
                                Text(shelfTitle, color = kidAccent, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Black)
                            }
                            item(key = "kids-row:$shelfIndex") {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                                    items(itemsForShelf.distinctBy { it.id }, key = { "kids:${shelfIndex}:${it.id}" }) { entry ->
                                        Column(Modifier.width(if (compact) 104.dp else 132.dp).clickable {
                                            if (entry.id.startsWith("youtube:")) onPlayYoutube(entry.id.removePrefix("youtube:"))
                                            else if (section == Section.MOVIES) onMovieDetails(entry) else onPlay(entry)
                                        }) {
                                            Surface(
                                                color = Color(0xFF24456B),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(2.dp, kidAccent.copy(alpha = 0.75f)),
                                                modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
                                            ) {
                                                Box {
                                                    if (entry.logoUrl.isNotBlank()) AsyncImage(
                                                        model = entry.logoUrl,
                                                        contentDescription = entry.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                    ) else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF7256D8), Color(0xFF22A9B8)))), contentAlignment = Alignment.Center) {
                                                        Text("🧸\n⭐", fontSize = if (compact) 28.sp else 36.sp, lineHeight = 40.sp)
                                                    }
                                                    Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color(0xCC151B3B), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                        Text(if (section == Section.LIVE) "▶ شاهد" else "✨ اكتشف", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 11.sp else 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                            if (entry.category.isNotBlank()) Text(entry.category, color = Color(0xFFB6D5E5), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    if (showYoutubeKidsSearch) {
        YouTubeKidsSearchDialog(onDismiss = { showYoutubeKidsSearch = false })
    }
}


@Composable
private fun KidsYouTubePlayerDialog(videoId: String, onDismiss: () -> Unit) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            factory = { context ->
                val root = android.widget.FrameLayout(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            customViewCallback?.onCustomViewHidden()
                            customView = view
                            customViewCallback = callback
                            this@apply.visibility = android.view.View.GONE
                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                            root.addView(view, android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            ))
                            val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
                                .filterIsInstance<Activity>().firstOrNull()
                            activity?.window?.decorView?.systemUiVisibility =
                                (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                        }

                        override fun onHideCustomView() {
                            customView?.let { root.removeView(it) }
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            this@apply.visibility = android.view.View.VISIBLE
                        }
                    }
                    webViewRef = this
                }
                root.addView(webView, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                ))
                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                      <style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
                      iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
                    </head>
                    <body>
                      <iframe
                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=0&fs=1&controls=1&rel=0&origin=https%3A%2F%2Fwww.youtube-nocookie.com"
                        title="YouTube video player"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen>
                      </iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL("https://www.youtube-nocookie.com/", html, "text/html", "UTF-8", null)
                root
            },
            onRelease = { root ->
                val activity = generateSequence(root.context) { (it as? android.content.ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>().firstOrNull()
                activity?.window?.decorView?.systemUiVisibility = 0
                webViewRef?.let { view ->
                    view.stopLoading()
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }
                webViewRef = null
                customView = null
                customViewCallback = null
            },
        )
    }
}

private fun searchYouTubeKids(query: String): List<MediaEntry> {
    val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    if (apiKey.isBlank()) {
        throw IllegalStateException("YouTube API key is not configured. Add YOUTUBE_API_KEY to your local Gradle properties.")
    }

    val params = listOf(
        "part" to "snippet",
        "q" to (query + " kids"),
        "type" to "video",
        "maxResults" to "20",
        "safeSearch" to "strict",
        "regionCode" to "MA",
        "relevanceLanguage" to "ar",
        "key" to apiKey,
    ).joinToString("&") { (key, value) ->
        java.net.URLEncoder.encode(key, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
    }
    val connection = (URL("https://www.googleapis.com/youtube/v3/search?$params").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
    }

    return try {
        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            val apiMessage = runCatching {
                JSONObject(responseText).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")
            throw IllegalStateException(
                if (apiMessage.isNotBlank()) "YouTube API: $apiMessage"
                else "YouTube API request failed (HTTP $responseCode)"
            )
        }

        val items = JSONObject(responseText).optJSONArray("items") ?: return emptyList()
        val found = mutableListOf<MediaEntry>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: continue
            val title = snippet.optString("title").trim()
            if (videoId.isBlank() || title.isBlank()) continue

            val thumbnails = snippet.optJSONObject("thumbnails")
            val thumbnail = thumbnails?.optJSONObject("high")?.optString("url").orEmpty()
                .ifBlank { thumbnails?.optJSONObject("medium")?.optString("url").orEmpty() }
                .ifBlank { thumbnails?.optJSONObject("default")?.optString("url").orEmpty() }

            found += MediaEntry(
                id = "youtube:" + videoId,
                title = title,
                category = "YouTube Kids",
                streamUrl = "https://www.youtube.com/watch?v=" + videoId,
                logoUrl = thumbnail.ifBlank { "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg" },
                kind = MediaKind.MOVIE,
                description = "YouTube Kids result • " + snippet.optString("channelTitle"),
            )
        }
        found.distinctBy { it.id }.take(20)
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun YouTubeKidsSearchDialog(onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            color = Color(0xFF101B36),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(0xFFFFD76A)),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("▶ YouTube", color = Color(0xFFFFD76A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("قلب وشاهد الفيديو هنا داخل TVI PLAYER", color = Color(0xFFE5F4FF), fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("رجوع ✕", color = Color.White) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("شنو بغيتي تتفرج؟") },
                    )
                    Button(
                        onClick = {
                            submittedQuery = query.trim()
                            val target = Uri.parse("https://m.youtube.com/").buildUpon()
                                .appendPath("search")
                                .appendQueryParameter("q", submittedQuery)
                                .build()
                            webViewRef?.loadUrl(target.toString())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7657D7), contentColor = Color.White),
                    ) { Text("بحث") }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.White),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            webViewClient = WebViewClient()
                            webViewRef = this
                            loadUrl("https://m.youtube.com/")
                        }
                    },
                    update = { view ->
                        webViewRef = view
                        if (submittedQuery.isNotBlank()) {
                            val expected = Uri.parse("https://m.youtube.com/").buildUpon()
                                .appendPath("search")
                                .appendQueryParameter("q", submittedQuery)
                                .build().toString()
                            if (view.url != expected) view.loadUrl(expected)
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                        if (webViewRef === view) webViewRef = null
                    },
                )
            }
        }
    }
}

@Composable
private fun KidsBackgroundDoodles(compact: Boolean) {
    // Decorative, low-contrast cartoon-style doodles stay behind all interactive content.
    val size = if (compact) 34.sp else 52.sp
    Box(Modifier.fillMaxSize()) {
        Text("🐭", modifier = Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 62.dp), fontSize = size, color = Color.White.copy(alpha = 0.13f))
        Text("🦄", modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp, top = 36.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🐻", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 50.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🚀", modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 38.dp), fontSize = size, color = Color.White.copy(alpha = 0.13f))
        Text("🌈", modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 92.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
        Text("⭐", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp, bottom = 46.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🐼", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, end = 150.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
        Text("🦊", modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 190.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
    }
}

/** Applies the device-local Kids Mode gate to every loaded catalogue page. */
private fun applyKidsModeFilter(entries: List<MediaEntry>, preferences: PlayerPreferences): List<MediaEntry> {
    if (!preferences.kidsModeEnabled()) return entries

    // Read the parent rules once per catalogue pass. Reading and parsing both JSON
    // preference arrays for every single item can stall/crash on very large IPTV lists.
    val allowedIds = preferences.kidsAllowedItemIds()
    val blockedIds = preferences.kidsBlockedItemIds()
    val childSafeTerms = listOf(
        "kids", "children", "educational", "education", "junior", "preschool",
        "toddler", "disney junior", "nick jr", "طفل", "أطفال", "براعم",
    )
    return entries.filter { entry ->
        val id = entry.id.trim()
        if (id.isEmpty() || id in blockedIds) {
            false
        } else if (id in allowedIds) {
            true
        } else {
            val searchable = (entry.title + " " + entry.category).lowercase()
            childSafeTerms.any { it in searchable }
        }
    }
}

private fun numberedChannels(channels: List<MediaEntry>, preferences: PlayerPreferences): List<Pair<MediaEntry, Int>> =
    channels.map { it to preferences.channelNumber(it.id) }.sortedBy { it.second }

private fun channelNumber(entry: MediaEntry, channels: List<MediaEntry>, preferences: PlayerPreferences): Int =
    numberedChannels(channels, preferences).firstOrNull { it.first.id == entry.id }?.second ?: 0

private fun importLocalPlaylist(context: Context, uri: Uri): PlayerSource {
    val folder = File(context.filesDir, "playlists").apply { mkdirs() }
    val id = UUID.randomUUID().toString()
    val destination = File(folder, "$id.m3u")
    context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output ->
            val buffer = ByteArray(16 * 1024)
            var size = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                size += count
                if (size > 512L * 1024 * 1024) throw IOException("Playlist exceeds the 512 MB import limit.")
                output.write(buffer, 0, count)
            }
        }
    } ?: throw IOException("Could not open the selected playlist.")
    return PlayerSource(
        id = id,
        name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Local playlist" } ?: "Local playlist",
        type = SourceType.LOCAL_M3U,
        serverUrl = destination.absolutePath,
    )
}