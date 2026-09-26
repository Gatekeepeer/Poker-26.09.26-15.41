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
    const val DEEPLINK_SCHEME = "pokerpay"

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
        // If already cached as purchased locally, keep it unlocked
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
                "Сервис оплаты RuStore недоступен на данном устройстве."
            )
            return
        }

        try {
            client.purchases.purchaseProduct(productId = productId)
                .addOnSuccessListener { paymentResult ->
                    when (paymentResult) {
                        is PaymentResult.Success -> {
                            Log.d(TAG, "Purchase succeeded: orderId=${paymentResult.orderId}, invoiceId=${paymentResult.invoiceId}")
                            // Confirm purchase if needed and persist locally
                            try {
                                client.purchases.confirmPurchase(paymentResult.purchaseId)
                            } catch (_: Throwable) {
                                // Ignore confirm errors
                            }
                            prefs.isFullAccessPurchased = true
                            onResult(true, null)
                        }
                        is PaymentResult.Cancelled -> {
                            Log.d(TAG, "Purchase cancelled by user")
                            onResult(false, "Покупка отменена")
                        }
                        is PaymentResult.Failure -> {
                            Log.e(TAG, "Purchase failed: code=${paymentResult.errorCode}")
                            onResult(false, "Ошибка оплаты (код: ${paymentResult.errorCode ?: "ошибка"})")
                        }
                        is PaymentResult.InvalidPaymentState -> {
                            Log.w(TAG, "Purchase invalid payment state")
                            onResult(false, "Некорректное состояние платежа")
                        }
                        else -> {
                            Log.w(TAG, "Unknown payment result: $paymentResult")
                            onResult(false, "Неизвестный результат платежа")
                        }
                    }
                }
                .addOnFailureListener { throwable ->
                    Log.e(TAG, "purchaseProduct exception: ${throwable.message}", throwable)
                    onResult(false, "Ошибка RuStore: ${throwable.message ?: "Не удалось начать оплату"}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception initiating purchase: ${e.message}", e)
            onResult(false, "Исключение при покупке: ${e.message}")
        }
    }

    /**
     * Restore purchases for user (required by store policies).
     */
    fun restorePurchases(
        context: Context,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        val prefs = ReplayerPreferences(context)
        if (prefs.isFullAccessPurchased) {
            onResult(true, "Полный доступ уже активен на данном устройстве.")
            return
        }

        if (!isRuStoreInstalled(context)) {
            onResult(
                false,
                "Приложение RuStore не установлено на данном устройстве."
            )
            return
        }

        val client = billingClient
        if (client == null) {
            onResult(false, "Не удалось связаться с RuStore. Проверьте подключение к сети.")
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
                        onResult(true, "Покупка успешно найдена и восстановлена! Полный доступ открыт.")
                    } else {
                        onResult(false, "Активных покупок для аккаунта RuStore не найдено.")
                    }
                }
                .addOnFailureListener { error ->
                    onResult(false, "Ошибка восстановления: ${error.message ?: "Сервер RuStore недоступен"}")
                }
        } catch (e: Throwable) {
            onResult(false, "Ошибка: ${e.message}")
        }
    }
}
