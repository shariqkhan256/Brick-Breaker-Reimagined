package com.example.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    private var engine: GameEngine? = null
    private var loop: GameLoop? = null
    private var lastTouchX = 0f

    // Coordinate scaling properties
    private val designWidth = 1080f
    private val designHeight = 1920f
    private var scaleX = 1f
    private var scaleY = 1f
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Visual paints
    private val paddlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Callbacks to Compose View Model
    var onGameStateChanged: ((GameEngine.GameState, Int, Int, Int, Int) -> Unit)? = null
    var onScoreUpdated: ((Int, Int, Int) -> Unit)? = null

    init {
        surfaceTextureListener = this
        isOpaque = false
        isFocusable = true
    }

    fun setEngine(gameEngine: GameEngine) {
        this.engine = gameEngine
    }

    private fun updateScalingFactors(physicalWidth: Float, physicalHeight: Float) {
        scaleX = physicalWidth / designWidth
        scaleY = physicalHeight / designHeight
        scaleFactor = Math.min(scaleX, scaleY)
        offsetX = (physicalWidth - designWidth * scaleFactor) / 2f
        offsetY = (physicalHeight - designHeight * scaleFactor) / 2f
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        updateScalingFactors(width.toFloat(), height.toFloat())
        engine?.let { eng ->
            // Re-initialize level using virtual dimensions if the game has not been loaded yet
            if (eng.bricks.isEmpty()) {
                eng.initGame(eng.level, eng.mode)
            }
        }

        // Start thread
        loop = GameLoop(
            lockCanvas = { lockCanvas() },
            unlockCanvasAndPost = { canvas -> unlockCanvasAndPost(canvas) },
            onUpdate = { fps ->
                engine?.let { eng ->
                    val prevState = eng.state
                    eng.update(fps)
                    
                    // Trigger Compose callbacks on state transitions
                    if (eng.state != prevState) {
                        post {
                            onGameStateChanged?.invoke(
                                eng.state,
                                eng.score,
                                eng.coinsEarned,
                                eng.xpEarned,
                                eng.starsEarned
                            )
                        }
                    }

                    // Score callback
                    post {
                        onScoreUpdated?.invoke(eng.score, eng.combo, eng.lives)
                    }
                }
            },
            onDraw = { canvas ->
                drawGame(canvas)
            }
        ).apply {
            running = true
            start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateScalingFactors(width.toFloat(), height.toFloat())
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        var retry = true
        loop?.running = false
        while (retry) {
            try {
                loop?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        loop = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eng = engine ?: return super.onTouchEvent(event)

        // Convert physical touch coordinate to virtual coordinate space
        val x = (event.x - offsetX) / if (scaleFactor > 0f) scaleFactor else 1f
        val action = event.action

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
            }
            MotionEvent.ACTION_MOVE -> {
                if (eng.state == GameEngine.GameState.PLAYING || eng.state == GameEngine.GameState.READY) {
                    val virtualDx = x - lastTouchX
                    eng.handleDrag(virtualDx)
                }
                lastTouchX = x
            }
            MotionEvent.ACTION_UP -> {
                if (eng.state == GameEngine.GameState.READY) {
                    eng.startGame()
                }
            }
        }
        return true
    }

    private fun drawGame(canvas: Canvas) {
        val eng = engine ?: return

        synchronized(eng.stateLock) {
            // 1. Draw World Themed Background fullscreen (edge-to-edge ambient gradient)
            drawBackground(canvas, eng.worldTheme)

            // 2. Save canvas, apply translation to center, and scale to fit virtual coordinate space
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scaleFactor, scaleFactor)

            // Apply Screen Shake inside the scaled coordinate system
            canvas.save()
            canvas.translate(eng.particleSystem.getShakeX(), eng.particleSystem.getShakeY())

            // 3. Draw Background Decorations based on World
            drawBackgroundDecorations(canvas, eng.worldTheme)

            // 4. Draw Bricks (Active and Fading)
            for (brick in eng.bricks) {
                drawBrick(canvas, brick, eng.worldTheme)
            }
            for (brick in eng.fadingBricks) {
                drawBrick(canvas, brick, eng.worldTheme)
            }

            // 5. Draw Bottom Shield if active
            if (eng.shieldActive) {
                uiPaint.style = Paint.Style.STROKE
                uiPaint.strokeWidth = 14f
                uiPaint.color = Color.parseColor("#00E5FF")
                uiPaint.shader = null
                canvas.drawLine(0f, designHeight - 15f, designWidth, designHeight - 15f, uiPaint)
            }

            // 6. Draw Lasers
            for (laser in eng.activeLasers) {
                ballPaint.style = Paint.Style.FILL
                ballPaint.color = Color.GREEN
                canvas.drawCircle(laser.x, laser.y, laser.radius, ballPaint)
            }

            // 7. Draw Power-ups
            for (pu in eng.activePowerUps) {
                drawPowerUp(canvas, pu)
            }

            // 8. Draw Boss
            eng.boss?.let { b ->
                drawBoss(canvas, b)
            }

            // 9. Draw Boss Projectiles
            for (p in eng.activeProjectiles) {
                ballPaint.color = p.color
                ballPaint.style = Paint.Style.FILL
                canvas.drawCircle(p.x, p.y, p.radius, ballPaint)
            }

            // 10. Draw Paddle
            paddlePaint.style = Paint.Style.FILL
            paddlePaint.color = eng.paddle.color
            val halfW = eng.paddle.width / 2f
            val paddleRect = RectF(eng.paddle.x - halfW, eng.paddle.y, eng.paddle.x + halfW, eng.paddle.y + eng.paddle.height)
            canvas.drawRoundRect(paddleRect, 20f, 20f, paddlePaint)

            // Draw laser cannons on paddle if laser active
            if (eng.paddle.isLaser) {
                paddlePaint.color = Color.RED
                canvas.drawRect(eng.paddle.x - halfW, eng.paddle.y - 15f, eng.paddle.x - halfW + 15f, eng.paddle.y, paddlePaint)
                canvas.drawRect(eng.paddle.x + halfW - 15f, eng.paddle.y - 15f, eng.paddle.x + halfW, eng.paddle.y, paddlePaint)
            }

            // 11. Draw Balls with trails
            for (ball in eng.activeBalls) {
                // Draw Trail
                for (i in ball.history.indices) {
                    val pt = ball.history[i]
                    val alpha = (255 * (1f - i / ball.history.size.toFloat()) * 0.45f).toInt()
                    ballPaint.color = ball.color
                    ballPaint.alpha = alpha
                    canvas.drawCircle(pt.first, pt.second, ball.radius * (1f - i / ball.history.size.toFloat() * 0.5f), ballPaint)
                }
                ballPaint.alpha = 255

                // Draw Core
                ballPaint.color = ball.color
                canvas.drawCircle(ball.x, ball.y, ball.radius, ballPaint)
                
                // Highlight shine
                ballPaint.color = Color.WHITE
                canvas.drawCircle(ball.x - ball.radius * 0.3f, ball.y - ball.radius * 0.3f, ball.radius * 0.25f, ballPaint)
            }

            // 12. Draw Particles
            eng.particleSystem.draw(canvas)

            // Restore screen shake translation
            canvas.restore()

            // 13. Ready launch Overlay
            if (eng.state == GameEngine.GameState.READY) {
                textPaint.color = Color.WHITE
                textPaint.textSize = 48f
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                val heartbeat = (180 + 75 * Math.sin(System.currentTimeMillis() / 200.0)).toInt()
                textPaint.alpha = clampInt(heartbeat, 0, 255)
                canvas.drawText("DRAG TO AIM & TAP TO LAUNCH", designWidth / 2f, eng.paddle.y - 120f, textPaint)
            }

            // Restore translation and scale
            canvas.restore()
        }
    }

    private fun drawBackground(canvas: Canvas, theme: WorldTheme) {
        val colors = theme.getBackgroundColors()
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors.first, colors.second, Shader.TileMode.CLAMP
        )
        paddlePaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paddlePaint)
        paddlePaint.shader = null
    }

    private fun drawBackgroundDecorations(canvas: Canvas, theme: WorldTheme) {
        uiPaint.style = Paint.Style.STROKE
        val vWidth = 1080f
        val vHeight = 1920f
        when (theme) {
            WorldTheme.FOREST -> {
                // Soft background leaves/vines
                uiPaint.color = Color.argb(20, 255, 255, 255)
                uiPaint.strokeWidth = 3f
                canvas.drawCircle(vWidth * 0.2f, vHeight * 0.3f, 150f, uiPaint)
                canvas.drawCircle(vWidth * 0.8f, vHeight * 0.6f, 250f, uiPaint)
            }
            WorldTheme.SPACE -> {
                // Stars & planetary rings
                uiPaint.color = Color.argb(30, 255, 255, 255)
                canvas.drawCircle(vWidth * 0.5f, vHeight * 0.4f, 400f, uiPaint)
                uiPaint.style = Paint.Style.FILL
                uiPaint.color = Color.argb(45, 255, 255, 255)
                canvas.drawCircle(vWidth * 0.15f, vHeight * 0.2f, 4f, uiPaint)
                canvas.drawCircle(vWidth * 0.85f, vHeight * 0.3f, 6f, uiPaint)
                canvas.drawCircle(vWidth * 0.7f, vHeight * 0.75f, 5f, uiPaint)
            }
            WorldTheme.VOLCANO -> {
                // Lava streams
                uiPaint.color = Color.argb(15, 255, 61, 0)
                uiPaint.strokeWidth = 100f
                canvas.drawLine(vWidth * 0.1f, 0f, vWidth * 0.4f, vHeight, uiPaint)
                canvas.drawLine(vWidth * 0.9f, 0f, vWidth * 0.6f, vHeight, uiPaint)
            }
            WorldTheme.ICE_CAVE -> {
                // Ice stalactite silhouettes
                uiPaint.color = Color.argb(25, 0, 229, 255)
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(100f, 150f)
                    lineTo(200f, 0f)
                    lineTo(350f, 200f)
                    lineTo(450f, 0f)
                    lineTo(vWidth, 300f)
                    lineTo(vWidth, 0f)
                    close()
                }
                uiPaint.style = Paint.Style.FILL
                canvas.drawPath(path, uiPaint)
            }
            WorldTheme.CYBER_CITY -> {
                // Futuristic tech grid
                uiPaint.color = Color.argb(25, 224, 64, 251)
                uiPaint.strokeWidth = 2f
                val gridSpacing = 120f
                var x = 0f
                while (x < vWidth) {
                    canvas.drawLine(x, 0f, x, vHeight, uiPaint)
                    x += gridSpacing
                }
                var y = 0f
                while (y < vHeight) {
                    canvas.drawLine(0f, y, vWidth, y, uiPaint)
                    y += gridSpacing
                }
            }
            WorldTheme.ANCIENT_TEMPLE -> {
                // Mystic geometric sigils
                uiPaint.color = Color.argb(20, 255, 215, 0)
                uiPaint.strokeWidth = 4f
                canvas.drawRect(50f, 100f, vWidth - 50f, vHeight - 100f, uiPaint)
                canvas.drawCircle(vWidth / 2f, vHeight / 2f, 200f, uiPaint)
            }
        }
    }

    private fun drawBrick(canvas: Canvas, brick: Brick, theme: WorldTheme) {
        val colors = theme.getBrickColors(brick.type, brick.hp)
        val rect = RectF(brick.x, brick.y, brick.x + brick.width, brick.y + brick.height)

        val oldBrickAlpha = brickPaint.alpha
        val oldTextAlpha = textPaint.alpha

        if (brick.isDestroyed) {
            val scale = 1f - brick.destroyProgress
            val alpha = ((1f - brick.destroyProgress) * 255).toInt().coerceIn(0, 255)
            
            canvas.save()
            val cx = brick.x + brick.width / 2f
            val cy = brick.y + brick.height / 2f
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            canvas.translate(-cx, -cy)
            
            brickPaint.alpha = alpha
            textPaint.alpha = alpha
        }

        // 3D Bevel look via Linear Gradient
        val grad = LinearGradient(
            brick.x, brick.y, brick.x, brick.y + brick.height,
            colors.first, colors.second, Shader.TileMode.CLAMP
        )
        brickPaint.shader = grad
        brickPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 10f, 10f, brickPaint)
        brickPaint.shader = null

        // Neon Outer border glow
        brickPaint.style = Paint.Style.STROKE
        brickPaint.strokeWidth = 3f
        brickPaint.color = colors.first
        canvas.drawRoundRect(rect, 10f, 10f, brickPaint)

        // Add visual cues per brick type
        when (brick.type) {
            BrickType.STRONG -> {
                // Draw crack lines based on damage
                brickPaint.color = Color.argb(160, 255, 255, 255)
                brickPaint.strokeWidth = 2f
                if (brick.hp < brick.maxHp) {
                    canvas.drawLine(brick.x + 10f, brick.y + 10f, brick.x + brick.width - 20f, brick.y + brick.height - 10f, brickPaint)
                }
                if (brick.hp == 1 && brick.maxHp > 2) {
                    canvas.drawLine(brick.x + brick.width - 15f, brick.y + 10f, brick.x + 15f, brick.y + brick.height - 10f, brickPaint)
                }
            }
            BrickType.METAL -> {
                // Draw industrial steel rivet markers
                brickPaint.color = Color.parseColor("#424242")
                brickPaint.style = Paint.Style.FILL
                canvas.drawCircle(brick.x + 10f, brick.y + 10f, 4f, brickPaint)
                canvas.drawCircle(brick.x + brick.width - 10f, brick.y + 10f, 4f, brickPaint)
                canvas.drawCircle(brick.x + 10f, brick.y + brick.height - 10f, 4f, brickPaint)
                canvas.drawCircle(brick.x + brick.width - 10f, brick.y + brick.height - 10f, 4f, brickPaint)
            }
            BrickType.EXPLOSIVE -> {
                // Draw glowing hazard center
                brickPaint.color = Color.BLACK
                brickPaint.style = Paint.Style.FILL
                canvas.drawCircle(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 12f, brickPaint)
                brickPaint.color = Color.YELLOW
                canvas.drawCircle(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 6f, brickPaint)
            }
            BrickType.MYSTERY -> {
                // Draw Question mark "?"
                textPaint.color = Color.WHITE
                textPaint.textSize = 28f
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("?", brick.x + brick.width / 2f, brick.y + brick.height / 2f + 10f, textPaint)
            }
            BrickType.TELEPORT -> {
                // Swirl spiral indicator
                brickPaint.color = Color.WHITE
                brickPaint.style = Paint.Style.STROKE
                brickPaint.strokeWidth = 3f
                canvas.drawCircle(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 12f, brickPaint)
                canvas.drawCircle(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 6f, brickPaint)
            }
            BrickType.MOVING -> {
                // Chevron arrows on edges
                brickPaint.color = Color.WHITE
                brickPaint.style = Paint.Style.FILL
                val path = Path().apply {
                    moveTo(brick.x + 5f, brick.y + brick.height / 2f)
                    lineTo(brick.x + 15f, brick.y + 8f)
                    lineTo(brick.x + 15f, brick.y + brick.height - 8f)
                    close()
                    moveTo(brick.x + brick.width - 5f, brick.y + brick.height / 2f)
                    lineTo(brick.x + brick.width - 15f, brick.y + 8f)
                    lineTo(brick.x + brick.width - 15f, brick.y + brick.height - 8f)
                    close()
                }
                canvas.drawPath(path, brickPaint)
            }
            else -> {}
        }

        if (brick.isDestroyed) {
            canvas.restore()
            brickPaint.alpha = oldBrickAlpha
            textPaint.alpha = oldTextAlpha
        }
    }

    private fun drawPowerUp(canvas: Canvas, pu: PowerUp) {
        val color = when (pu.type) {
            PowerUpType.BIGGER_PADDLE, PowerUpType.EXTRA_LIFE -> Color.parseColor("#00E676") // Green
            PowerUpType.SMALLER_PADDLE, PowerUpType.FAST_BALL -> Color.parseColor("#FF1744") // Red
            PowerUpType.MULTI_BALL, PowerUpType.BOMB_BALL -> Color.parseColor("#FF9100") // Orange
            PowerUpType.LASER_PADDLE -> Color.parseColor("#E040FB") // Purple
            PowerUpType.SLOW_BALL -> Color.parseColor("#2979FF") // Blue
            PowerUpType.SHIELD -> Color.parseColor("#00E5FF") // Teal
        }

        ballPaint.color = color
        ballPaint.style = Paint.Style.FILL
        canvas.drawCircle(pu.x, pu.y, pu.radius, ballPaint)

        // Draw inner ring
        ballPaint.color = Color.WHITE
        ballPaint.style = Paint.Style.STROKE
        ballPaint.strokeWidth = 3f
        canvas.drawCircle(pu.x, pu.y, pu.radius - 4f, ballPaint)

        // Draw character representation
        val label = when (pu.type) {
            PowerUpType.BIGGER_PADDLE -> "W+"
            PowerUpType.SMALLER_PADDLE -> "W-"
            PowerUpType.MULTI_BALL -> "3X"
            PowerUpType.EXTRA_LIFE -> "1U"
            PowerUpType.LASER_PADDLE -> "LA"
            PowerUpType.SLOW_BALL -> "S>"
            PowerUpType.FAST_BALL -> "F>"
            PowerUpType.SHIELD -> "SH"
            PowerUpType.BOMB_BALL -> "BO"
        }

        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(label, pu.x, pu.y + 7f, textPaint)
    }

    private fun drawBoss(canvas: Canvas, b: Boss) {
        // Boss Outer frame
        paddlePaint.color = Color.RED
        paddlePaint.style = Paint.Style.FILL
        val r = RectF(b.x, b.y, b.x + b.width, b.y + b.height)
        canvas.drawRoundRect(r, 15f, 15f, paddlePaint)

        // Draw scary metallic pattern
        paddlePaint.color = Color.BLACK
        paddlePaint.style = Paint.Style.STROKE
        paddlePaint.strokeWidth = 6f
        canvas.drawRoundRect(r, 15f, 15f, paddlePaint)
        
        // Angry core eye
        paddlePaint.color = Color.argb(190 + (65 * Math.sin(System.currentTimeMillis() / 80.0)).toInt(), 255, 0, 0)
        paddlePaint.style = Paint.Style.FILL
        canvas.drawCircle(b.x + b.width / 2f, b.y + b.height / 2f, 25f, paddlePaint)

        // Health Bar container
        val hbLeft = b.x
        val hbRight = b.x + b.width
        val hbTop = b.y - 30f
        val hbBottom = b.y - 18f

        uiPaint.color = Color.parseColor("#33000000")
        uiPaint.style = Paint.Style.FILL
        canvas.drawRect(hbLeft, hbTop, hbRight, hbBottom, uiPaint)

        // Fill bar based on HP
        val ratio = b.hp.toFloat() / b.maxHp
        uiPaint.color = Color.parseColor("#FF1744")
        canvas.drawRect(hbLeft, hbTop, hbLeft + b.width * ratio, hbBottom, uiPaint)

        // Boss label
        textPaint.color = Color.WHITE
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText(b.name, b.x + b.width / 2f, b.y - 40f, textPaint)
    }

    private fun clampInt(value: Int, min: Int, max: Int): Int {
        return Math.max(min, Math.min(max, value))
    }
}
