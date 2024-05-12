package cn.yiiguxing.plugin.translate.tts.sound

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Decode the audio stream, convert it to PCM format.
 */
fun AudioInputStream.decode(): AudioInputStream {
    val sourceFormat = format
    var targetSampleSizeInBits = sourceFormat.sampleSizeInBits
    if (targetSampleSizeInBits <= 0) {
        targetSampleSizeInBits = 16
    }
    if ((sourceFormat.encoding === AudioFormat.Encoding.ULAW) ||
        (sourceFormat.encoding === AudioFormat.Encoding.ALAW)
    ) {
        targetSampleSizeInBits = 16
    }
    if (targetSampleSizeInBits != 8) {
        targetSampleSizeInBits = 16
    }
    val targetFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        sourceFormat.sampleRate,
        targetSampleSizeInBits,
        sourceFormat.channels,
        sourceFormat.channels * (targetSampleSizeInBits / 8),
        sourceFormat.sampleRate,
        false
    )

    return AudioSystem.getAudioInputStream(targetFormat, this)
}