package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.audio.AudioManager
import com.example.game.GameEngine
import com.example.game.ParticleSystem
import com.example.game.WorldTheme
import com.example.save.SaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = SaveManager(application)
    private val audioManager = AudioManager(application, saveManager)
    private val particleSystem = ParticleSystem()
    
    val gameEngine = GameEngine(application, saveManager, audioManager, particleSystem)

    val skinShopStateManager = com.example.shop.SkinShopStateManager(saveManager)

    // Player state flows for UI delegated to state manager
    val coins: StateFlow<Int> = skinShopStateManager.coins
    val activePaddleSkin: StateFlow<String> = skinShopStateManager.activePaddleSkin
    val activeBallSkin: StateFlow<String> = skinShopStateManager.activeBallSkin
    val activeTheme: StateFlow<String> = skinShopStateManager.activeTheme

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Settings
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(true)
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _vibeEnabled = MutableStateFlow(true)
    val vibeEnabled: StateFlow<Boolean> = _vibeEnabled.asStateFlow()

    // Active screen routes
    private val _currentScreen = MutableStateFlow("Splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Live gameplay HUD values
    private val _liveScore = MutableStateFlow(0)
    val liveScore: StateFlow<Int> = _liveScore.asStateFlow()

    private val _liveCombo = MutableStateFlow(0)
    val liveCombo: StateFlow<Int> = _liveCombo.asStateFlow()

    private val _liveLives = MutableStateFlow(3)
    val liveLives: StateFlow<Int> = _liveLives.asStateFlow()

    private val _gameState = MutableStateFlow(GameEngine.GameState.READY)
    val gameState: StateFlow<GameEngine.GameState> = _gameState.asStateFlow()

    // End-game result statistics values
    var lastGameScore = 0
    var lastGameCoins = 0
    var lastGameXP = 0
    var lastGameStars = 0

    init {
        refreshPlayerStats()
    }

    fun refreshPlayerStats() {
        skinShopStateManager.refresh()
        _xp.value = saveManager.getXP()
        _level.value = saveManager.getLevel()
        _highScore.value = saveManager.getHighScore()
        _soundEnabled.value = saveManager.isSoundEnabled()
        _musicEnabled.value = saveManager.isMusicEnabled()
        _vibeEnabled.value = saveManager.isVibeEnabled()
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        refreshPlayerStats()
    }

    // Toggle Settings
    fun toggleSound() {
        saveManager.setSoundEnabled(!saveManager.isSoundEnabled())
        _soundEnabled.value = saveManager.isSoundEnabled()
    }

    fun toggleMusic() {
        saveManager.setMusicEnabled(!saveManager.isMusicEnabled())
        val isEnabled = saveManager.isMusicEnabled()
        _musicEnabled.value = isEnabled
        audioManager.setMusicEnabled(isEnabled)
    }

    fun toggleVibe() {
        saveManager.setVibeEnabled(!saveManager.isVibeEnabled())
        _vibeEnabled.value = saveManager.isVibeEnabled()
    }

    // Live Gameplay HUD callbacks
    fun updateLiveHud(score: Int, combo: Int, lives: Int) {
        _liveScore.value = score
        _liveCombo.value = combo
        _liveLives.value = lives
    }

    fun setGameplayState(state: GameEngine.GameState, score: Int, coins: Int, xp: Int, stars: Int) {
        _gameState.value = state
        if (state == GameEngine.GameState.VICTORY || state == GameEngine.GameState.GAME_OVER) {
            lastGameScore = score
            lastGameCoins = coins
            lastGameXP = xp
            lastGameStars = stars
            navigateTo(if (state == GameEngine.GameState.VICTORY) "Victory" else "GameOver")
        }
    }

    // Shop Purchasing Logic delegated to state manager
    fun isSkinUnlocked(skinId: String): Boolean = skinShopStateManager.isSkinUnlocked(skinId)

    fun selectPaddleSkin(skinName: String) {
        skinShopStateManager.equipPaddleSkin(skinName)
    }

    fun selectBallSkin(skinName: String) {
        skinShopStateManager.equipBallSkin(skinName)
    }

    fun selectTheme(themeName: String) {
        skinShopStateManager.equipTheme(themeName)
    }

    fun buySkin(skinId: String, cost: Int): Boolean {
        return skinShopStateManager.purchaseSkin(skinId, cost)
    }

    // Achievements catalog list
    fun getAchievements(): List<AchievementItem> {
        return listOf(
            AchievementItem("ach_score_10k", "Score 10K", "Reach a total score of 10,000 in a game.", "🏆", saveManager.isAchievementUnlocked("ach_score_10k")),
            AchievementItem("ach_score_50k", "Score 50K", "Reach an amazing score of 50,000 points.", "🎖️", saveManager.isAchievementUnlocked("ach_score_50k")),
            AchievementItem("ach_bricks_100", "Brick Buster", "Destroy a total of 100 breakable bricks.", "🧱", saveManager.isAchievementUnlocked("ach_bricks_100")),
            AchievementItem("ach_bricks_1000", "Brick Overlord", "Destroy 1,000 bricks total across games.", "💥", saveManager.isAchievementUnlocked("ach_bricks_1000")),
            AchievementItem("ach_boss_1", "Sentry Slayer", "Defeat the Level 10 Guardian Giga-Sentry.", "👹", saveManager.isAchievementUnlocked("ach_boss_1")),
            AchievementItem("ach_boss_3", "Volcanic Vanquisher", "Defeat the Level 30 Magma Goliath.", "🌋", saveManager.isAchievementUnlocked("ach_boss_3")),
            AchievementItem("ach_boss_5", "Nexus Eradicator", "Defeat the Level 50 Cyber Core Boss.", "🤖", saveManager.isAchievementUnlocked("ach_boss_5"))
        )
    }

    // Handcrafted Levels definitions
    fun getLevelsList(): List<LevelItem> {
        val maxUnlocked = saveManager.getMaxUnlockedLevel()
        val items = mutableListOf<LevelItem>()
        for (i in 1..50) {
            val stars = saveManager.getLevelStars(i)
            val isBoss = i % 10 == 0
            items.add(
                LevelItem(
                    levelNumber = i,
                    stars = stars,
                    isUnlocked = i <= maxUnlocked,
                    isBoss = isBoss
                )
            )
        }
        return items
    }

    // Statistics definitions
    fun getStats(): Map<String, String> {
        return mapOf(
            "Games Played" to saveManager.getGamesPlayed().toString(),
            "Total Bricks Destroyed" to saveManager.getBricksDestroyed().toString(),
            "Max Combo Multiplier" to "${saveManager.getMaxCombo()}X",
            "Current Level Rank" to "Level ${saveManager.getLevel()}",
            "Current XP Progress" to "${saveManager.getXP()} / ${saveManager.getXPNeededForNextLevel()}"
        )
    }

    // Daily rewards handling
    fun claimDailyReward(): Int? {
        val lastClaim = saveManager.getLastDailyRewardTime()
        val current = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        if (current - lastClaim >= dayInMillis) {
            saveManager.claimDailyReward()
            val bonusCoins = 100
            skinShopStateManager.earnCoins(bonusCoins)
            return bonusCoins
        }
        return null // Already claimed today
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopMusic()
    }
}

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean
)

data class LevelItem(
    val levelNumber: Int,
    val stars: Int,
    val isUnlocked: Boolean,
    val isBoss: Boolean
)
