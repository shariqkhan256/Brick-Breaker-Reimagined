package com.example.ui

import android.graphics.Color
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.GameEngine
import com.example.game.GameView

// Cyber Theme Colors
val DarkCanvas = ComposeColor(0xFF0D0E15)
val CyberPink = ComposeColor(0xFFFF007F)
val NeonGreen = ComposeColor(0xFF00E676)
val ElectricCyan = ComposeColor(0xFF00E5FF)
val GoldElite = ComposeColor(0xFFFFD700)
val SlateGray = ComposeColor(0xFF1E2030)
val DarkOverlay = ComposeColor(0xCC05050A)

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "Splash" -> SplashScreen(onFinished = { viewModel.navigateTo("MainMenu") })
                "MainMenu" -> MainMenuScreen(viewModel)
                "Play" -> PlayScreen(viewModel)
                "LevelSelect" -> LevelSelectScreen(viewModel)
                "Shop" -> ShopScreen(viewModel)
                "Achievements" -> AchievementsScreen(viewModel)
                "Stats" -> StatsScreen(viewModel)
                "Settings" -> SettingsScreen(viewModel)
                "GameOver" -> GameOverScreen(viewModel)
                "Victory" -> VictoryScreen(viewModel)
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(2000, easing = LinearEasing),
        label = "ProgressBar"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        kotlinx.coroutines.delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas),
        contentAlignment = Alignment.Center
    ) {
        // Floating ambient particle circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.08f),
                radius = 300f,
                center = Offset(size.width * 0.2f, size.height * 0.2f)
            )
            drawCircle(
                color = CyberPink.copy(alpha = 0.08f),
                radius = 400f,
                center = Offset(size.width * 0.8f, size.height * 0.7f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideogameAsset,
                contentDescription = "Arcade Logo",
                tint = ElectricCyan,
                modifier = Modifier
                    .size(90.dp)
                    .scale(if (progress > 0.1f) 1f else 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BRICK BREAKER\nREIMAGINED",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = CyberPink,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ULTIMATE ARCADE EXPERIENCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElectricCyan,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(60.dp))
            
            // Loading Progress Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CyberPink,
                    trackColor = SlateGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "GET READY...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val coins by viewModel.coins.collectAsState()
    val level by viewModel.level.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val highScore by viewModel.highScore.collectAsState()

    // Handlers
    val onClaimRewardClick = {
        val bonus = viewModel.claimDailyReward()
        if (bonus != null) {
            Toast.makeText(context, "🎁 Claimed Today! +$bonus Coins!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Already claimed today! Check back tomorrow.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Player Dashboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateGray)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CyberPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LV\n$level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = ComposeColor.White,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "RANKED HERO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = "XP: $xp pts",
                        fontSize = 12.sp,
                        color = ComposeColor.White.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = GoldElite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = coins.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White
                )
            }
        }

        // 2. High Score Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGray.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PERSONAL HIGH RECORD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = highScore.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            shadow = Shadow(color = ElectricCyan.copy(alpha = 0.4f), blurRadius = 4f)
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Record Icon",
                    tint = GoldElite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 3. Central Giant PLAY Button
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberPink, DarkCanvas),
                        radius = 260f
                    )
                )
                .clickable {
                    // Start from the highest completed level
                    val startLvl = viewModel.gameEngine.saveManager.getMaxUnlockedLevel()
                    viewModel.gameEngine.initGame(startLvl, GameEngine.GameMode.CLASSIC)
                    viewModel.navigateTo("Play")
                }
                .testTag("submit_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(CyberPink)
                    .border(4.dp, ComposeColor.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Arrow",
                    tint = ComposeColor.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // 4. Daily Reward and Zen quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onClaimRewardClick,
                colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = "Reward", tint = CyberPink)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Daily Gift", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    viewModel.gameEngine.initGame(1, GameEngine.GameMode.ZEN)
                    viewModel.navigateTo("Play")
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Spa, contentDescription = "Zen Mode", tint = ElectricCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Zen Play", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 5. Grid of categories (Shop, Level Select, Achievements, Stats, Settings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuGridItem(
                    title = "LEVELS",
                    icon = Icons.Default.GridOn,
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("LevelSelect") }
                )
                MenuGridItem(
                    title = "STORE",
                    icon = Icons.Default.ShoppingBag,
                    color = CyberPink,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("Shop") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuGridItem(
                    title = "AWARDS",
                    icon = Icons.Default.EmojiEvents,
                    color = GoldElite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("Achievements") }
                )
                MenuGridItem(
                    title = "STATS",
                    icon = Icons.Default.Assessment,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("Stats") }
                )
                MenuGridItem(
                    title = "SETTINGS",
                    icon = Icons.Default.Settings,
                    color = ComposeColor.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("Settings") }
                )
            }
        }
    }
}

@Composable
fun MenuGridItem(
    title: String,
    icon: ImageVector,
    color: ComposeColor,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGray)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ComposeColor.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PlayScreen(viewModel: GameViewModel) {
    val score by viewModel.liveScore.collectAsState()
    val combo by viewModel.liveCombo.collectAsState()
    val lives by viewModel.liveLives.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val currentLevel = viewModel.gameEngine.level
    val gameModeName = viewModel.gameEngine.mode.name
    
    // Timer checks
    val isShieldActive = viewModel.gameEngine.shieldActive
    val laserLeft = viewModel.gameEngine.laserPaddleDuration

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Custom Canvas view hosting the engine
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GameView(ctx).apply {
                    setEngine(viewModel.gameEngine)
                    
                    // Hook engine callback to Compose
                    onGameStateChanged = { st, s, coins, xp, stars ->
                        viewModel.setGameplayState(st, s, coins, xp, stars)
                    }
                    onScoreUpdated = { s, c, l ->
                        viewModel.updateLiveHud(s, c, l)
                    }
                }
            }
        )

        // Game HUD overlays (Score, Lives, Level counters)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lives Hearts Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lives < 10) {
                        repeat(lives) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart",
                                tint = CyberPink,
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart",
                            tint = CyberPink,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "X$lives",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ComposeColor.White
                        )
                    }
                }

                // Level / Mode Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEVEL $currentLevel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ComposeColor.White
                    )
                    Text(
                        text = gameModeName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )
                }

                // Pause Control Button
                IconButton(
                    onClick = {
                        viewModel.gameEngine.pauseGame()
                        viewModel.setGameplayState(GameEngine.GameState.PAUSED, 0, 0, 0, 0)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(SlateGray, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = ComposeColor.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score and active Combos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = ComposeColor.White
                    )
                }

                // Combo badge with animated enter transition
                AnimatedVisibility(
                    visible = combo > 1,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberPink)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMBO ${combo}X",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = ComposeColor.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active timers/shields indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isShieldActive) {
                    ShieldBadge(label = "SHIELD ACTIVE", color = ElectricCyan)
                }
                if (laserLeft > 0f) {
                    ShieldBadge(label = "LASER PADDLE", color = CyberPink)
                }
            }
        }

        if (gameState == GameEngine.GameState.PAUSED) {
            PauseMenuOverlay(
                viewModel = viewModel,
                onResume = {
                    viewModel.gameEngine.resumeGame()
                    viewModel.setGameplayState(viewModel.gameEngine.state, 0, 0, 0, 0)
                },
                onRestart = {
                    viewModel.gameEngine.initGame(viewModel.gameEngine.level, viewModel.gameEngine.mode)
                    viewModel.setGameplayState(GameEngine.GameState.READY, 0, 0, 0, 0)
                },
                onQuit = {
                    viewModel.gameEngine.state = GameEngine.GameState.READY
                    viewModel.setGameplayState(GameEngine.GameState.READY, 0, 0, 0, 0)
                    viewModel.navigateTo("MainMenu")
                }
            )
        }
    }
}

@Composable
fun ShieldBadge(label: String, color: ComposeColor) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun PauseMenuOverlay(
    viewModel: GameViewModel,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val vibeEnabled by viewModel.vibeEnabled.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkOverlay)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume touch events underneath
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .border(2.dp, ElectricCyan, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCanvas)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "GAME PAUSED",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberPink,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(color = CyberPink.copy(alpha = 0.5f), offset = Offset(2f, 2f), blurRadius = 4f)
                    )
                )

                Text(
                    text = "LEVEL ${viewModel.gameEngine.level} • ${viewModel.gameEngine.mode.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options stacked
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPink)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                }

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGray)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART LEVEL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                }

                Button(
                    onClick = onQuit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGray.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Quit", tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUIT TO MENU", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "QUICK SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingToggleRow(
                        label = "SOUND FX",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.toggleSound() },
                        icon = Icons.Default.VolumeUp
                    )
                    SettingToggleRow(
                        label = "MUSIC",
                        checked = musicEnabled,
                        onCheckedChange = { viewModel.toggleMusic() },
                        icon = Icons.Default.MusicNote
                    )
                    SettingToggleRow(
                        label = "VIBRATION",
                        checked = vibeEnabled,
                        onCheckedChange = { viewModel.toggleVibe() },
                        icon = Icons.Default.Vibration
                    )
                }
            }
        }
    }
}

@Composable
fun LevelSelectScreen(viewModel: GameViewModel) {
    val levels = viewModel.getLevelsList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MainMenu") },
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Text(
                text = "LEVEL SELECT",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(48.dp)) // Equalizer space
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of levels
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(levels) { item ->
                LevelGridCard(item, onClick = {
                    if (item.isUnlocked) {
                        viewModel.gameEngine.initGame(item.levelNumber, GameEngine.GameMode.CLASSIC)
                        viewModel.navigateTo("Play")
                    }
                })
            }
        }
    }
}

@Composable
fun LevelGridCard(item: LevelItem, onClick: () -> Unit) {
    val alpha = if (item.isUnlocked) 1f else 0.4f
    val background = if (item.isBoss) CyberPink.copy(alpha = 0.2f) else SlateGray
    val borderColor = if (item.isBoss) CyberPink else if (item.isUnlocked) ElectricCyan else ComposeColor.Gray

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = item.isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = alpha))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!item.isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ComposeColor.Gray, modifier = Modifier.size(20.dp))
            } else {
                if (item.isBoss) {
                    Icon(Icons.Default.Dangerous, contentDescription = "Boss", tint = CyberPink, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = item.levelNumber.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stars row
                Row {
                    repeat(3) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = if (index < item.stars) GoldElite else ComposeColor.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val coins by viewModel.coins.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MainMenu") },
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Text(
                text = "SKIN EMPORIUM",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            // Coin balance display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateGray)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = GoldElite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(coins.toString(), color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCanvas,
            contentColor = ElectricCyan
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("PADDLES", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 0) CyberPink else ComposeColor.White)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("BALLS", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 1) CyberPink else ComposeColor.White)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("THEMES", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 2) CyberPink else ComposeColor.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        val itemsList = when (selectedTab) {
            0 -> listOf(
                ShopItemData("Default", "Standard Green skin", 0, "Emerald Guardian"),
                ShopItemData("Neon Blue", "Futuristic light blue aura", 100, "Neon Blue"),
                ShopItemData("Cyber Pink", "Striking pink neon beam", 200, "Cyber Pink"),
                ShopItemData("Gold Elite", "Prestigious luxury gold bars", 500, "Gold Elite"),
                ShopItemData("Volcano Red", "Molten lava paddle", 350, "Volcano Red")
            )
            1 -> listOf(
                ShopItemData("Default", "Original white sphere", 0, "Default"),
                ShopItemData("Plasma", "Hot magenta energy core", 150, "Plasma"),
                ShopItemData("Comet Glow", "Icy blue comet sparkle", 250, "Comet Glow"),
                ShopItemData("Fireball", "Enraged flame sphere", 400, "Fireball"),
                ShopItemData("Disco", "Flickering purple dance ball", 300, "Disco")
            )
            else -> listOf(
                ShopItemData("Forest", "Lush woodland design", 0, "Forest"),
                ShopItemData("Space", "Infinite stellar starlight", 200, "Space"),
                ShopItemData("Volcano", "Searing dynamic lava streams", 300, "Volcano"),
                ShopItemData("Ice Cave", "Glistening frozen prisms", 400, "Ice Cave"),
                ShopItemData("Cyber City", "Electrified synth grid lines", 500, "Cyber City"),
                ShopItemData("Ancient Temple", "Sacred structures of gold", 600, "Ancient Temple")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(itemsList) { item ->
                val isUnlocked = viewModel.isSkinUnlocked(item.id)
                val isActive = when (selectedTab) {
                    0 -> viewModel.activePaddleSkin.value == item.id
                    1 -> viewModel.activeBallSkin.value == item.id
                    else -> viewModel.activeTheme.value == item.id
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            Text(item.description, fontSize = 12.sp, color = ComposeColor.White.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = {
                                if (isUnlocked) {
                                    when (selectedTab) {
                                        0 -> viewModel.selectPaddleSkin(item.id)
                                        1 -> viewModel.selectBallSkin(item.id)
                                        else -> viewModel.selectTheme(item.id)
                                    }
                                } else {
                                    val success = viewModel.buySkin(item.id, item.cost)
                                    if (success) {
                                        Toast.makeText(context, "🛒 Unlocked ${item.title}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ Insufficient Coins!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) NeonGreen else if (isUnlocked) ElectricCyan else CyberPink
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isActive) {
                                Icon(Icons.Default.Check, contentDescription = "Equipped", tint = ComposeColor.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Equipped", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else if (isUnlocked) {
                                Text("Equip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = GoldElite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${item.cost}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ShopItemData(val id: String, val title: String, val cost: Int, val description: String)

@Composable
fun AchievementsScreen(viewModel: GameViewModel) {
    val items = viewModel.getAchievements()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MainMenu") },
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Text(
                text = "ACHIEVEMENTS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                val alpha = if (item.isUnlocked) 1f else 0.4f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGray.copy(alpha = alpha))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (item.isUnlocked) GoldElite.copy(alpha = 0.2f) else SlateGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.iconEmoji, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (item.isUnlocked) GoldElite else ComposeColor.White)
                            Text(item.description, fontSize = 12.sp, color = ComposeColor.White.copy(alpha = 0.6f))
                        }

                        if (item.isUnlocked) {
                            Icon(Icons.Default.Verified, contentDescription = "Unlocked", tint = GoldElite, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ComposeColor.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(viewModel: GameViewModel) {
    val stats = viewModel.getStats()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MainMenu") },
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Text(
                text = "PLAYER STATISTICS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                stats.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(key, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.7f))
                        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ElectricCyan)
                    }
                    HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val vibeEnabled by viewModel.vibeEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MainMenu") },
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Text(
                text = "SETTINGS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sound FX Row
                SettingToggleRow(
                    label = "SOUND FX SFX",
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.toggleSound() },
                    icon = Icons.Default.VolumeUp
                )
                HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.08f))

                // Music Row
                SettingToggleRow(
                    label = "BACKGROUND MUSIC",
                    checked = musicEnabled,
                    onCheckedChange = { viewModel.toggleMusic() },
                    icon = Icons.Default.MusicNote
                )
                HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.08f))

                // Vibration Row
                SettingToggleRow(
                    label = "HAPTIC VIBRATIONS",
                    checked = vibeEnabled,
                    onCheckedChange = { viewModel.toggleVibe() },
                    icon = Icons.Default.Vibration
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Credits Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateGray.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("BRICK BREAKER REIMAGINED", fontWeight = FontWeight.Bold, color = CyberPink, fontSize = 13.sp)
                Text("Version 1.0.0 (Build 42)", color = ComposeColor.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Engine, design, and graphics implemented entirely in Jetpack Compose and custom Canvas architecture.", color = ComposeColor.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = ElectricCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ComposeColor.White,
                checkedTrackColor = CyberPink,
                uncheckedThumbColor = ComposeColor.Gray,
                uncheckedTrackColor = DarkCanvas
            )
        )
    }
}

@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val score = viewModel.lastGameScore

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkOverlay),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Dangerous,
                contentDescription = "Failed Logo",
                tint = CyberPink,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PADDLE DESTROYED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = CyberPink,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(color = CyberPink.copy(alpha = 0.4f), blurRadius = 8f)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "THE BALL FELL BELOW THE DEFENSE FIELD",
                fontSize = 12.sp,
                color = ComposeColor.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Score Summary card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FINAL SCORE", fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text(
                        text = score.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val currentLevel = viewModel.gameEngine.level
                        viewModel.gameEngine.initGame(currentLevel, viewModel.gameEngine.mode)
                        viewModel.navigateTo("Play")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETRY", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo("MainMenu") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MENU", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VictoryScreen(viewModel: GameViewModel) {
    val score = viewModel.lastGameScore
    val coins = viewModel.lastGameCoins
    val xp = viewModel.lastGameXP
    val stars = viewModel.lastGameStars

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkOverlay),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Victory Logo",
                tint = NeonGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "LEVEL COMPLETE",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = NeonGreen,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(color = NeonGreen.copy(alpha = 0.4f), blurRadius = 8f)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Star Rating row with animation scale
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val color = if (index < stars) GoldElite else ComposeColor.White.copy(alpha = 0.15f)
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star Indicator",
                        tint = color,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (index < stars) 1.15f else 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Reward Summary Box
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL SCORE GATHERED", fontSize = 10.sp, color = ComposeColor.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Text(score.toString(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                    }

                    HorizontalDivider(color = ComposeColor.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS REWARD", fontSize = 10.sp, color = ComposeColor.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = GoldElite, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+$coins", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("XP GAINED", fontSize = 10.sp, color = ComposeColor.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = "XP", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+$xp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val nextLvl = viewModel.gameEngine.level + 1
                        val clampedNext = if (nextLvl > 50) 1 else nextLvl
                        viewModel.gameEngine.initGame(clampedNext, viewModel.gameEngine.mode)
                        viewModel.navigateTo("Play")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NavigateNext, contentDescription = "Next Level", tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEXT LEVEL", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.navigateTo("MainMenu") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MENU", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
