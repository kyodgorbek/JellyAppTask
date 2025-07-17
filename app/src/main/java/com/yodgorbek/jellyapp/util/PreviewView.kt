package com.yodgorbek.jellyapp.util

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import java.io.File

class Camera2PreviewView(
    context: Context,
    val cameraSelector: CameraSelector
) : TextureView(context) {

    val cameraId: String

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = getCameraId(manager, cameraSelector)
        startPreview(manager)
    }

    private fun getCameraId(manager: CameraManager, selector: CameraSelector): String {
        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if ((selector == CameraSelector.DEFAULT_FRONT_CAMERA && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (selector == CameraSelector.DEFAULT_BACK_CAMERA && facing == CameraCharacteristics.LENS_FACING_BACK)
            ) {
                return id
            }
        }
        throw IllegalArgumentException("Camera not found")
    }

    private fun startPreview(manager: CameraManager) {
        surfaceTextureListener = object : SurfaceTextureListener {
            @RequiresPermission(Manifest.permission.CAMERA)
            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val surface = Surface(texture)
                        val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        previewRequestBuilder.addTarget(surface)

                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, null)
                    }

                    override fun onDisconnected(camera: CameraDevice) {}
                    override fun onError(camera: CameraDevice, error: Int) {}
                }, null)
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
        }
    }
}

class Camera2Recorder(private val context: Context, private val cameraId: String) {

    private var mediaRecorder: MediaRecorder? = null
    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startRecording() {
        val mediaRecorder = MediaRecorder()
        val outputFile = File(context.getExternalFilesDir(null), "video_${System.currentTimeMillis()}.mp4")

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(outputFile.absolutePath)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setVideoEncodingBitRate(5_000_000)
        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setVideoSize(1280, 720)
        mediaRecorder.prepare()

        val surface = mediaRecorder.surface
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                requestBuilder.addTarget(surface)

                camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        this@Camera2Recorder.session = session
                        session.setRepeatingRequest(requestBuilder.build(), null, null)
                        mediaRecorder.start()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, null)

        this.mediaRecorder = mediaRecorder
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("Camera2Recorder", "Stop failed", e)
        }

        session?.close()
        cameraDevice?.close()

        session = null
        cameraDevice = null
        mediaRecorder = null
    }
}


