package com.yodgorbek.jellyapp.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import java.io.IOException

class VideoDecoder(
    private val videoPath: String,
    private val outputSurface: Surface
) {
    private lateinit var extractor: MediaExtractor
    private lateinit var decoder: MediaCodec
    private var inputDone = false
    private var outputDone = false

    fun start() {
        extractor = MediaExtractor()
        extractor.setDataSource(videoPath)

        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                videoTrackIndex = i
                break
            }
        }

        if (videoTrackIndex == -1) throw RuntimeException("No video track found in $videoPath")

        extractor.selectTrack(videoTrackIndex)
        val format = extractor.getTrackFormat(videoTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IOException("MIME type not found")

        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, outputSurface, null, 0)
        decoder.start()
    }

    fun hasFrames(): Boolean {
        if (outputDone) return false

        val inputBufferId = decoder.dequeueInputBuffer(10000)
        if (inputBufferId >= 0 && !inputDone) {
            val inputBuffer = decoder.getInputBuffer(inputBufferId) ?: return true
            val sampleSize = extractor.readSampleData(inputBuffer, 0)

            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputDone = true
            } else {
                val presentationTimeUs = extractor.sampleTime
                decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                extractor.advance()
            }
        }

        val bufferInfo = MediaCodec.BufferInfo()
        val outputBufferId = decoder.dequeueOutputBuffer(bufferInfo, 10000)
        if (outputBufferId >= 0) {
            decoder.releaseOutputBuffer(outputBufferId, true)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                outputDone = true
            }
            return true
        }

        return !outputDone
    }

    fun release() {
        try {
            decoder.stop()
            decoder.release()
            extractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
