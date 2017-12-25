package com.github.jbarr21.wearscreenshotsaver

import android.app.Application

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    createNotificationChannels(this)
  }
}
