package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object SpeciesImageStorage {

    fun importToInternalStorage(
        context: Context,
        sourceUri: Uri,
        normalizedSpecies: String
    ): String? {
        return try {
            val dir = File(context.filesDir, "species_images")
            if (!dir.exists()) dir.mkdirs()

            val outFile = File(dir, "$normalizedSpecies.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Uri.fromFile(outFile).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun deleteInternalImage(context: Context, normalizedSpecies: String) {
        val dir = File(context.filesDir, "species_images")
        val file = File(dir, "$normalizedSpecies.jpg")
        if (file.exists()) file.delete()
    }
}
