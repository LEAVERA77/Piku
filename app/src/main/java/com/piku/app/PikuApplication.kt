package com.piku.app

import android.app.Application
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.data.security.InstallSessionGuard
import org.osmdroid.config.Configuration
import java.io.File

class PikuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        InstallSessionGuard.apply(this)
        val osmdroidBase = File(cacheDir, "osmdroid").apply { mkdirs() }
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = File(osmdroidBase, "tiles").apply { mkdirs() }
        }
        RetrofitInstance.init(this)
    }
}
