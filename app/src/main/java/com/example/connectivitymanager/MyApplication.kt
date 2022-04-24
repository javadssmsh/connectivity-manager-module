package com.example.connectivitymanager

import android.app.Application
import com.example.connectivitymanagermodule.CheckConnectivityModule

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CheckConnectivityModule.initialize(applicationContext)

    }
}