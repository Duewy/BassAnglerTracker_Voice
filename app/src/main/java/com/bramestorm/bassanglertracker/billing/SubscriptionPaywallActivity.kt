package com.bramestorm.bassanglertracker.billing

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.ProductDetails

/**
 *  SubscriptionPaywallActivity.kt
 *  ──────────────────────────────────────────────────
 *  This is the Android equivalent of the iOS file:
 *      CatchAndCall/Views/SubscriptionPaywallView.swift
 *
 *  It shows the user what they can upgrade to and lets
 *  them pick Monthly or Yearly, then launches the
 *  Google Play purchase sheet.
 *
 *  HOW IT WORKS:
 *  1. MainActivity passes an intent extra "target_tier" = "tracker" or "provc"
 *  2. This Activity shows the features + pricing
 *  3. User taps Subscribe → Google Play sheet appears
 *  4. On success → finish() back to MainActivity
 *  ──────────────────────────────────────────────────
 */
class SubscriptionPaywallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_TIER = "target_tier"   // "tracker" or "provc"
        private const val TAG = "SubscriptionPaywall"
    }

    private lateinit var subscriptionManager: SubscriptionManager
    private var targetTier: String = "tracker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetTier = intent.getStringExtra(EXTRA_TARGET_TIER) ?: "tracker"

        // ── Build the subscription manager ──
        subscriptionManager = SubscriptionManager(this)

        // ── Build the UI programmatically (no XML layout needed) ──
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scrollView.addView(layout)
        setContentView(scrollView)

        // ── Title ──
        val tierEmoji = if (targetTier == "provc") "🎙️" else "🗺️"
        val tierName  = if (targetTier == "provc") "ProVC" else "Tracker"

        layout.addView(TextView(this).apply {
            text = "$tierEmoji Upgrade to $tierName"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })

        // ── Features List (matches iOS SubscriptionPaywallView tierFeatures) ──
        val features = if (targetTier == "provc") {
            listOf(
                "✅ Everything in Tracker",
                "✅ Hands-free voice control logging",
                "✅ Voice assistant setup",
                "✅ Full voice command navigation"
            )
        } else {
            listOf(
                "✅ Everything in Base",
                "✅ GPS catch location logging",
                "✅ Map with GPS pins",
                "✅ KML file export"
            )
        }

        for (feature in features) {
            layout.addView(TextView(this).apply {
                text = feature
                textSize = 16f
                setPadding(0, 8, 0, 8)
            })
        }

        // ── Spacer ──
        layout.addView(TextView(this).apply {
            text = "\n─────────────────\n"
            gravity = Gravity.CENTER
        })

        // ── Loading text (replaced once products load) ──
        val loadingText = TextView(this).apply {
            text = "⏳ Loading subscription options..."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        layout.addView(loadingText)

        // ── Close button ──
        layout.addView(Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        })

        // ── Connect & load products ──
        subscriptionManager.onProductsLoaded = {
            runOnUiThread {
                layout.removeView(loadingText)

                val product = if (targetTier == "provc") {
                    subscriptionManager.proVCProducts.firstOrNull()
                } else {
                    subscriptionManager.trackerProducts.firstOrNull()
                }

                val tierLabel = if (targetTier == "provc") "ProVC" else "Tracker"

                if (product == null) {
                    val missingProductId = if (targetTier == "provc") {
                        SubscriptionManager.PROVC_SUBSCRIPTION
                    } else {
                        SubscriptionManager.TRACKER_SUBSCRIPTION
                    }

                    layout.addView(TextView(this).apply {
                        text = "⚠️ No $tierLabel subscription product found.\n\nMake sure $missingProductId exists in Google Play Console and is available."
                        textSize = 14f
                        gravity = Gravity.CENTER
                    }, layout.childCount - 1)
                    return@runOnUiThread
                }

                val offers = product.subscriptionOfferDetails.orEmpty()

                val monthlyBasePlanId = if (targetTier == "provc") {
                    SubscriptionManager.PROVC_MONTHLY_BASE_PLAN
                } else {
                    SubscriptionManager.TRACKER_MONTHLY_BASE_PLAN
                }

                val yearlyBasePlanId = if (targetTier == "provc") {
                    SubscriptionManager.PROVC_YEARLY_BASE_PLAN
                } else {
                    SubscriptionManager.TRACKER_YEARLY_BASE_PLAN
                }

                val monthlyOffer = offers.firstOrNull { it.basePlanId == monthlyBasePlanId }
                val yearlyOffer = offers.firstOrNull { it.basePlanId == yearlyBasePlanId }

                fun addPlanButton(
                    title: String,
                    offer: ProductDetails.SubscriptionOfferDetails?,
                    basePlanId: String
                ) {
                    if (offer == null) return

                    val price = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: "N/A"
                    val period = when (basePlanId) {
                        SubscriptionManager.PROVC_YEARLY_BASE_PLAN,
                        SubscriptionManager.TRACKER_YEARLY_BASE_PLAN -> "/ year"

                        SubscriptionManager.PROVC_MONTHLY_BASE_PLAN,
                        SubscriptionManager.TRACKER_MONTHLY_BASE_PLAN -> "/ month"

                        else -> ""
                    }

                    val buttonText = "$title\n$price $period"

                    val btn = Button(this).apply {
                        text = buttonText
                        textSize = 16f
                        setPadding(16, 24, 16, 24)
                        setOnClickListener {
                            Log.d(TAG, "🛒 User tapped basePlanId=$basePlanId for ${product.productId}")
                            subscriptionManager.launchPurchase(
                                product,
                                basePlanId,
                                this@SubscriptionPaywallActivity
                            )
                        }
                    }

                    layout.addView(btn, layout.childCount - 1)
                }

                val monthlyTitle = if (targetTier == "provc") {
                    "Catch And Call ProVC Monthly"
                } else {
                    "Catch And Call Tracker Monthly"
                }

                val yearlyTitle = if (targetTier == "provc") {
                    "Catch And Call ProVC Yearly"
                } else {
                    "Catch And Call Tracker Yearly"
                }

                addPlanButton(monthlyTitle, monthlyOffer, monthlyBasePlanId)
                addPlanButton(yearlyTitle, yearlyOffer, yearlyBasePlanId)

                if (monthlyOffer == null && yearlyOffer == null) {
                    layout.addView(TextView(this).apply {
                        text = "⚠️ No $tierLabel base plans found.\n\nMake sure $monthlyBasePlanId and $yearlyBasePlanId are active in Google Play Console."
                        textSize = 14f
                        gravity = Gravity.CENTER
                    }, layout.childCount - 1)
                }
            }
        }

        subscriptionManager.onPurchaseComplete = {
            runOnUiThread {
                Toast.makeText(this, "🎉 Subscription activated! Thank you!", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        subscriptionManager.onError = { msg ->
            runOnUiThread {
                Toast.makeText(this, "Error: $msg", Toast.LENGTH_LONG).show()
            }
        }

        subscriptionManager.startConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionManager.endConnection()
    }
}