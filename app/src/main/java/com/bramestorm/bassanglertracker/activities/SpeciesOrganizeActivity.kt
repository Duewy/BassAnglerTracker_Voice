package com.bramestorm.bassanglertracker.activities

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.SpeciesReorderAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageStorage

class SpeciesOrganizeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesReorderAdapter
    private lateinit var btnAddSpecies: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var pendingImageUri: Uri? = null

    // 🖼️ Hold a reference to the dialog's preview ImageView
    private var dialogImagePreview: ImageView? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pendingImageUri = uri

            // 🖼️ Update the preview in the dialog when user picks an image
            uri?.let { pickedUri ->
                dialogImagePreview?.let { preview ->
                    try {
                        contentResolver.openInputStream(pickedUri)?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                preview.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        // If preview fails, keep the default image
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_organize)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSave = findViewById(R.id.btnSaveListOrder)
        btnAddSpecies = findViewById(R.id.btnAddSpeciesList)
        btnCancel = findViewById(R.id.btnCancelSpecies)

        val speciesList =
            SharedPreferencesManager.loadSpeciesList(this)

        adapter = SpeciesReorderAdapter(
            speciesList,
            onDeleteRequested = { speciesName ->
                confirmDeleteSpecies(speciesName)
            }
        )


        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Drag & drop support
        val touchHelper = ItemTouchHelper(adapter.itemTouchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)

        btnAddSpecies.setOnClickListener {
            showAddSpeciesDialog()
        }

        btnSave.setOnClickListener {
            SharedPreferencesManager.saveSpeciesList(
                this,
                adapter.getCurrentList()
            )
            finish()
        }



        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showAddSpeciesDialog() {

        // 🖼️ Image preview — starts with the default fish image
        val imgPreview = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                200,   // width in pixels (adjust as needed)
                200    // height in pixels
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = 16
                bottomMargin = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.fish_default)
            contentDescription = "Species image preview"
        }

        // Save reference so the imagePicker callback can update it
        dialogImagePreview = imgPreview

        val input = EditText(this).apply {
            hint = "Enter species name"
        }

        val btnSelectImage = Button(this).apply {
            text = context.getString(R.string.select_image_optional)
            setOnClickListener {
                imagePicker.launch("image/*")
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(imgPreview)        // 🖼️ Image preview at the top
            addView(btnSelectImage)    // Select Image button below image
            addView(input)             // Species name field at the bottom
        }

        AlertDialog.Builder(this)
            .setTitle("Add Species")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val rawName = input.text.toString().trim()
                if (rawName.isBlank()) return@setPositiveButton

                // 🚫 Prevent near-duplicate species (human variants)
                val existingSpecies =
                    SharedPreferencesManager.loadSpeciesList(this)

                val canonicalInput =
                    SharedPreferencesManager.canonicalizeSpeciesName(rawName)

                if (existingSpecies.any {
                        SharedPreferencesManager.canonicalizeSpeciesName(it) == canonicalInput
                    }) {
                    pendingImageUri = null
                    dialogImagePreview = null
                    return@setPositiveButton
                }

                // 🔑 Canonical normalized key (single source of truth)
                val normalizedLowerCasedName =
                    SharedPreferencesManager.normalizeSpeciesName(rawName)

                // 1️⃣ Save species name
                SharedPreferencesManager.addSpecies(this, rawName)

                // 2️⃣ Ensure default initials are seeded
                SharedPreferencesManager.ensureDefaultSpeciesInitials(this)

                // 3️⃣ Assign a UNIQUE initial
                val speciesInitial =
                    SharedPreferencesManager.assignUniqueSpeciesInitial(
                        this,
                        normalizedLowerCasedName
                    )

                // 4️⃣ Persist initials
                val initialsMap =
                    SharedPreferencesManager.loadSpeciesInitialsMap(this)

                initialsMap[normalizedLowerCasedName] = speciesInitial

                SharedPreferencesManager.saveSpeciesInitialsMap(
                    this,
                    initialsMap
                )

                // 5️⃣ Import & save species image (if selected)
                pendingImageUri?.let { pickedUri ->
                    val stableUri =
                        SpeciesImageStorage.importToInternalStorage(
                            context = this,
                            sourceUri = pickedUri,
                            normalizedSpecies = normalizedLowerCasedName
                        )

                    SharedPreferencesManager.saveSpeciesImageUri(
                        this,
                        normalizedLowerCasedName,
                        stableUri
                    )
                }

                // 6️⃣ Clear picker state
                pendingImageUri = null
                dialogImagePreview = null

                // 7️⃣ Refresh list
                adapter.updateList(
                    SharedPreferencesManager.loadSpeciesList(this)
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                pendingImageUri = null
                dialogImagePreview = null
            }
            .setOnDismissListener {
                // 🧹 Safety cleanup if dialog is dismissed any other way
                dialogImagePreview = null
            }
            .show()
    }


    private fun confirmDeleteSpecies(speciesName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Species")
            .setMessage("Delete \"$speciesName\"?\nThis will not remove past catches.")
            .setPositiveButton("Delete") { _, _ ->
                SharedPreferencesManager.removeSpecies(this, speciesName)
                // 🗑️ Remove initials mapping as well
                val normalized =
                    SharedPreferencesManager.normalizeSpeciesName(speciesName)

                val initialsMap =
                    SharedPreferencesManager.loadSpeciesInitialsMap(this)

                initialsMap.remove(normalized)

                SharedPreferencesManager.saveSpeciesInitialsMap(
                    this,
                    initialsMap
                )

                adapter.updateList(
                    SharedPreferencesManager.loadSpeciesList(this)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}