package com.example.foldanim

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.NotificationCompat

class OverlayService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var sensorManager: SensorManager? = null
    private var hingeSensor: Sensor? = null

    private var lastTriggerAngle: Float? = null
    private var isAnimating = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        setupOverlay()
        setupSensor()
    }

    private fun startForegroundWithNotification() {
        val channelId = "fold_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Fold Transition", NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fold Transition đang chạy")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val view = View(this)
        view.setBackgroundColor(0xFF000000.toInt())
        view.alpha = 0f

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        windowManager.addView(view, params)
        overlayView = view
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        hingeSensor = sensorManager?.getSensorList(Sensor.TYPE_ALL)
            ?.firstOrNull { it.name.contains("Hinge", ignoreCase = true) }

        hingeSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val angle = event.values.getOrNull(0) ?: return
        val last = lastTriggerAngle
        if (last == null) {
            lastTriggerAngle = angle
            return
        }
        if (kotlin.math.abs(angle - last) >= 15f && !isAnimating) {
            lastTriggerAngle = angle
            playPulse()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun playPulse() {
        val view = overlayView ?: return
        isAnimating = true

        val animator = ValueAnimator.ofFloat(0f, 0.55f, 0f)
        animator.duration = 550
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { anim ->
            view.alpha = anim.animatedValue as Float
        }
        animator.start()
        view.postDelayed({ isAnimating = false }, 600)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
    }
}
