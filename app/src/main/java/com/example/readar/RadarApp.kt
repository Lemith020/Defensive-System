package com.example.readar

import android.app.Application

class RadarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RadarEngine.init(this)
    }
}