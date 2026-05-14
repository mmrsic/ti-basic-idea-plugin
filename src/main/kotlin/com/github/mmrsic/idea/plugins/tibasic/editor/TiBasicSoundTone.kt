package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement

internal const val CALL_SOUND_SUBPROGRAM = "SOUND"

private const val SOUND_DURATION_ARG_INDEX = 0
private const val SOUND_TONE_FIRST_ARG_INDEX = 1
private const val SOUND_TONE_ARGUMENT_COUNT = 2
private const val SOUND_PITCH_ARG_OFFSET = 0
private const val SOUND_VOLUME_ARG_OFFSET = 1
private const val MAX_SOUND_TONE_CHANNEL_COUNT = 3
internal const val SOUND_TONE3_CHANNEL_INDEX = 2
private val VALID_SOUND_ARGUMENT_COUNTS = setOf(3, 5, 7, 9)
private const val MIN_SOUND_DURATION = 1
private const val MIN_SOUND_PITCH = 1
private const val MIN_SOUND_NOISE_SELECTOR = -8
private const val MAX_SOUND_NOISE_SELECTOR = -1
internal const val MAX_SOUND_VOLUME = 30

internal data class TiBasicSoundTone(
    val pitch: Int,
    val volume: Int,
)

internal enum class TiBasicNoiseType(
    val tooltipText: String,
) {
    PERIODIC("periodic"),
    WHITE("white"),
}

internal enum class TiBasicNoiseShiftRate(
    val tooltipText: String,
) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    TONE3("tone3"),
}

internal data class TiBasicSoundNoise(
    val selector: Int,
    val volume: Int,
    val tone3Pitch: Int? = null,
) {
    constructor(selector: Int, volume: Int) : this(selector, volume, null)

    val type: TiBasicNoiseType
        get() = when (selector) {
            -1, -2, -3, -4 -> TiBasicNoiseType.PERIODIC
            -5, -6, -7, -8 -> TiBasicNoiseType.WHITE
            else -> error("Unsupported noise selector: $selector")
        }

    val shiftRate: TiBasicNoiseShiftRate
        get() = when (selector) {
            -1, -5 -> TiBasicNoiseShiftRate.HIGH
            -2, -6 -> TiBasicNoiseShiftRate.MEDIUM
            -3, -7 -> TiBasicNoiseShiftRate.LOW
            -4, -8 -> TiBasicNoiseShiftRate.TONE3
            else -> error("Unsupported noise selector: $selector")
        }
}

internal data class TiBasicSoundPlayback(
    val duration: Int,
    val tones: List<TiBasicSoundTone>,
    val noise: TiBasicSoundNoise? = null,
)

internal fun resolveSoundPlayback(callStatement: TiBasicCallStatement, file: TiBasicFile?): TiBasicSoundPlayback? {
    if (callStatement.subprogramName() != CALL_SOUND_SUBPROGRAM) return null
    val arguments = callStatement.arguments()
    if (arguments.size !in VALID_SOUND_ARGUMENT_COUNTS) return null
    val duration = resolveConstantNumericValue(
        arguments.getOrNull(SOUND_DURATION_ARG_INDEX),
        file,
    ) ?: return null
    val channelArguments = arguments
        .drop(SOUND_TONE_FIRST_ARG_INDEX)
        .chunked(SOUND_TONE_ARGUMENT_COUNT)
    val tones = mutableListOf<TiBasicSoundTone>()
    var noise: TiBasicSoundNoise? = null
    channelArguments.forEach { channelArgument ->
        val pitch = resolveConstantNumericValue(
            channelArgument.getOrNull(SOUND_PITCH_ARG_OFFSET),
            file,
        ) ?: return null
        val volume = resolveConstantNumericValue(
            channelArgument.getOrNull(SOUND_VOLUME_ARG_OFFSET),
            file,
        ) ?: return null
        when {
            pitch >= MIN_SOUND_PITCH -> tones += TiBasicSoundTone(pitch, volume)
            pitch in MIN_SOUND_NOISE_SELECTOR..MAX_SOUND_NOISE_SELECTOR && noise == null -> {
                noise = TiBasicSoundNoise(pitch, volume)
            }

            else -> return null
        }
    }
    if (tones.size > MAX_SOUND_TONE_CHANNEL_COUNT) return null
    if (noise?.shiftRate == TiBasicNoiseShiftRate.TONE3) {
        noise = noise.copy(
            tone3Pitch = tones.getOrNull(SOUND_TONE3_CHANNEL_INDEX)?.pitch
                ?: resolvePreviousTone3Pitch(callStatement, file),
        )
    }
    return TiBasicSoundPlayback(duration, tones.toList(), noise)
        .takeIf(::isPlayableSoundPlayback)
}

internal fun isPlayableSoundTone(tone: TiBasicSoundTone): Boolean =
    tone.pitch >= MIN_SOUND_PITCH &&
            tone.volume in 0..MAX_SOUND_VOLUME

internal fun isPlayableSoundNoise(noise: TiBasicSoundNoise): Boolean =
    noise.selector in MIN_SOUND_NOISE_SELECTOR..MAX_SOUND_NOISE_SELECTOR &&
            noise.volume in 0..MAX_SOUND_VOLUME

internal fun isPlayableSoundPlayback(playback: TiBasicSoundPlayback): Boolean =
    playback.duration >= MIN_SOUND_DURATION &&
            (playback.tones.isNotEmpty() || playback.noise != null) &&
            playback.tones.all(::isPlayableSoundTone) &&
            (playback.noise == null || isPlayableSoundNoise(playback.noise))

internal fun callSoundTooltip(playback: TiBasicSoundPlayback): String =
    buildString {
        append("CALL SOUND: dur=${playback.duration}")
        playback.tones.forEachIndexed { index, tone ->
            append(", tone${index + 1}=pitch=${tone.pitch}/vol=${tone.volume}")
        }
        playback.noise?.let { noise ->
            append(", noise=selector=${noise.selector}/type=${noise.type.tooltipText}/rate=${noise.shiftRate.tooltipText}/vol=${noise.volume}")
        }
    }

private fun resolvePreviousTone3Pitch(callStatement: TiBasicCallStatement, file: TiBasicFile?): Int? {
    file ?: return null
    return file.callStatements()
        .asReversed()
        .asSequence()
        .filter { previousCall ->
            previousCall !== callStatement &&
                    previousCall.textRange.startOffset < callStatement.textRange.startOffset &&
                    previousCall.subprogramName() == CALL_SOUND_SUBPROGRAM
        }
        .map(::resolveExplicitTone3Pitch)
        .firstOrNull { result -> result != Tone3PitchResolution.UNCHANGED }
        ?.pitch
}

private fun resolveExplicitTone3Pitch(callStatement: TiBasicCallStatement): Tone3PitchResolution {
    val arguments = callStatement.arguments()
    if (arguments.size < FIRST_SOUND_THREE_TONE_ARGUMENT_COUNT) return Tone3PitchResolution.UNCHANGED
    val thirdChannelPitch = callStatement.containingFile
        .let { it as? TiBasicFile }
        ?.let { file ->
            resolveConstantNumericValue(
                arguments[THIRD_SOUND_CHANNEL_PITCH_ARG_INDEX],
                file,
            )
        }
        ?: return Tone3PitchResolution.UNKNOWN
    return if (thirdChannelPitch >= MIN_SOUND_PITCH) {
        Tone3PitchResolution(thirdChannelPitch)
    } else {
        Tone3PitchResolution.UNCHANGED
    }
}

private data class Tone3PitchResolution(val pitch: Int?) {
    companion object {
        val UNCHANGED = Tone3PitchResolution(Int.MIN_VALUE)
        val UNKNOWN = Tone3PitchResolution(null)
    }
}

private const val THIRD_SOUND_CHANNEL_PITCH_ARG_INDEX = 5
private const val FIRST_SOUND_THREE_TONE_ARGUMENT_COUNT = 7
