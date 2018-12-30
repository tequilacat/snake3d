package tequilacat.org.snake3d.playfeature

import android.content.Context
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView

class DirectionControlActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var gameView : GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direction_contrrol)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gameView = GameView(this)
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME);

    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
//            textView.text = "${event.values[0]} \n" +
//                    "${event.values[1]}\n" +
//                    "${event.values[2]}"

            gameView!!.game.updatePositionalControls(event.values[0] , event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    class DrawThread (surfaceHolder: SurfaceHolder, panel : GameView) : Thread() {
        private var surfaceHolder :SurfaceHolder ?= null
        private var gameView : GameView ?= null
        private var run = false

        init {
            this.surfaceHolder = surfaceHolder
            this.gameView = panel
        }

        fun setRunning(run : Boolean){
            this.run = run
        }

        private val TARGET_FPS = 50

        override fun run() {
            var c: Canvas?
            var startTime: Long
            //var timeMillis: Long
            val targetTime = (1000 / TARGET_FPS).toLong()

            while (run){
                startTime = System.nanoTime()
                c = null

                try {
                    c = surfaceHolder!!.lockCanvas(null)
                    synchronized(surfaceHolder!!){
//                        val stt = System.nanoTime()
                        gameView!!.game.tick()
                        gameView!!.draw(c)
//                        val end = (System.nanoTime() - stt) / 1_000_000 // in ms
//                        Log.d("perf", "tick and paint ${end} ms")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (c!= null){
                        try {
                            surfaceHolder!!.unlockCanvasAndPost(c)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val timeMillis = (System.nanoTime() - startTime) / 1_000_000
                val waitTime = targetTime - timeMillis

                if(waitTime > 0) {
                    try {
                        sleep(waitTime)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }
}

class GameView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback {
    private var thread: DirectionControlActivity.DrawThread
    var game: Game = Game()
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    init {
        // focusable = true - min level > 15
        holder.addCallback(this)
        //create a thread
        thread = DirectionControlActivity.DrawThread(holder, this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        var retry = true
        while (retry) {
            try {
                thread.setRunning(false)
                thread.join()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            retry = false
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        game.continueGame()

        thread!!.setRunning(true)
        thread!!.start()
    }

    override fun draw(canvas: Canvas?) {
        //val t0 = System.nanoTime()

        super.draw(canvas)

        if (canvas != null){
            game.drawGameScreen(canvas, viewWidth, viewHeight)
        }

//        val paintTime = (System.nanoTime() - t0)/1000000 // in ms
//        Log.d("perf", "paint time: $paintTime ms")
    }

    override public fun onDraw(canvas: Canvas?) {
        if (canvas != null){
            game.drawGameScreen(canvas, viewWidth, viewHeight)
        }
    }
}
