package com.yodgorbek.jellyapp.presentation.camera

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yodgorbek.jellyapp.data.repository.CameraRepository
import com.yodgorbek.jellyapp.domain.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraViewModel(
    private val appContext: Application,
    private val cameraRepo: CameraRepository
) : AndroidViewModel(appContext) {

    private val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var frontCamera: CameraDevice? = null
    private var backCamera: CameraDevice? = null
    private var frontSession: CameraCaptureSession? = null
    private var backSession: CameraCaptureSession? = null

    private var frontRecorder: MediaRecorder? = null
    private var backRecorder: MediaRecorder? = null

    private var frontSurface: Surface? = null
    private var backSurface: Surface? = null

    private var frontVideoUri: Uri? = null
    private var backVideoUri: Uri? = null

    val isRecording = MutableStateFlow(false)
    val elapsedSeconds = MutableStateFlow(0)
    val isFrontCameraReady = MutableStateFlow(false)
    val isBackCameraReady = MutableStateFlow(false)

    private val handlerThread = HandlerThread("CameraThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    @RequiresPermission(Manifest.permission.CAMERA)
    fun initCameras(context: Context, frontHolder: SurfaceHolder, backHolder: SurfaceHolder) {
        manager.cameraIdList.forEach { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    openCamera(id, frontHolder.surface, isFrontCameraReady) { device, surface ->
                        frontCamera = device
                        frontSurface = surface
                    }
                }
                CameraCharacteristics.LENS_FACING_BACK -> {
                    openCamera(id, backHolder.surface, isBackCameraReady) { device, surface ->
                        backCamera = device
                        backSurface = surface
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCamera(
        id: String,
        previewSurface: Surface,
        readinessFlag: MutableStateFlow<Boolean>,
        onOpened: (CameraDevice, Surface) -> Unit
    ) {
        manager.openCamera(id, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                onOpened(camera, previewSurface)
                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(previewSurface)
                }
                camera.createCaptureSession(
                    listOf(previewSurface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.setRepeatingRequest(request.build(), null, handler)
                            readinessFlag.value = true
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e("CameraViewModel", "Failed to configure camera session.")
                        }
                    },
                    handler
                )
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("CameraViewModel", "Camera error: $error")
            }
        }, handler)
    }

    fun startRecording(context: Context, onNavigateToGallery: () -> Unit = {}) {
        if (isRecording.value) return

        isRecording.value = true
        viewModelScope.launch {
            startFrontRecording(context)
            startBackRecording(context)

            for (i in 1..15) {
                elapsedSeconds.value = i
                delay(1000)
            }

            stopFront()
            stopBack()
            isRecording.value = false
            elapsedSeconds.value = 0

            if (frontVideoUri != null && backVideoUri != null) {
                val mergeResult = cameraRepo.mergeDualCameraVideos(frontVideoUri!!, backVideoUri!!, context)

                if (mergeResult.isSuccess) {
                    val mergedFilePath = mergeResult.getOrThrow()
                    val file = File(mergedFilePath)
                    val uploadResult = cameraRepo.recordDualCameraVideo(file)

                    if (uploadResult.isSuccess) {
                        Toast.makeText(context, "✅ Uploaded to Supabase", Toast.LENGTH_LONG).show()
                        onNavigateToGallery()
                    } else {
                        Toast.makeText(context, "❌ Upload failed: ${uploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "❌ Merge failed: ${mergeResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun prepareMediaRecorder(context: Context, filename: String, includeAudio: Boolean): Triple<MediaRecorder, Surface, Uri>? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/DuoCam")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null

        resolver.openFileDescriptor(uri, "w")?.use { pfd ->
            val recorder = MediaRecorder().apply {
                if (includeAudio) setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(pfd.fileDescriptor)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (includeAudio) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(10_000_000)
                setVideoFrameRate(30)
                setVideoSize(1920, 1080)
                prepare()
            }
            return@withContext Triple(recorder, recorder.surface, uri)
        }

        return@withContext null
    }

    private suspend fun startFrontRecording(context: Context) {
        val result = prepareMediaRecorder(context, "front_${System.currentTimeMillis()}.mp4", true) ?: return
        val (recorder, surface, uri) = result
        frontRecorder = recorder
        frontVideoUri = uri

        frontCamera?.createCaptureSession(listOfNotNull(frontSurface, surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                frontSession = session
                val request = frontCamera!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(frontSurface!!)
                    addTarget(surface)
                }
                session.setRepeatingRequest(request.build(), null, handler)
                recorder.start()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, handler)
    }

    private suspend fun startBackRecording(context: Context) {
        val result = prepareMediaRecorder(context, "back_${System.currentTimeMillis()}.mp4", false) ?: return
        val (recorder, surface, uri) = result
        backRecorder = recorder
        backVideoUri = uri

        backCamera?.createCaptureSession(listOfNotNull(backSurface, surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                backSession = session
                val request = backCamera!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(backSurface!!)
                    addTarget(surface)
                }
                session.setRepeatingRequest(request.build(), null, handler)
                recorder.start()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, handler)
    }

    private fun stopFront() {
        try {
            frontRecorder?.stop()
        } catch (_: Exception) {}
        frontRecorder?.release()
        frontRecorder = null
        frontSession?.close()
        frontSession = null
    }

    private fun stopBack() {
        try {
            backRecorder?.stop()
        } catch (_: Exception) {}
        backRecorder?.release()
        backRecorder = null
        backSession?.close()
        backSession = null
    }
}
