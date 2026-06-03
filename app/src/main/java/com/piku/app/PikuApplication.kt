package com.piku.app

import android.app.Application
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.data.security.InstallSessionGuard
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class PikuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            InstallSessionGuard.apply(this@PikuApplication)
            AuthDataStore.warmCache(this@PikuApplication)
        }
        val osmdroidBase = File(cacheDir, "osmdroid").apply { mkdirs() }
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = File(osmdroidBase, "tiles").apply { mkdirs() }
        }
        RetrofitInstance.init(this)
    }
}
