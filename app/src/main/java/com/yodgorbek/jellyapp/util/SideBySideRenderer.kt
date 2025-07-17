package com.yodgorbek.jellyapp.util

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface

class SideBySideRenderer(
    private val outputSurface: Surface,
    private val width: Int,
    private val height: Int
) {
    private lateinit var eglDisplay: EGLDisplay
    private lateinit var eglContext: EGLContext
    private lateinit var eglSurface: EGLSurface

    fun init() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, null, 0, null, 0)

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, configs.size, numConfigs, 0)
        val config = configs[0]!!

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, outputSurface, surfaceAttribs, 0)

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

        GLES20.glViewport(0, 0, width, height)
    }

    fun draw(leftTextureId: Int, rightTextureId: Int) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        drawTexture(leftTextureId, left = true)
        drawTexture(rightTextureId, left = false)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun drawTexture(textureId: Int, left: Boolean) {
        val scaleX = 0.5f
        val translateX = if (left) -0.5f else 0.5f

        GLES20.glUseProgram(SimpleShader.programId)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glUniform1f(SimpleShader.uScaleX, scaleX)
        GLES20.glUniform1f(SimpleShader.uTranslateX, translateX)

        GLES20.glEnableVertexAttribArray(SimpleShader.aPosition)
        GLES20.glEnableVertexAttribArray(SimpleShader.aTexCoord)

        GLES20.glVertexAttribPointer(SimpleShader.aPosition, 2, GLES20.GL_FLOAT, false, 0, SimpleShader.vertexBuffer)
        GLES20.glVertexAttribPointer(SimpleShader.aTexCoord, 2, GLES20.GL_FLOAT, false, 0, SimpleShader.texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(SimpleShader.aPosition)
        GLES20.glDisableVertexAttribArray(SimpleShader.aTexCoord)
    }

    fun release() {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }
}
