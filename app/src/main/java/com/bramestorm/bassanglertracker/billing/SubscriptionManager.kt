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
 *  SubscriptionManager.kt
 *  ──────────────────────────────────────────────────
 *  This is the Android equivalent of the iOS file:
 *      CatchAndCall/Services/SubscriptionManager.swift
 *
 *  It uses Google Play Billing Library 7.x to:
 *      • Connect to Google Play
 *      • Load available subscription products
 *      • Launch the purchase flow
 *      • Check current subscription status
 *      • Acknowledge purchases (REQUIRED by Google)
 *
 *  The 4 product IDs below MUST match what you create
 *  in Google Play Console → Monetize → Subscriptions.
 *  ──────────────────────────────────────────────────
 */

class SubscriptionManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "SubscriptionManager"

        // ──── Product IDs ────
        //  These must match EXACTLY what you create in Google Play Console
        //  iOS equivalents:
        //     com.bramestorm.CatchAndCall.tracker.monthly  →  tracker_monthly
        //     com.bramestorm.CatchAndCall.tracker.yearly   →  tracker_yearly
        //     com.bramestorm.CatchAndCall.provc.monthly    →  provc_monthly
        //     com.bramestorm.CatchAndCall.provc.yearly     →  provc_yearly
        //
        //  NOTE: Google Play product IDs can only use lowercase, numbers, and underscores.
        //  That's why they look different from the iOS dot-notation.

        const val TRACKER_MONTHLY = "tracker_monthly"
        const val TRACKER_YEARLY  = "tracker_yearly"
        const val PROVC_MONTHLY   = "provc_monthly"
        const val PROVC_YEARLY    = "provc_yearly"

        val ALL_PRODUCT_IDS = listOf(
            TRACKER_MONTHLY, TRACKER_YEARLY,
            PROVC_MONTHLY, PROVC_YEARLY
        )
    }

    // ──── Subscription Tier (matches iOS SubscriptionTier enum) ────
    enum class SubscriptionTier { BASE, TRACKER, PROVC }

    // ──── State ────
    var currentTier: SubscriptionTier = SubscriptionTier.BASE
        private set

    var availableProducts: List<ProductDetails> = emptyList()
        private set

    var errorMessage: String? = null
        private set

    // ──── Callbacks (your Activity/Fragment can set these) ────
    var onTierChanged: ((SubscriptionTier) -> Unit)? = null
    var onProductsLoaded: ((List<ProductDetails>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onPurchaseComplete: (() -> Unit)? = null

    // ──── Billing Client ────
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)               // "this" because we implement PurchasesUpdatedListener
        .enablePendingPurchases()         // REQUIRED by Google
        .build()

    // ══════════════════════════════════════════════════════════════
    // STEP 1:  CONNECT to Google Play
    // ══════════════════════════════════════════════════════════════
    //  Call this from your Activity's onCreate().
    //  Once connected it auto-loads products + checks existing subs.

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing connected")
                    queryProducts()          // load what's for sale
                    queryExistingPurchases() // check what user already owns
                } else {
                    val msg = "Billing setup failed: ${result.debugMessage}"
                    Log.e(TAG, msg)
                    errorMessage = msg
                    onError?.invoke(msg)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing disconnected — will reconnect on next call")
                // Google recommends lazy reconnect; startConnection() again when needed.
            }
        })
    }

    // ══════════════════════════════════════════════════════════════
    // STEP 2:  QUERY PRODUCTS (like iOS loadProducts())
    // ══════════════════════════════════════════════════════════════
    //  Asks Google Play "what subscriptions are available?"

    private fun queryProducts() {
        val productList = ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)   // subscriptions
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                availableProducts = productDetailsList
                Log.d(TAG, "📦 Loaded ${productDetailsList.size} products")
                onProductsLoaded?.invoke(productDetailsList)
            } else {
                val msg = "Failed to load products: ${billingResult.debugMessage}"
                Log.e(TAG, msg)
                errorMessage = msg
                onError?.invoke(msg)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // STEP 3:  LAUNCH PURCHASE (like iOS purchase(_ product:))
    // ══════════════════════════════════════════════════════════════
    //  Call from your Paywall when user taps a "Subscribe" button.
    //
    //  productDetails = the ProductDetails object from availableProducts
    //  activity       = the Activity that's launching the purchase sheet

    fun launchPurchase(productDetails: ProductDetails, activity: Activity) {
        // Google subscriptions have "offers" — pick the first base plan
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            val msg = "No offer found for ${productDetails.productId}"
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
            Log.e(TAG, "Launch billing flow failed: ${result.debugMessage}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    // STEP 4:  HANDLE PURCHASE RESULT (callback from Google)
    // ══════════════════════════════════════════════════════════════
    //  This fires when the user completes/cancels the Google purchase sheet.
    //  It's the PurchasesUpdatedListener callback.

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

    // ══════════════════════════════════════════════════════════════
    // STEP 5:  ACKNOWLEDGE PURCHASE (REQUIRED — or Google refunds it!)
    // ══════════════════════════════════════════════════════════════
    //  iOS does transaction.finish() — Android does acknowledgePurchase().
    //  If you skip this, Google auto-refunds after 3 days!

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // ── Acknowledge if not already ──
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
            // ── Update the tier based on what was purchased ──
            updateTierFromProducts(purchase.products)
            onPurchaseComplete?.invoke()
        }
    }

    // ══════════════════════════════════════════════════════════════
    // STEP 6:  CHECK EXISTING PURCHASES (like iOS updateSubscriptionStatus())
    // ══════════════════════════════════════════════════════════════
    //  Called on app start to restore the user's current tier.
    //  This is the Android equivalent of iOS "restore purchases".

    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var bestTier = SubscriptionTier.BASE

                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // Acknowledge any un-acknowledged purchases
                        if (!purchase.isAcknowledged) {
                            handlePurchase(purchase)
                        }
                        // Determine the highest tier
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

    // ══════════════════════════════════════════════════════════════
    //  HELPER:  Map product IDs → Tier
    // ══════════════════════════════════════════════════════════════
    //  Same logic as iOS SubscriptionManager.updateSubscriptionStatus()

    private fun tierFromProducts(productIds: List<String>): SubscriptionTier {
        return when {
            productIds.any { it == PROVC_MONTHLY || it == PROVC_YEARLY }     -> SubscriptionTier.PROVC
            productIds.any { it == TRACKER_MONTHLY || it == TRACKER_YEARLY } -> SubscriptionTier.TRACKER
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

    // ══════════════════════════════════════════════════════════════
    //  HELPER:  Convenience getters (match iOS hasTrackerAccess / hasProVCAccess)
    // ══════════════════════════════════════════════════════════════

    val hasTrackerAccess: Boolean
        get() = currentTier == SubscriptionTier.TRACKER || currentTier == SubscriptionTier.PROVC

    val hasProVCAccess: Boolean
        get() = currentTier == SubscriptionTier.PROVC

    // ══════════════════════════════════════════════════════════════
    //  HELPER:  Filtered product lists (match iOS trackerProducts / proVCProducts)
    // ══════════════════════════════════════════════════════════════

    val trackerProducts: List<ProductDetails>
        get() = availableProducts.filter {
            it.productId == TRACKER_MONTHLY || it.productId == TRACKER_YEARLY
        }

    val proVCProducts: List<ProductDetails>
        get() = availableProducts.filter {
            it.productId == PROVC_MONTHLY || it.productId == PROVC_YEARLY
        }

    // ══════════════════════════════════════════════════════════════
    //  CLEANUP
    // ══════════════════════════════════════════════════════════════

    fun endConnection() {
        billingClient.endConnection()
    }
}