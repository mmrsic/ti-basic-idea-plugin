package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.*
import java.util.concurrent.Executor
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.math.roundToInt

private const val PCM_SAMPLE_RATE_HZ = 44_100f
private const val PCM_SAMPLE_SIZE_BITS = 16
private const val PCM_CHANNEL_COUNT = 1
private const val PCM_FRAME_SIZE_BYTES = 2
private const val PCM_WRITE_CHUNK_SIZE_BYTES = 4_096
private const val MILLISECONDS_PER_SECOND = 1_000
private const val MAX_PCM_AMPLITUDE = 32_767
private const val NOISE_SHIFT_REGISTER_BITS = 15
private const val NOISE_SHIFT_REGISTER_MASK = (1 shl NOISE_SHIFT_REGISTER_BITS) - 1
private const val INITIAL_NOISE_SHIFT_REGISTER = 1 shl (NOISE_SHIFT_REGISTER_BITS - 1)
private const val LOW_NOISE_CLOCK_HZ = 1_748.0
private const val MEDIUM_NOISE_CLOCK_HZ = 3_496.0
private const val HIGH_NOISE_CLOCK_HZ = 6_991.0
private const val TONE3_LINKED_NOISE_CLOCK_DIVISOR = 2.0
private const val NOISE_TONE3_FALLBACK_CLOCK_HZ = HIGH_NOISE_CLOCK_HZ
private const val SOUND_PLAYBACK_EXECUTOR_NAME = "TI-Basic SOUND playback"
private const val SOUND_PLAYBACK_NOTIFICATION_GROUP = "TI-Basic"
private const val SOUND_PLAYBACK_FAILURE_TITLE = "CALL SOUND playback failed"

internal data class TiBasicPcmAudio(
    val format: AudioFormat,
    val sampleData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TiBasicPcmAudio

        if (format != other.format) return false
        if (!sampleData.contentEquals(other.sampleData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + sampleData.contentHashCode()
        return result
    }
}

internal interface TiBasicAudioOutput {
    @Throws(LineUnavailableException::class, IllegalArgumentException::class)
    fun play(
        audio: TiBasicPcmAudio,
        playbackInterrupter: TiBasicPlaybackInterrupter = uninterruptedSoundPlayback,
    )
}

internal class TiBasicJavaSoundAudioOutput : TiBasicAudioOutput {

    override fun play(audio: TiBasicPcmAudio, playbackInterrupter: TiBasicPlaybackInterrupter) {
        val line = AudioSystem.getSourceDataLine(audio.format)
        line.open(audio.format)
        line.start()
        try {
            var offset = 0
            while (offset < audio.sampleData.size && !playbackInterrupter.isInterrupted()) {
                val chunkSize = minOf(PCM_WRITE_CHUNK_SIZE_BYTES, audio.sampleData.size - offset)
                offset += line.write(audio.sampleData, offset, chunkSize)
            }
            if (offset >= audio.sampleData.size && !playbackInterrupter.isInterrupted()) {
                line.drain()
            } else {
                line.flush()
            }
        } finally {
            line.stop()
            line.close()
        }
    }
}

internal class TiBasicSoundPlaybackService(
    private val audioOutput: TiBasicAudioOutput = TiBasicJavaSoundAudioOutput(),
    private val executor: Executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
        SOUND_PLAYBACK_EXECUTOR_NAME,
        1,
    ),
) {
    private val playbackMonitor = Any()
    private val queuedPlaybacks = ArrayDeque<TiBasicSoundPlaybackRequest>()
    private var playbackWorkerScheduled = false
    private var activePlayback: ActiveSoundPlayback? = null

    fun playSound(project: Project?, playback: TiBasicSoundPlayback) {
        val request = TiBasicSoundPlaybackRequest(project, playback)
        var startPlaybackWorker = false
        synchronized(playbackMonitor) {
            val runningPlayback = activePlayback
            if (runningPlayback?.isInterruptible == true) {
                runningPlayback.interrupt()
                queuedPlaybacks.addFirst(request)
            } else {
                queuedPlaybacks.addLast(request)
            }
            if (!playbackWorkerScheduled) {
                playbackWorkerScheduled = true
                startPlaybackWorker = true
            }
        }
        if (startPlaybackWorker) {
            executor.execute(::drainPlaybackQueue)
        }
    }

    internal fun playSoundNow(playback: TiBasicSoundPlayback) {
        playSoundNow(playback, uninterruptedSoundPlayback)
    }

    private fun playSoundNow(playback: TiBasicSoundPlayback, playbackInterrupter: TiBasicPlaybackInterrupter) {
        audioOutput.play(renderSoundAudio(playback), playbackInterrupter)
    }

    private fun drainPlaybackQueue() {
        while (true) {
            val request = synchronized(playbackMonitor) {
                val nextPlayback = queuedPlaybacks.pollFirst()
                if (nextPlayback == null) {
                    playbackWorkerScheduled = false
                    activePlayback = null
                    return
                }
                val runningPlayback = ActiveSoundPlayback(nextPlayback.playback)
                activePlayback = runningPlayback
                nextPlayback to runningPlayback
            }
            try {
                playSoundNow(request.first.playback, request.second.interrupter)
            } catch (exception: Exception) {
                reportPlaybackFailure(request.first.project, exception)
            } finally {
                synchronized(playbackMonitor) {
                    if (activePlayback === request.second) {
                        activePlayback = null
                    }
                }
            }
        }
    }

    private fun reportPlaybackFailure(project: Project?, exception: Exception) {
        logger.warn(SOUND_PLAYBACK_FAILURE_TITLE, exception)
        Notifications.Bus.notify(
            Notification(
                SOUND_PLAYBACK_NOTIFICATION_GROUP,
                SOUND_PLAYBACK_FAILURE_TITLE,
                exception.toString(),
                NotificationType.ERROR,
            ),
            project,
        )
    }
}

internal fun interface TiBasicPlaybackInterrupter {
    fun isInterrupted(): Boolean
}

private data class TiBasicSoundPlaybackRequest(
    val project: Project?,
    val playback: TiBasicSoundPlayback,
)

private class ActiveSoundPlayback(playback: TiBasicSoundPlayback) {
    private val interrupted = java.util.concurrent.atomic.AtomicBoolean(false)

    val interrupter = TiBasicPlaybackInterrupter { interrupted.get() }
    val isInterruptible = playback.duration < 0

    fun interrupt() {
        interrupted.set(true)
    }
}

private val uninterruptedSoundPlayback = TiBasicPlaybackInterrupter { false }

internal fun renderSoundAudio(playback: TiBasicSoundPlayback): TiBasicPcmAudio {
    val sampleCount = ((playback.duration.toDouble().absoluteValue * PCM_SAMPLE_RATE_HZ) / MILLISECONDS_PER_SECOND)
        .roundToInt()
        .coerceAtLeast(1)
    val audioBytes = ByteArray(sampleCount * PCM_FRAME_SIZE_BYTES)
    val renderableTones = playback.tones
        .map { tone ->
            RenderableToneChannel(
                amplitude = resolveAmplitude(tone.volume),
                pitch = tone.pitch,
                phaseIncrement = tone.pitch / PCM_SAMPLE_RATE_HZ.toDouble(),
            )
        }
    val renderableNoise = playback.noise?.let { noise ->
        RenderableNoiseChannel(
            amplitude = resolveAmplitude(noise.volume),
            type = noise.type,
            shiftRate = noise.shiftRate,
            tone3Pitch = noise.tone3Pitch ?: playback.tones.getOrNull(SOUND_TONE3_CHANNEL_INDEX)?.pitch,
        )
    }
    val audibleChannelCount = renderableTones.count { it.amplitude > 0 } + listOfNotNull(renderableNoise)
        .count { it.amplitude > 0 }

    repeat(sampleCount) { index ->
        val mixedSample = renderableTones.sumOf(RenderableToneChannel::currentSample) +
                (renderableNoise?.currentSample() ?: 0)
        val sample = if (audibleChannelCount == 0) {
            0
        } else {
            round(mixedSample.toDouble() / audibleChannelCount)
                .toInt()
                .coerceIn(-MAX_PCM_AMPLITUDE, MAX_PCM_AMPLITUDE)
        }
        val sampleOffset = index * PCM_FRAME_SIZE_BYTES
        audioBytes[sampleOffset] = (sample and 0xFF).toByte()
        audioBytes[sampleOffset + 1] = ((sample shr 8) and 0xFF).toByte()
        renderableTones.forEach(RenderableToneChannel::advance)
        renderableNoise?.advance()
    }

    return TiBasicPcmAudio(
        format = AudioFormat(PCM_SAMPLE_RATE_HZ, PCM_SAMPLE_SIZE_BITS, PCM_CHANNEL_COUNT, true, false),
        sampleData = audioBytes,
    )
}

private fun resolveAmplitude(volume: Int): Int =
    (((MAX_SOUND_VOLUME - volume).toDouble() / MAX_SOUND_VOLUME) * MAX_PCM_AMPLITUDE)
        .roundToInt()

private data class RenderableToneChannel(
    val amplitude: Int,
    val pitch: Int,
    val phaseIncrement: Double,
    var phase: Double = 0.0,
) {
    fun currentSample(): Int =
        when {
            amplitude == 0 -> 0
            phase < 0.5 -> amplitude
            else -> -amplitude
        }

    fun advance() {
        phase = (phase + phaseIncrement) % 1.0
    }
}

private data class RenderableNoiseChannel(
    val amplitude: Int,
    val type: TiBasicNoiseType,
    val shiftRate: TiBasicNoiseShiftRate,
    val tone3Pitch: Int?,
    var phase: Double = 0.0,
    var shiftRegister: Int = INITIAL_NOISE_SHIFT_REGISTER,
) {
    fun currentSample(): Int =
        when {
            amplitude == 0 -> 0
            (shiftRegister and 1) == 0 -> -amplitude
            else -> amplitude
        }

    fun advance() {
        phase += noiseClockFrequency(shiftRate, tone3Pitch) / PCM_SAMPLE_RATE_HZ
        while (phase >= 1.0) {
            phase -= 1.0
            tick()
        }
    }

    private fun tick() {
        val feedbackBit = when (type) {
            TiBasicNoiseType.PERIODIC -> shiftRegister and 1
            TiBasicNoiseType.WHITE -> (shiftRegister and 1) xor ((shiftRegister shr 1) and 1)
        }
        shiftRegister = ((shiftRegister shr 1) or (feedbackBit shl (NOISE_SHIFT_REGISTER_BITS - 1))) and
                NOISE_SHIFT_REGISTER_MASK
        if (shiftRegister == 0) {
            shiftRegister = INITIAL_NOISE_SHIFT_REGISTER
        }
    }
}

internal fun noiseClockFrequency(shiftRate: TiBasicNoiseShiftRate, tone3Pitch: Int?): Double =
    when (shiftRate) {
        TiBasicNoiseShiftRate.HIGH -> HIGH_NOISE_CLOCK_HZ
        TiBasicNoiseShiftRate.MEDIUM -> MEDIUM_NOISE_CLOCK_HZ
        TiBasicNoiseShiftRate.LOW -> LOW_NOISE_CLOCK_HZ
        TiBasicNoiseShiftRate.TONE3 -> tone3Pitch
            ?.toDouble()
            ?.div(TONE3_LINKED_NOISE_CLOCK_DIVISOR)
            ?: NOISE_TONE3_FALLBACK_CLOCK_HZ
    }

internal val tiBasicSoundPlaybackService = TiBasicSoundPlaybackService()

private val logger = Logger.getInstance(TiBasicSoundPlaybackService::class.java)
