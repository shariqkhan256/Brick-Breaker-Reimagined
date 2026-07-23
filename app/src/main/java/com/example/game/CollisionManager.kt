package com.example.game

import android.graphics.RectF
import com.example.audio.AudioManager
import com.example.save.SaveManager
import java.util.UUID
import kotlin.math.abs

class CollisionManager(
    private val audioManager: AudioManager,
    private val saveManager: SaveManager,
    private val particleSystem: ParticleSystem
) {

    fun checkWallCollisions(ball: Ball, width: Float, height: Float, shieldActive: Boolean, onBallLost: () -> Unit) {
        // Left & Right walls
        if (ball.x - ball.radius <= 0f) {
            ball.x = ball.radius
            ball.vx = -ball.vx
            audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
        } else if (ball.x + ball.radius >= width) {
            ball.x = width - ball.radius
            ball.vx = -ball.vx
            audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
        }

        // Top wall
        if (ball.y - ball.radius <= 0f) {
            ball.y = ball.radius
            ball.vy = -ball.vy
            audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
        }

        // Bottom wall
        if (ball.y + ball.radius >= height) {
            if (shieldActive) {
                // Bounce back up
                ball.y = height - ball.radius - 15f
                ball.vy = -abs(ball.vy)
                particleSystem.triggerShake(5, 5f)
                audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
            } else {
                onBallLost()
            }
        }
    }

    fun checkPaddleCollision(ball: Ball, paddle: Paddle) {
        // Simple AABB vs Circle intersection
        val halfW = paddle.width / 2f
        val paddleLeft = paddle.x - halfW
        val paddleRight = paddle.x + halfW
        val paddleTop = paddle.y
        val paddleBottom = paddle.y + paddle.height

        // Find closest point on paddle to ball
        val closestX = clamp(ball.x, paddleLeft, paddleRight)
        val closestY = clamp(ball.y, paddleTop, paddleBottom)

        val distX = ball.x - closestX
        val distY = ball.y - closestY
        val distanceSq = (distX * distX) + (distY * distY)

        if (distanceSq < ball.radius * ball.radius) {
            // Collision detected! Push ball out of paddle to prevent sticking
            ball.y = paddleTop - ball.radius
            
            // Variable bounce angle based on where it hit the paddle
            val hitPoint = (ball.x - paddle.x) / halfW // Between -1.0 (left edge) and 1.0 (right edge)
            val maxAngle = Math.toRadians(65.0) // 65 degree max bounce angle
            val angle = hitPoint * maxAngle

            // Compute speed
            val speed = kotlin.math.sqrt((ball.vx * ball.vx) + (ball.vy * ball.vy))
            
            ball.vx = (speed * kotlin.math.sin(angle)).toFloat()
            ball.vy = -(speed * kotlin.math.cos(angle)).toFloat()

            // Safe minimum vertical speed to avoid ball sliding completely horizontal
            if (abs(ball.vy) < 2f) {
                ball.vy = -3f
            }

            particleSystem.triggerShake(3, 4f)
            audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
        }
    }

    fun checkBrickCollisions(
        ball: Ball,
        bricks: MutableList<Brick>,
        onScoreGained: (Int, BrickType) -> Unit,
        onPowerUpSpawned: (PowerUpType, Float, Float) -> Unit,
        onTeleport: (Ball, Brick) -> Unit,
        onBrickDestroyed: (Brick) -> Unit
    ) {
        val iterator = bricks.iterator()
        while (iterator.hasNext()) {
            val brick = iterator.next()
            
            // Check collision with brick bounding box
            val closestX = clamp(ball.x, brick.x, brick.x + brick.width)
            val closestY = clamp(ball.y, brick.y, brick.y + brick.height)

            val distX = ball.x - closestX
            val distY = ball.y - closestY
            val distSq = (distX * distX) + (distY * distY)

            if (distSq < ball.radius * ball.radius) {
                // Collision! Calculate overlap side
                val overlapX = ball.radius - abs(distX)
                val overlapY = ball.radius - abs(distY)

                if (brick.type != BrickType.TELEPORT) {
                    if (overlapX < overlapY) {
                        // Collision on left/right side
                        ball.vx = if (distX > 0) abs(ball.vx) else -abs(ball.vx)
                        ball.x += if (distX > 0) overlapX else -overlapX
                    } else {
                        // Collision on top/bottom side
                        ball.vy = if (distY > 0) abs(ball.vy) else -abs(ball.vy)
                        ball.y += if (distY > 0) overlapY else -overlapY
                    }
                }

                // Handle brick damage
                if (brick.type != BrickType.METAL) {
                    val damage = if (ball.isBomb) brick.hp else 1
                    brick.hp -= damage
                    
                    if (brick.hp <= 0) {
                        brick.isDestroyed = true
                        brick.destroyProgress = 0f
                        onBrickDestroyed(brick)
                        
                        iterator.remove()
                        onScoreGained(100, brick.type)
                        saveManager.incrementBricksDestroyed()
                        particleSystem.addExplosion(
                            brick.x + brick.width / 2f,
                            brick.y + brick.height / 2f,
                            WorldTheme.valueOf(saveManager.getActiveTheme()).getBrickColors(brick.type, 1).first
                        )
                        audioManager.playSfx(AudioManager.SoundType.BRICK_BREAK)

                        // 15% drop rate for any brick, or 100% for Mystery bricks
                        if (brick.type == BrickType.MYSTERY) {
                            val randomPowerUp = PowerUpType.values().random()
                            onPowerUpSpawned(randomPowerUp, brick.x + brick.width / 2f, brick.y + brick.height / 2f)
                        } else if (Math.random() < 0.15) {
                            val randomPowerUp = PowerUpType.values().random()
                            onPowerUpSpawned(randomPowerUp, brick.x + brick.width / 2f, brick.y + brick.height / 2f)
                        }

                        // Special bricks
                        if (brick.type == BrickType.EXPLOSIVE) {
                            particleSystem.triggerShake(12, 16f)
                            audioManager.playSfx(AudioManager.SoundType.EXPLOSION)
                            // Destroy surrounding bricks
                            triggerExplosiveReaction(brick, bricks, onScoreGained, onPowerUpSpawned, onBrickDestroyed)
                        }
                    } else {
                        // Normal hit sfx
                        audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
                        particleSystem.addExplosion(closestX, closestY, WorldTheme.valueOf(saveManager.getActiveTheme()).getBrickColors(brick.type, brick.hp).first, 4)
                    }
                } else {
                    // Indestructible metal hit sfx
                    audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
                    particleSystem.triggerShake(4, 3f)
                }

                if (brick.type == BrickType.TELEPORT) {
                    onTeleport(ball, brick)
                }

                break // Collide with only one brick per update
            }
        }
    }

    private fun triggerExplosiveReaction(
        expBrick: Brick,
        bricks: MutableList<Brick>,
        onScoreGained: (Int, BrickType) -> Unit,
        onPowerUpSpawned: (PowerUpType, Float, Float) -> Unit,
        onBrickDestroyed: (Brick) -> Unit
    ) {
        val range = 200f // Detonation radius
        val centerX = expBrick.x + expBrick.width / 2f
        val centerY = expBrick.y + expBrick.height / 2f

        val affected = mutableListOf<Brick>()
        for (b in bricks) {
            val bCenterX = b.x + b.width / 2f
            val bCenterY = b.y + b.height / 2f
            val dist = kotlin.math.hypot(centerX - bCenterX, centerY - bCenterY)
            if (dist <= range && b.type != BrickType.METAL) {
                affected.add(b)
            }
        }

        for (b in affected) {
            b.isDestroyed = true
            b.destroyProgress = 0f
            onBrickDestroyed(b)
            
            bricks.remove(b)
            onScoreGained(100, b.type)
            saveManager.incrementBricksDestroyed()
            particleSystem.addExplosion(
                b.x + b.width / 2f,
                b.y + b.height / 2f,
                WorldTheme.valueOf(saveManager.getActiveTheme()).getBrickColors(b.type, 1).first
            )

            // 15% powerup spawn chance from explosion-hit bricks, or 100% for Mystery bricks
            if (b.type == BrickType.MYSTERY) {
                val randomPowerUp = PowerUpType.values().random()
                onPowerUpSpawned(randomPowerUp, b.x + b.width / 2f, b.y + b.height / 2f)
            } else if (Math.random() < 0.15) {
                val randomPowerUp = PowerUpType.values().random()
                onPowerUpSpawned(randomPowerUp, b.x + b.width / 2f, b.y + b.height / 2f)
            }
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return Math.max(min, Math.min(max, value))
    }
}
