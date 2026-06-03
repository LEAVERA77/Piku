package com.piku.app.utils

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

object CartoMapTiles {

    val DarkMatter: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "CartoDB.DarkMatter",
        0,
        20,
        256,
        "",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String =
            baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + ".png"
    }

    fun tileSource(oscuro: Boolean) = if (oscuro) DarkMatter else TileSourceFactory.MAPNIK
}
