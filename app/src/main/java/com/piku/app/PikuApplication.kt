package com.piku.app

import android.app.Application
import com.piku.app.data.network.RetrofitInstance

class PikuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this)
    }
}
