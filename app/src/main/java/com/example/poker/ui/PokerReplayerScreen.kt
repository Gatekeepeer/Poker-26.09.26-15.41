package com.example.poker.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.R
import com.example.poker.model.HandFilter
import com.example.poker.model.PlayerReplayState
import com.example.poker.model.PokerHand
import com.example.poker.ui.components.HandListBottomSheet
import com.example.poker.ui.components.LandscapeReplayerView
import com.example.poker.ui.components.PaywallDialog
import com.example.poker.ui.components.PokerTableView
import com.example.poker.ui.components.PrivacyPolicyDialog
import com.example.poker.ui.components.ReplayerControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokerReplayerScreen(
    viewModel: PokerReplayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // System File Picker (SAF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            var fileName = "История турнира.txt"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Ignore cursor reading fallback
            }
            viewModel.loadFromUri(uri, fileName)
        }
    }

    // Display error snackbar if error happens
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Логотип Разбор рук",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "Разбор рук",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Анализ покерных турниров",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Демо-режим
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24)
                        )
                    },
                    label = {
                        Text(
                            text = "Демо-режим",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.loadSampleTournament(restoreState = false)
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_demo_mode_button"),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                // 2. Выбрать турнир
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    },
                    label = {
                        Text(
                            text = "Выбрать турнир",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        filePickerLauncher.launch(arrayOf("text/plain", "text/xml", "application/xml", "*/*"))
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_select_tournament_button"),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                // 3. Купить полный доступ
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFACC15)
                        )
                    },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Купить полный доступ",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.isFullAccessPurchased) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF16A34A).copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.showPaywall()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_buy_pro_button"),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                // 4. Политика конфиденциальности
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    label = {
                        Text(
                            text = "Политика конфиденциальности",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.showPrivacyPolicy()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_privacy_policy_button"),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Разбор рук v1.0",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Автономная обработка файлов",
                        color = Color(0xFF475569),
                        fontSize = 10.sp
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("poker_replayer_screen"),
            containerColor = Color(0xFF090D16), // Dark premium casino aesthetic
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isLandscape) {
                    LandscapeCompactTopBar(
                        fileName = uiState.fileName,
                        currentHand = uiState.currentHand,
                        currentHandIndex = uiState.currentHandIndex,
                        totalHands = uiState.hands.size,
                        useBbUnits = uiState.useBbUnits,
                        isFullAccessPurchased = uiState.isFullAccessPurchased,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onToggleUnits = { viewModel.toggleUseBbUnits() },
                        onOpenPaywall = { viewModel.showPaywall() }
                    )
                } else {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Меню",
                                    tint = Color.White
                                )
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = "Разбор рук",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = uiState.fileName ?: "Турнирные раздачи",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        actions = {
                            // Toggle BB / Chips display unit
                            TextButton(
                                onClick = { viewModel.toggleUseBbUnits() },
                                modifier = Modifier.testTag("toggle_units_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (uiState.useBbUnits) Color(0xFF0284C7) else Color(0xFF334155))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (uiState.useBbUnits) "BB" else "Фишки",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Unlock PRO Full Access button (if not purchased)
                            if (!uiState.isFullAccessPurchased) {
                                IconButton(
                                    onClick = { viewModel.showPaywall() },
                                    modifier = Modifier.testTag("open_paywall_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Полный доступ (31+ раздач)",
                                        tint = Color(0xFFFACC15)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Парсинг истории турнира...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (uiState.hands.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Файл истории раздач не выбран",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Выберите файл истории раздач турнира (GGPoker TXT или RedStar / iPoker XML) или загрузите демонстрационный турнир.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Button(
                        onClick = { filePickerLauncher.launch(arrayOf("text/plain", "text/xml", "application/xml", "*/*")) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        )
                    ) {
                        Text("Выбрать файл (TXT / XML)")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.loadSampleTournament(restoreState = false) }
                    ) {
                        Text("Загрузить демо-турнир")
                    }
                }
            } else {
                val currentHand = uiState.currentHand
                val currentStep = uiState.currentStepState

                if (currentHand != null && currentStep != null) {
                    if (isLandscape) {
                        // Horizontal variant (landscape) - dedicated un-cramped view
                        LandscapeReplayerView(
                            currentHand = currentHand,
                            currentStepState = currentStep,
                            handIndex = uiState.currentHandIndex,
                            totalHands = uiState.hands.size,
                            isPlaying = uiState.isPlaying,
                            useBbUnits = uiState.useBbUnits,
                            activeFilter = uiState.activeFilter,
                            selectedPositions = uiState.selectedPositions,
                            filteredCurrentIndex = uiState.filteredCurrentIndex,
                            totalFilteredHands = uiState.totalFilteredHands,
                            onStepBack = { viewModel.stepBackward() },
                            onStepForward = { viewModel.stepForward() },
                            onStepTo = { viewModel.jumpToStep(it) },
                            onPrevHand = { viewModel.prevHand() },
                            onNextHand = { viewModel.nextHand() },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onOpenHandList = { viewModel.setHandListVisible(true) },
                            onClearFilter = { viewModel.clearFilter() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Vertical variant (portrait) - full width navigation and full width "Раздача 1 из 6" below
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Hand Info Header Strip (compact, minimal spacing)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF131D2E)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    val tableNamePart = if (currentHand.tableNumber.isNotBlank()) " - Стол ${currentHand.tableNumber}" else ""
                                    Text(
                                        text = "${currentHand.tournamentName}$tableNamePart",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sbStr = PlayerReplayState.formatChipCount(currentHand.smallBlind)
                                    val bbStr = PlayerReplayState.formatChipCount(currentHand.bigBlind)
                                    val anteStr = if (currentHand.ante > 0L) ", анте ${PlayerReplayState.formatChipCount(currentHand.ante)}" else ""
                                    Text(
                                        text = "Уровень ${currentHand.levelNumber} ($sbStr / $bbStr$anteStr)",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Poker Table
                            PokerTableView(
                                stepState = currentStep,
                                bigBlindChips = currentHand.bigBlind,
                                useBbUnits = uiState.useBbUnits
                            )

                            // Replayer Controls (full width navigation, full width 'Раздача 1 из 6' below)
                            ReplayerControls(
                                currentHand = currentHand,
                                currentStepState = currentStep,
                                handIndex = uiState.currentHandIndex,
                                totalHands = uiState.hands.size,
                                isPlaying = uiState.isPlaying,
                                activeFilter = uiState.activeFilter,
                                selectedPositions = uiState.selectedPositions,
                                filteredCurrentIndex = uiState.filteredCurrentIndex,
                                totalFilteredHands = uiState.totalFilteredHands,
                                onStepBack = { viewModel.stepBackward() },
                                onStepForward = { viewModel.stepForward() },
                                onStepTo = { viewModel.jumpToStep(it) },
                                onPrevHand = { viewModel.prevHand() },
                                onNextHand = { viewModel.nextHand() },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onOpenHandList = { viewModel.setHandListVisible(true) },
                                onClearFilter = { viewModel.clearFilter() }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Hand List Bottom Sheet
            if (uiState.isHandListVisible) {
                HandListBottomSheet(
                    sheetState = sheetState,
                    hands = uiState.hands,
                    selectedIndex = uiState.currentHandIndex,
                    activeFilter = uiState.activeFilter,
                    selectedPositions = uiState.selectedPositions,
                    isFullAccessPurchased = uiState.isFullAccessPurchased,
                    onSelectFilter = { viewModel.setFilter(it) },
                    onTogglePosition = { viewModel.togglePositionFilter(it) },
                    onClearPositions = { viewModel.clearPositionFilter() },
                    onSelectAllPositions = { viewModel.selectAllPositions(it) },
                    onSelectHand = { idx ->
                        viewModel.selectHand(idx)
                    },
                    onDismiss = {
                        viewModel.setHandListVisible(false)
                    }
                )
            }

            // Paywall Dialog (30 free hands limit / RuStore purchase)
            if (uiState.isPaywallVisible) {
                val context = LocalContext.current
                val activity = context as? Activity
                PaywallDialog(
                    isPurchasing = uiState.isPurchasing,
                    isRuStoreInstalled = uiState.isRuStoreInstalled,
                    statusMessage = uiState.purchaseStatusMessage,
                    onPurchase = {
                        activity?.let { viewModel.purchaseFullAccess(it) }
                    },
                    onRestore = {
                        viewModel.restorePurchases(context)
                    },
                    onDismiss = {
                        viewModel.hidePaywall()
                    }
                )
            }

            // Privacy Policy & Disclaimer Dialog (RuStore compliance requirement)
            if (uiState.isPrivacyPolicyVisible) {
                PrivacyPolicyDialog(
                    onDismiss = {
                        viewModel.hidePrivacyPolicy()
                    }
                )
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeCompactTopBar(
    fileName: String?,
    currentHand: PokerHand?,
    currentHandIndex: Int,
    totalHands: Int,
    useBbUnits: Boolean,
    isFullAccessPurchased: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleUnits: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hamburger menu + Title + Tournament / Blinds / Hand info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Меню",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Разбор рук",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                if (currentHand != null) {
                    val tableNamePart = if (currentHand.tableNumber.isNotBlank()) " • Стол ${currentHand.tableNumber}" else ""
                    val tourneyTitle = currentHand.tournamentName.ifBlank { fileName ?: "Турнир" } + tableNamePart
                    Text(
                        text = " • ",
                        color = Color(0xFF475569),
                        fontSize = 11.sp
                    )
                    Text(
                        text = tourneyTitle,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val sbStr = PlayerReplayState.formatChipCount(currentHand.smallBlind)
                    val bbStr = PlayerReplayState.formatChipCount(currentHand.bigBlind)
                    val anteStr = if (currentHand.ante > 0L) ", анте ${PlayerReplayState.formatChipCount(currentHand.ante)}" else ""
                    Text(
                        text = " • ",
                        color = Color(0xFF475569),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Ур. ${currentHand.levelNumber} ($sbStr / $bbStr$anteStr)",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    if (totalHands > 0) {
                        Text(
                            text = " • ",
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Раздача ${currentHandIndex + 1}/$totalHands",
                            color = Color(0xFFF1F5F9),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else if (!fileName.isNullOrBlank()) {
                    Text(
                        text = " • $fileName",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Toggle BB / Chips + Pro Star
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // BB / Chips toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (useBbUnits) Color(0xFF0284C7) else Color(0xFF334155))
                        .clickable { onToggleUnits() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .testTag("toggle_units_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (useBbUnits) "BB" else "Фишки",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (!isFullAccessPurchased) {
                    IconButton(
                        onClick = onOpenPaywall,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("open_paywall_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Купить полный доступ",
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

