package com.example.save

import android.content.Context
import android.content.SharedPreferences

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brick_breaker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_XP = "xp"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_VIBE_ENABLED = "vibe_enabled"
        private const val KEY_ACTIVE_PADDLE_SKIN = "active_paddle_skin"
        private const val KEY_ACTIVE_BALL_SKIN = "active_ball_skin"
        private const val KEY_ACTIVE_THEME = "active_theme"
        private const val KEY_LAST_DAILY_REWARD = "last_daily_reward"
        private const val KEY_LEVEL_STARS_PREFIX = "level_stars_"
        private const val KEY_UNLOCKED_SKIN_PREFIX = "unlocked_skin_"
        private const val KEY_ACHIEVEMENT_PREFIX = "achievement_"
        private const val KEY_STAT_BRICKS_DESTROYED = "stat_bricks_destroyed"
        private const val KEY_STAT_GAMES_PLAYED = "stat_games_played"
        private const val KEY_STAT_MAX_COMBO = "stat_max_combo"
    }

    // Coins
    fun getCoins(): Int = prefs.getInt(KEY_COINS, 200) // Default starting coins
    fun addCoins(amount: Int) {
        prefs.edit().putInt(KEY_COINS, getCoins() + amount).apply()
    }
    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        return if (current >= amount) {
            prefs.edit().putInt(KEY_COINS, current - amount).apply()
            true
        } else {
            false
        }
    }

    // XP and Leveling
    fun getXP(): Int = prefs.getInt(KEY_XP, 0)
    fun addXP(amount: Int): Boolean {
        val prevLevel = getLevel()
        val currentXP = getXP() + amount
        prefs.edit().putInt(KEY_XP, currentXP).apply()
        val newLevel = getLevel()
        return newLevel > prevLevel // Return true if leveled up
    }
    fun getLevel(): Int {
        val xp = getXP()
        // Level formula: level = 1 + sqrt(xp / 100)
        return 1 + (kotlin.math.sqrt(xp.toDouble() / 100.0)).toInt()
    }
    fun getXPNeededForNextLevel(): Int {
        val nextLvl = getLevel()
        return (nextLvl * nextLvl) * 100
    }

    // High Score
    fun getHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)
    fun updateHighScore(score: Int): Boolean {
        val current = getHighScore()
        return if (score > current) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
            true
        } else {
            false
        }
    }

    // Toggles
    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()

    fun isMusicEnabled(): Boolean = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    fun setMusicEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()

    fun isVibeEnabled(): Boolean = prefs.getBoolean(KEY_VIBE_ENABLED, true)
    fun setVibeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VIBE_ENABLED, enabled).apply()

    // Skin Customization
    fun getActivePaddleSkin(): String = prefs.getString(KEY_ACTIVE_PADDLE_SKIN, "Default") ?: "Default"
    fun setActivePaddleSkin(skin: String) = prefs.edit().putString(KEY_ACTIVE_PADDLE_SKIN, skin).apply()

    fun getActiveBallSkin(): String = prefs.getString(KEY_ACTIVE_BALL_SKIN, "Default") ?: "Default"
    fun setActiveBallSkin(skin: String) = prefs.edit().putString(KEY_ACTIVE_BALL_SKIN, skin).apply()

    fun getActiveTheme(): String = prefs.getString(KEY_ACTIVE_THEME, "Forest") ?: "Forest"
    fun setActiveTheme(theme: String) = prefs.edit().putString(KEY_ACTIVE_THEME, theme).apply()

    // Unlocked skins & themes
    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "Default" || skinId == "Forest" || skinId == "Classic") return true
        return prefs.getBoolean(KEY_UNLOCKED_SKIN_PREFIX + skinId, false)
    }
    fun unlockSkin(skinId: String) {
        prefs.edit().putBoolean(KEY_UNLOCKED_SKIN_PREFIX + skinId, true).apply()
    }

    // Level Progress & Stars
    fun getLevelStars(level: Int): Int = prefs.getInt(KEY_LEVEL_STARS_PREFIX + level, 0) // 0 means uncompleted
    fun saveLevelProgress(level: Int, stars: Int) {
        val currentStars = getLevelStars(level)
        if (stars > currentStars) {
            prefs.edit().putInt(KEY_LEVEL_STARS_PREFIX + level, stars).apply()
        }
    }
    fun getMaxUnlockedLevel(): Int {
        var maxCompleted = 0
        for (i in 1..100) {
            if (getLevelStars(i) > 0) {
                maxCompleted = i
            }
        }
        return maxCompleted + 1
    }

    // Daily Rewards
    fun getLastDailyRewardTime(): Long = prefs.getLong(KEY_LAST_DAILY_REWARD, 0L)
    fun claimDailyReward() {
        prefs.edit().putLong(KEY_LAST_DAILY_REWARD, System.currentTimeMillis()).apply()
    }

    // Achievements
    fun isAchievementUnlocked(id: String): Boolean = prefs.getBoolean(KEY_ACHIEVEMENT_PREFIX + id, false)
    fun unlockAchievement(id: String) {
        prefs.edit().putBoolean(KEY_ACHIEVEMENT_PREFIX + id, true).apply()
    }

    // Stats
    fun getBricksDestroyed(): Int = prefs.getInt(KEY_STAT_BRICKS_DESTROYED, 0)
    fun incrementBricksDestroyed(count: Int = 1) {
        prefs.edit().putInt(KEY_STAT_BRICKS_DESTROYED, getBricksDestroyed() + count).apply()
    }

    fun getGamesPlayed(): Int = prefs.getInt(KEY_STAT_GAMES_PLAYED, 0)
    fun incrementGamesPlayed() {
        prefs.edit().putInt(KEY_STAT_GAMES_PLAYED, getGamesPlayed() + 1).apply()
    }

    fun getMaxCombo(): Int = prefs.getInt(KEY_STAT_MAX_COMBO, 0)
    fun updateMaxCombo(combo: Int) {
        val current = getMaxCombo()
        if (combo > current) {
            prefs.edit().putInt(KEY_STAT_MAX_COMBO, combo).apply()
        }
    }
}
