package com.example.game

import android.content.Context
import android.graphics.Color
import com.example.audio.AudioManager
import com.example.save.SaveManager
import java.util.UUID
import kotlin.math.abs

class GameEngine(
    private val context: Context,
    val saveManager: SaveManager,
    private val audioManager: AudioManager,
    val particleSystem: ParticleSystem
) {

    enum class GameState {
        READY,
        PLAYING,
        PAUSED,
        VICTORY,
        GAME_OVER
    }

    enum class GameMode {
        CLASSIC,
        SURVIVAL,
        TIME_ATTACK,
        CHAOS,
        ZEN
    }

    var state = GameState.READY
    private var stateBeforePause = GameState.READY
    var mode = GameMode.CLASSIC
    var level = 1
    var worldTheme = WorldTheme.FOREST

    // Engine variables
    var screenWidth = 1080f
    var screenHeight = 1920f
    var lastInitWidth = 0f
    var lastInitHeight = 0f

    val activeBalls = mutableListOf<Ball>()
    val activePowerUps = mutableListOf<PowerUp>()
    val activeProjectiles = mutableListOf<BossProjectile>()
    val activeLasers = mutableListOf<LaserBeam>()
    val bricks = mutableListOf<Brick>()
    val fadingBricks = mutableListOf<Brick>()
    val stateLock = Any()
    var boss: Boss? = null
    var paddle = Paddle(540f, 1500f, 240f, 40f)

    // Game stats
    var score = 0
    var combo = 0
    var comboTimer = 0f
    var lives = 3
    var maxLives = 3
    var coinsEarned = 0
    var xpEarned = 0
    var starsEarned = 0

    // Time-based variables
    var timeElapsed = 0f
    var timeRemaining = 60f // For Time Attack
    var totalBricksInitially = 0

    // Power-up indicators
    var shieldActive = false
    var shieldDuration = 0f
    var laserPaddleDuration = 0f
    var laserShootTimer = 0f

    private val levelManager = LevelManager()
    private val collisionManager = CollisionManager(audioManager, saveManager, particleSystem)
    private val powerUpManager = PowerUpManager(audioManager, particleSystem)

    fun initGame(level: Int, mode: GameMode) {
        this.level = level
        this.mode = mode
        this.worldTheme = levelManager.getWorldThemeForLevel(level)

        // Record dimensions used for initialization
        lastInitWidth = screenWidth
        lastInitHeight = screenHeight

        // Reset and reposition paddle relative to actual screen size
        paddle.width = 240f
        paddle.height = 40f
        paddle.x = screenWidth / 2f
        paddle.y = screenHeight * 0.82f

        // Clear previous entities
        activeBalls.clear()
        activePowerUps.clear()
        activeProjectiles.clear()
        activeLasers.clear()
        bricks.clear()
        fadingBricks.clear()
        boss = null
        particleSystem.clear()

        score = 0
        combo = 0
        comboTimer = 0f
        coinsEarned = 0
        xpEarned = 0
        starsEarned = 0
        timeElapsed = 0f
        shieldActive = false
        shieldDuration = 0f
        laserPaddleDuration = 0f

        // Game mode differences
        when (mode) {
            GameMode.CLASSIC -> {
                lives = 3
                maxLives = 3
            }
            GameMode.SURVIVAL -> {
                lives = 1
                maxLives = 1
            }
            GameMode.TIME_ATTACK -> {
                lives = 99
                maxLives = 99
                timeRemaining = 60f // 60 seconds limit
            }
            GameMode.CHAOS -> {
                lives = 5
                maxLives = 5
            }
            GameMode.ZEN -> {
                lives = 9999
                maxLives = 9999
            }
        }

        // Initialize Paddle skin colors
        val paddleSkin = saveManager.getActivePaddleSkin()
        paddle.color = when (paddleSkin) {
            "Neon Blue" -> Color.parseColor("#00E5FF")
            "Cyber Pink" -> Color.parseColor("#FF007F")
            "Gold Elite" -> Color.parseColor("#FFD700")
            "Volcano Red" -> Color.parseColor("#FF3D00")
            "Emerald Guardian" -> Color.parseColor("#00E676")
            else -> Color.parseColor("#00E676") // Default green
        }

        // Generate Level Bricks
        val generatedBricks = levelManager.generateLevel(level, screenWidth, screenHeight)
        bricks.addAll(generatedBricks)
        totalBricksInitially = bricks.count { it.type != BrickType.METAL }

        // Spawn Boss every 10 levels
        if (level % 10 == 0) {
            val bossHp = 20 + (level / 10) * 15
            boss = Boss(
                x = screenWidth / 2f - 150f,
                y = screenHeight * 0.18f,
                width = 300f,
                height = 100f,
                vx = 3f + (level / 15f),
                vy = 0f,
                hp = bossHp,
                maxHp = bossHp,
                name = levelManager.getBossNameForLevel(level),
                type = level / 10
            )
        }

        // Initialize Ball skin color and speeds
        val ballSkin = saveManager.getActiveBallSkin()
        val ballColor = when (ballSkin) {
            "Plasma" -> Color.parseColor("#FF007F")
            "Comet Glow" -> Color.parseColor("#00E5FF")
            "Fireball" -> Color.parseColor("#FF3D00")
            "Disco" -> Color.parseColor("#E040FB")
            else -> Color.WHITE
        }

        // Add main Ball
        val startVx = if (Math.random() > 0.5) 6f else -6f
        val startVy = -11f - (level / 20f) // Scale speed slowly with levels
        activeBalls.add(
            Ball(
                x = screenWidth / 2f,
                y = paddle.y - 40f,
                vx = startVx,
                vy = startVy,
                radius = 20f,
                color = ballColor
            )
        )

        state = GameState.READY
    }

    fun startGame() {
        if (state == GameState.READY) {
            state = GameState.PLAYING
            saveManager.incrementGamesPlayed()
        }
    }

    fun pauseGame() {
        if (state == GameState.PLAYING || state == GameState.READY) {
            stateBeforePause = state
            state = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (state == GameState.PAUSED) {
            state = stateBeforePause
        }
    }

    fun update(fps: Float) {
        synchronized(stateLock) {
            if (state != GameState.PLAYING) return

            val dt = 1f / if (fps > 10f) fps else 60f
            val timeScale = dt * 60f
            timeElapsed += dt

            if (mode == GameMode.TIME_ATTACK) {
                timeRemaining -= dt
                if (timeRemaining <= 0f) {
                    timeRemaining = 0f
                    endGame(victory = false)
                    return
                }
            }

            // Update active timers
            if (comboTimer > 0f) {
                comboTimer -= dt
                if (comboTimer <= 0f) {
                    combo = 0
                }
            }

            if (shieldActive) {
                shieldDuration -= dt
                if (shieldDuration <= 0f) {
                    shieldActive = false
                }
            }

            if (laserPaddleDuration > 0f) {
                laserPaddleDuration -= dt
                laserShootTimer += dt
                if (laserShootTimer >= 0.4f) { // Fire lasers every 0.4s
                    laserShootTimer = 0f
                    val leftL = LaserBeam(paddle.x - paddle.width / 2f + 10f, paddle.y)
                    val rightL = LaserBeam(paddle.x + paddle.width / 2f - 10f, paddle.y)
                    activeLasers.add(leftL)
                    activeLasers.add(rightL)
                    audioManager.playSfx(AudioManager.SoundType.LASER)
                }
                if (laserPaddleDuration <= 0f) {
                    paddle.isLaser = false
                }
            }

            // 1. Update ball positions and physics
            val ballIterator = activeBalls.iterator()
            while (ballIterator.hasNext()) {
                val ball = ballIterator.next()
                ball.x += ball.vx * timeScale
                ball.y += ball.vy * timeScale
                ball.updateTrail()

                // Wall bounces
                collisionManager.checkWallCollisions(ball, screenWidth, screenHeight, shieldActive) {
                    // Ball lost block
                    if (activeBalls.size > 1) {
                        ballIterator.remove()
                    } else {
                        loseLife()
                    }
                }

                // Paddle bounce
                collisionManager.checkPaddleCollision(ball, paddle)

                // Brick collisions
                collisionManager.checkBrickCollisions(
                    ball, bricks,
                    onScoreGained = { points, type ->
                        addScore(points)
                        if (mode == GameMode.TIME_ATTACK) {
                            timeRemaining += 2f // Earn 2 extra seconds per brick
                        }
                    },
                    onPowerUpSpawned = { type, x, y ->
                        activePowerUps.add(PowerUp(x, y, 22f, type))
                    },
                    onTeleport = { b, teleportBrick ->
                        // Find another teleport brick to warp the ball to
                        val target = bricks.find { it.id != teleportBrick.id && it.type == BrickType.TELEPORT }
                        if (target != null) {
                            b.x = target.x + target.width / 2f
                            b.y = target.y + target.height + b.radius + 10f
                            b.vy = abs(b.vy) // Send downward to avoid infinite loop
                            particleSystem.addExplosion(b.x, b.y, Color.MAGENTA)
                            audioManager.playSfx(AudioManager.SoundType.POWERUP)
                        }
                    },
                    onBrickDestroyed = { destroyedBrick ->
                        fadingBricks.add(destroyedBrick)
                    }
                )

                // Boss hit test
                boss?.let { b ->
                    val closestX = Math.max(b.x, Math.min(ball.x, b.x + b.width))
                    val closestY = Math.max(b.y, Math.min(ball.y, b.y + b.height))
                    val distX = ball.x - closestX
                    val distY = ball.y - closestY
                    val distSq = (distX * distX) + (distY * distY)

                    if (distSq < ball.radius * ball.radius) {
                        // Reflect ball
                        ball.vy = -ball.vy
                        ball.y = closestY + (if (ball.vy > 0) ball.radius else -ball.radius)
                        
                        // Direct Boss hit
                        val damage = if (ball.isBomb) 3 else 1
                        b.hp -= damage
                        audioManager.playSfx(AudioManager.SoundType.BRICK_BREAK)
                        particleSystem.triggerShake(6, 8f)
                        particleSystem.addExplosion(closestX, closestY, Color.RED, 12)

                        if (b.hp <= 0) {
                            boss = null
                            addScore(1000)
                            particleSystem.addExplosion(b.x + b.width / 2f, b.y + b.height / 2f, Color.YELLOW, 40)
                            audioManager.playSfx(AudioManager.SoundType.EXPLOSION)
                            checkVictoryCondition()
                        }
                    }
                }
            }

            // 2. Update Moving Bricks
            for (brick in bricks) {
                if (brick.type == BrickType.MOVING) {
                    brick.x += brick.speed * brick.moveDir * timeScale
                    val leftBound = brick.startX - brick.moveRange
                    val rightBound = brick.startX + brick.moveRange
                    if (brick.x < leftBound) {
                        brick.x = leftBound
                        brick.moveDir = 1
                    } else if (brick.x > rightBound) {
                        brick.x = rightBound
                        brick.moveDir = -1
                    }
                }
            }

            // 3. Update Boss AI & Projectiles
            boss?.let { b ->
                b.x += b.vx * timeScale
                if (b.x <= 0f || b.x + b.width >= screenWidth) {
                    b.vx = -b.vx
                    b.x = clamp(b.x, 0f, screenWidth - b.width)
                }

                // Shoot bullets based on time
                if (Math.random() < 0.015) { // 1.5% chance per frame
                    activeProjectiles.add(BossProjectile(b.x + b.width / 2f, b.y + b.height, 12f, 7f + level / 20f))
                }
            }

            // Update Boss Projectiles
            val projIterator = activeProjectiles.iterator()
            while (projIterator.hasNext()) {
                val p = projIterator.next()
                p.y += p.vy * timeScale

                // Collision with paddle
                val halfW = paddle.width / 2f
                if (p.x >= paddle.x - halfW && p.x <= paddle.x + halfW &&
                    p.y + p.radius >= paddle.y && p.y - p.radius <= paddle.y + paddle.height
                ) {
                    projIterator.remove()
                    loseLife()
                    continue
                }

                if (p.y - p.radius > screenHeight) {
                    projIterator.remove()
                }
            }

            // 4. Update Laser beams
            val laserIterator = activeLasers.iterator()
            while (laserIterator.hasNext()) {
                val laser = laserIterator.next()
                laser.y += laser.vy * timeScale

                // Hit brick test
                var hit = false
                val brickIterator = bricks.iterator()
                while (brickIterator.hasNext()) {
                    val brick = brickIterator.next()
                    if (laser.x >= brick.x && laser.x <= brick.x + brick.width &&
                        laser.y <= brick.y + brick.height && laser.y >= brick.y
                    ) {
                        hit = true
                        if (brick.type != BrickType.METAL) {
                            brick.hp--
                            if (brick.hp <= 0) {
                                brick.isDestroyed = true
                                brick.destroyProgress = 0f
                                fadingBricks.add(brick)
                                
                                brickIterator.remove()
                                addScore(100)
                                saveManager.incrementBricksDestroyed()
                                particleSystem.addExplosion(brick.x + brick.width / 2f, brick.y + brick.height / 2f, Color.GREEN)
                                audioManager.playSfx(AudioManager.SoundType.BRICK_BREAK)

                                // 15% powerup spawn chance from laser-hit bricks, or 100% for Mystery bricks
                                if (brick.type == BrickType.MYSTERY) {
                                    val randomPowerUp = PowerUpType.values().random()
                                    activePowerUps.add(PowerUp(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 22f, randomPowerUp))
                                } else if (Math.random() < 0.15) {
                                    val randomPowerUp = PowerUpType.values().random()
                                    activePowerUps.add(PowerUp(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 22f, randomPowerUp))
                                }
                            } else {
                                particleSystem.addExplosion(laser.x, laser.y, Color.GREEN, 3)
                            }
                        }
                        break
                    }
                }

                // Hit Boss test
                boss?.let { b ->
                    if (!hit && laser.x >= b.x && laser.x <= b.x + b.width &&
                        laser.y <= b.y + b.height && laser.y >= b.y
                    ) {
                        hit = true
                        b.hp--
                        particleSystem.addExplosion(laser.x, laser.y, Color.GREEN, 5)
                        audioManager.playSfx(AudioManager.SoundType.PADDLE_HIT)
                        if (b.hp <= 0) {
                            boss = null
                            addScore(1000)
                            particleSystem.addExplosion(b.x + b.width / 2f, b.y + b.height / 2f, Color.YELLOW, 40)
                            audioManager.playSfx(AudioManager.SoundType.EXPLOSION)
                        }
                    }
                }

                if (hit || laser.y < 0) {
                    laserIterator.remove()
                }
            }

            // 5. Update Power-ups
            val powerIterator = activePowerUps.iterator()
            while (powerIterator.hasNext()) {
                val pu = powerIterator.next()
                pu.y += pu.vy * timeScale

                // Collision with paddle
                val halfW = paddle.width / 2f
                if (pu.x >= paddle.x - halfW && pu.x <= paddle.x + halfW &&
                    pu.y + pu.radius >= paddle.y && pu.y - pu.radius <= paddle.y + paddle.height
                ) {
                    // Collect power-up!
                    applyPowerUp(pu.type)
                    powerIterator.remove()
                    continue
                }

                // Offscreen
                if (pu.y - pu.radius > screenHeight) {
                    powerIterator.remove()
                }
            }

            // Update fading/dying bricks smoothly
            val fadeIterator = fadingBricks.iterator()
            while (fadeIterator.hasNext()) {
                val fb = fadeIterator.next()
                fb.destroyProgress += dt * 4f // takes 0.25 seconds (4 * dt)
                if (fb.destroyProgress >= 1f) {
                    fadeIterator.remove()
                }
            }

            // 6. Particle system
            particleSystem.update()

            // 7. Check Victory condition
            checkVictoryCondition()
        }
    }

    private fun checkVictoryCondition() {
        val breakableRemaining = bricks.count { it.type != BrickType.METAL }
        if (breakableRemaining == 0 && boss == null) {
            endGame(victory = true)
        }
    }

    private fun applyPowerUp(type: PowerUpType) {
        audioManager.playSfx(AudioManager.SoundType.POWERUP)
        particleSystem.triggerShake(3, 4f)
        addScore(50)

        powerUpManager.applyPowerUp(
            type = type,
            activeBalls = activeBalls,
            paddle = paddle,
            onAddLife = { lives++ },
            onSetLaser = { active, duration ->
                paddle.isLaser = active
                laserPaddleDuration = duration
            },
            onSetShield = { active, duration ->
                shieldActive = active
                shieldDuration = duration
            }
        )
    }

    private fun loseLife() {
        lives--
        audioManager.playSfx(AudioManager.SoundType.DEATH)
        particleSystem.triggerShake(10, 15f)

        if (lives <= 0) {
            endGame(victory = false)
        } else {
            // Reset ball position on paddle
            activeBalls.clear()
            activePowerUps.clear()
            activeProjectiles.clear()
            activeLasers.clear()
            paddle.isLaser = false
            paddle.width = 240f // Reset paddle size
            
            val startVx = if (Math.random() > 0.5) 6f else -6f
            val ballSkin = saveManager.getActiveBallSkin()
            val ballColor = when (ballSkin) {
                "Plasma" -> Color.parseColor("#FF007F")
                "Comet Glow" -> Color.parseColor("#00E5FF")
                "Fireball" -> Color.parseColor("#FF3D00")
                "Disco" -> Color.parseColor("#E040FB")
                else -> Color.WHITE
            }
            activeBalls.add(
                Ball(
                    x = paddle.x,
                    y = paddle.y - 40f,
                    vx = startVx,
                    vy = -11f - (level / 20f),
                    radius = 20f,
                    color = ballColor
                )
            )
            state = GameState.READY
        }
    }

    private fun addScore(points: Int) {
        combo++
        saveManager.updateMaxCombo(combo)
        comboTimer = 2.0f // Reset combo window to 2 seconds
        
        val multiplier = 1 + (combo / 5)
        score += points * multiplier
    }

    private fun endGame(victory: Boolean) {
        state = if (victory) GameState.VICTORY else GameState.GAME_OVER
        if (victory) {
            audioManager.playSfx(AudioManager.SoundType.VICTORY)
            
            // Calculate Stars (1 to 3 stars based on performance/lives remaining)
            starsEarned = when {
                lives >= maxLives -> 3
                lives >= 2 -> 2
                else -> 1
            }

            // Save Level Progress
            saveManager.saveLevelProgress(level, starsEarned)

            // Coins earned reward
            coinsEarned = 15 + level * 5 + starsEarned * 10
            saveManager.addCoins(coinsEarned)

            // XP reward
            xpEarned = 50 + level * 10
            val leveledUp = saveManager.addXP(xpEarned)

            // High Score
            saveManager.updateHighScore(score)

            // Dynamic World Unlocks based on level completed
            if (level == 10) saveManager.unlockSkin("Space")
            if (level == 20) saveManager.unlockSkin("Volcano")
            if (level == 30) saveManager.unlockSkin("Ice Cave")
            if (level == 40) saveManager.unlockSkin("Cyber City")
            if (level == 50) saveManager.unlockSkin("Ancient Temple")

            // Achievements Check
            checkAchievements()

        } else {
            // High Score saving
            saveManager.updateHighScore(score)
        }
    }

    private fun checkAchievements() {
        if (score >= 10000) saveManager.unlockAchievement("ach_score_10k")
        if (score >= 50000) saveManager.unlockAchievement("ach_score_50k")
        if (saveManager.getBricksDestroyed() >= 100) saveManager.unlockAchievement("ach_bricks_100")
        if (saveManager.getBricksDestroyed() >= 1000) saveManager.unlockAchievement("ach_bricks_1000")
        if (level >= 10) saveManager.unlockAchievement("ach_boss_1")
        if (level >= 30) saveManager.unlockAchievement("ach_boss_3")
        if (level >= 50) saveManager.unlockAchievement("ach_boss_5")
    }

    fun handleDrag(deltaX: Float) {
        synchronized(stateLock) {
            if (state == GameState.PLAYING || state == GameState.READY) {
                val adjustedDeltaX = deltaX * 1.25f
                paddle.x = clamp(paddle.x + adjustedDeltaX, paddle.width / 2f, screenWidth - paddle.width / 2f)
                
                // If game is in READY state, center the ball on top of paddle
                if (state == GameState.READY && activeBalls.isNotEmpty()) {
                    activeBalls.first().x = paddle.x
                }
            }
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return Math.max(min, Math.min(max, value))
    }
}
