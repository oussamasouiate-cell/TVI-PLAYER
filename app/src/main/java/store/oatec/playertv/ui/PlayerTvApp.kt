package store.oatec.playertv.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import coil.compose.AsyncImage
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import store.oatec.playertv.data.PlayerPreferences
import store.oatec.playertv.data.OatecAboutRepository
import store.oatec.playertv.data.SecureSourceStore
import store.oatec.playertv.data.SourceRepository
import store.oatec.playertv.data.MediaCatalogStore
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

private data class VlcPlaybackSession(
    val libVlc: LibVLC,
    val player: VlcMediaPlayer,
    val videoLayout: VLCVideoLayout,
)

private data class VlcAudioTrack(val id: Int, val name: String)

private enum class Section(val label: String) {
    HOME("Home"), LIVE("Live TV"), MOVIES("Movies"), SERIES("Series"), SOURCES("Sources"), SETTINGS("Settings"),
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
    var premiumCode by remember { mutableStateOf(runCatching { sourceStore.loadPremiumCode() }.getOrNull().orEmpty()) }
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
    var section by rememberSaveable { mutableStateOf(Section.HOME.name) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<MediaEntry?>(null) }
    var selectedDetails by remember { mutableStateOf<MediaEntry?>(null) }
    var organizingChannel by remember { mutableStateOf<MediaEntry?>(null) }
    var pendingSeries by remember { mutableStateOf<MediaEntry?>(null) }
    var episodes by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var activeLoadCount by remember { mutableStateOf(0) }
    var preferenceRevision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var showAddSource by remember { mutableStateOf(false) }
    var showNumberPad by remember { mutableStateOf(false) }
    var numberInput by rememberSaveable { mutableStateOf("") }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currentSection = Section.entries.firstOrNull { it.name == section } ?: Section.HOME

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
        activeLoadCount++
        busy = true
        scope.launch {
            try {
                // Show the last successfully saved catalogue immediately while a large
                // Xtream response is being downloaded. Read it in bounded DB pages.
                if (source.id == activeSourceId) {
                    val cached = withContext(Dispatchers.IO) {
                        buildList {
                            var offset = 0
                            while (true) {
                                val page = mediaCatalogStore.page(source.id, catalogSection.name, limit = 500, offset = offset)
                                addAll(page)
                                if (page.size < 500) break
                                offset += page.size
                            }
                        }
                    }
                    if (cached.isNotEmpty() && source.id == activeSourceId) {
                        catalog = when (catalogSection) {
                            CatalogSection.LIVE -> catalog.copy(liveChannels = cached)
                            CatalogSection.MOVIES -> catalog.copy(movies = cached)
                            CatalogSection.SERIES -> catalog.copy(series = cached)
                        }
                        message = "Showing saved catalogue (" + cached.size + " items) while refreshing…"
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
                                }
                                session.commit()
                                loaded
                            }
                            val allEntries = buildList {
                                var offset = 0
                                while (true) {
                                    val page = mediaCatalogStore.page(source.id, catalogSection.name, limit = 500, offset = offset)
                                    addAll(page)
                                    if (page.size < 500) break
                                    offset += page.size
                                }
                            }
                            result to allEntries
                        }
                    }
                        .onSuccess { (result, allEntries) ->
                            if (source.id == activeSourceId) {
                                catalog = when (catalogSection) {
                                    CatalogSection.LIVE -> catalog.copy(liveChannels = allEntries)
                                    CatalogSection.MOVIES -> catalog.copy(movies = allEntries)
                                    CatalogSection.SERIES -> catalog.copy(series = allEntries)
                                }
                                if (catalogSection == CatalogSection.LIVE) selectedCategory = "All"
                                loadedCatalogs = loadedCatalogs + loadKey
                                message = "${result.liveChannels.size} live channels | ${result.movies.size} movies | ${result.series.size} series"
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

    LaunchedEffect(activeSourceId) {
        catalog = SourceCatalog()
        loadedCatalogs = emptySet()
        episodes = emptyList()
        pendingSeries = null
    }

    LaunchedEffect(activeSourceId, section) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        refresh(source)
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
        if (needsInitialSetup) {
            InitialSetupScreen(
                savedPremiumCode = premiumCode,
                onComplete = { code, source ->
                    sourceStore.savePremiumCode(code)
                    premiumCode = code
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
                        AndroidKeyEvent.KEYCODE_BACK -> {
                            if (currentSection != Section.HOME) { section = Section.HOME.name; true } else false
                        }
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
            if (currentSection == Section.HOME) {
                HomeDashboard(
                    compact = compactLandscape,
                    sourceName = activeSource?.name,
                    onSelect = { section = it.name; selectedCategory = "All"; search = "" },
                    onExit = { (context as? Activity)?.finishAndRemoveTask() },
                )
            } else {
            Column(Modifier.fillMaxSize()) {
                NavigationRail(
                    selected = if (currentSection == Section.SOURCES) Section.SETTINGS else currentSection,
                    sourceName = activeSource?.name,
                    compact = compactLandscape,
                    onSelect = { section = it.name; selectedCategory = "All"; search = "" },
                    onHome = { section = Section.HOME.name; selectedCategory = "All"; search = "" },
                    onExit = { (context as? Activity)?.finishAndRemoveTask() },
                )
                Column(Modifier.weight(1f).fillMaxWidth()) {
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
                    when (currentSection) {
                        Section.HOME -> Unit // HomeDashboard is rendered above this content switch.
                        Section.LIVE -> LiveScreen(
                            catalog = catalog,
                            category = selectedCategory,
                            search = search,
                            preferences = preferences,
                            preferenceRevision = preferenceRevision,
                            message = message,
                            busy = busy,
                            onCategory = { selectedCategory = it },
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
                            entries = catalog.movies,
                            search = search,
                            busy = busy,
                            message = message,
                            preferences = preferences,
                            recentEntries = preferences.recentlyWatched(catalog.movies + episodes),
                            onPlay = { selectedDetails = it },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.SERIES -> SeriesScreen(
                            entries = catalog.series,
                            episodes = episodes,
                            selectedSeries = pendingSeries,
                            search = search,
                            busy = busy,
                            message = message,
                            preferences = preferences,
                            onSelect = { series ->
                                selectedDetails = series
                                val source = activeSource
                                if (source == null || source.type != SourceType.XTREAM) {
                                    message = "Series episodes require an Xtream source."
                                } else {
                                    pendingSeries = series
                                    busy = true
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                repository.loadSeriesEpisodes(source, series.id.substringAfterLast(':'))
                                            }
                                        }.onSuccess { episodes = it }
                                            .onFailure { message = it.message ?: "Could not load episodes." }
                                        busy = false
                                    }
                                }
                            },
                            onPlay = { selectedMedia = it },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.SOURCES -> SourcesScreen(
                            sources = sources,
                            activeId = activeSourceId,
                            busy = busy,
                            onSelect = { source -> activeSourceId = source.id; section = Section.LIVE.name },
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
                            preferences = preferences,
                            aboutRepository = aboutRepository,
                            onManageSources = { section = Section.SOURCES.name },
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
    var premiumCode by rememberSaveable { mutableStateOf(savedPremiumCode) }
    var sourceType by rememberSaveable { mutableStateOf(SourceType.XTREAM.name) }
    var sourceName by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    val isXtream = sourceType == SourceType.XTREAM.name

    Box(
        Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(2.dp, Lime),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("IPTV", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Column {
                    Text("OATEC", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("PLAYER TV", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome to PlayerTv", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Set up your account and IPTV source", color = TextMuted, fontSize = 12.sp)
            }

            Column(Modifier.widthIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OATEC PREMIUM CODE (OPTIONAL)", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                OutlinedTextField(
                    value = premiumCode,
                    onValueChange = { premiumCode = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Premium code (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                Text(
                    "You can continue without a code. Code verification requires the OATEC activation service.",
                    color = TextMuted,
                    fontSize = 10.sp,
                )
            }

            HorizontalDivider(Modifier.widthIn(max = 620.dp), color = Color.White.copy(alpha = 0.12f))

            Column(Modifier.widthIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("IPTV SOURCE", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPill("Xtream Code", isXtream) { sourceType = SourceType.XTREAM.name; error = "" }
                    CategoryPill("M3U", !isXtream) { sourceType = SourceType.M3U.name; error = "" }
                }
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Source name (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isXtream) "Xtream server URL" else "M3U playlist URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                if (isXtream) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
                if (serverUrl.startsWith("http://", true)) {
                    Text("HTTP is unencrypted. Use HTTPS when your provider supports it.", color = ErrorRed, fontSize = 10.sp)
                }
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/"))) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("OATEC website", fontSize = 11.sp)
                    }
                    Button(
                    onClick = {
                        val parsed = Uri.parse(serverUrl.trim())
                        when {
                            parsed.host.isNullOrBlank() || parsed.scheme !in setOf("http", "https") -> error = "Enter a valid HTTP or HTTPS source URL."
                            isXtream && (username.isBlank() || password.isBlank()) -> error = "Enter the Xtream username and password."
                            else -> onComplete(
                                premiumCode.trim(),
                                PlayerSource(
                                    id = UUID.randomUUID().toString(),
                                    name = sourceName.trim().ifBlank { if (isXtream) "Xtream source" else "M3U playlist" },
                                    type = if (isXtream) SourceType.XTREAM else SourceType.M3U,
                                    serverUrl = serverUrl.trim(),
                                    username = username.trim(),
                                    password = password,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: PlayerPreferences,
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
        Column {
            Text("Settings", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Playback preferences and OATEC information", color = TextMuted, fontSize = 12.sp)
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
                    HomeCardData(Section.SERIES, "SERIES", "Séries & épisodes", "▤", "SERIES", listOf(Color(0xFF263746), Color(0xFF0A0D12))),
                    HomeCardData(Section.SETTINGS, "SETTING", "Préférences & sources", "⚙", "OATEC", listOf(Color(0xFF3C2B13), Color(0xFF080A0D))),
                )
                cards.forEachIndexed { index, card ->
                    val active = index == 0
                    Surface(
                        color = Color(0xCC05080C),
                        shape = RoundedCornerShape(if (compact) 12.dp else 20.dp),
                        border = BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) Brush.linearGradient(listOf(GoldLight, Lime, Color(0xFFB87816)))
                            else Brush.linearGradient(listOf(Color(0xFF41434A), Color(0xFF20242B))),
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .focusable()
                            .clickable { onSelect(card.section) }
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
            OutlinedButton(onClick = onHome, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                Text("⌂ Home", fontSize = 10.sp)
            }
            OutlinedButton(onClick = onExit, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                Text("Exit", fontSize = 10.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val mainItems = listOf(Section.LIVE, Section.MOVIES, Section.SERIES, Section.SETTINGS)
            mainItems.forEach { item ->
                val active = selected == item
                Surface(
                    color = if (active) Color(0xFF242D38) else Color(0xFF111722),
                    shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
                    border = BorderStroke(
                        if (active) 2.dp else 1.dp,
                        if (active) Brush.horizontalGradient(listOf(Lime, GoldLight)) else Brush.horizontalGradient(listOf(Color(0xFF303743), Color(0xFF242B35))),
                    ),
                    modifier = Modifier.weight(1f).height(if (compact) 58.dp else 76.dp)
                        .focusable()
                        .clickable { onSelect(item) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = if (compact) 5.dp else 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            when (item) {
                                Section.LIVE -> "▣"
                                Section.MOVIES -> "▶"
                                Section.SERIES -> "▤"
                                else -> "⚙"
                            },
                            color = if (active) Lime else TextMuted,
                            fontSize = if (compact) 16.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            when (item) {
                                Section.LIVE -> "TV LIVE"
                                Section.MOVIES -> "FILM"
                                Section.SERIES -> "SERIES"
                                else -> "SETTING"
                            },
                            color = if (active) Lime else TextMain,
                            fontSize = if (compact) 9.sp else 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
    category: String,
    search: String,
    preferences: PlayerPreferences,
    preferenceRevision: Int,
    message: String,
    busy: Boolean,
    onCategory: (String) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
    onOrganize: (MediaEntry) -> Unit,
    onHide: (MediaEntry) -> Unit,
) {
    val channels = remember(catalog.liveChannels, category, search, preferenceRevision) {
        visibleChannels(catalog, category, search, preferences)
    }
    var previewChannel by remember { mutableStateOf<MediaEntry?>(null) }
    val categories = listOf("All", "Favorites", "Hidden") +
        catalog.liveChannels.map { preferences.categoryFor(it.id, it.category) }.distinct().sorted() +
        preferences.customCategories()
    val distinctCategories = categories.distinct()
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("YOUR ENTERTAINMENT", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Live channels, ready when you are", color = TextMuted, fontSize = 11.sp)
            }
            Surface(
                color = Color(0x1F43D19E),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0x6643D19E)),
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(6.dp).background(Color(0xFF43D19E), CircleShape))
                    Text("LIVE TV", color = Color(0xFF7AE6B7), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(distinctCategories, key = { it }) { item ->
                CategoryPill(item, category == item) { onCategory(item) }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(1.2f).fillMaxHeight()) {
                Text("CHANNEL LINEUP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                when {
                    busy && catalog.liveChannels.isEmpty() -> LoadingState("Loading channels")
                    channels.isEmpty() -> EmptyState("No channels here", message.ifBlank { "Add or refresh a source to load live TV." })
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                        itemsIndexed(channels, key = { _, item -> item.id }) { _, item ->
                            ChannelRow(
                                number = channelNumber(item, catalog.liveChannels, preferences),
                                entry = item.copy(title = preferences.channelTitle(item.id, item.title)),
                                favorite = item.id in preferences.favorites(),
                                hidden = item.id in preferences.hiddenChannels(),
                                category = preferences.categoryFor(item.id, item.category),
                                onSelect = {
                                    val selected = item.copy(title = preferences.channelTitle(item.id, item.title))
                                    if (previewChannel?.id == item.id) {
                                        previewChannel = null
                                        onPlay(selected)
                                    } else {
                                        previewChannel = selected
                                    }
                                },
                                onPlay = { onPlay(item.copy(title = preferences.channelTitle(item.id, item.title))) },
                                onFavorite = { onFavorite(item) },
                                onOrganize = { onOrganize(item) },
                                onHide = { onHide(item) },
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(0.8f).fillMaxHeight().padding(top = 3.dp)) {
                Text(if (previewChannel == null) "CHANNEL INFO" else "CHANNEL INFO", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF141B25),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x55C4F05A)),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.75f),
                ) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(Color(0xFF263342), Color(0xFF121720), Color(0xFF30251B))),
                        ),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopStart)
                                .background(Brush.horizontalGradient(listOf(Lime, GoldLight, Color.Transparent))),
                        )
                        Column(
                            Modifier.fillMaxSize().padding(22.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color(0x3324D18A), shape = RoundedCornerShape(6.dp)) {
                                    Text("● LIVE", color = Color(0xFF7AE6B7), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                Text("OATEC", color = TextMain.copy(alpha = 0.65f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                val preview = previewChannel
                                if (preview != null && preview.logoUrl.isNotBlank()) {
                                    AsyncImage(preview.logoUrl, contentDescription = preview.title, modifier = Modifier.size(42.dp))
                                }
                                Text(preview?.title ?: "Choose a channel", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(preview?.category ?: "Channel information appears here.", color = TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(if (preview == null) "Select a channel to view details." else "Press twice to watch this channel.", color = Lime, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("REMOTE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("CH + / -  Change channel", color = TextMain, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Text("Number keys  Direct channel", color = TextMain, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                Text("OK  Play selected channel", color = TextMain, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                if (message.isNotBlank()) Text(message, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 15.dp))
            }
        }
    }
}

@Composable
private fun ChannelRow(
    number: Int,
    entry: MediaEntry,
    favorite: Boolean,
    hidden: Boolean,
    category: String,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onOrganize: () -> Unit,
    onHide: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        color = if (focused) Raised else Panel,
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(1.dp, Lime) else null,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.focusable().clickable { onSelect() },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(number.toString().padStart(3, '0'), color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
            if (entry.logoUrl.isNotBlank()) {
                AsyncImage(entry.logoUrl, contentDescription = null, modifier = Modifier.size(34.dp).padding(end = 8.dp))
            } else {
                Box(Modifier.size(28.dp).background(Raised, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text(entry.title.firstOrNull()?.uppercase() ?: "-", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(entry.title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
            TextButton(onClick = onFavorite) { Text(if (favorite) "Saved" else "+ Fav", color = Lime, fontSize = 11.sp) }
            TextButton(onClick = onOrganize) { Text("Edit", color = TextMuted, fontSize = 11.sp) }
            TextButton(onClick = onHide) { Text(if (hidden) "Restore" else "Delete", color = if (hidden) TextMuted else ErrorRed, fontSize = 11.sp) }
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
    preferences: PlayerPreferences,
    recentEntries: List<MediaEntry>,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    val filtered = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
        Text("$title | ${filtered.size}", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(14.dp))
        when {
            busy && entries.isEmpty() -> LoadingState("Loading library")
            filtered.isEmpty() -> EmptyState("Nothing to watch yet", message.ifBlank { "This source may not provide ${title.lowercase()} metadata." })
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                if (recentEntries.isNotEmpty()) {
                    item(key = "continue-watching") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Continue watching", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(recentEntries.take(8), key = { it.id }) { entry ->
                                    PosterCard(
                                        entry = entry,
                                        favorite = entry.id in preferences.favorites(),
                                        onClick = { onPlay(entry) },
                                        onFavorite = { onFavorite(entry) },
                                    )
                                }
                            }
                        }
                    }
                }
                val grouped = filtered.groupBy { it.category.ifBlank { "Other" } }.toSortedMap()
                grouped.forEach { (category, items) ->
                    item(key = "header:$category") {
                        Text(category, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }
                    item(key = "row:$category") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items, key = { it.id }) { entry ->
                                PosterCard(
                                    entry = entry,
                                    favorite = entry.id in preferences.favorites(),
                                    onClick = { onPlay(entry) },
                                    onFavorite = { onFavorite(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(entry: MediaEntry, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(142.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.68f).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        ) {
            Surface(
                color = Raised,
                shape = RoundedCornerShape(12.dp),
                border = if (focused) BorderStroke(2.dp, Lime) else null,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (entry.logoUrl.isNotBlank()) AsyncImage(entry.logoUrl, contentDescription = entry.title, modifier = Modifier.fillMaxSize())
                else Box(contentAlignment = Alignment.Center) {
                    Text(entry.title.take(1).uppercase(), color = Lime, fontSize = 35.sp, fontWeight = FontWeight.Black)
                }
            }
            TextButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                Text(if (favorite) "Saved" else "+ Fav", color = Lime, fontSize = 10.sp)
            }
        }
        Text(entry.title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        if (entry.description.isNotBlank()) Text(entry.description, color = TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MediaDetailsDialog(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onBrowseEpisodes: () -> Unit,
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
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = if (isSeries) onBrowseEpisodes else onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) {
                        Text(if (isSeries) "Episodes" else "Play")
                    }
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
    onSelect: (MediaEntry) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text("SERIES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(14.dp))
            val filtered = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) }
            if (filtered.isEmpty()) EmptyState("No series available", message.ifBlank { "Series metadata is available from supported Xtream sources." })
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    var focused by remember { mutableStateOf(false) }
                    Surface(
                        color = if (focused || entry.id == selectedSeries?.id) Raised else Panel,
                        border = if (focused) BorderStroke(1.dp, Lime) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.focusable().clickable { onSelect(entry) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            AsyncImage(entry.logoUrl, contentDescription = null, modifier = Modifier.size(58.dp, 76.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(entry.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(entry.category, color = TextMuted, fontSize = 11.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onFavorite(entry) }) {
                                Text(if (entry.id in preferences.favorites()) "Saved" else "+ Fav", color = Lime, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(selectedSeries?.title ?: "SEASONS & EPISODES", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            selectedSeries?.let { series ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    if (series.logoUrl.isNotBlank()) {
                        AsyncImage(series.logoUrl, contentDescription = series.title, modifier = Modifier.size(54.dp, 72.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val details = listOfNotNull(
                            series.year.takeIf(String::isNotBlank),
                            series.rating.takeIf(String::isNotBlank)?.let { "Rating $it" },
                            series.genre.takeIf(String::isNotBlank),
                        )
                        if (details.isNotEmpty()) Text(details.joinToString(" | "), color = GoldLight, fontSize = 10.sp)
                        if (series.description.isNotBlank()) {
                            Text(series.description, color = TextMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            when {
                busy -> LoadingState("Loading episodes")
                selectedSeries == null -> EmptyState("Select a series", "Choose a title to load its seasons and episodes.")
                episodes.isEmpty() -> EmptyState("No episode data", "This provider did not return playable episodes.")
                else -> LazyColumn {
                    episodes.groupBy { it.season }.forEach { (season, seasonEpisodes) ->
                        item(key = "season:$season") {
                            Text(season, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(seasonEpisodes, key = { it.id }) { episode ->
                            OutlinedButton(onClick = { onPlay(episode) }, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Text("${episode.episodeNumber.padStart(2, '0')}  ${episode.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                if (type == SourceType.XTREAM.name) {
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
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
                else onSave(
                    PlayerSource(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = SourceType.valueOf(type),
                        serverUrl = url.trim(),
                        username = username.trim(),
                        password = password,
                    ),
                )
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
            parameters = buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .build()
        }
    }
    val renderersFactory = remember(context) {
        DefaultRenderersFactory(context).setEnableDecoderFallback(true)
    }
    val player = remember(context) {
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                val mediaAudioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(mediaAudioAttributes, true)
                volume = 1f
            }
    }
    var vlcSession by remember { mutableStateOf<VlcPlaybackSession?>(null) }
    var useVlc by rememberSaveable { mutableStateOf(false) }
    var vlcPlaying by remember { mutableStateOf(false) }
    var vlcTracks by remember { mutableStateOf<List<VlcAudioTrack>>(emptyList()) }
    var showAudioTracks by remember { mutableStateOf(false) }
    var vlcPosition by remember { mutableStateOf(0L) }
    var vlcDuration by remember { mutableStateOf(0L) }
    var vlcResumePosition by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var showList by remember { mutableStateOf(false) }
    var showChannelInfo by remember { mutableStateOf(entry.kind == MediaKind.LIVE) }
    var digitEntry by rememberSaveable { mutableStateOf("") }
    var loadedEntry by remember { mutableStateOf<MediaEntry?>(null) }
    var loadedWithVlc by remember { mutableStateOf(false) }
    val latestUseVlc by rememberUpdatedState(useVlc)
    val latestEntry by rememberUpdatedState(entry)
    val latestPreferences by rememberUpdatedState(preferences)
    val latestVlcSession by rememberUpdatedState(vlcSession)
    val latestLoadedEntry by rememberUpdatedState(loadedEntry)
    val channelIndex = channels.indexOfFirst { it.id == entry.id }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!latestUseVlc) {
                    loading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                }
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                // Use current Compose state: this listener is installed once and otherwise
                // retains the values captured during its first composition.
                if (!latestUseVlc) {
                    try {
                        val currentEntry = latestEntry
                        val currentPreferences = latestPreferences
                        if (currentEntry.kind != MediaKind.LIVE && currentPreferences.resumePlayback()) {
                            currentPreferences.saveProgress(
                                currentEntry.id,
                                player.currentPosition.coerceAtLeast(0L),
                            )
                        }
                        if (latestVlcSession == null) {
                            vlcSession = createVlcPlaybackSession(context)
                        }
                        error = ""
                        loading = true
                        useVlc = true
                        return
                    } catch (failure: RuntimeException) {
                        error = failure.localizedMessage
                            ?: playbackException.localizedMessage
                            ?: "Playback failed. Check the stream or try VLC manually."
                    }
                } else {
                    error = playbackException.localizedMessage ?: "Playback failed. Check the stream or try again."
                }
                loading = false
            }
        }
        player.addListener(listener)
        onDispose {
            latestLoadedEntry?.let { lastEntry ->
                if (!latestUseVlc && lastEntry.kind != MediaKind.LIVE && latestPreferences.resumePlayback()) {
                    latestPreferences.saveProgress(lastEntry.id, player.currentPosition.coerceAtLeast(0L))
                }
            }
            player.stop()
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(vlcSession) {
        val session = vlcSession
        if (session != null) {
            session.player.setEventListener(VlcMediaPlayer.EventListener { event ->
                when (event.type) {
                    VlcMediaPlayer.Event.Opening, VlcMediaPlayer.Event.Buffering -> loading = true
                    VlcMediaPlayer.Event.Playing -> {
                        loading = false
                        vlcPlaying = true
                        error = ""
                        vlcTracks = session.player.audioTracks
                            ?.filter { it.id >= 0 }
                            ?.map { VlcAudioTrack(it.id, it.name) }
                            .orEmpty()
                        vlcDuration = session.player.length.coerceAtLeast(0L)
                        if (vlcResumePosition > 5_000L && session.player.isSeekable) {
                            session.player.setTime(vlcResumePosition)
                            vlcResumePosition = 0L
                        }
                    }
                    VlcMediaPlayer.Event.Paused -> {
                        loading = false
                        vlcPlaying = false
                    }
                    VlcMediaPlayer.Event.Stopped, VlcMediaPlayer.Event.EndReached -> {
                        loading = false
                        vlcPlaying = false
                    }
                    VlcMediaPlayer.Event.ESAdded, VlcMediaPlayer.Event.ESSelected ->
                        vlcTracks = session.player.audioTracks
                            ?.filter { it.id >= 0 }
                            ?.map { VlcAudioTrack(it.id, it.name) }
                            .orEmpty()
                    VlcMediaPlayer.Event.EncounteredError -> {
                        loading = false
                        vlcPlaying = false
                        error = "VLC could not play this stream. Check its audio/video codecs and URL."
                    }
                }
            })
            session.player.attachViews(session.videoLayout, null, true, false)
        }
        onDispose {
            session?.let {
                val lastEntry = latestEntry
                if (latestUseVlc && lastEntry.kind != MediaKind.LIVE && latestPreferences.resumePlayback()) {
                    latestPreferences.saveProgress(lastEntry.id, it.player.time.coerceAtLeast(0L))
                }
                it.player.setEventListener(null)
                it.player.stop()
                it.player.detachViews()
                it.player.release()
                it.libVlc.release()
            }
        }
    }

    LaunchedEffect(entry.id, player, useVlc, vlcSession) {
        loadedEntry?.let { previousEntry ->
            if (previousEntry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                val position = if (loadedWithVlc) vlcSession?.player?.time ?: player.currentPosition else player.currentPosition
                preferences.saveProgress(previousEntry.id, position.coerceAtLeast(0L))
            }
        }
        error = ""
        if (entry.kind != MediaKind.LIVE) preferences.recordWatched(entry.id)
        if (useVlc) {
            player.playWhenReady = false
            player.stop()
            val session = vlcSession ?: return@LaunchedEffect
            loading = true
            vlcPlaying = false
            vlcTracks = emptyList()
            vlcPosition = 0L
            vlcDuration = 0L
            session.player.stop()
            vlcResumePosition = if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                preferences.progress(entry.id)
            } else {
                0L
            }
            session.player.play(Uri.parse(entry.streamUrl))
        } else {
            vlcSession?.player?.stop()
            vlcPlaying = false
            showAudioTracks = false
            loading = true
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(entry.streamUrl))
            player.prepare()
            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                val resumePosition = preferences.progress(entry.id)
                if (resumePosition > 5_000) player.seekTo(resumePosition)
            }
            player.playWhenReady = true
        }
        loadedEntry = entry
        loadedWithVlc = useVlc
    }

    LaunchedEffect(useVlc, vlcSession) {
        if (useVlc && vlcSession != null) {
            while (true) {
                vlcPosition = vlcSession?.player?.time?.coerceAtLeast(0L) ?: 0L
                vlcDuration = vlcSession?.player?.length?.coerceAtLeast(0L) ?: 0L
                delay(500)
            }
        }
    }

    LaunchedEffect(entry.id) {
        showList = false
        digitEntry = ""
        showChannelInfo = entry.kind == MediaKind.LIVE
    }

    LaunchedEffect(entry.id, showChannelInfo) {
        if (showChannelInfo && entry.kind == MediaKind.LIVE) {
            delay(CHANNEL_INFO_DURATION_MS)
            showChannelInfo = false
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
                        if (showList) {
                            showList = false
                            true
                        } else false
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (channels.isEmpty()) false else {
                            val index = if (channelIndex <= 0) channels.lastIndex else channelIndex - 1
                            onPlay(channels[index])
                            true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (channels.isEmpty()) false else {
                            val index = if (channelIndex < 0 || channelIndex == channels.lastIndex) 0 else channelIndex + 1
                            onPlay(channels[index])
                            true
                        }
                    }
                    in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9,
                    in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> {
                        val digit = if (key.keyCode in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9) {
                            key.keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0
                        } else {
                            key.keyCode - AndroidKeyEvent.KEYCODE_0
                        }
                        digitEntry = (digitEntry + digit).takeLast(4)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_BUTTON_A -> {
                        if (digitEntry.isBlank()) {
                            showChannelInfo = entry.kind == MediaKind.LIVE
                        } else {
                            digitEntry.toIntOrNull()?.let { number ->
                                channels.firstOrNull { preferences.channelNumber(it.id) == number }?.let {
                                    showList = false
                                    onPlay(it)
                                }
                            }
                        }
                        digitEntry = ""
                        true
                    }
                    else -> false
                }
            },
        ) {
            val currentVlcSession = vlcSession
            if (useVlc && currentVlcSession != null) {
            AndroidView(
                factory = { currentVlcSession.videoLayout },
                modifier = Modifier.fillMaxSize(),
            )
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
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (showChannelInfo && entry.kind == MediaKind.LIVE) {
                Column(
                    Modifier.align(Alignment.TopStart).padding(28.dp).background(Ink.copy(alpha = 0.82f), RoundedCornerShape(8.dp)).padding(14.dp),
                ) {
                    Text("${(channelIndex + 1).coerceAtLeast(1).toString().padStart(3, '0')}  |  LIVE", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(entry.title, color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(entry.category, color = TextMuted, fontSize = 12.sp)
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (useVlc) {
                            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                                preferences.saveProgress(entry.id, vlcSession?.player?.time?.coerceAtLeast(0L) ?: 0L)
                            }
                            useVlc = false
                        } else {
                            try {
                                if (vlcSession == null) vlcSession = createVlcPlaybackSession(context)
                                if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                                    preferences.saveProgress(entry.id, player.currentPosition)
                                }
                                useVlc = true
                            } catch (failure: RuntimeException) {
                                error = failure.localizedMessage ?: "Could not start the VLC decoder."
                                loading = false
                            }
                        }
                    },
                ) {
                    Text(if (useVlc) "Media3" else "No sound? VLC")
                }
                if (useVlc) {
                    OutlinedButton(onClick = { showAudioTracks = true }, enabled = vlcTracks.isNotEmpty()) {
                        Text("Audio (${vlcTracks.size})")
                    }
                }
                if (entry.kind == MediaKind.LIVE && channels.isNotEmpty()) {
                    OutlinedButton(onClick = { showList = !showList }) { Text(if (showList) "Hide channels" else "Channels") }
                }
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Lime)
            if (error.isNotBlank()) {
                Column(Modifier.align(Alignment.Center).background(Ink.copy(alpha = 0.9f), RoundedCornerShape(10.dp)).padding(24.dp)) {
                    Text("Playback unavailable", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(error, color = TextMuted, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                    Button(onClick = {
                        error = ""
                        if (useVlc) vlcSession?.player?.play() else {
                            player.prepare()
                            player.play()
                        }
                    }) { Text("Retry") }
                }
            }
            if (useVlc && currentVlcSession != null) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Ink.copy(alpha = 0.9f)).padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatPlaybackTime(vlcPosition), color = TextMain, fontSize = 11.sp)
                        Slider(
                            value = vlcPosition.coerceIn(0L, vlcDuration.coerceAtLeast(1L)).toFloat(),
                            onValueChange = { vlcPosition = it.toLong() },
                            onValueChangeFinished = { vlcSession?.player?.setTime(vlcPosition) },
                            valueRange = 0f..vlcDuration.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        )
                        Text(formatPlaybackTime(vlcDuration), color = TextMain, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            vlcSession?.player?.setTime((vlcPosition - 10_000L).coerceAtLeast(0L))
                        }) { Text("-10s") }
                        Button(
                            onClick = {
                                vlcSession?.player?.let { if (vlcPlaying) it.pause() else it.play() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                        ) { Text(if (vlcPlaying) "Pause" else "Play") }
                        OutlinedButton(onClick = {
                            vlcSession?.player?.setTime((vlcPosition + 10_000L).coerceAtMost(vlcDuration))
                        }) { Text("+10s") }
                        Text("VLC audio/video decoder", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
            if (showList) {
                LazyColumn(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(330.dp).background(Ink.copy(alpha = 0.96f)).padding(14.dp),
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
            if (showAudioTracks) {
                AlertDialog(
                    onDismissRequest = { showAudioTracks = false },
                    title = { Text("Audio tracks", color = TextMain) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            vlcTracks.forEach { track ->
                                TextButton(
                                    onClick = {
                                        vlcSession?.player?.setAudioTrack(track.id)
                                        showAudioTracks = false
                                    },
                                ) {
                                    Text(track.name, color = TextMain)
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showAudioTracks = false }) { Text("Close") } },
                )
            }
        }
    }
}

private fun createVlcPlaybackSession(context: Context): VlcPlaybackSession {
    val libVlc = LibVLC(context, listOf("--no-video-title-show"))
    return try {
        VlcPlaybackSession(
            libVlc = libVlc,
            player = VlcMediaPlayer(libVlc),
            videoLayout = VLCVideoLayout(context),
        )
    } catch (failure: RuntimeException) {
        libVlc.release()
        throw failure
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(totalSeconds / 60, seconds)
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
private fun LoadingState(label: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(color = Lime)
        Text(label, color = TextMuted, fontSize = 13.sp)
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