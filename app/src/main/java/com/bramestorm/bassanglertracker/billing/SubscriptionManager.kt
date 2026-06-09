package com.bramestorm.bassanglertracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * SubscriptionManager.kt
 *
 * Google Play Billing manager for subscription handling.
 *
 * Current Google Play setup for ProVC:
 * - Subscription product ID: catch_and_call_pro_vc
 * - Base plan IDs: monthly-provc, yearly-provc
 *
 * For now, Tracker billing is not queried until it is ready in Play Console.
 */
class SubscriptionManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "SubscriptionManager"

        // Google Play subscription product IDs
        const val PROVC_SUBSCRIPTION = "catch_and_call_pro_vc"
        const val TRACKER_SUBSCRIPTION = "catch_and_call_tracker"

        // Base plan IDs / offer IDs for the ProVC subscription
        const val PROVC_MONTHLY_BASE_PLAN = "monthly-provc"
        const val PROVC_YEARLY_BASE_PLAN = "yearly-provc"

        const val TRACKER_MONTHLY_BASE_PLAN = "monthly-tracker"
        const val TRACKER_YEARLY_BASE_PLAN = "yearly-tracker"

        val ALL_PRODUCT_IDS = listOf(
            TRACKER_SUBSCRIPTION,
            PROVC_SUBSCRIPTION
        )
    }

    enum class SubscriptionTier { BASE, TRACKER, PROVC }

    var currentTier: SubscriptionTier = SubscriptionTier.BASE
        private set

    var availableProducts: List<ProductDetails> = emptyList()
        private set

    var errorMessage: String? = null
        private set

    var onTierChanged: ((SubscriptionTier) -> Unit)? = null
    var onProductsLoaded: ((List<ProductDetails>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onPurchaseComplete: (() -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing connected")
                    queryProducts()
                    queryExistingPurchases()
                } else {
                    val msg = "Billing setup failed: ${result.debugMessage}"
                    Log.e(TAG, msg)
                    errorMessage = msg
                    onError?.invoke(msg)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing disconnected — will reconnect on next call")
            }
        })
    }

    private fun queryProducts() {
        val productList = ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                availableProducts = productDetailsList
                Log.d(TAG, "📦 Loaded ${productDetailsList.size} subscription product(s)")
                onProductsLoaded?.invoke(productDetailsList)
            } else {
                val msg = "Failed to load products: ${billingResult.debugMessage}"
                Log.e(TAG, msg)
                errorMessage = msg
                onError?.invoke(msg)
            }
        }
    }

    /**
     * Launch purchase for a specific base plan under the ProVC subscription.
     *
     * @param productDetails The subscription product details for catch_and_call_pro_vc
     * @param basePlanId     The selected base plan ID: monthly-provc or yearly-provc
     * @param activity       Activity launching the purchase flow
     */
    fun launchPurchase(productDetails: ProductDetails, basePlanId: String, activity: Activity) {
        val selectedOffer = productDetails.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.basePlanId == basePlanId
        }

        val offerToken = selectedOffer?.offerToken
        if (offerToken == null) {
            val msg = "No offer found for ${productDetails.productId} basePlanId=$basePlanId"
            Log.e(TAG, msg)
            errorMessage = msg
            onError?.invoke(msg)
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = "Launch billing flow failed: ${result.debugMessage}"
            Log.e(TAG, msg)
            errorMessage = msg
            onError?.invoke(msg)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                val msg = "Purchase error: ${billingResult.debugMessage}"
                Log.e(TAG, msg)
                errorMessage = msg
                onError?.invoke(msg)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "✅ Purchase acknowledged")
                    } else {
                        Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
                    }
                }
            }

            updateTierFromProducts(purchase.products)
            onPurchaseComplete?.invoke()
        }
    }

    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var bestTier = SubscriptionTier.BASE

                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            val params = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()

                            billingClient.acknowledgePurchase(params) { result ->
                                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "✅ Restored purchase acknowledged")
                                } else {
                                    Log.e(TAG, "Restore acknowledge failed: ${result.debugMessage}")
                                }
                            }
                        }

                        val tier = tierFromProducts(purchase.products)
                        if (tier.ordinal > bestTier.ordinal) {
                            bestTier = tier
                        }
                    }
                }

                currentTier = bestTier
                Log.d(TAG, "🎯 Current tier: $currentTier")
                onTierChanged?.invoke(currentTier)
            }
        }
    }

    private fun tierFromProducts(productIds: List<String>): SubscriptionTier {
        return when {
            productIds.any { it == PROVC_SUBSCRIPTION } -> SubscriptionTier.PROVC
            productIds.any { it == TRACKER_SUBSCRIPTION } -> SubscriptionTier.TRACKER
            else -> SubscriptionTier.BASE
        }
    }

    private fun updateTierFromProducts(productIds: List<String>) {
        val newTier = tierFromProducts(productIds)
        if (newTier.ordinal > currentTier.ordinal) {
            currentTier = newTier
            onTierChanged?.invoke(currentTier)
        }
    }

    val hasTrackerAccess: Boolean
        get() = currentTier == SubscriptionTier.TRACKER || currentTier == SubscriptionTier.PROVC

    val hasProVCAccess: Boolean
        get() = currentTier == SubscriptionTier.PROVC

    /**
     * Since ProVC is now one subscription product with multiple base plans,
     * the paywall should work from this one product.
     */
    val proVCProducts: List<ProductDetails>
        get() = availableProducts.filter {
            it.productId == PROVC_SUBSCRIPTION
        }

    val trackerProducts: List<ProductDetails>
        get() = availableProducts.filter {
            it.productId == TRACKER_SUBSCRIPTION
        }

    fun endConnection() {
        billingClient.endConnection()
    }
}