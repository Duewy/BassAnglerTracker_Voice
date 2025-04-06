package com.bramestorm.bassanglertracker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MapCatchLocationsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var btnApplyFilters: Button
    private lateinit var btnCloseMap : Button
    private lateinit var btnMapSettings : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_catch_locations)


        btnApplyFilters = findViewById(R.id.btnApplyFilters)
        btnCloseMap = findViewById(R.id.btnCloseMap)
        btnMapSettings = findViewById(R.id.btnMapSettings)



        val speciesOptions = SharedPreferencesManager.getAllSpecies(this)
        val dateOptions = listOf("This Month", "Last 7 Days", "All Time")

        spinnerSpecies.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, speciesOptions)
        spinnerDate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dateOptions)

        btnMapSettings.setOnClickListener {
            Toast.makeText(this, "⚙️ Map Settings Coming Soon!", Toast.LENGTH_SHORT).show()
            // In the future, launch a settings dialog or bottom sheet here.
        }

        btnCloseMap.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Optional: close this activity to remove it from back stack
        }

        btnApplyFilters.setOnClickListener {
            val selectedSpecies = spinnerSpecies.selectedItem.toString().lowercase()
            val dateFilter = spinnerDate.selectedItem.toString()

            val fromDate = when (dateFilter) {
                "This Month" -> SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(Date())
                "Last 7 Days" -> {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                }
                else -> "1970-01-01"
            }

            val db = CatchDatabaseHelper(this)
            val catches = db.getFilteredCatchesWithLocation(
                species = selectedSpecies,
                catchType = "lbsOzs",
                minWeightOz = 80,
                fromDate = fromDate,
                toDate = getTodayAsString()
            )

            map.clear()
            for (catch in catches) {
                if (catch.latitude != null && catch.longitude != null) {
                    val position = LatLng(catch.latitude!!, catch.longitude!!)

                    // Choose icon based on catch type
                    val markerIcon = if (catch.catchType.lowercase() == "tournament") {
                        BitmapDescriptorFactory.fromResource(R.drawable.map_icon_tournament)
                    } else {
                        BitmapDescriptorFactory.fromResource(R.drawable.map_icon_fun_day)
                    }

                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .icon(markerIcon)
                            .title(catch.species)
                            .snippet("Caught: ${catch.dateTime}")
                    )
                }
            }


            val firstWithGps = catches.firstOrNull { it.latitude != null && it.longitude != null }
            firstWithGps?.let { catch ->
                val lat = catch.latitude ?: return@let
                val lon = catch.longitude ?: return@let
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 10f))
            }
                if (firstWithGps == null) {
                    val fallbackLocation = LatLng(44.43342, -76.34939) // Gilmour Point Dog Lake
                    map.addMarker(
                        MarkerOptions()
                            .position(fallbackLocation)
                            .icon(getResizedMapIcon(R.drawable.map_icon_fall_back, 64, 64)                            )
                            .title("No GPS data found")
                            .snippet("Default location")
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackLocation, 10f))

                    val toast = Toast.makeText(this, "⚠️ No GPS-tagged catches found.\nShowing default location.", Toast.LENGTH_LONG)
                    toast.setGravity(android.view.Gravity.CENTER, 0, 0)
                    toast.show()
                }


        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }//---------------- END On Create -----------------------------

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val db = CatchDatabaseHelper(this)
        val catches = db.getFilteredCatchesWithLocation(
            species = "largemouth",
            catchType = "lbsOzs",
            minWeightOz = 0,
            fromDate = getFirstDayOfMonth(),
            toDate = getTodayAsString()
        )

        for (catch in catches) {
            if (catch.latitude != null && catch.longitude != null) {
                val position = LatLng(catch.latitude!!, catch.longitude!!)

                // Choose icon based on catch type
                val markerIcon = if (catch.catchType.lowercase() == "tournament") {
                    BitmapDescriptorFactory.fromResource(R.drawable.map_icon_tournament)
                } else {
                    BitmapDescriptorFactory.fromResource(R.drawable.map_icon_fun_day)
                }

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .icon(markerIcon)
                        .title(catch.species)
                        .snippet("Caught: ${catch.dateTime}")
                )
            }
        }


        val firstWithGps = catches.firstOrNull { it.latitude != null && it.longitude != null }
        firstWithGps?.let { catch ->
            val lat = catch.latitude ?: return@let
            val lon = catch.longitude ?: return@let
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 8f))
        }
        if (firstWithGps == null) {
            val fallbackLocation = LatLng(44.43342, -76.34939) // Gilmour Point Dog Lake
            map.addMarker(
                MarkerOptions()
                    .position(fallbackLocation)
                    .icon(getResizedMapIcon(R.drawable.map_icon_fall_back, 64, 64))
                    .title("No GPS data found")
                    .snippet("Default location")
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackLocation, 10f))

            val toast = Toast.makeText(this, "⚠️ No GPS-tagged catches found.\nShowing default location.", Toast.LENGTH_LONG)
            toast.setGravity(android.view.Gravity.CENTER, 0, 0)
            toast.show()
        }


    }//---------------- END On Create

    //----- Resize Pin Icons --------------------------
    private fun getResizedMapIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor {
        val imageBitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resized = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resized)
    }

    private fun getFirstDayOfMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM-01", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTodayAsString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
