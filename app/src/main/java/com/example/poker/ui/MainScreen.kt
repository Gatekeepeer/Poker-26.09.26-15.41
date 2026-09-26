package com.example.poker.ui

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.billing.RuStoreBillingManager
import com.example.poker.data.DemoTournaments
import com.example.poker.data.ReplayerPreferences
import com.example.poker.engine.ReplayerEngine
import com.example.poker.model.PokerHand
import com.example.poker.model.TournamentData
import com.example.poker.parser.TournamentParser
import com.example.poker.ui.components.HandListDrawer
import com.example.poker.ui.components.PaywallDialog
import com.example.poker.ui.components.PokerTableView
import com.example.poker.ui.components.PrivacyPolicyDialog
import com.example.poker.ui.components.ReplayerControls
import com.example.poker.ui.theme.GoldAccent
import com.example.poker.ui.theme.PokerBgDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val preferences = remember { ReplayerPreferences(context) }
    val engine = remember { ReplayerEngine(scope) }

    var currentTournament by remember { mutableStateOf<TournamentData?>(DemoTournaments.getDemoTournament()) }
    var currentHandIndex by remember { mutableIntStateOf(0) }
    var isFullAccess by remember { mutableStateOf(preferences.isFullAccessPurchased) }
    var displayInBB by remember { mutableStateOf(preferences.isDisplayInBigBlinds) }

    var showPaywallDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseStatusMsg by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentHand by engine.currentHand.collectAsState()
    val currentStepIndex by engine.currentStepIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val playbackSpeedMs by engine.playbackSpeedMs.collectAsState()

    // Load initial hand
    LaunchedEffect(currentTournament) {
        currentTournament?.hands?.firstOrNull()?.let { hand ->
            currentHandIndex = 0
            engine.loadHand(hand)
        }
    }

    // Check purchases with RuStore on launch
    LaunchedEffect(Unit) {
        RuStoreBillingManager.checkPurchases(context) { purchased ->
            if (purchased) {
                isFullAccess = true
            }
        }
    }

    // File Picker for Hand History (.txt / .xml)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        }
                    }
                    if (!content.isNullOrBlank()) {
                        val fileName = uri.lastPathSegment ?: "tournament.txt"
                        val parsed = TournamentParser.parseTournament(content, fileName)
                        if (parsed.hands.isNotEmpty()) {
                            currentTournament = parsed
                            currentHandIndex = 0
                            parsed.hands.firstOrNull()?.let { engine.loadHand(it) }
                            Toast.makeText(context, "Загружено раздач: ${parsed.hands.size}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Не удалось распознать раздачи в файле", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HandListDrawer(
                tournament = currentTournament,
                currentHandIndex = currentHandIndex,
                isFullAccess = isFullAccess,
                freeLimit = preferences.getFreeHandLimit(),
                onSelectHand = { idx ->
                    currentTournament?.hands?.getOrNull(idx)?.let { selectedHand ->
                        currentHandIndex = idx
                        engine.loadHand(selectedHand)
                    }
                },
                onOpenFilePicker = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onOpenPaywall = {
                    showPaywallDialog = true
                },
                onClose = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = PokerBgDark
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top App Bar
                HeaderBar(
                    tournamentName = currentTournament?.name ?: "Разбор рук",
                    displayInBB = displayInBB,
                    isFullAccess = isFullAccess,
                    onToggleBB = {
                        val newVal = !displayInBB
                        displayInBB = newVal
                        preferences.isDisplayInBigBlinds = newVal
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onOpenPaywall = {
                        showPaywallDialog = true
                    },
                    onOpenPrivacy = {
                        showPrivacyDialog = true
                    }
                )

                // Current Hand Meta Bar
                currentHand?.let { hand ->
                    HandMetaHeader(
                        hand = hand,
                        totalHands = currentTournament?.hands?.size ?: 0,
                        currentHandIndex = currentHandIndex,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }

                // Table Replayer Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    currentHand?.let { hand ->
                        PokerTableView(
                            hand = hand,
                            step = engine.currentStep,
                            displayInBB = displayInBB
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Выберите файл истории раздач",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                }

                // Controls
                currentHand?.let { hand ->
                    ReplayerControls(
                        hand = hand,
                        currentStep = engine.currentStep,
                        currentStepIndex = currentStepIndex,
                        isPlaying = isPlaying,
                        playbackSpeedMs = playbackSpeedMs,
                        onPlayPause = { engine.togglePlayPause() },
                        onStepPrev = { engine.stepBackward() },
                        onStepNext = { engine.stepForward() },
                        onJumpStart = { engine.jumpToStart() },
                        onJumpEnd = { engine.jumpToEnd() },
                        onJumpStreet = { street -> engine.jumpToStreet(street) },
                        onSpeedChange = { speed -> engine.setPlaybackSpeed(speed) }
                    )
                }
            }
        }
    }

    // Paywall Dialog
    if (showPaywallDialog) {
        PaywallDialog(
            isPurchasing = isPurchasing,
            isRuStoreInstalled = RuStoreBillingManager.isRuStoreInstalled(context),
            statusMessage = purchaseStatusMsg,
            onPurchase = {
                if (activity != null) {
                    isPurchasing = true
                    purchaseStatusMsg = "Инициализация покупки..."
                    RuStoreBillingManager.purchase(activity) { success, errorMsg ->
                        isPurchasing = false
                        if (success) {
                            isFullAccess = true
                            showPaywallDialog = false
                            Toast.makeText(context, "Полный доступ успешно активирован!", Toast.LENGTH_LONG).show()
                        } else {
                            purchaseStatusMsg = errorMsg ?: "Покупка не завершена"
                        }
                    }
                }
            },
            onRestore = {
                isPurchasing = true
                purchaseStatusMsg = "Проверка покупок в RuStore..."
                RuStoreBillingManager.checkPurchases(context) { purchased ->
                    isPurchasing = false
                    if (purchased) {
                        isFullAccess = true
                        showPaywallDialog = false
                        Toast.makeText(context, "Покупки успешно восстановлены!", Toast.LENGTH_SHORT).show()
                    } else {
                        purchaseStatusMsg = "Активных покупок не найдено"
                    }
                }
            },
            onDismiss = {
                showPaywallDialog = false
                purchaseStatusMsg = null
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@Composable
private fun HeaderBar(
    tournamentName: String,
    displayInBB: Boolean,
    isFullAccess: Boolean,
    onToggleBB: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(36.dp).testTag("btn_menu")) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Разбор рук",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tournamentName,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // BB / Chips Toggle
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (displayInBB) Color(0xFF0284C7) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (displayInBB) Color(0xFF38BDF8) else Color(0xFF475569)),
                    modifier = Modifier
                        .clickable { onToggleBB() }
                        .testTag("btn_toggle_bb")
                ) {
                    Text(
                        text = if (displayInBB) "BB" else "Фишки",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Privacy Policy Button
                IconButton(onClick = onOpenPrivacy, modifier = Modifier.size(32.dp).testTag("btn_privacy")) {
                    Icon(Icons.Default.Shield, contentDescription = "Политика", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }

                // Premium Crown Button
                if (!isFullAccess) {
                    IconButton(onClick = onOpenPaywall, modifier = Modifier.size(32.dp).testTag("btn_paywall")) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "Премиум", tint = GoldAccent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HandMetaHeader(
    hand: PokerHand,
    totalHands: Int,
    currentHandIndex: Int,
    onOpenDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${hand.tableName} • ${hand.levelText}",
                color = Color(0xFF38BDF8),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.clickable { onOpenDrawer() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Раздача ${currentHandIndex + 1} из $totalHands",
                        color = GoldAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
