package tequilacat.org.snake3d.playfeature

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameGLRenderer(val gameScene: OGLGameScene) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
         gameScene.createLevel()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gameScene.initView(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
//        glClear(GL_COLOR_BUFFER_BIT);
        gameScene.draw()
    }
}
