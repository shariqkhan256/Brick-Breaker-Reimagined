package com.example.game

import android.graphics.Color
import com.example.audio.AudioManager
import kotlin.math.abs

class PowerUpManager(
    private val audioManager: AudioManager,
    private val particleSystem: ParticleSystem
) {
    /**
     * Spawns additional balls for a multi-ball power-up, ensuring they are correctly
     * initialized with offset velocities so they diverge and interact properly in the physics loop.
     */
    fun spawnMultiBall(activeBalls: MutableList<Ball>) {
        if (activeBalls.isEmpty()) return

        val newBalls = mutableListOf<Ball>()
        for (baseBall in activeBalls) {
            // Spawn 2 extra balls with varied angles/velocities relative to the original ball
            val extraBall1 = Ball(
                x = baseBall.x,
                y = baseBall.y,
                vx = baseBall.vx + 3f,
                vy = -abs(baseBall.vy),
                radius = baseBall.radius,
                isBomb = baseBall.isBomb,
                color = baseBall.color
            )
            val extraBall2 = Ball(
                x = baseBall.x,
                y = baseBall.y,
                vx = baseBall.vx - 3f,
                vy = -abs(baseBall.vy),
                radius = baseBall.radius,
                isBomb = baseBall.isBomb,
                color = baseBall.color
            )
            newBalls.add(extraBall1)
            newBalls.add(extraBall2)

            // Trigger a burst of sparks/explosion at the original ball position
            particleSystem.addExplosion(baseBall.x, baseBall.y, baseBall.color, 12)
        }
        activeBalls.addAll(newBalls)
        audioManager.playSfx(AudioManager.SoundType.POWERUP)
    }

    /**
     * Applies the power-up of the specified type to the game state.
     */
    fun applyPowerUp(
        type: PowerUpType,
        activeBalls: MutableList<Ball>,
        paddle: Paddle,
        onAddLife: () -> Unit,
        onSetLaser: (Boolean, Float) -> Unit,
        onSetShield: (Boolean, Float) -> Unit
    ) {
        when (type) {
            PowerUpType.MULTI_BALL -> {
                spawnMultiBall(activeBalls)
            }
            PowerUpType.BIGGER_PADDLE -> {
                paddle.width = Math.min(450f, paddle.width + 80f)
            }
            PowerUpType.SMALLER_PADDLE -> {
                paddle.width = Math.max(120f, paddle.width - 60f)
            }
            PowerUpType.EXTRA_LIFE -> {
                onAddLife()
            }
            PowerUpType.LASER_PADDLE -> {
                paddle.isLaser = true
                onSetLaser(true, 8f)
            }
            PowerUpType.SLOW_BALL -> {
                for (b in activeBalls) {
                    b.vx *= 0.7f
                    b.vy *= 0.7f
                }
            }
            PowerUpType.FAST_BALL -> {
                for (b in activeBalls) {
                    b.vx *= 1.3f
                    b.vy *= 1.3f
                }
            }
            PowerUpType.SHIELD -> {
                onSetShield(true, 10f)
            }
            PowerUpType.BOMB_BALL -> {
                for (b in activeBalls) {
                    b.isBomb = true
                    b.color = Color.parseColor("#FF5722")
                }
            }
        }
    }
}
