package com.learning.dashboard

import android.app.Application
import com.learning.dashboard.di.AppContainer
import com.learning.dashboard.di.DefaultAppContainer

class LearningApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
