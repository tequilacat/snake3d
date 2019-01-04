package tequilacat.org.snake3d.playfeature

import android.content.Context
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

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
}
