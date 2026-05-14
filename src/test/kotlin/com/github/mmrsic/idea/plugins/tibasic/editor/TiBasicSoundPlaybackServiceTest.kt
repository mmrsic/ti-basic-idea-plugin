package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import java.util.concurrent.Executor

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

    private class RecordingAudioOutput : TiBasicAudioOutput {
        var playedAudio: TiBasicPcmAudio? = null

        override fun play(audio: TiBasicPcmAudio) {
            playedAudio = audio
        }
    }
}
