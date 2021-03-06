package tequilacat.org.snake3d.playfeature

import android.content.Context
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import tequilacat.org.snake3d.playfeature.oglgame.GameRenderer

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

        private val mRenderer: GameRenderer

        init {
            // Create an OpenGL ES 2.0 context
            setEGLContextClientVersion(2)

            mRenderer = GameRenderer(context)
            // Set the Renderer for drawing on the GLSurfaceView
            setRenderer(mRenderer)
            // renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }

        private var viewWidth = 0
        private var viewHeight = 0

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            viewWidth = w
            viewHeight = h
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            mRenderer.inputController.processTouchEvent(event!!)
            return true
        }
    }
}
