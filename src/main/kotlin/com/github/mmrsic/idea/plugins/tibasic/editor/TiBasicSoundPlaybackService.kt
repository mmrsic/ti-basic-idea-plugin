package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Executor
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import kotlin.math.round
import kotlin.math.roundToInt

private const val PCM_SAMPLE_RATE_HZ = 44_100f
private const val PCM_SAMPLE_SIZE_BITS = 16
private const val PCM_CHANNEL_COUNT = 1
private const val PCM_FRAME_SIZE_BYTES = 2
private const val MILLISECONDS_PER_SECOND = 1_000
private const val MAX_PCM_AMPLITUDE = 32_767
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
    fun play(audio: TiBasicPcmAudio)
}

internal class TiBasicJavaSoundAudioOutput : TiBasicAudioOutput {

    override fun play(audio: TiBasicPcmAudio) {
        val line = AudioSystem.getSourceDataLine(audio.format)
        line.open(audio.format)
        line.start()
        try {
            line.write(audio.sampleData, 0, audio.sampleData.size)
            line.drain()
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

    fun playSound(project: Project?, playback: TiBasicSoundPlayback) {
        executor.execute {
            try {
                playSoundNow(playback)
            } catch (exception: Exception) {
                reportPlaybackFailure(project, exception)
            }
        }
    }

    internal fun playSoundNow(playback: TiBasicSoundPlayback) {
        audioOutput.play(renderSquareWaveAudio(playback))
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

internal fun renderSquareWaveAudio(playback: TiBasicSoundPlayback): TiBasicPcmAudio {
    val sampleCount = ((playback.duration.toDouble() * PCM_SAMPLE_RATE_HZ) / MILLISECONDS_PER_SECOND)
        .roundToInt()
        .coerceAtLeast(1)
    val audioBytes = ByteArray(sampleCount * PCM_FRAME_SIZE_BYTES)
    val renderableTones = playback.tones
        .map { tone ->
            RenderableSoundTone(
                amplitude = (((MAX_SOUND_VOLUME - tone.volume).toDouble() / MAX_SOUND_VOLUME) * MAX_PCM_AMPLITUDE)
                    .roundToInt(),
                phaseIncrement = tone.pitch / PCM_SAMPLE_RATE_HZ.toDouble(),
            )
        }
    val audibleToneCount = renderableTones.count { it.amplitude > 0 }

    repeat(sampleCount) { index ->
        val mixedSample = renderableTones.sumOf { tone ->
            when {
                tone.amplitude == 0 -> 0
                tone.phase < 0.5 -> tone.amplitude
                else -> -tone.amplitude
            }
        }
        val sample = if (audibleToneCount == 0) {
            0
        } else {
            round(mixedSample.toDouble() / audibleToneCount)
                .toInt()
                .coerceIn(-MAX_PCM_AMPLITUDE, MAX_PCM_AMPLITUDE)
        }
        val sampleOffset = index * PCM_FRAME_SIZE_BYTES
        audioBytes[sampleOffset] = (sample and 0xFF).toByte()
        audioBytes[sampleOffset + 1] = ((sample shr 8) and 0xFF).toByte()
        renderableTones.forEach { tone ->
            tone.phase = (tone.phase + tone.phaseIncrement) % 1.0
        }
    }

    return TiBasicPcmAudio(
        format = AudioFormat(PCM_SAMPLE_RATE_HZ, PCM_SAMPLE_SIZE_BITS, PCM_CHANNEL_COUNT, true, false),
        sampleData = audioBytes,
    )
}

private data class RenderableSoundTone(
    val amplitude: Int,
    val phaseIncrement: Double,
    var phase: Double = 0.0,
)

internal val tiBasicSoundPlaybackService = TiBasicSoundPlaybackService()

private val logger = Logger.getInstance(TiBasicSoundPlaybackService::class.java)
