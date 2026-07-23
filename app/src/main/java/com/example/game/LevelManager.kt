package com.example.game

import java.util.UUID

class LevelManager {

    fun generateLevel(level: Int, screenWidth: Float, screenHeight: Float): List<Brick> {
        val bricks = mutableListOf<Brick>()
        val topMargin = screenHeight * 0.15f
        
        // Define grid
        val cols = 8
        val rows = 6
        val brickWidth = (screenWidth - 80f) / cols
        val brickHeight = 50f
        val padding = 6f

        // Get world theme and active difficulty multiplier based on level
        val theme = getWorldThemeForLevel(level)

        // Select layout archetype based on level index
        val layoutType = when {
            level % 10 == 0 -> "Boss" // Boss levels do not have standard grids, or only have shield bricks
            level % 6 == 1 -> "Rectangle"
            level % 6 == 2 -> "Pyramid"
            level % 6 == 3 -> "Heart"
            level % 6 == 4 -> "Diamond"
            level % 6 == 5 -> "Spiral"
            else -> "Maze"
        }

        if (layoutType == "Boss") {
            // Place a few shield bricks at the top to guard the boss
            for (c in 0 until cols) {
                if (c == 0 || c == 1 || c == 6 || c == 7) continue // Leave opening
                val x = 40f + c * brickWidth
                val y = topMargin + brickHeight * 3
                bricks.add(
                    Brick(
                        id = UUID.randomUUID().toString(),
                        x = x + padding,
                        y = y + padding,
                        width = brickWidth - padding * 2,
                        height = brickHeight - padding * 2,
                        type = BrickType.NORMAL,
                        hp = 1,
                        maxHp = 1
                    )
                )
            }
            return bricks
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val shouldPlace = when (layoutType) {
                    "Rectangle" -> true
                    "Pyramid" -> c >= r && c < (cols - r)
                    "Heart" -> {
                        // A rough heart pattern in an 8x6 grid
                        val heartMap = arrayOf(
                            intArrayOf(0, 1, 1, 0, 0, 1, 1, 0),
                            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
                            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
                            intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
                            intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
                            intArrayOf(0, 0, 0, 1, 1, 0, 0, 0)
                        )
                        heartMap[r][c] == 1
                    }
                    "Diamond" -> {
                        val midR = 3
                        val midC = 4
                        val dist = kotlin.math.abs(r - midR) + kotlin.math.abs(c - midC)
                        dist <= 3
                    }
                    "Spiral" -> {
                        // Outer border and center core
                        r == 0 || r == rows - 1 || c == 0 || c == cols - 1 || (r == 3 && c in 2..5)
                    }
                    "Maze" -> {
                        (r % 2 == 0 && c % 2 == 0) || (r % 2 == 1 && c % 2 == 1)
                    }
                    else -> true
                }

                if (shouldPlace) {
                    val x = 40f + c * brickWidth
                    val y = topMargin + r * brickHeight

                    // Determine brick type based on level and position
                    val typeRand = (r + c + level) % 15
                    val type = when {
                        typeRand == 0 && level >= 5 -> BrickType.METAL
                        typeRand == 1 && level >= 3 -> BrickType.STRONG
                        typeRand == 2 -> BrickType.EXPLOSIVE
                        typeRand == 3 -> BrickType.MYSTERY
                        typeRand == 4 && level >= 7 -> BrickType.MOVING
                        typeRand == 5 && level >= 6 -> BrickType.TELEPORT
                        else -> BrickType.NORMAL
                    }

                    val maxHp = when (type) {
                        BrickType.STRONG -> if (level > 20) 3 else 2
                        BrickType.METAL -> 999999
                        else -> 1
                    }

                    // For moving bricks, configure ranges
                    val moveRange = if (type == BrickType.MOVING) 80f else 0f

                    bricks.add(
                        Brick(
                            id = UUID.randomUUID().toString(),
                            x = x + padding,
                            y = y + padding,
                            width = brickWidth - padding * 2,
                            height = brickHeight - padding * 2,
                            type = type,
                            hp = maxHp,
                            maxHp = maxHp,
                            moveRange = moveRange,
                            speed = 2f + (level / 15f)
                        )
                    )
                }
            }
        }

        return bricks
    }

    fun getWorldThemeForLevel(level: Int): WorldTheme {
        return when {
            level <= 10 -> WorldTheme.FOREST
            level <= 20 -> WorldTheme.SPACE
            level <= 30 -> WorldTheme.VOLCANO
            level <= 40 -> WorldTheme.ICE_CAVE
            level <= 45 -> WorldTheme.CYBER_CITY
            else -> WorldTheme.ANCIENT_TEMPLE
        }
    }

    fun getBossNameForLevel(level: Int): String {
        return when (level) {
            10 -> "Giga-Sentry 1.0"
            20 -> "Nebula Viper"
            30 -> "Magma Goliath"
            40 -> "Glacier Beast"
            50 -> "Cyber-Nexus Core"
            else -> "Aether Guardian"
        }
    }
}
