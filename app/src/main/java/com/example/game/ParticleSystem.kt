package com.example.game

import android.graphics.Canvas
import android.graphics.Paint
import java.util.Random

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val random = Random()

    // Screen shake variables
    private var shakeDuration = 0
    private var shakeIntensity = 0f
    private var shakeX = 0f
    private var shakeY = 0f

    fun addExplosion(x: Float, y: Float, color: Int, count: Int = 15) {
        for (i in 0 until count) {
            val angle = random.nextDouble() * 2 * Math.PI
            val speed = 2f + random.nextFloat() * 6f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.sin(angle) * speed).toFloat()
            val radius = 3f + random.nextFloat() * 6f
            val decay = 4 + random.nextInt(8)
            
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    radius = radius,
                    color = color,
                    alpha = 255,
                    decay = decay
                )
            )
        }
    }

    fun addSparkle(x: Float, y: Float, color: Int) {
        val vx = -2f + random.nextFloat() * 4f
        val vy = -1f - random.nextFloat() * 4f
        val radius = 2f + random.nextFloat() * 3f
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                radius = radius,
                color = color,
                alpha = 255,
                decay = 10
            )
        )
    }

    fun triggerShake(duration: Int = 10, intensity: Float = 12f) {
        shakeDuration = duration
        shakeIntensity = intensity
    }

    fun update() {
        // Update screen shake
        if (shakeDuration > 0) {
            shakeX = (-shakeIntensity + random.nextFloat() * shakeIntensity * 2)
            shakeY = (-shakeIntensity + random.nextFloat() * shakeIntensity * 2)
            shakeDuration--
        } else {
            shakeX = 0f
            shakeY = 0f
        }

        // Update particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha -= p.decay
            if (p.alpha <= 0) {
                iterator.remove()
            }
        }
    }

    fun draw(canvas: Canvas) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
        }
        for (p in particles) {
            paint.color = p.color
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }

    fun getShakeX(): Float = shakeX
    fun getShakeY(): Float = shakeY

    fun clear() {
        particles.clear()
        shakeDuration = 0
        shakeX = 0f
        shakeY = 0f
    }
}
