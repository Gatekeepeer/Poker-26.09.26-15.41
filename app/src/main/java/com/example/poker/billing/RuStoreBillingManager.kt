package com.example.poker.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.poker.data.ReplayerPreferences
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory
import ru.rustore.sdk.billingclient.model.purchase.PaymentResult
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState

object RuStoreBillingManager {

    private const val TAG = "RuStoreBilling"
    
    // Product ID configured in RuStore Console for lifetime full access unlock
    const val PRODUCT_ID_FULL_ACCESS = "full_access_unlock"
    
    // Deeplink scheme for banking applications (SBP/T-Pay/Mir) redirect back to app
    const val DEEPLINK_SCHEME = "ru.mazitov.poker.replayer"

    // RuStore Console Application ID placeholder (configure in RuStore developer console)
    private const val DEFAULT_CONSOLE_APP_ID = "poker_replayer_rustore"

    private var billingClient: RuStoreBillingClient? = null
    private var isInitialized = false

    fun isBillingAvailable(context: Context): Boolean {
        return isRuStoreInstalled(context) && billingClient != null
    }

    fun isRuStoreInstalled(context: Context): Boolean {
        val pm = context.packageManager
        val packages = listOf("ru.vk.store", "com.vk.store", "ru.rustore")
        return packages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun init(context: Context, consoleAppId: String = DEFAULT_CONSOLE_APP_ID) {
        if (isInitialized) return
        try {
            billingClient = RuStoreBillingClientFactory.create(
                context = context.applicationContext,
                consoleApplicationId = consoleAppId,
                deeplinkScheme = DEEPLINK_SCHEME
            )
            isInitialized = true
            Log.d(TAG, "RuStoreBillingClient initialized successfully with scheme: $DEEPLINK_SCHEME")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize RuStoreBillingClient: ${e.message}")
        }
    }

    fun onNewIntent(intent: Intent) {
        try {
            billingClient?.onNewIntent(intent)
        } catch (e: Throwable) {
            Log.w(TAG, "Error handling new intent in billing client: ${e.message}")
        }
    }

    /**
     * Checks if the user already purchased "full_access_unlock".
     * Syncs with local preferences.
     */
    fun checkPurchases(
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        val prefs = ReplayerPreferences(context)
        if (prefs.isFullAccessPurchased) {
            onResult(true)
            return
        }

        if (!isRuStoreInstalled(context)) {
            Log.d(TAG, "RuStore is not installed on this device, skipping remote getPurchases")
            onResult(prefs.isFullAccessPurchased)
            return
        }

        val client = billingClient
        if (client == null) {
            onResult(prefs.isFullAccessPurchased)
            return
        }

        try {
            client.purchases.getPurchases()
                .addOnSuccessListener { purchases ->
                    val isPurchased = purchases.any { purchase ->
                        purchase.productId == PRODUCT_ID_FULL_ACCESS &&
                            (purchase.purchaseState == PurchaseState.PAID || purchase.purchaseState == PurchaseState.CONFIRMED)
                    }
                    if (isPurchased) {
                        prefs.isFullAccessPurchased = true
                    }
                    onResult(isPurchased)
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "getPurchases failed: ${error.message}")
                    onResult(prefs.isFullAccessPurchased)
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Error calling getPurchases: ${e.message}")
            onResult(prefs.isFullAccessPurchased)
        }
    }

    /**
     * Initiates purchase of product "full_access_unlock".
     */
    fun purchase(
        activity: Activity,
        productId: String = PRODUCT_ID_FULL_ACCESS,
        onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit
    ) {
        val prefs = ReplayerPreferences(activity)

        if (!isRuStoreInstalled(activity)) {
            Log.w(TAG, "RuStore application is not installed on this device")
            onResult(
                false,
                "Приложение RuStore не установлено на данном устройстве. Установите RuStore или воспользуйтесь тестовой разблокировкой."
            )
            return
        }

        val client = billingClient
        if (client == null) {
            onResult(
                false,
                "Платёжный клиент RuStore не инициализирован. Проверьте подключение к сети."
            )
            return
        }

        try {
            client.purchases.purchaseProduct(
                productId = productId,
                quantity = 1,
                developerPayload = null
            )
                .addOnSuccessListener { paymentResult ->
                    when (paymentResult) {
                        is PaymentResult.Success -> {
                            Log.d(TAG, "Payment successful: purchaseId=${paymentResult.purchaseId}")
                            prefs.isFullAccessPurchased = true
                            onResult(true, null)
                        }
                        is PaymentResult.Cancelled -> {
                            Log.d(TAG, "Payment cancelled by user")
                            onResult(false, "Оплата отменена пользователем")
                        }
                        is PaymentResult.Failure -> {
                            Log.w(TAG, "Payment failure: ${paymentResult.purchaseId}")
                            onResult(false, "Ошибка платежа. Попробуйте снова.")
                        }
                        else -> {
                            Log.d(TAG, "Payment in other state")
                            onResult(false, null)
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "purchaseProduct failed: ${error.message}", error)
                    onResult(false, "Ошибка при оформлении покупки: ${error.localizedMessage ?: "Неизвестная ошибка"}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during purchase: ${e.message}", e)
            onResult(false, "Исключение при покупке: ${e.message}")
        }
    }
}
