package com.example.redmibuttonreplacement

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.graphics.PixelFormat
import androidx.appcompat.app.AppCompatActivity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MainActivity : AppCompatActivity() {

    private var lastY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupFaceDownLock()
        setupEdgeVolumeControl()
    }

    private fun setupFaceDownLock() {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val z = event.values[2]
                if (z < -9) { // face-down threshold
                    val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    if (dpm.isAdminActive(ComponentName(this@MainActivity, MyDeviceAdminReceiver::class.java))) {
                        dpm.lockNow()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun setupEdgeVolumeControl() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            100, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val edgeView = View(this)
        edgeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> lastY = event.y
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - lastY
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    audioManager.adjustVolume(
                        if (deltaY > 0) AudioManager.ADJUST_LOWER else AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    lastY = event.y
                }
            }
            true
        }

        windowManager.addView(edgeView, params)
    }
}