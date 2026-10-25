package com.sxanalys.music.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.core.content.getSystemService
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Skips to the next track after three distinct hard shakes. The accelerometer is only
 * registered while the feature is enabled and audio is actually playing.
 */
class ShakeSkipController(
    context: Context,
    private val scope: CoroutineScope,
    private val enabled: Flow<Boolean>,
    private val onShake: () -> Unit,
) : SensorEventListener,
    Player.Listener {
    private val sensorManager = context.getSystemService<SensorManager>()
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isEnabled = false
    private var isPlaying = false
    private var registered = false
    private var attachedPlayer: Player? = null
    private var job: Job? = null

    private var spikeCount = 0
    private var lastSpikeAt = 0L
    private var lastTriggerAt = 0L

    fun start() {
        if (accelerometer == null) return
        job =
            scope.launch {
                enabled.distinctUntilChanged().collect {
                    isEnabled = it
                    refresh()
                }
            }
    }

    fun attach(player: Player) {
        attachedPlayer?.removeListener(this)
        attachedPlayer = player
        player.addListener(this)
        isPlaying = player.isPlaying
        refresh()
    }

    fun stop() {
        job?.cancel()
        attachedPlayer?.removeListener(this)
        attachedPlayer = null
        unregister()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        refresh()
    }

    private fun refresh() {
        if (isEnabled && isPlaying) register() else unregister()
    }

    private fun register() {
        if (registered || accelerometer == null) return
        registered = sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI) == true
        spikeCount = 0
    }

    private fun unregister() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val delta = abs(sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH)
        if (delta < SPIKE_THRESHOLD) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAt < COOLDOWN_MS) return
        if (now - lastSpikeAt < MIN_SPIKE_GAP_MS) return

        spikeCount = if (now - lastSpikeAt > WINDOW_MS) 1 else spikeCount + 1
        lastSpikeAt = now
        if (spikeCount >= REQUIRED_SPIKES) {
            spikeCount = 0
            lastTriggerAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SPIKE_THRESHOLD = 12f
        const val MIN_SPIKE_GAP_MS = 150L
        const val WINDOW_MS = 900L
        const val COOLDOWN_MS = 1500L
        const val REQUIRED_SPIKES = 3
    }
}
