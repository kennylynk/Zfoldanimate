package com.example.foldanim

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var shaderView: FoldShaderView
    private var sensorManager: SensorManager? = null
    private var hingeSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shaderView = FoldShaderView(this)
        findViewById<FrameLayout>(R.id.rootContainer).addView(shaderView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        hingeSensor = sensorManager?.getSensorList(Sensor.TYPE_ALL)
            ?.firstOrNull { it.name.contains("Hinge", ignoreCase = true) }
    }

    override fun onResume() {
        super.onResume()
        hingeSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val angle = event.values.getOrNull(0) ?: return
        val progress = (angle / 180f).coerceIn(0f, 1f)
        shaderView.setProgress(progress)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
