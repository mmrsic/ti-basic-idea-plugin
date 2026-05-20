package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TiBasicSoundPlaybackServiceTest : TiBasicTestBase() {

    fun `test render sound audio uses mono 16-bit pcm format`() {
        val audio = renderSoundAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 0))))
        assertEquals(44_100f.toInt(), audio.format.sampleRate.toInt())
        assertEquals(16, audio.format.sampleSizeInBits)
        assertEquals(1, audio.format.channels)
        assertTrue(audio.format.isBigEndian.not())
    }

    fun `test render sound audio byte count matches duration`() {
        val audio = renderSoundAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 0))))
        assertEquals(8_820, audio.sampleData.size)
    }

    fun `test render sound audio byte count matches absolute duration for negative values`() {
        val audio = renderSoundAudio(TiBasicSoundPlayback(duration = -100, tones = listOf(TiBasicSoundTone(440, 0))))
        assertEquals(8_820, audio.sampleData.size)
    }

    fun `test render sound audio is silent for volume 30`() {
        val audio = renderSoundAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 30))))
        assertTrue("Volume 30 must render silence", audio.sampleData.all { it == 0.toByte() })
    }

    fun `test render sound audio mixes multiple tones`() {
        val audio = renderSoundAudio(
            TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(220, 0), TiBasicSoundTone(294, 0))),
        )
        assertEquals(8_820, audio.sampleData.size)
        assertTrue("Mixed audio must not be silent", audio.sampleData.any { it != 0.toByte() })
    }

    fun `test render sound audio includes noise channel`() {
        val audio = renderSoundAudio(
            TiBasicSoundPlayback(
                100,
                listOf(TiBasicSoundTone(220, 30), TiBasicSoundTone(294, 30), TiBasicSoundTone(330, 30)),
                TiBasicSoundNoise(-8, 0),
            ),
        )
        assertEquals(8_820, audio.sampleData.size)
        assertTrue("Noise channel must not render silence", audio.sampleData.any { it != 0.toByte() })
    }

    fun `test noise clock frequencies match TMS9919 divider rates`() {
        assertEquals(6_991.0, noiseClockFrequency(TiBasicNoiseShiftRate.HIGH, null))
        assertEquals(3_496.0, noiseClockFrequency(TiBasicNoiseShiftRate.MEDIUM, null))
        assertEquals(1_748.0, noiseClockFrequency(TiBasicNoiseShiftRate.LOW, null))
    }

    fun `test tone3 noise rate uses tone3 pitch when available`() {
        assertEquals(220.0, noiseClockFrequency(TiBasicNoiseShiftRate.TONE3, 440))
    }

    fun `test playback service writes rendered audio to output`() {
        val output = RecordingAudioOutput()
        val service = TiBasicSoundPlaybackService(output, Executor(Runnable::run))
        service.playSoundNow(TiBasicSoundPlayback(50, listOf(TiBasicSoundTone(440, 0), TiBasicSoundTone(494, 0))))

        val audio = output.playedAudio
        assertNotNull("Playback service must forward generated audio to the output adapter", audio)
        assertEquals(4_410, audio?.sampleData?.size)
    }

    fun `test new click interrupts running negative duration playback but keeps queued sounds`() {
        val firstPlayback = TiBasicSoundPlayback(120, listOf(TiBasicSoundTone(220, 0)))
        val interruptiblePlayback = TiBasicSoundPlayback(-140, listOf(TiBasicSoundTone(330, 0)))
        val queuedPlayback = TiBasicSoundPlayback(160, listOf(TiBasicSoundTone(440, 0)))
        val replacingPlayback = TiBasicSoundPlayback(180, listOf(TiBasicSoundTone(550, 0)))
        val output = ControlledAudioOutput()
        val service = TiBasicSoundPlaybackService(output, asyncExecutor())

        service.playSound(null, firstPlayback)
        output.awaitStarted(firstPlayback)
        service.playSound(null, interruptiblePlayback)
        service.playSound(null, queuedPlayback)

        output.release(firstPlayback)
        output.awaitStarted(interruptiblePlayback)
        service.playSound(null, replacingPlayback)

        output.awaitCompleted(interruptiblePlayback)
        output.awaitStarted(replacingPlayback)
        output.release(replacingPlayback)
        output.awaitCompleted(replacingPlayback)
        output.awaitStarted(queuedPlayback)
        output.release(queuedPlayback)
        output.awaitCompleted(queuedPlayback)

        assertEquals(
            listOf(
                audioByteCount(firstPlayback),
                audioByteCount(interruptiblePlayback),
                audioByteCount(replacingPlayback),
                audioByteCount(queuedPlayback),
            ),
            output.startedPlaybackSizes,
        )
        assertEquals(listOf(audioByteCount(interruptiblePlayback)), output.interruptedPlaybackSizes)
    }

    fun `test running positive duration playback is not interrupted by new click`() {
        val runningPlayback = TiBasicSoundPlayback(120, listOf(TiBasicSoundTone(220, 0)))
        val queuedPlayback = TiBasicSoundPlayback(-140, listOf(TiBasicSoundTone(330, 0)))
        val output = ControlledAudioOutput()
        val service = TiBasicSoundPlaybackService(output, asyncExecutor())

        service.playSound(null, runningPlayback)
        output.awaitStarted(runningPlayback)
        service.playSound(null, queuedPlayback)

        assertFalse(
            "Positive-duration playback must continue until it completes",
            output.hasStartedPlayback(queuedPlayback),
        )

        output.release(runningPlayback)
        output.awaitStarted(queuedPlayback)
        output.release(queuedPlayback)
        output.awaitCompleted(queuedPlayback)
    }

    private class RecordingAudioOutput : TiBasicAudioOutput {
        var playedAudio: TiBasicPcmAudio? = null

        override fun play(audio: TiBasicPcmAudio, playbackInterrupter: TiBasicPlaybackInterrupter) {
            playedAudio = audio
        }
    }

    private class ControlledAudioOutput : TiBasicAudioOutput {
        val startedPlaybackSizes = mutableListOf<Int>()
        val interruptedPlaybackSizes = mutableListOf<Int>()

        private val releaseFlags = ConcurrentHashMap<Int, AtomicBoolean>()
        private val startedLatches = ConcurrentHashMap<Int, CountDownLatch>()
        private val completedLatches = ConcurrentHashMap<Int, CountDownLatch>()

        override fun play(audio: TiBasicPcmAudio, playbackInterrupter: TiBasicPlaybackInterrupter) {
            val byteCount = audio.sampleData.size
            synchronized(startedPlaybackSizes) {
                startedPlaybackSizes += byteCount
            }
            startedLatches.getOrPut(byteCount) { CountDownLatch(1) }.countDown()
            while (!releaseFlags.getOrPut(byteCount) { AtomicBoolean(false) }.get() && !playbackInterrupter.isInterrupted()) {
                Thread.sleep(10)
            }
            if (playbackInterrupter.isInterrupted()) {
                synchronized(interruptedPlaybackSizes) {
                    interruptedPlaybackSizes += byteCount
                }
            }
            completedLatches.getOrPut(byteCount) { CountDownLatch(1) }.countDown()
        }

        fun awaitStarted(playback: TiBasicSoundPlayback) {
            assertTrue(
                "Playback ${playback.duration} must start",
                startedLatches.getOrPut(audioByteCount(playback)) { CountDownLatch(1) }.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
        }

        fun awaitCompleted(playback: TiBasicSoundPlayback) {
            assertTrue(
                "Playback ${playback.duration} must complete",
                completedLatches.getOrPut(audioByteCount(playback)) { CountDownLatch(1) }.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
        }

        fun release(playback: TiBasicSoundPlayback) {
            releaseFlags.getOrPut(audioByteCount(playback)) { AtomicBoolean(false) }.set(true)
        }

        fun hasStartedPlayback(playback: TiBasicSoundPlayback): Boolean =
            synchronized(startedPlaybackSizes) {
                startedPlaybackSizes.contains(audioByteCount(playback))
            }
    }

    private companion object {
        const val TEST_TIMEOUT_SECONDS = 5L

        fun asyncExecutor(): Executor =
            Executor { runnable ->
                Thread(runnable, "TiBasicSoundPlaybackServiceTest")
                    .apply { isDaemon = true }
                    .start()
            }

        fun audioByteCount(playback: TiBasicSoundPlayback): Int = renderSoundAudio(playback).sampleData.size
    }
}
