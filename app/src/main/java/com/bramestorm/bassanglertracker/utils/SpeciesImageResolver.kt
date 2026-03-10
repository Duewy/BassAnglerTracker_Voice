package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File

object SpeciesImageResolver {

    fun loadInto(
        context: Context,
        speciesName: String,
        imageView: ImageView
    ) {
        val normalized =
            SharedPreferencesManager.normalizeSpeciesName(speciesName)

        // 1️⃣ User-selected image (URI)
        val uriString =
            SharedPreferencesManager.getSpeciesImageUri(context, normalized)

        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                // For file:// URIs, decode the bitmap directly from the file path
                if (uri.scheme == "file") {
                    val file = File(uri.path!!)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                            return
                        }
                    }
                } else {
                    // For content:// URIs, use the content resolver
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                            return
                        }
                    }
                }

                // If we get here, the URI was bad → clean it up
                SharedPreferencesManager.saveSpeciesImageUri(context, normalized, null)

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