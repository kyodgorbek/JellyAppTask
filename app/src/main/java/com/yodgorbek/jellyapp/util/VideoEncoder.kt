package com.yodgorbek.jellyapp.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import java.io.File

class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val outputFile: File
) {
    enum class State {
        UNINITIALIZED,
        INITIALIZED,
        STARTED,
        FINISHED,
        RELEASED
    }

    private lateinit var encoder: MediaCodec
    internal lateinit var inputSurface: Surface
    private lateinit var muxer: MediaMuxer
    private var trackIndex = -1
    private var muxerStarted = false
    @Volatile
    private var state: State = State.UNINITIALIZED
    private val lock = Any()

    val isReleased: Boolean
        get() = state == State.RELEASED

    fun init() {
        synchronized(lock) {
            if (state != State.UNINITIALIZED) {
                Log.w("VideoEncoder", "init() called in invalid state: $state")
                return
            }

            val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 5_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType("video/avc")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            state = State.STARTED
        }
    }

    fun encodeFrame(renderFrame: (Surface) -> Unit) {
        synchronized(lock) {
            if (state != State.STARTED) {
                Log.w("VideoEncoder", "encodeFrame() called in invalid state: $state, skipping.")
                return
            }

            renderFrame(inputSurface)
            drainEncoder()
        }
    }

    private fun drainEncoder(endOfStream: Boolean = false) {
        synchronized(lock) {
            if (state != State.STARTED && state != State.FINISHED) return

            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val outputBufferId = try {
                    encoder.dequeueOutputBuffer(bufferInfo, 0)
                } catch (e: IllegalStateException) {
                    Log.e("VideoEncoder", "dequeueOutputBuffer failed: MediaCodec likely released", e)
                    state = State.RELEASED
                    return
                } catch (e: Exception) {
                    Log.e("VideoEncoder", "Unexpected error during dequeueOutputBuffer", e)
                    state = State.RELEASED
                    return
                }

                when (outputBufferId) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) break
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) {
                            Log.e("VideoEncoder", "Output format changed again, ignoring")
                            return
                        }
                        try {
                            Log.i("VideoEncoder", "Output format changed; starting muxer")
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        } catch (e: Exception) {
                            Log.e("VideoEncoder", "Error starting muxer", e)
                            state = State.RELEASED
                            return
                        }
                    }
                    else -> {
                        if (outputBufferId < 0) {
                            Log.w("VideoEncoder", "Unexpected outputBufferId: $outputBufferId")
                            continue
                        }
                        val encodedData = try {
                            encoder.getOutputBuffer(outputBufferId)
                        } catch (e: Exception) {
                            Log.e("VideoEncoder", "Error getting output buffer", e)
                            state = State.RELEASED
                            return
                        } ?: return

                        if (bufferInfo.size != 0 && muxerStarted) {
                            try {
                                Log.i("VideoEncoder", "Writing sample data, size=${bufferInfo.size}, pts=${bufferInfo.presentationTimeUs}")
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            } catch (e: Exception) {
                                Log.e("VideoEncoder", "Error writing sample data", e)
                                state = State.RELEASED
                                return
                            }
                        }

                        try {
                            encoder.releaseOutputBuffer(outputBufferId, false)
                        } catch (e: Exception) {
                            Log.e("VideoEncoder", "Error releasing output buffer", e)
                            state = State.RELEASED
                            return
                        }

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }
    }

    fun finish() {
        synchronized(lock) {
            if (state == State.RELEASED) {
                Log.w("VideoEncoder", "finish() called after already released")
                return
            }

            if (state != State.STARTED && state != State.FINISHED) {
                Log.w("VideoEncoder", "finish() called in invalid state: $state")
                return
            }

            state = State.FINISHED

            try {
                encoder.signalEndOfInputStream()
            } catch (e: IllegalStateException) {
                Log.e("VideoEncoder", "signalEndOfInputStream failed, codec likely already released", e)
            } catch (e: Exception) {
                Log.e("VideoEncoder", "Unexpected error signaling end of input stream", e)
            }

            drainEncoder(endOfStream = true)

            try {
                encoder.stop()
            } catch (e: Exception) {
                Log.e("VideoEncoder", "Error stopping encoder", e)
            }

            try {
                encoder.release()
            } catch (e: Exception) {
                Log.e("VideoEncoder", "Error releasing encoder", e)
            }

            try {
                if (muxerStarted) {
                    muxer.stop()
                }
            } catch (e: Exception) {
                Log.e("VideoEncoder", "Error stopping muxer", e)
            }

            try {
                muxer.release()
            } catch (e: Exception) {
                Log.e("VideoEncoder", "Error releasing muxer", e)
            }

            state = State.RELEASED
        }
    }
}
