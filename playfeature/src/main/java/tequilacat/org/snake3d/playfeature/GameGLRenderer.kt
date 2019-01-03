package tequilacat.org.snake3d.playfeature

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glViewport
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT

class GameGLRenderer : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height);
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT);
    }
}
