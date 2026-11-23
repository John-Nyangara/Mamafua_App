package com.mamafua.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.domain.model.EldoretEstate
import com.mamafua.app.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class OsmMapMarker(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val subtitle: String = "",
    val badgeText: String = "",
    val colorHex: String = "#0077B6",
    val isPulsing: Boolean = false
)

/**
 * Interactive OpenStreetMap (OSM) + Leaflet Picker & Radar Card.
 * Supports:
 * - Tap anywhere or drag pin on real OpenStreetMap street tiles
 * - Reverse geocoding via OpenStreetMap Nominatim (auto-fills landmark/road + nearest Eldoret Estate)
 * - Place/landmark search within Eldoret / Uasin Gishu
 * - 1-tap "My GPS" device location via FusedLocationProviderClient / LocationManager
 * - Optional 5 km Geofence radius circle & multi-marker overlays (nearby Mama Fuas or Job Radar pins)
 * - Expandable Full-Screen Map modal for effortless two-finger zoom & street-level pin placement
 */
@Composable
fun OsmLocationPickerCard(
    title: String = "Interactive OpenStreetMap Pin",
    subtitle: String = "Tap the map, drag the pin, or tap My GPS so no one guesses your gate",
    latitude: Double,
    longitude: Double,
    selectedEstateName: String? = null,
    addressLabel: String = "",
    estates: List<EldoretEstate> = EldoretCatalogDefaults.defaultEstates,
    readOnly: Boolean = false,
    radiusMeters: Double = 0.0,
    markers: List<OsmMapMarker> = emptyList(),
    mapHeight: Dp = 250.dp,
    accentColor: Color = PremiumTealDark,
    onLocationSelected: (lat: Double, lng: Double, nearestEstate: EldoretEstate, resolvedAddress: String?) -> Unit = { _, _, _, _ -> },
    onMarkerClicked: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var showFullScreen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLocatingGps by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val nearestEstate = remember(latitude, longitude, estates) {
        EldoretCatalogDefaults.findNearestEstate(latitude, longitude, estates)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocatingGps = true
            statusBanner = "Acquiring GPS coordinates..."
            fetchDeviceGpsLocation(
                context = context,
                onResult = { lat, lng ->
                    isLocatingGps = false
                    val est = EldoretCatalogDefaults.findNearestEstate(lat, lng, estates)
                    statusBanner = "GPS locked near ${est.name} (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                    onLocationSelected(lat, lng, est, null)
                    webViewRef?.evaluateJavascript("window.setPinFromAndroid($lat, $lng, true);", null)
                },
                onError = { err ->
                    isLocatingGps = false
                    statusBanner = err
                }
            )
        } else {
            statusBanner = "Location permission denied — tap any street on the OSM map instead."
        }
    }

    val triggerGps = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            isLocatingGps = true
            statusBanner = "Acquiring GPS coordinates..."
            fetchDeviceGpsLocation(
                context = context,
                onResult = { lat, lng ->
                    isLocatingGps = false
                    val est = EldoretCatalogDefaults.findNearestEstate(lat, lng, estates)
                    statusBanner = "GPS locked near ${est.name} (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                    onLocationSelected(lat, lng, est, null)
                    webViewRef?.evaluateJavascript("window.setPinFromAndroid($lat, $lng, true);", null)
                },
                onError = { err ->
                    isLocatingGps = false
                    statusBanner = err
                }
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Title + GPS & Fullscreen Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!readOnly) {
                        FilledTonalButton(
                            onClick = triggerGps,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLocatingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "My GPS",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("My GPS", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(
                        onClick = { showFullScreen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            Icons.Default.OpenInFull,
                            contentDescription = "Expand Map",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar (only in interactive picker mode)
            if (!readOnly) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Eldoret street, hostel, or landmark...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val escaped = searchQuery.replace("'", "\\'")
                                    webViewRef?.evaluateJavascript("window.searchPlaceInEldoret('$escaped');", null)
                                }
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Search", tint = accentColor)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                val escaped = searchQuery.replace("'", "\\'")
                                webViewRef?.evaluateJavascript("window.searchPlaceInEldoret('$escaped');", null)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick-Jump Eldoret Estate Chips
            if (!readOnly && estates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(estates) { estate ->
                        val isSelected = (selectedEstateName ?: nearestEstate.name)
                            .equals(estate.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onLocationSelected(
                                    estate.latitude,
                                    estate.longitude,
                                    estate,
                                    "${estate.name}, Eldoret"
                                )
                                webViewRef?.evaluateJavascript(
                                    "window.setPinFromAndroid(${estate.latitude}, ${estate.longitude}, false);",
                                    null
                                )
                            },
                            label = { Text(estate.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Embedded Interactive OpenStreetMap WebView
            // NOTE: Do NOT apply Modifier.clip() here — clipping a WebView's parent to a rounded shape
            // prevents Android WebView from painting GPU-composited CSS layers (Leaflet panes/tiles),
            // leaving only the page background visible. Rounded corners are applied in HTML/CSS instead.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(1.dp)
            ) {
                OsmLeafletWebView(
                    latitude = latitude,
                    longitude = longitude,
                    readOnly = readOnly,
                    radiusMeters = radiusMeters,
                    markers = markers,
                    onWebViewCreated = { webViewRef = it },
                    onPinMoved = { lat, lng, roadLabel ->
                        val est = EldoretCatalogDefaults.findNearestEstate(lat, lng, estates)
                        statusBanner = roadLabel?.takeIf { it.isNotBlank() }
                            ?: "Pinned in ${est.name} (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                        onLocationSelected(lat, lng, est, roadLabel)
                    },
                    onMarkerClicked = onMarkerClicked
                )

                // Floating Bottom-Left Coordinate & Estate Pill
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreenDark)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedEstateName ?: nearestEstate.name} • ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Floating Top-Right "Expand" Button
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clickable { showFullScreen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Full Screen",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Full Map",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            // Resolved Address / Status Row + External Maps Navigation Link
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayAddr = statusBanner
                        ?: addressLabel.takeIf { it.isNotBlank() }
                        ?: "${selectedEstateName ?: nearestEstate.name}, Eldoret"
                    Text(
                        text = displayAddr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (readOnly) {
                    Button(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(addressLabel.ifBlank { "Mama Fua Job" })})")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    // Full-Screen Interactive OpenStreetMap Modal
    if (showFullScreen) {
        var modalLat by remember { mutableStateOf(latitude) }
        var modalLng by remember { mutableStateOf(longitude) }
        var modalAddr by remember { mutableStateOf<String?>(addressLabel.takeIf { it.isNotBlank() }) }
        var modalWebView by remember { mutableStateOf<WebView?>(null) }
        var modalSearch by remember { mutableStateOf("") }

        val modalEstate = remember(modalLat, modalLng, estates) {
            EldoretCatalogDefaults.findNearestEstate(modalLat, modalLng, estates)
        }

        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showFullScreen = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                    Column {
                                        Text(
                                            text = if (readOnly) "OpenStreetMap Location View" else "Pin Exact Gate on OpenStreetMap",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${modalEstate.name} • ${"%.5f".format(modalLat)}, ${"%.5f".format(modalLng)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor
                                        )
                                    }
                                }
                                if (!readOnly) {
                                    FilledTonalButton(
                                        onClick = {
                                            fetchDeviceGpsLocation(
                                                context = context,
                                                onResult = { lat, lng ->
                                                    modalLat = lat
                                                    modalLng = lng
                                                    modalWebView?.evaluateJavascript(
                                                        "window.setPinFromAndroid($lat, $lng, true);",
                                                        null
                                                    )
                                                },
                                                onError = {}
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GPS")
                                    }
                                }
                            }

                            if (!readOnly) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = modalSearch,
                                    onValueChange = { modalSearch = it },
                                    placeholder = { Text("Search Eldoret road, apartment, or hostel...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (modalSearch.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val esc = modalSearch.replace("'", "\\'")
                                                    modalWebView?.evaluateJavascript("window.searchPlaceInEldoret('$esc');", null)
                                                }
                                            ) {
                                                Icon(Icons.Default.ArrowForward, contentDescription = "Go")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(estates) { est ->
                                        FilterChip(
                                            selected = modalEstate.name.equals(est.name, ignoreCase = true),
                                            onClick = {
                                                modalLat = est.latitude
                                                modalLng = est.longitude
                                                modalAddr = "${est.name}, Eldoret"
                                                modalWebView?.evaluateJavascript(
                                                    "window.setPinFromAndroid(${est.latitude}, ${est.longitude}, false);",
                                                    null
                                                )
                                            },
                                            label = { Text(est.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Full-Screen Map Body
                    Box(modifier = Modifier.weight(1f)) {
                        OsmLeafletWebView(
                            latitude = modalLat,
                            longitude = modalLng,
                            readOnly = readOnly,
                            radiusMeters = radiusMeters,
                            markers = markers,
                            onWebViewCreated = { modalWebView = it },
                            onPinMoved = { lat, lng, roadLabel ->
                                modalLat = lat
                                modalLng = lng
                                if (!roadLabel.isNullOrBlank()) {
                                    modalAddr = roadLabel
                                }
                            },
                            onMarkerClicked = { id ->
                                onMarkerClicked(id)
                            }
                        )
                    }

                    // Bottom Confirmation Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = modalAddr ?: "${modalEstate.name}, Eldoret",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Nearest Estate: ${modalEstate.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    if (!readOnly) {
                                        onLocationSelected(modalLat, modalLng, modalEstate, modalAddr)
                                        webViewRef?.evaluateJavascript(
                                            "window.setPinFromAndroid($modalLat, $modalLng, false);",
                                            null
                                        )
                                    }
                                    showFullScreen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (readOnly) "Done" else "Confirm Pin Location", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private object LeafletAssetCache {
    @Volatile
    private var cachedCss: String? = null
    @Volatile
    private var cachedJs: String? = null

    fun getCss(context: Context): String {
        return cachedCss ?: synchronized(this) {
            cachedCss ?: runCatching {
                context.assets.open("osm/leaflet.css").bufferedReader().use { it.readText() }
            }.getOrNull().also { if (!it.isNullOrBlank()) cachedCss = it } ?: ""
        }
    }

    fun getJs(context: Context): String {
        return cachedJs ?: synchronized(this) {
            cachedJs ?: runCatching {
                context.assets.open("osm/leaflet.js").bufferedReader().use { it.readText() }
            }.getOrNull().also { if (!it.isNullOrBlank()) cachedJs = it } ?: ""
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun OsmLeafletWebView(
    latitude: Double,
    longitude: Double,
    readOnly: Boolean,
    radiusMeters: Double,
    markers: List<OsmMapMarker>,
    onWebViewCreated: (WebView) -> Unit,
    onPinMoved: (Double, Double, String?) -> Unit,
    onMarkerClicked: (Int) -> Unit
) {
    val currentOnPinMoved by rememberUpdatedState(onPinMoved)
    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)
    var isMapReady by remember { mutableStateOf(false) }

    val markersJson = remember(markers) {
        buildString {
            append("[")
            markers.forEachIndexed { index, m ->
                if (index > 0) append(",")
                val safeTitle = m.title.replace("\"", "\\\"").replace("\n", " ")
                val safeSub = m.subtitle.replace("\"", "\\\"").replace("\n", " ")
                val safeBadge = m.badgeText.replace("\"", "\\\"").replace("\n", " ")
                append(
                    """{"id":${m.id},"lat":${m.latitude},"lng":${m.longitude},"title":"$safeTitle","subtitle":"$safeSub","badge":"$safeBadge","color":"${m.colorHex}","pulsing":${m.isPulsing}}"""
                )
            }
            append("]")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#E2E8F0"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.userAgentString = WebSettings.getDefaultUserAgent(ctx) + " MamaFuaEldoretApp/1.0 (com.mamafua.app)"

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            android.util.Log.d("OsmLocationMap", "[JS] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript("if (window.map) { window.map.invalidateSize(); }", null)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.w("OsmLocationMap", "WebView resource error: ${error?.description} for ${request?.url}")
                        }
                    }

                    // Prevent parent ScrollView / LazyColumn from stealing touch events while panning/zooming the map
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onMapReady() {
                                post {
                                    isMapReady = true
                                }
                            }

                            @JavascriptInterface
                            fun onLocationPicked(lat: Double, lng: Double, address: String?) {
                                post {
                                    currentOnPinMoved(lat, lng, address?.takeIf { it.isNotBlank() })
                                }
                            }

                            @JavascriptInterface
                            fun onMarkerTap(markerId: Int) {
                                post {
                                    currentOnMarkerClicked(markerId)
                                }
                            }
                        },
                        "AndroidBridge"
                    )

                    val html = buildLeafletHtml(
                        context = ctx,
                        initLat = latitude,
                        initLng = longitude,
                        readOnly = readOnly,
                        radiusMeters = radiusMeters,
                        markersJson = markersJson
                    )
                    loadDataWithBaseURL("https://com.mamafua.app/", html, "text/html", "UTF-8", null)
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webView.evaluateJavascript(
                    "if (typeof window.syncMarkersFromAndroid === 'function') { window.syncMarkersFromAndroid($markersJson); }",
                    null
                )
            }
        )

        AnimatedVisibility(
            visible = !isMapReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading Eldoret Map...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun buildLeafletHtml(
    context: Context,
    initLat: Double,
    initLng: Double,
    readOnly: Boolean,
    radiusMeters: Double,
    markersJson: String
): String {
    val localCss = LeafletAssetCache.getCss(context)
    val localJs = LeafletAssetCache.getJs(context)

    val cssSnippet = if (localCss.isNotBlank()) {
        "<style>\n$localCss\n</style>"
    } else {
        """<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />"""
    }

    val jsSnippet = if (localJs.isNotBlank()) {
        "<script>window.L_DISABLE_3D = true;</script>\n<script>\n$localJs\n</script>"
    } else {
        """<script>window.L_DISABLE_3D = true;</script><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>"""
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
$cssSnippet
<style>
  html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;
    overflow: hidden;
    background-color: #E2E8F0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  #map {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    min-height: 100%;
    background-color: #E2E8F0;
    border-radius: 15px;
    overflow: hidden;
  }
  .main-pin {
    width: 30px;
    height: 30px;
    border-radius: 50% 50% 50% 0;
    background: #0077B6;
    position: absolute;
    transform: rotate(-45deg);
    left: 50%;
    top: 50%;
    margin: -22px 0 0 -15px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.4);
    border: 3px solid #ffffff;
  }
  .main-pin::after {
    content: '';
    width: 10px;
    height: 10px;
    margin: 7px 0 0 7px;
    background: #ffffff;
    position: absolute;
    border-radius: 50%;
  }
  .job-marker {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 5px 9px;
    border-radius: 14px;
    color: #ffffff;
    font-weight: 700;
    font-size: 11px;
    white-space: nowrap;
    box-shadow: 0 3px 8px rgba(0,0,0,0.35);
    border: 2px solid #ffffff;
  }
  @keyframes pulseRing {
    0% { box-shadow: 0 0 0 0 rgba(247, 127, 0, 0.7); }
    70% { box-shadow: 0 0 0 12px rgba(247, 127, 0, 0); }
    100% { box-shadow: 0 0 0 0 rgba(247, 127, 0, 0); }
  }
  .pulsing {
    animation: pulseRing 1.6s infinite;
  }
</style>
</head>
<body>
<div id="map"></div>
$jsSnippet
<script>
  var readOnly = $readOnly;
  var mainLat = $initLat;
  var mainLng = $initLng;
  var radiusMeters = $radiusMeters;
  var map = null;
  var mainMarker = null;
  var radiusCircle = null;
  var extraLayer = null;
  var initialMarkers = $markersJson;

  function initMap() {
    if (typeof L === 'undefined') {
      console.warn("Leaflet not yet loaded, retrying in 100ms...");
      setTimeout(initMap, 100);
      return;
    }
    if (map) return;

    try {
      map = L.map('map', {
        zoomControl: true,
        attributionControl: false,
        bounceAtZoomLimits: false,
        zoomAnimation: false,
        fadeAnimation: false,
        markerZoomAnimation: false,
        inertia: false
      }).setView([mainLat, mainLng], 15);

      // Primary: official OpenStreetMap tile server (no API key required)
      var osmStandard = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
      });

      // Fallback: OpenStreetMap France humanitarian tiles (no API key required)
      var osmFallback = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
        maxZoom: 19,
        subdomains: ['a', 'b', 'c']
      });

      osmStandard.addTo(map);

      // Invalidate layout sizes after render to guarantee full tile display in Android WebViews
      setTimeout(function() {
        if (map) map.invalidateSize();
      }, 150);
      setTimeout(function() {
        if (map) map.invalidateSize();
      }, 600);

      var pinIcon = L.divIcon({
        className: 'custom-div-icon',
        html: "<div class='main-pin'></div>",
        iconSize: [32, 44],
        iconAnchor: [16, 44]
      });

      mainMarker = L.marker([mainLat, mainLng], {
        icon: pinIcon,
        draggable: !readOnly
      }).addTo(map);

      if (radiusMeters > 0) {
        radiusCircle = L.circle([mainLat, mainLng], {
          radius: radiusMeters,
          color: '#0077B6',
          fillColor: '#00B4D8',
          fillOpacity: 0.12,
          weight: 1.8
        }).addTo(map);
      }

      extraLayer = L.layerGroup().addTo(map);

      if (!readOnly) {
        map.on('click', function(e) {
          var lat = e.latlng.lat;
          var lng = e.latlng.lng;
          mainMarker.setLatLng([lat, lng]);
          reverseGeocodeAndNotify(lat, lng);
        });

        mainMarker.on('dragend', function(e) {
          var pos = e.target.getLatLng();
          reverseGeocodeAndNotify(pos.lat, pos.lng);
        });
      }

      if (initialMarkers && initialMarkers.length) {
        window.syncMarkersFromAndroid(initialMarkers);
      }

      if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
        window.AndroidBridge.onMapReady();
      }
      var sz = map.getSize();
      console.log("Map ready: size=" + sz.x + "x" + sz.y + " any3d=" + L.Browser.any3d);
      osmStandard.on('tileerror', function(ev) {
        console.warn("OSM tile failed, switching to fallback tiles: " + (ev.tile && ev.tile.src));
        if (map.hasLayer(osmStandard)) {
          map.removeLayer(osmStandard);
          osmFallback.addTo(map);
        }
      });
    } catch (err) {
      console.error("Map initialization failed: " + err);
    }
  }

  function reverseGeocodeAndNotify(lat, lng) {
    if (radiusCircle) {
      radiusCircle.setLatLng([lat, lng]);
    }
    var url = 'https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=' + lat + '&lon=' + lng;
    fetch(url, { headers: { 'Accept-Language': 'en' } })
      .then(function(r) { return r.json(); })
      .then(function(data) {
        var addr = '';
        if (data && data.address) {
          var parts = [];
          if (data.address.road) parts.push(data.address.road);
          if (data.address.neighbourhood || data.address.suburb || data.address.residential) {
            parts.push(data.address.neighbourhood || data.address.suburb || data.address.residential);
          }
          if (data.name && parts.indexOf(data.name) === -1) {
            parts.unshift(data.name);
          }
          addr = parts.join(', ');
        }
        if (!addr && data && data.display_name) {
          addr = data.display_name.split(',').slice(0, 2).join(', ');
        }
        if (window.AndroidBridge && window.AndroidBridge.onLocationPicked) {
          window.AndroidBridge.onLocationPicked(lat, lng, addr);
        }
      })
      .catch(function() {
        if (window.AndroidBridge && window.AndroidBridge.onLocationPicked) {
          window.AndroidBridge.onLocationPicked(lat, lng, '');
        }
      });
  }

  window.setPinFromAndroid = function(lat, lng, shouldReverseGeocode) {
    mainLat = lat;
    mainLng = lng;
    if (mainMarker) {
      mainMarker.setLatLng([lat, lng]);
    }
    if (radiusCircle) {
      radiusCircle.setLatLng([lat, lng]);
    }
    if (map) {
      map.flyTo([lat, lng], 15, { duration: 0.6 });
      map.invalidateSize();
    }
    if (shouldReverseGeocode) {
      reverseGeocodeAndNotify(lat, lng);
    }
  };

  window.searchPlaceInEldoret = function(query) {
    if (!query) return;
    var q = encodeURIComponent(query + ', Eldoret, Kenya');
    fetch('https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=' + q)
      .then(function(r) { return r.json(); })
      .then(function(results) {
        if (results && results.length > 0) {
          var lat = parseFloat(results[0].lat);
          var lng = parseFloat(results[0].lon);
          var label = (results[0].display_name || query).split(',').slice(0, 2).join(', ');
          if (mainMarker) mainMarker.setLatLng([lat, lng]);
          if (radiusCircle) radiusCircle.setLatLng([lat, lng]);
          if (map) {
            map.flyTo([lat, lng], 16, { duration: 0.7 });
            map.invalidateSize();
          }
          if (window.AndroidBridge && window.AndroidBridge.onLocationPicked) {
            window.AndroidBridge.onLocationPicked(lat, lng, label);
          }
        }
      })
      .catch(function() {});
  };

  window.syncMarkersFromAndroid = function(markers) {
    if (!extraLayer) {
        initialMarkers = markers;
        return;
    }
    extraLayer.clearLayers();
    if (!markers || !markers.length) return;
    markers.forEach(function(m) {
      var pulseClass = m.pulsing ? 'job-marker pulsing' : 'job-marker';
      var badgeLabel = m.badge ? m.badge : m.title;
      var icon = L.divIcon({
        className: '',
        html: "<div class='" + pulseClass + "' style='background:" + (m.color || '#F77F00') + ";'>" + badgeLabel + "</div>",
        iconSize: [70, 26],
        iconAnchor: [35, 13]
      });
      var mk = L.marker([m.lat, m.lng], { icon: icon }).addTo(extraLayer);
      var popupHtml = "<b>" + m.title + "</b>";
      if (m.subtitle) popupHtml += "<br/><small>" + m.subtitle + "</small>";
      mk.bindPopup(popupHtml);
      mk.on('click', function() {
        if (window.AndroidBridge && window.AndroidBridge.onMarkerTap) {
          window.AndroidBridge.onMarkerTap(m.id);
        }
      });
    });
  };

  window.addEventListener('resize', function() {
    if (map) map.invalidateSize();
  });

  if (document.readyState === 'complete' || document.readyState === 'interactive') {
    initMap();
  } else {
    window.addEventListener('DOMContentLoaded', initMap);
  }
</script>
</body>
</html>
    """.trimIndent()
}

@SuppressLint("MissingPermission")
fun fetchDeviceGpsLocation(
    context: Context,
    onResult: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onResult(location.latitude, location.longitude)
                } else {
                    fused.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onResult(last.latitude, last.longitude)
                        } else {
                            fallbackLocationManager(context, onResult, onError)
                        }
                    }.addOnFailureListener {
                        fallbackLocationManager(context, onResult, onError)
                    }
                }
            }
            .addOnFailureListener {
                fallbackLocationManager(context, onResult, onError)
            }
    } catch (e: Exception) {
        fallbackLocationManager(context, onResult, onError)
    }
}

@SuppressLint("MissingPermission")
private fun fallbackLocationManager(
    context: Context,
    onResult: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (gpsLoc != null) {
            onResult(gpsLoc.latitude, gpsLoc.longitude)
        } else {
            onError("GPS signal unavailable — tap your street on the OpenStreetMap to drop a pin.")
        }
    } catch (e: Exception) {
        onError("Could not read GPS — tap your location directly on the map.")
    }
}

