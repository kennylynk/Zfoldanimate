package com.example.foldanim

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import kotlin.math.pow

class OverlayService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private var overlayContainer: ViewGroup? = null
    private var leftPanel: FrameLayout? = null
    private var rightPanel: FrameLayout? = null
    private var hingeShadow: FrameLayout? = null

    private var sensorManager: SensorManager? = null
    private var hingeSensor: Sensor? = null

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
            .setContentTitle("Z Fold Animation đang chạy")
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
        val density = resources.displayMetrics.density

        // Layout chính chứa 2 nửa màn hình (Trái / Phải)
        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt()) // Nền đen cơ bản
        }

        // Cấu hình cameraDistance để hiển thị hiệu ứng 3D không bị méo hình
        val cameraDist = 8000f * density

        // Nửa trái
        leftPanel = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { width = resources.displayMetrics.widthPixels / 2 }
            setBackgroundColor(0xFF111111.toInt()) // Màu nền test nửa trái
            pivotX = width.toFloat()
            pivotY = height.toFloat() / 2f
            cameraDistance = cameraDist
        }

        // Nửa phải
        rightPanel = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { 
                width = resources.displayMetrics.widthPixels / 2
                gravity = Gravity.END
            }
            setBackgroundColor(0xFF1a1a1a.toInt()) // Màu nền test nửa phải
            pivotX = 0f
            pivotY = height.toFloat() / 2f
            cameraDistance = cameraDist
        }

        // Dải bóng đổ nếp gấp ở giữa
        hingeShadow = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (60 * density).toInt(),
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setBackgroundColor(0x88000000.toInt())
        }

        container.addView(leftPanel)
        container.addView(rightPanel)
        container.addView(hingeShadow)

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

        windowManager.addView(container, params)
        overlayContainer = container
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Sửa lại cách gọi chuẩn phần cứng thay vì lọc chuỗi tên
        hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

        hingeSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val angle = event.values.getOrNull(0) ?: return
        val clampedAngle = angle.coerceIn(0f, 180f)
        
        val fraction = clampedAngle / 180f
        val leftRot = (180f - clampedAngle) / 2f
        val rightRot = -(180f - clampedAngle) / 2f
        val shadowAlpha = (1f - fraction).toDouble().pow(2).toFloat()

        // Áp dụng trực tiếp thông số góc gập lên View trong Service
        leftPanel?.rotationY = leftRot
        rightPanel?.rotationY = rightRot
        
        overlayContainer?.scaleX = 0.85f + (0.15f * fraction)
        overlayContainer?.scaleY = 0.85f + (0.15f * fraction)
        
        hingeShadow?.alpha = shadowAlpha
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        overlayContainer?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
    }
}
