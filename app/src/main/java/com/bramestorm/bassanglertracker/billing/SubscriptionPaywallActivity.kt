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
        subscriptionManager.onProductsLoaded = { products ->
            runOnUiThread {
                // Remove the loading text
                layout.removeView(loadingText)

                // Pick the right products for this tier
                val relevantProducts = if (targetTier == "provc") {
                    subscriptionManager.proVCProducts
                } else {
                    subscriptionManager.trackerProducts
                }

                if (relevantProducts.isEmpty()) {
                    layout.addView(TextView(this).apply {
                        text = "⚠️ No subscription products found.\n\nMake sure you've created them in Google Play Console."
                        textSize = 14f
                        gravity = Gravity.CENTER
                    }, layout.childCount - 1)  // before close button
                    return@runOnUiThread
                }

                // ── Add a button for each product ──
                for (product in relevantProducts) {
                    val offer = product.subscriptionOfferDetails?.firstOrNull()
                    val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "N/A"
                    val period = if (product.productId.contains("yearly")) "/ year" else "/ month"

                    val btn = Button(this).apply {
                        text = "${product.name}\n$price $period"
                        textSize = 16f
                        setPadding(16, 24, 16, 24)
                        setOnClickListener {
                            Log.d(TAG, "🛒 User tapped: ${product.productId}")
                            subscriptionManager.launchPurchase(product, this@SubscriptionPaywallActivity)
                        }
                    }
                    // Insert before the Close button
                    layout.addView(btn, layout.childCount - 1)
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