package com.example.game

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameLoop(
    private val lockCanvas: () -> Canvas?,
    private val unlockCanvasAndPost: (Canvas) -> Unit,
    private val onUpdate: (fps: Float) -> Unit,
    private val onDraw: (canvas: Canvas) -> Unit
) : Thread() {

    @Volatile
    var running = false
    private val targetFps = 60
    private val targetTime = 1000L / targetFps

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        var totalTime = 0L
        var frameCount = 0
        var fps = 60f

        while (running) {
            startTime = System.currentTimeMillis()
            var canvas: Canvas? = null

            try {
                canvas = lockCanvas()
                if (canvas != null) {
                    synchronized(this) {
                        onUpdate(fps)
                        onDraw(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            timeMillis = System.currentTimeMillis() - startTime
            waitTime = targetTime - timeMillis

            if (waitTime > 0) {
                try {
                    sleep(waitTime)
                } catch (ignored: Exception) {}
            }

            totalTime += System.currentTimeMillis() - startTime
            frameCount++

            if (frameCount == targetFps) {
                fps = (1000f / (totalTime / frameCount.toFloat()))
                frameCount = 0
                totalTime = 0L
            }
        }
    }
}
