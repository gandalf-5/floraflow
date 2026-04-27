package com.floraflow.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.floraflow.app.BuildConfig
import kotlinx.coroutines.*

/**
 * Singleton that wraps Google Play Billing for FloraFlow's monthly subscription.
 *
 * In DEBUG builds the billing client is never started — premium is already
 * unlocked via BuildConfig.DEBUG in PreferencesManager.
 *
 * Product ID must match exactly what is created in Google Play Console:
 *   floraflow_premium_monthly  (subscription, monthly, 1.99 €)
 */
class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "BillingManager"
        const val SUB_ID = "floraflow_premium_monthly"

        @Volatile private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    sealed class PurchaseState {
        object Unknown  : PurchaseState()
        object Premium  : PurchaseState()
        object Free     : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    private val _purchaseState = MutableLiveData<PurchaseState>(PurchaseState.Unknown)
    val purchaseState: LiveData<PurchaseState> = _purchaseState

    private val _subDetails = MutableLiveData<ProductDetails?>()
    val subDetails: LiveData<ProductDetails?> = _subDetails

    init {
        if (!BuildConfig.DEBUG) connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    scope.launch {
                        queryExistingPurchases()
                        queryProductDetails()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected — will retry on next operation")
            }
        })
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        val activePurchase = result.purchasesList.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(SUB_ID)
        }
        if (activePurchase != null) {
            acknowledgeIfNeeded(activePurchase)
        }
        val isPremium = activePurchase != null
        PreferencesManager(context).setIsPremium(isPremium)
        withContext(Dispatchers.Main) {
            _purchaseState.value = if (isPremium) PurchaseState.Premium else PurchaseState.Free
        }
    }

    private suspend fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()
        val result = billingClient.queryProductDetails(params)
        withContext(Dispatchers.Main) {
            _subDetails.value = result.productDetailsList?.firstOrNull()
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val details = _subDetails.value ?: run {
            Log.w(TAG, "Product details not yet loaded — is the subscription created in Play Console?")
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            ))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases
                    ?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    ?.forEach { purchase ->
                        scope.launch {
                            acknowledgeIfNeeded(purchase)
                            PreferencesManager(context).setIsPremium(true)
                            withContext(Dispatchers.Main) {
                                _purchaseState.value = PurchaseState.Premium
                            }
                        }
                    }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.d(TAG, "Purchase cancelled by user")
            else ->
                Log.w(TAG, "Purchase error ${result.responseCode}: ${result.debugMessage}")
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
        }
    }
}
