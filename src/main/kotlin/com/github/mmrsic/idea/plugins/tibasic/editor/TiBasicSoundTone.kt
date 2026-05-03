package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement

internal const val CALL_SOUND_SUBPROGRAM = "SOUND"

private const val SOUND_DURATION_ARG_INDEX = 0
private const val SOUND_TONE_FIRST_ARG_INDEX = 1
private const val SOUND_TONE_ARGUMENT_COUNT = 2
private const val SOUND_PITCH_ARG_OFFSET = 0
private const val SOUND_VOLUME_ARG_OFFSET = 1
private val VALID_SOUND_ARGUMENT_COUNTS = setOf(3, 5, 7, 9)
private const val MIN_SOUND_DURATION = 1
private const val MIN_SOUND_PITCH = 1
internal const val MAX_SOUND_VOLUME = 30

internal data class TiBasicSoundTone(
    val pitch: Int,
    val volume: Int,
)

internal data class TiBasicSoundPlayback(
    val duration: Int,
    val tones: List<TiBasicSoundTone>,
)

internal fun resolveSoundPlayback(callStatement: TiBasicCallStatement, file: TiBasicFile?): TiBasicSoundPlayback? {
    if (callStatement.subprogramName() != CALL_SOUND_SUBPROGRAM) return null
    val arguments = callStatement.arguments()
    if (arguments.size !in VALID_SOUND_ARGUMENT_COUNTS) return null
    val duration = resolveConstantNumericValue(
        arguments.getOrNull(SOUND_DURATION_ARG_INDEX),
        file,
    ) ?: return null
    val tones = arguments
        .drop(SOUND_TONE_FIRST_ARG_INDEX)
        .chunked(SOUND_TONE_ARGUMENT_COUNT)
        .map { toneArguments ->
            val pitch = resolveConstantNumericValue(
                toneArguments.getOrNull(SOUND_PITCH_ARG_OFFSET),
                file,
            ) ?: return null
            val volume = resolveConstantNumericValue(
                toneArguments.getOrNull(SOUND_VOLUME_ARG_OFFSET),
                file,
            ) ?: return null
            TiBasicSoundTone(pitch, volume)
        }
    return TiBasicSoundPlayback(duration, tones)
        .takeIf(::isPlayableSoundPlayback)
}

internal fun isPlayableSoundTone(tone: TiBasicSoundTone): Boolean =
    tone.pitch >= MIN_SOUND_PITCH &&
        tone.volume in 0..MAX_SOUND_VOLUME

internal fun isPlayableSoundPlayback(playback: TiBasicSoundPlayback): Boolean =
    playback.duration >= MIN_SOUND_DURATION &&
        playback.tones.isNotEmpty() &&
        playback.tones.all(::isPlayableSoundTone)

internal fun callSoundTooltip(playback: TiBasicSoundPlayback): String =
    buildString {
        append("CALL SOUND: dur=${playback.duration}")
        playback.tones.forEachIndexed { index, tone ->
            append(", tone${index + 1}=pitch=${tone.pitch}/vol=${tone.volume}")
        }
    }
