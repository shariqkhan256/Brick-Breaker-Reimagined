package com.example.game

import android.graphics.Color

enum class BrickType {
    NORMAL,
    STRONG,
    METAL,
    EXPLOSIVE,
    MYSTERY,
    MOVING,
    TELEPORT
}

enum class PowerUpType {
    BIGGER_PADDLE,
    SMALLER_PADDLE,
    MULTI_BALL,
    EXTRA_LIFE,
    LASER_PADDLE,
    SLOW_BALL,
    FAST_BALL,
    SHIELD,
    BOMB_BALL
}

enum class WorldTheme {
    FOREST,
    SPACE,
    VOLCANO,
    ICE_CAVE,
    CYBER_CITY,
    ANCIENT_TEMPLE;

    fun getBackgroundColors(): Pair<Int, Int> {
        return when (this) {
            FOREST -> Pair(Color.parseColor("#0F3010"), Color.parseColor("#051405"))
            SPACE -> Pair(Color.parseColor("#0B0B26"), Color.parseColor("#020208"))
            VOLCANO -> Pair(Color.parseColor("#3A0707"), Color.parseColor("#150000"))
            ICE_CAVE -> Pair(Color.parseColor("#0D2D3E"), Color.parseColor("#030F17"))
            CYBER_CITY -> Pair(Color.parseColor("#1C0324"), Color.parseColor("#060009"))
            ANCIENT_TEMPLE -> Pair(Color.parseColor("#291E0A"), Color.parseColor("#0F0A02"))
        }
    }

    fun getBrickColors(type: BrickType, hp: Int): Pair<Int, Int> {
        return when (type) {
            BrickType.NORMAL -> Pair(Color.parseColor("#FF5722"), Color.parseColor("#D84315"))
            BrickType.STRONG -> {
                if (hp >= 3) Pair(Color.parseColor("#E91E63"), Color.parseColor("#880E4F"))
                else if (hp == 2) Pair(Color.parseColor("#9C27B0"), Color.parseColor("#4A148C"))
                else Pair(Color.parseColor("#673AB7"), Color.parseColor("#311B92"))
            }
            BrickType.METAL -> Pair(Color.parseColor("#9E9E9E"), Color.parseColor("#424242"))
            BrickType.EXPLOSIVE -> Pair(Color.parseColor("#FFEB3B"), Color.parseColor("#FF5722"))
            BrickType.MYSTERY -> Pair(Color.parseColor("#00BCD4"), Color.parseColor("#006064"))
            BrickType.MOVING -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#1B5E20"))
            BrickType.TELEPORT -> Pair(Color.parseColor("#E040FB"), Color.parseColor("#AA00FF"))
        }
    }
}

data class Ball(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var isBomb: Boolean = false,
    var color: Int = Color.WHITE
) {
    val history = mutableListOf<Pair<Float, Float>>() // For trail effect
    private val maxHistory = 10

    fun updateTrail() {
        history.add(0, Pair(x, y))
        if (history.size > maxHistory) {
            history.removeAt(history.size - 1)
        }
    }
}

data class Paddle(
    var x: Float, // Center X
    var y: Float, // Top Y
    var width: Float,
    var height: Float,
    var isLaser: Boolean = false,
    var color: Int = Color.parseColor("#00E676")
)

data class Brick(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val type: BrickType,
    var hp: Int,
    var maxHp: Int,
    var startX: Float = x,
    var moveRange: Float = 0f,
    var moveDir: Int = 1, // 1 for right, -1 for left
    var speed: Float = 2f,
    var isDestroyed: Boolean = false,
    var destroyProgress: Float = 0f
)

data class PowerUp(
    var x: Float,
    var y: Float,
    var radius: Float,
    val type: PowerUpType,
    var vy: Float = 6f
)

data class Boss(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var vx: Float,
    var vy: Float,
    var hp: Int,
    var maxHp: Int,
    var name: String,
    val type: Int // Boss type
)

data class BossProjectile(
    var x: Float,
    var y: Float,
    var radius: Float,
    var vy: Float,
    var color: Int = Color.RED
)

data class LaserBeam(
    var x: Float,
    var y: Float,
    var radius: Float = 4f,
    var vy: Float = -12f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Int,
    var alpha: Int = 255,
    var decay: Int = 8 // Alpha reduction per frame
)
