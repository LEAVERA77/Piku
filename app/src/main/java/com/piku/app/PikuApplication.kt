package com.piku.app

import android.app.Application
import com.piku.app.data.network.RetrofitInstance
import org.osmdroid.config.Configuration

class PikuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        RetrofitInstance.init(this)
    }
}
