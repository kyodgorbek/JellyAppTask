package com.yodgorbek.jellyapp.util

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object SimpleShader {
    val vertexShader = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform float uScaleX;
        uniform float uTranslateX;
        void main() {
            vec4 scaled = aPosition;
            scaled.x = aPosition.x * uScaleX + uTranslateX;
            gl_Position = scaled;
            vTexCoord = aTexCoord;
        }
    """

    val fragmentShader = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTexCoord);
        }
    """

    val vertexBuffer: FloatBuffer = createFloatBuffer(
        floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f,  1f,
            1f,  1f
        )
    )

    val texCoordBuffer: FloatBuffer = createFloatBuffer(
        floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    )

    var programId: Int = -1
    var aPosition: Int = -1
    var aTexCoord: Int = -1
    var uScaleX: Int = -1
    var uTranslateX: Int = -1

    fun init() {
        programId = createProgram(vertexShader, fragmentShader)
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uScaleX = GLES20.glGetUniformLocation(programId, "uScaleX")
        uTranslateX = GLES20.glGetUniformLocation(programId, "uTranslateX")
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            throw RuntimeException("Could not link program: ${GLES20.glGetProgramInfoLog(program)}")
        }
        return program
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            throw RuntimeException("Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
        }
        return shader
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(array.size * 4).order(ByteOrder.nativeOrder())
        return buffer.asFloatBuffer().apply {
            put(array)
            position(0)
        }
    }
}
