package com.floraflow.app

import android.app.Application
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.worker.NotificationWorker

class FloraFlowApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val unsplashApi by lazy { RetrofitClient.unsplashApi }
    val openAiApi by lazy { RetrofitClient.openAiApi }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationWorker.createChannel(this)
    }

    companion object {
        lateinit var instance: FloraFlowApp
            private set
    }
}
