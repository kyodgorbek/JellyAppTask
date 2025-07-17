package com.yodgorbek.jellyapp.util

import android.opengl.GLES20

object VideoMergerRenderer {
    private const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    fun drawTextures(frontTexId: Int, backTexId: Int) {
        SimpleShader.init()
        GLES20.glUseProgram(SimpleShader.programId)

        // Set vertex attributes
        GLES20.glEnableVertexAttribArray(SimpleShader.aPosition)
        GLES20.glVertexAttribPointer(SimpleShader.aPosition, 2, GLES20.GL_FLOAT, false, 0, SimpleShader.vertexBuffer)

        GLES20.glEnableVertexAttribArray(SimpleShader.aTexCoord)
        GLES20.glVertexAttribPointer(SimpleShader.aTexCoord, 2, GLES20.GL_FLOAT, false, 0, SimpleShader.texCoordBuffer)

        // Draw left side (front camera)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, frontTexId)
        GLES20.glUniform1f(SimpleShader.uScaleX, 0.5f)
        GLES20.glUniform1f(SimpleShader.uTranslateX, -0.5f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Draw right side (back camera)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, backTexId)
        GLES20.glUniform1f(SimpleShader.uScaleX, 0.5f)
        GLES20.glUniform1f(SimpleShader.uTranslateX, 0.5f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup
        GLES20.glDisableVertexAttribArray(SimpleShader.aPosition)
        GLES20.glDisableVertexAttribArray(SimpleShader.aTexCoord)
    }
}
