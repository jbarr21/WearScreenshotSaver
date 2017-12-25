package com.github.jbarr21.wearscreenshotsaver

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES

val CHANNEL_ID_DEFAULT = "default"

private val SUPPORTS_CHANNELS = Build.VERSION.SDK_INT >= VERSION_CODES.O

@TargetApi(VERSION_CODES.O)
fun createNotificationChannels(context: Context) {
  if (SUPPORTS_CHANNELS) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CHANNEL_ID_DEFAULT, "Default", NotificationManager.IMPORTANCE_DEFAULT))
  }
}

fun createNotification(context: Context): Notification.Builder {
  return if (SUPPORTS_CHANNELS) {
    Notification.Builder(context, CHANNEL_ID_DEFAULT)
  } else {
    Notification.Builder(context)
  }
}
