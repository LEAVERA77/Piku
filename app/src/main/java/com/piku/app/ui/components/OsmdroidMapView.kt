package com.piku.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.piku.app.R
import com.piku.app.data.TipoComercio
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.Rubro
import com.piku.app.ui.media.PikuImages
import com.piku.app.utils.CartoMapTiles
import com.piku.app.utils.MapClusterBitmap
import com.piku.app.utils.MapClusterEngine
import com.piku.app.utils.MapLabelPriority
import com.piku.app.utils.MapLogoCache
import com.piku.app.utils.MapMarkerItem
import com.piku.app.utils.MapPinBitmap
import com.piku.app.utils.MapRubroUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import android.util.Log
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import kotlin.math.abs

@Composable
fun OsmdroidMapView(
    comercios: List<Comercio>,
    centerLat: Double,
    centerLon: Double,
    userLat: Double?,
    userLon: Double?,
    onComercioClick: (Comercio) -> Unit,
    onClusterClick: (lat: Double, lon: Double, zoomActual: Double) -> Unit,
    onViewportChanged: ((minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) -> Unit)?,
    zoomLevel: Double = 15.0,
    mapDarkTheme: Boolean = false,
    rubros: List<Rubro> = emptyList(),
    rubrosSeleccionados: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapZoom by remember { mutableDoubleStateOf(zoomLevel) }
    var pulsePhase by remember { mutableFloatStateOf(0f) }
    var logosPorId by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }

    val hayNovedades = remember(comercios) { comercios.any { it.ofertasNuevas > 0 } }
    val filtroActivo = rubrosSeleccionados.isNotEmpty()

    LaunchedEffect(hayNovedades) {
        if (!hayNovedades) return@LaunchedEffect
        while (isActive) {
            delay(380)
            pulsePhase = (pulsePhase + 0.11f) % 1f
        }
    }

    LaunchedEffect(comercios) {
        val cloud = ConfigLoader.cloudinaryCloudName(context)
        val loaded = withContext(Dispatchers.IO) {
            val map = LinkedHashMap<String, Bitmap>()
            comercios
                .filter { !it.esOpenStreetMap() }
                .forEach { c ->
                    val url = PikuImages.resolve(c.logoUrl, c.id, c.nombre, cloud)
                    MapLogoCache.load(context, url, 128)?.let { map[c.id] = it }
                }
            map
        }
        logosPorId = loaded
    }

    val userIcon = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_map_user_location)?.let { drawable ->
            BitmapDrawable(context.resources, drawable.toBitmap(48, 48))
        }
    }
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 14.0
            maxZoomLevel = 20.0
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(centerLat, centerLon))
        }
    }

    LaunchedEffect(mapDarkTheme) {
        mapView.setTileSource(CartoMapTiles.tileSource(mapDarkTheme))
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    DisposableEffect(mapView, onViewportChanged) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                notifyViewport(mapView, onViewportChanged)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val z = mapView.zoomLevelDouble
                if (abs(z - mapZoom) > 0.04) {
                    mapZoom = z
                }
                notifyViewport(mapView, onViewportChanged)
                return false
            }
        }
        mapView.addMapListener(listener)
        mapZoom = mapView.zoomLevelDouble
        mapView.post { notifyViewport(mapView, onViewportChanged) }
        onDispose { mapView.removeMapListener(listener) }
    }

    LaunchedEffect(centerLat, centerLon, zoomLevel) {
        mapView.controller.setZoom(zoomLevel)
        mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
        mapZoom = zoomLevel
    }

    LaunchedEffect(
        comercios,
        userLat,
        userLon,
        userIcon,
        mapZoom,
        mapDarkTheme,
        rubros,
        rubrosSeleccionados,
        logosPorId,
        pulsePhase,
        hayNovedades
    ) {
        val zoomScale = MapPinBitmap.escalaDesdeZoom(mapZoom)
        val etiquetasPermitidas = MapLabelPriority.idsConEtiqueta(comercios, mapZoom)
        val refLat = userLat ?: centerLat
        val items = MapClusterEngine.agrupar(comercios, mapZoom, refLat)

        mapView.overlays.clear()
        if (userLat != null && userLon != null) {
            val userPoint = GeoPoint(userLat, userLon)
            val precision = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(userPoint, RADIO_UBICACION_METROS)
                fillPaint.color = Color.parseColor(if (mapDarkTheme) "#334CDB94" else "#330D9488")
                outlinePaint.color = Color.parseColor(if (mapDarkTheme) "#4CDB94" else "#0D9488")
                outlinePaint.strokeWidth = 2f
            }
            mapView.overlays.add(precision)
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = userPoint
                    title = "Tú estás aquí"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = userIcon
                }
            )
        }

        data class MarkerData(
            val lat: Double,
            val lon: Double,
            val title: String,
            val snippet: String,
            val icon: BitmapDrawable,
            val anchorY: Float,
            val onClick: () -> Unit
        )

        val markers = withContext(Dispatchers.Default) {
            items.mapNotNull { item ->
                when (item) {
                    is MapMarkerItem.Cluster -> {
                        val count = item.comercios.size
                        val icon = if (mapZoom >= 15.2) {
                            MapClusterBitmap.crearConEtiqueta(context, count, zoomScale, mapDarkTheme)
                        } else {
                            MapClusterBitmap.crear(context, count, zoomScale, mapDarkTheme)
                        }
                        MarkerData(
                            lat = item.lat,
                            lon = item.lon,
                            title = "$count comercios",
                            snippet = "Tocá para acercar",
                            icon = icon,
                            anchorY = MapClusterBitmap.anchorY(),
                            onClick = { onClusterClick(item.lat, item.lon, mapZoom) }
                        )
                    }
                    is MapMarkerItem.Single -> {
                        val comercio = item.comercio
                        val lat = comercio.lat ?: return@mapNotNull null
                        val lon = comercio.lon ?: return@mapNotNull null
                        val resalta = !filtroActivo || MapRubroUtil.coincideRubro(comercio, rubros, rubrosSeleccionados)
                        val atenuado = filtroActivo && !resalta
                        val mostrarEtiqueta = MapPinBitmap.mostrarEtiqueta(mapZoom) &&
                            comercio.id in etiquetasPermitidas
                        val emoji = TipoComercio.emojiPara(comercio)
                        val icon = MapPinBitmap.crear(
                            context = context,
                            emoji = emoji,
                            nombre = comercio.nombre,
                            cantidadOfertas = comercio.cantidadOfertas,
                            ofertasNuevas = comercio.ofertasNuevas,
                            realizaEnvios = comercio.realizaEnvios,
                            destacado = comercio.destacado && !comercio.esOpenStreetMap(),
                            zoomScale = zoomScale,
                            mostrarEtiqueta = mostrarEtiqueta,
                            modoOscuro = mapDarkTheme,
                            atenuado = atenuado,
                            pulsePhase = if (comercio.ofertasNuevas > 0 && hayNovedades) pulsePhase else -1f,
                            logoBitmap = logosPorId[comercio.id]
                        )
                        val snippet = when {
                            comercio.ofertasNuevas > 0 && comercio.cantidadOfertas > 0 ->
                                "${comercio.ofertasNuevas} nueva(s) · ${comercio.cantidadOfertas} oferta(s)"
                            comercio.ofertasNuevas > 0 ->
                                "${comercio.ofertasNuevas} oferta(s) nueva(s)"
                            comercio.cantidadOfertas > 0 ->
                                "${comercio.cantidadOfertas} oferta(s) activa(s)"
                            else -> comercio.direccion ?: ""
                        }
                        MarkerData(
                            lat = lat,
                            lon = lon,
                            title = comercio.nombre,
                            snippet = snippet,
                            icon = icon,
                            anchorY = MapPinBitmap.anchorY(
                                nombre = comercio.nombre,
                                ofertasNuevas = comercio.ofertasNuevas,
                                cantidadOfertas = comercio.cantidadOfertas,
                                zoomScale = zoomScale,
                                mostrarEtiqueta = mostrarEtiqueta
                            ),
                            onClick = { onComercioClick(comercio) }
                        )
                    }
                }
            }
        }

        markers.forEach { data ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(data.lat, data.lon)
                    title = data.title
                    snippet = data.snippet
                    setAnchor(Marker.ANCHOR_CENTER, data.anchorY)
                    icon = data.icon
                    setOnMarkerClickListener { _, _ ->
                        data.onClick()
                        InfoWindow.closeAllInfoWindowsOn(mapView)
                        true
                    }
                }
            )
        }

        Log.d(
            TAG,
            "Mapa: ${items.size} items, zoom=${"%.1f".format(mapZoom)}, dark=$mapDarkTheme, logos=${logosPorId.size}"
        )
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { it.invalidate() }
    )
}

private const val TAG = "OsmdroidMapView"
private const val RADIO_UBICACION_METROS = 15.0

private fun notifyViewport(
    mapView: MapView,
    onViewportChanged: ((minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) -> Unit)?
) {
    if (onViewportChanged == null) return
    val bb: BoundingBox = mapView.boundingBox ?: return
    onViewportChanged(bb.latSouth, bb.latNorth, bb.lonWest, bb.lonEast)
}
