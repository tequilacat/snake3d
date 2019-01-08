package tequilacat.org.snake3d.playfeature

import android.content.Context
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import tequilacat.org.snake3d.playfeature.oglgame.OGLGameScene
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OGLGameActivity : AppCompatActivity() {

    private lateinit var glView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        glView = GameOGLSurfaceView(this)
        setContentView(glView)
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    class GameOGLSurfaceView(context: Context) : GLSurfaceView(context) {

        private val mRenderer: GameGLRenderer

        init {
            // Create an OpenGL ES 2.0 context
            setEGLContextClientVersion(2)

            mRenderer = GameGLRenderer(OGLGameScene(Game()))
            // Set the Renderer for drawing on the GLSurfaceView
            setRenderer(mRenderer)
            // renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    class GameGLRenderer(val gameScene: OGLGameScene) : GLSurfaceView.Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            gameScene.initInstance()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            gameScene.initView(width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            val stt = System.nanoTime()
            gameScene.draw()
            val time = (System.nanoTime() - stt)/ 1_000_000.0
            Log.d("render", "onDrawFrame: $time ms")
        }
    }

}
