package com.example.shop

import com.example.save.SaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SkinShopStateManager(private val saveManager: SaveManager) {

    // Coin tracking
    private val _coins = MutableStateFlow(saveManager.getCoins())
    val coins: StateFlow<Int> = _coins.asStateFlow()

    // Skin customization states
    private val _activePaddleSkin = MutableStateFlow(saveManager.getActivePaddleSkin())
    val activePaddleSkin: StateFlow<String> = _activePaddleSkin.asStateFlow()

    private val _activeBallSkin = MutableStateFlow(saveManager.getActiveBallSkin())
    val activeBallSkin: StateFlow<String> = _activeBallSkin.asStateFlow()

    private val _activeTheme = MutableStateFlow(saveManager.getActiveTheme())
    val activeTheme: StateFlow<String> = _activeTheme.asStateFlow()

    // Unlocked skins cache/set
    private val _unlockedSkins = MutableStateFlow<Set<String>>(emptySet())
    val unlockedSkins: StateFlow<Set<String>> = _unlockedSkins.asStateFlow()

    init {
        loadUnlockedSkins()
    }

    /**
     * Re-loads all states from SaveManager to keep local flow state synchronized.
     */
    fun refresh() {
        _coins.value = saveManager.getCoins()
        _activePaddleSkin.value = saveManager.getActivePaddleSkin()
        _activeBallSkin.value = saveManager.getActiveBallSkin()
        _activeTheme.value = saveManager.getActiveTheme()
        loadUnlockedSkins()
    }

    private fun loadUnlockedSkins() {
        val knownSkins = listOf(
            "Default", "Neon Blue", "Cyber Pink", "Gold Elite", "Volcano Red",
            "Plasma", "Comet Glow", "Fireball", "Disco",
            "Forest", "Space", "Volcano", "Ice Cave", "Cyber City", "Ancient Temple"
        )
        val unlocked = knownSkins.filter { saveManager.isSkinUnlocked(it) }.toSet()
        _unlockedSkins.value = unlocked
    }

    /**
     * Earn coins during gameplay or from rewards.
     */
    fun earnCoins(amount: Int) {
        if (amount <= 0) return
        saveManager.addCoins(amount)
        _coins.value = saveManager.getCoins()
    }

    /**
     * Spend coins directly (e.g., custom actions).
     */
    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val success = saveManager.spendCoins(amount)
        if (success) {
            _coins.value = saveManager.getCoins()
        }
        return success
    }

    /**
     * Checks if a specific skin or theme is unlocked.
     */
    fun isSkinUnlocked(skinId: String): Boolean {
        return saveManager.isSkinUnlocked(skinId)
    }

    /**
     * Purchases a skin or theme if the player has enough coins.
     */
    fun purchaseSkin(skinId: String, cost: Int): Boolean {
        if (isSkinUnlocked(skinId)) return true // Already unlocked
        
        if (saveManager.getCoins() >= cost) {
            val spent = saveManager.spendCoins(cost)
            if (spent) {
                saveManager.unlockSkin(skinId)
                _coins.value = saveManager.getCoins()
                loadUnlockedSkins()
                return true
            }
        }
        return false
    }

    /**
     * Selects/equips an unlocked paddle skin.
     */
    fun equipPaddleSkin(skinName: String): Boolean {
        return if (isSkinUnlocked(skinName)) {
            saveManager.setActivePaddleSkin(skinName)
            _activePaddleSkin.value = skinName
            true
        } else {
            false
        }
    }

    /**
     * Selects/equips an unlocked ball skin.
     */
    fun equipBallSkin(skinName: String): Boolean {
        return if (isSkinUnlocked(skinName)) {
            saveManager.setActiveBallSkin(skinName)
            _activeBallSkin.value = skinName
            true
        } else {
            false
        }
    }

    /**
     * Selects/equips an unlocked theme skin.
     */
    fun equipTheme(themeName: String): Boolean {
        return if (isSkinUnlocked(themeName)) {
            saveManager.setActiveTheme(themeName)
            _activeTheme.value = themeName
            true
        } else {
            false
        }
    }
}
