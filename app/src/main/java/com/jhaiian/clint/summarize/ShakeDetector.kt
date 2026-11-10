package com.jhaiian.clint.summarize

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Fires [onShake] on a single firm shake: a gravity spike above
 * [THRESHOLD_GRAVITY] between consecutive accelerometer samples, rate-limited
 * by a per-spike slop and a cooldown between triggers (similar to the
 * shake-to-report gestures in Firefox for Android).
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var accelX = 0f
    private var accelY = 0f
    private var accelZ = 0f
    private var lastSpikeAt = 0L
    private var lastFireAt = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val deltaX = x - accelX
        val deltaY = y - accelY
        val deltaZ = z - accelZ
        accelX = x
        accelY = y
        accelZ = z
        val gX = deltaX / SensorManager.GRAVITY_EARTH
        val gY = deltaY / SensorManager.GRAVITY_EARTH
        val gZ = deltaZ / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        if (gForce < THRESHOLD_GRAVITY) return
        val now = System.currentTimeMillis()
        if (now - lastSpikeAt < SPIKE_SLOP_MS) return // same gesture
        lastSpikeAt = now
        if (now - lastFireAt < COOLDOWN_MS) return
        lastFireAt = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun start(context: Context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop(context: Context) {
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(this)
    }

    companion object {
        private const val THRESHOLD_GRAVITY = 2.7
        private const val SPIKE_SLOP_MS = 400L
        private const val COOLDOWN_MS = 1500L
    }
}
