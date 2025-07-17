package com.yodgorbek.jellyapp.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoMerger {

    suspend fun merge(context: Context, frontPath: String, backPath: String): File = withContext(Dispatchers.Default) {
        val outputFile = File(context.filesDir, "merged_output.mp4")
        val encoder = VideoEncoder(1920, 1080, outputFile)
        encoder.init()

        val eglHelper = EGLHelper(encoder.inputSurface)

        val frontTexId = createExternalTexture()
        val frontSurfaceTexture = SurfaceTexture(frontTexId)
        val frontSurface = Surface(frontSurfaceTexture)
        val frontDecoder = VideoDecoder(frontPath, frontSurface)
        frontDecoder.start()

        val backTexId = createExternalTexture()
        val backSurfaceTexture = SurfaceTexture(backTexId)
        val backSurface = Surface(backSurfaceTexture)
        val backDecoder = VideoDecoder(backPath, backSurface)
        backDecoder.start()

        try {
            while (frontDecoder.hasFrames() && backDecoder.hasFrames()) {
                frontSurfaceTexture.updateTexImage()
                backSurfaceTexture.updateTexImage()

                eglHelper.makeCurrent()

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
                VideoMergerRenderer.drawTextures(frontTexId, backTexId)

                eglHelper.swapBuffers()

                encoder.encodeFrame { /* No extra rendering needed here */ }
            }
        } finally {
            frontDecoder.release()
            backDecoder.release()
            encoder.finish()
            eglHelper.release()

            frontSurface.release()
            backSurface.release()
            frontSurfaceTexture.release()
            backSurfaceTexture.release()
        }

        return@withContext outputFile
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }
}
