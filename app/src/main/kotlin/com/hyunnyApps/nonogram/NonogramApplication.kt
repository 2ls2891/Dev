package com.hyunnyApps.nonogram

import android.app.Application
import com.hyunnyApps.nonogram.reminder.DailyReminder

class NonogramApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DailyReminder.createChannel(this)
    }
}
