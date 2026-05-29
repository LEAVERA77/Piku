package com.piku.app.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.piku.app.R
import com.piku.app.data.model.Comercio
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

@Composable
fun OsmdroidMapView(
    comercios: List<Comercio>,
    centerLat: Double,
    centerLon: Double,
    onComercioClick: (Comercio) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinIcon = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_map_pin)?.let { drawable ->
            BitmapDrawable(context.resources, drawable.toBitmap(64, 80))
        }
    }
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    LaunchedEffect(centerLat, centerLon) {
        mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
    }

    LaunchedEffect(comercios, pinIcon) {
        mapView.overlays.clear()
        comercios.forEach { comercio ->
            val lat = comercio.lat ?: return@forEach
            val lon = comercio.lon ?: return@forEach
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = comercio.nombre
                snippet = comercio.direccion ?: ""
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinIcon
                setOnMarkerClickListener { m, _ ->
                    onComercioClick(comercio)
                    InfoWindow.closeAllInfoWindowsOn(mapView)
                    m.showInfoWindow()
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
