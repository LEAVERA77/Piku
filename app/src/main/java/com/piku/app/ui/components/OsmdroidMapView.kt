package com.piku.app.ui.components

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.piku.app.data.model.Comercio
import com.piku.app.utils.MapPinBitmap
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow

@Composable
fun OsmdroidMapView(
    comercios: List<Comercio>,
    centerLat: Double,
    centerLon: Double,
    userLat: Double?,
    userLon: Double?,
    onComercioClick: (Comercio) -> Unit,
    onViewportChanged: ((minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) -> Unit)?,
    zoomLevel: Double = 14.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pinIcon = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_map_pin)?.let { drawable ->
            BitmapDrawable(context.resources, drawable.toBitmap(64, 80))
        }
    }
    val userIcon = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_map_user_location)?.let { drawable ->
            BitmapDrawable(context.resources, drawable.toBitmap(48, 48))
        }
    }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(centerLat, centerLon))
        }
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
        if (onViewportChanged == null) return@DisposableEffect onDispose {}
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                notifyViewport(mapView, onViewportChanged)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                notifyViewport(mapView, onViewportChanged)
                return false
            }
        }
        mapView.addMapListener(listener)
        mapView.post { notifyViewport(mapView, onViewportChanged) }
        onDispose { mapView.removeMapListener(listener) }
    }

    LaunchedEffect(centerLat, centerLon, zoomLevel) {
        mapView.controller.setZoom(zoomLevel)
        mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
    }

    LaunchedEffect(comercios, userLat, userLon, pinIcon, userIcon) {
        mapView.overlays.clear()
        if (userLat != null && userLon != null) {
            val userPoint = GeoPoint(userLat, userLon)
            val precision = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(userPoint, 80.0)
                fillPaint.color = Color.argb(0x33, 0x25, 0x63, 0xEB)
                outlinePaint.color = Color.argb(0x88, 0x25, 0x63, 0xEB)
                outlinePaint.strokeWidth = 2f
            }
            mapView.overlays.add(precision)
            val userMarker = Marker(mapView).apply {
                position = userPoint
                title = "Tú estás aquí"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = userIcon
            }
            mapView.overlays.add(userMarker)
        }
        comercios.forEach { comercio ->
            val lat = comercio.lat ?: return@forEach
            val lon = comercio.lon ?: return@forEach
            val emoji = TipoComercio.emojiPara(comercio)
            val markerIcon = MapPinBitmap.crear(
                context = context,
                emoji = emoji,
                nombre = comercio.nombre,
                cantidadOfertas = comercio.cantidadOfertas,
                realizaEnvios = comercio.realizaEnvios
            )
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = comercio.nombre
                snippet = if (comercio.cantidadOfertas > 0) {
                    "${comercio.cantidadOfertas} oferta(s) activa(s)"
                } else {
                    comercio.direccion ?: ""
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = markerIcon
                setOnMarkerClickListener { _, _ ->
                    onComercioClick(comercio)
                    InfoWindow.closeAllInfoWindowsOn(mapView)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { it.invalidate() }
    )
}

private fun notifyViewport(
    mapView: MapView,
    onViewportChanged: (minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) -> Unit
) {
    val bb: BoundingBox = mapView.boundingBox ?: return
    onViewportChanged(bb.latSouth, bb.latNorth, bb.lonWest, bb.lonEast)
}
