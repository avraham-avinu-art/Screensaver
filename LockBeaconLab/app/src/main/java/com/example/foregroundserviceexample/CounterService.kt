package com.example.foregroundserviceexample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CounterService : Service() {

    private var isRunning = false
    private var counter = 0
    private val channelId = "counter_channel"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        val counterState = MutableStateFlow(0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            "START" -> startCounter()
            "STOP" -> stopCounter()
            /*"NOTIFICATION_CLICK" -> {
                stopForeground(true)   // 🔥 remove notification
                openMainActivity()
            }*/
        }

        return START_STICKY
    }

    private fun openMainActivity() {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(activityIntent)
    }

    private fun startCounter() {
        if (isRunning) return

        isRunning = true
        createNotificationChannel()
        startForeground(1, createNotification(counterState.value))

        serviceScope.launch {
            while (isRunning) {
                delay(1000)
                counter++
                counterState.value = counter
                updateNotification(counterState.value)
            }
        }
    }

    fun updateNotification(value: Int) {
        val notify= createNotification(value)
        val manger= getSystemService(NotificationManager::class.java)
        manger.notify(1,notify)
    }

    private fun stopCounter() {
        isRunning = false
        counter = 0
        counterState.value = counter
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()   // prevent memory leak
    }
    private fun createNotification(value: Int): Notification {

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Counter Running $value")
            .setContentText("Counter is running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)   // 🔥 Important
            .setAutoCancel(false)              // Foreground me generally false
            .build()
    }

    /*private fun createNotification(value: Int): Notification {
        val intent = Intent(this, CounterService::class.java).apply {
            action = "NOTIFICATION_CLICK"
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Counter Running $value")
            .setContentText("Counter is running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }*/

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Counter Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /*override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, CounterService::class.java)
        restartIntent.action = "START"
        ContextCompat.startForegroundService(applicationContext, restartIntent)
        super.onTaskRemoved(rootIntent)
    }*/

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isRunning) {
            startForeground(1, createNotification(counter))
        }
    }
}