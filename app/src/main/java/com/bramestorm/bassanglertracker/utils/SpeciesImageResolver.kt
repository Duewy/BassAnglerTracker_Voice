package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView

object SpeciesImageResolver {

    fun loadInto(
        context: Context,
        speciesName: String,
        imageView: ImageView
    ) {
        val normalized =
            SharedPreferencesManager.normalizeSpeciesName(speciesName)

        // 1️⃣ User-selected image (URI)
        val uri =
            SharedPreferencesManager.getSpeciesImageUri(context, normalized)

        if (!uri.isNullOrBlank()) {
            try {
                imageView.setImageURI(Uri.parse(uri))
                return
            } catch (e: Exception) {
                // Bad or legacy URI → clean it up and fall back
                SharedPreferencesManager.saveSpeciesImageUri(
                    context,
                    normalized,
                    null
                )
            }
        }

        // 2️⃣ Built-in drawable fallback
        imageView.setImageResource(
            SpeciesImageHelper.getSpeciesImageResId(normalized)
        )
    }
}
