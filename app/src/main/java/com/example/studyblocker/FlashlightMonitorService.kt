package com.example.studyblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast

class FlashlightMonitorService : Service() {

    private lateinit var cameraManager: CameraManager
    private val handler = Handler(Looper.getMainLooper())
    private var isTorchOn = false
    private val REQUIRED_DURATION_MS = 10 * 60 * 1000L

    private val unlockRunnable = Runnable {
        if (isTorchOn) {
            LockManager.unlockDevice(applicationContext)
            Toast.makeText(applicationContext, "히든 모드: 10분 플래시 감지로 잠금이 해제되었습니다.", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isTorchOn = enabled
            if (enabled) {
                handler.postDelayed(unlockRunnable, REQUIRED_DURATION_MS)
            } else {
                handler.removeCallbacks(unlockRunnable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.registerTorchCallback(torchCallback, handler)
    }

    private fun startForegroundNotification() {
        val channelId = "FlashMonitorChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "보안 모니터링", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("보안 정책 활성화 중")
                .setContentText("네트워크가 차단되었습니다.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("보안 정책 활성화 중")
                .setContentText("네트워크가 차단되었습니다.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }
        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterTorchCallback(torchCallback)
        handler.removeCallbacks(unlockRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
