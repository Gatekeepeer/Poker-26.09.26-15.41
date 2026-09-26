package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.poker.billing.RuStoreBillingManager
import com.example.poker.ui.MainScreen
import com.example.poker.ui.theme.PokerReplayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize RuStore Billing SDK
        RuStoreBillingManager.init(this)

        setContent {
            PokerReplayerTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle RuStore billing redirect deeplink
        RuStoreBillingManager.onNewIntent(intent)
    }
}
