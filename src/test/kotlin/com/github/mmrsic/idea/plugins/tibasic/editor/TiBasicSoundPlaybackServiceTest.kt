package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import java.util.concurrent.Executor

class TiBasicSoundPlaybackServiceTest : TiBasicTestBase() {

    fun `test render square wave audio uses mono 16-bit pcm format`() {
        val audio = renderSquareWaveAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 0))))
        assertEquals(44_100f.toInt(), audio.format.sampleRate.toInt())
        assertEquals(16, audio.format.sampleSizeInBits)
        assertEquals(1, audio.format.channels)
        assertTrue(audio.format.isBigEndian.not())
    }

    fun `test render square wave audio byte count matches duration`() {
        val audio = renderSquareWaveAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 0))))
        assertEquals(8_820, audio.sampleData.size)
    }

    fun `test render square wave audio is silent for volume 30`() {
        val audio = renderSquareWaveAudio(TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 30))))
        assertTrue("Volume 30 must render silence", audio.sampleData.all { it == 0.toByte() })
    }

    fun `test render square wave audio mixes multiple tones`() {
        val audio = renderSquareWaveAudio(
            TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(220, 0), TiBasicSoundTone(294, 0))),
        )
        assertEquals(8_820, audio.sampleData.size)
        assertTrue("Mixed audio must not be silent", audio.sampleData.any { it != 0.toByte() })
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
