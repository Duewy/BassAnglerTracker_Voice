package com.bramestorm.bassanglertracker.database

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bramestorm.bassanglertracker.CatchItem
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, "catch_database.db", null, 6) {

    private val prefs by lazy { context.getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE) }

    companion object {
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_TOTAL_WEIGHT_OZ = "total_weight_oz"
        private const val COLUMN_TOTAL_LENGTH_8THS = "total_length_8ths"
        private const val COLUMN_TOTAL_WEIGHT_KG = "total_weight_hundredth_kg"
        private const val COLUMN_TOTAL_LENGTH_TENTHS = "total_length_tenths"
        private const val COLUMN_CATCH_TYPE = "catch_type"
        private const val COLUMN_MARKER_TYPE = "marker_type"
        private const val COLUMN_CLIP_COLOR = "clip_color"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCatchesTable = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_DATE_TIME TEXT NOT NULL,
            $COLUMN_LATITUDE REAL,
            $COLUMN_LONGITUDE REAL,
            $COLUMN_SPECIES TEXT NOT NULL,
            $COLUMN_TOTAL_WEIGHT_OZ INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_8THS INTEGER DEFAULT 0,
            $COLUMN_TOTAL_WEIGHT_KG INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_TENTHS INTEGER DEFAULT 0,
            $COLUMN_CATCH_TYPE TEXT NOT NULL,
            $COLUMN_MARKER_TYPE TEXT,
            $COLUMN_CLIP_COLOR TEXT
        )
    """.trimIndent()
        db.execSQL(createCatchesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB_DEBUG", "⚠️ Upgrading database from version $oldVersion to $newVersion...")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertCatch(catch: CatchItem): Boolean {
        val db = this.writableDatabase
        var rowId: Long = -1

        try {
            val values = ContentValues().apply {
                put(COLUMN_DATE_TIME, catch.dateTime)
                put(COLUMN_SPECIES, catch.species)
                put(COLUMN_TOTAL_WEIGHT_OZ, catch.totalWeightOz)
                put(COLUMN_TOTAL_LENGTH_8THS, catch.totalLengthA8th)
                put(COLUMN_TOTAL_LENGTH_TENTHS, catch.totalLengthTenths)
                put(COLUMN_TOTAL_WEIGHT_KG, catch.totalWeightHundredthKg)
                put(COLUMN_CATCH_TYPE, catch.catchType)
                put(COLUMN_MARKER_TYPE, catch.markerType)
                put(COLUMN_CLIP_COLOR, catch.clipColor)
            }

            rowId = db.insert(TABLE_NAME, null, values)

            if (rowId == -1L) {
                Log.e("DB_ERROR", "❌ Failed to insert catch.")
                return false
            }

            Log.d("DB_DEBUG", "✅ Catch inserted with ID: $rowId")

            // Enable GPS automatically
            prefs.edit().putBoolean("GPS_ENABLED", true).apply()
            Log.d("GPS_DEBUG", "✅ GPS forced ON internally in insertCatch()")

            // Try to get GPS after delay
            Handler(Looper.getMainLooper()).postDelayed({
                getLastKnownLocation { location ->
                    if (location != null) {
                        updateCatchGPS(rowId.toInt(), location.latitude, location.longitude)
                        Toast.makeText(
                            context,
                            "📍 GPS Saved: ${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "⚠️ No GPS Location for that Catch.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.w("GPS_DEBUG", "⚠️ No location found to save for catch ID=$rowId")
                    }
                }
            }, 3000)

            return true
        } catch (e: Exception) {
            Log.e("DB_ERROR", "❌ insertCatch error: ${e.message}")
            return false
        } finally {
            Log.d("GPS_DEBUG", "GPS_ENABLED is ${prefs.getBoolean("GPS_ENABLED", false)}")
            db.close()
        }
    }

    fun getCatchesForToday(catchType: String, todaysDate: String): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE strftime('%Y-%m-%d', $COLUMN_DATE_TIME) = ? AND $COLUMN_CATCH_TYPE = ? ORDER BY $COLUMN_TOTAL_WEIGHT_OZ DESC",
            arrayOf(todaysDate, catchType)
        )

        if (cursor.moveToFirst()) {
            do {
                catchList.add(parseCatch(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return catchList
    }

    fun getFilteredCatchesWithLocation(
        species: String,
        catchType: String,
        minWeightOz: Int,
        fromDate: String,
        toDate: String
    ): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE species = ?
              AND catch_type = ?
              AND total_weight_oz > ?
              AND date_time BETWEEN ? AND ?
              AND latitude IS NOT NULL
              AND longitude IS NOT NULL
        """.trimIndent(),
            arrayOf(species, catchType, minWeightOz.toString(), fromDate, toDate)
        )

        if (cursor.moveToFirst()) {
            do {
                catchList.add(parseCatch(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return catchList
    }

    fun updateCatchGPS(catchId: Int, lat: Double, lon: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, lat)
            put(COLUMN_LONGITUDE, lon)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    fun updateCatch(
        catchId: Int,
        newWeightOz: Int? = null,
        newWeightKg: Int? = null,
        newLengthA8ths: Int? = null,
        newLengthCm: Int? = null,
        species: String
    ) {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_SPECIES, species)
        if (newWeightOz != null) values.put(COLUMN_TOTAL_WEIGHT_OZ, newWeightOz)
        if (newWeightKg != null) values.put(COLUMN_TOTAL_WEIGHT_KG, newWeightKg)
        if (newLengthA8ths != null) values.put(COLUMN_TOTAL_LENGTH_8THS, newLengthA8ths)
        if (newLengthCm != null) values.put(COLUMN_TOTAL_LENGTH_TENTHS, newLengthCm)
        db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    private fun parseCatch(cursor: Cursor): CatchItem {
        return CatchItem(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME)),
            species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES)),
            totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_OZ)),
            totalLengthA8th = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_8THS)),
            totalLengthTenths = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_TENTHS)),
            totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_KG)),
            catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE)),
            markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)),
            clipColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIP_COLOR)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
        )
    }

    private fun getLastKnownLocation(callback: (android.location.Location?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("GPS_DEBUG", "❌ Location permission not granted")
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("GPS_DEBUG", "✅ Got fused location: ${location.latitude}, ${location.longitude}")
                    callback(location)
                } else {
                    val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                        priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                        interval = 1000
                        fastestInterval = 500
                        numUpdates = 1
                    }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        object : com.google.android.gms.location.LocationCallback() {
                            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                val freshLocation = result.lastLocation
                                Log.d("GPS_DEBUG", "📡 Fresh location received: ${freshLocation?.latitude}, ${freshLocation?.longitude}")
                                callback(freshLocation)
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        },
                        Looper.getMainLooper()
                    )
                }
            }
            .addOnFailureListener {
                Log.e("GPS_DEBUG", "❌ Failed to get fused location: ${it.message}")
                callback(null)
            }
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun enableGps() {
        prefs.edit().putBoolean("GPS_ENABLED", true).apply()
    }

    fun getFilteredCatchesWithLocationAdvanced(
        species: String,
        catchType: String,
        measurementType: String,
        minValue: Float,
        maxValue: Float,
        fromDate: String,
        toDate: String
    ): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase

        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        // Only include catches with GPS
        whereClauses.add("latitude IS NOT NULL AND longitude IS NOT NULL")

        // Optional species filter
        if (species.lowercase() != "all") {
            whereClauses.add("REPLACE(LOWER(species), ' ', '') = ?")
            args.add(species.lowercase().replace(" ", ""))
        }

        // Optional catchType filter
        if (catchType.isNotEmpty()) {
            whereClauses.add("LOWER(catchType) = ?")
            args.add(catchType.lowercase())
        }

        // Date filter
        whereClauses.add("$COLUMN_DATE_TIME BETWEEN ? AND ?")
        args.add(fromDate)
        args.add(toDate)

        // Measurement filter
        when (measurementType.lowercase()) {
            "weight" -> {
                if (measurementType.contains("kg", true)) {
                    whereClauses.add("$COLUMN_TOTAL_WEIGHT_KG BETWEEN ? AND ?")
                    args.add((minValue * 100).toInt().toString())
                    args.add((maxValue * 100).toInt().toString())
                } else {
                    whereClauses.add("$COLUMN_TOTAL_WEIGHT_OZ BETWEEN ? AND ?")
                    args.add((minValue * 16).toInt().toString()) // Convert lbs to oz
                    args.add((maxValue * 16).toInt().toString())
                }
            }
            "length" -> {
                if (measurementType.contains("cm", true)) {
                    whereClauses.add("$COLUMN_TOTAL_LENGTH_TENTHS BETWEEN ? AND ?")
                    args.add((minValue * 10).toInt().toString())
                    args.add((maxValue * 10).toInt().toString())
                } else {
                    whereClauses.add("$COLUMN_TOTAL_LENGTH_8THS BETWEEN ? AND ?")
                    args.add((minValue * 8).toInt().toString())
                    args.add((maxValue * 8).toInt().toString())
                }
            }
        }

        val query = """
        SELECT * FROM $TABLE_NAME
        WHERE ${whereClauses.joinToString(" AND ")}
        ORDER BY $COLUMN_DATE_TIME DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, args.toTypedArray())

        while (cursor.moveToNext()) {
            val catch = parseCatch(cursor)
            catches.add(catch)
        }

        Log.d("DB_QUERY", "WHERE: ${whereClauses.joinToString(" AND ")}")
        Log.d("DB_QUERY", "ARGS: ${args.joinToString()}")

        cursor.close()
        db.close()

        return catches

        Log.d("MapCatch", "Query returned ${catches.size} rows")
        catches.forEach {
            Log.d("MapCatch", "${it.species} @ ${it.latitude}, ${it.longitude}")
        }

    }

    // for Map Searches TOP 5 of Length or Weight in set Species...
    fun getTopCatchesForSpeciesThisMonth(
        species: String,
        minOz: Int,
        maxOz: Int,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER(species) = ?
              AND total_weight_oz BETWEEN ? AND ?
              AND strftime('%Y-%m', $COLUMN_DATE_TIME) = ?
            ORDER BY total_weight_oz DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minOz.toString(),
                maxOz.toString(),
                monthPrefix,
                limit.toString()
            )
        )

        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }

        cursor.close()
        db.close()
        return list
    }

    fun insertFakeCatchesForTesting() {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        val testSpeciesList = listOf("Bass", "Walleye", "Pike", "Crappie", "Catfish")

        for (i in 1..20) {
            val values = ContentValues().apply {
                put("date_time", currentTime - (i * 3600000L)) // spread across time
                put("species", "TEST_${testSpeciesList.random()}")
                put("total_weight_oz", (64..120).random())  // 4–7.5 lbs
                put("total_length_8ths", (96..160).random()) // 12–20 inches
                put("total_length_tenths", 0) // unused here
                put("total_weight_hundredth_kg", 0) // unused here
                put("catch_type", "TEST_FAKE")
                put("marker_type", "")
                put("clip_color", "")
                put("latitude", 43.12 + (0..30).random() / 1000.0)
                put("longitude", -79.32 - (0..30).random() / 1000.0)
            }
            db.insert("catches", null, values)
        }
    }

    fun logAllCatches() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT species, date_time, latitude, longitude FROM $TABLE_NAME", null)
        while (cursor.moveToNext()) {
            val species = cursor.getString(0)
            val date = cursor.getString(1)
            val lat = cursor.getDouble(2)
            val lon = cursor.getDouble(3)
            Log.d("CatchLog", "$species @ $date - ($lat, $lon)")
        }
        cursor.close()
        db.close()
    }

    fun getTopCatchesByKgForSpeciesThisMonth(
        species: String,
        minHundredthsKg: Int,
        maxHundredthsKg: Int,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val cursor = db.rawQuery(
            """
        SELECT * FROM catches
        WHERE LOWER(species) = ?
          AND total_weight_hundredth_kg BETWEEN ? AND ?
          AND strftime('%Y-%m', date_time) = ?
        ORDER BY total_weight_hundredth_kg DESC
        LIMIT ?
        """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minHundredthsKg.toString(),
                maxHundredthsKg.toString(),
                monthPrefix,
                limit.toString()
            )
        )

        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }

        cursor.close()
        db.close()
        return list
    }
// ------------------ TOP 5 Look UP -------------------------

    fun getTopCatchesByInchesForSpeciesThisMonth(
        species: String,
        min8ths: Int,
        max8ths: Int,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val cursor = db.rawQuery(
            """
        SELECT * FROM catches
        WHERE LOWER(species) = ?
          AND total_length_8ths BETWEEN ? AND ?
          AND strftime('%Y-%m', date_time) = ?
        ORDER BY total_length_8ths DESC
        LIMIT ?
        """.trimIndent(),
            arrayOf(
                species.lowercase(),
                min8ths.toString(),
                max8ths.toString(),
                monthPrefix,
                limit.toString()
            )
        )

        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }

        cursor.close()
        db.close()
        return list
    }

    fun getTopCatchesByCmForSpeciesThisMonth(
        species: String,
        minTenths: Int,
        maxTenths: Int,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val cursor = db.rawQuery(
            """
        SELECT * FROM catches
        WHERE LOWER(species) = ?
          AND total_length_tenths BETWEEN ? AND ?
          AND strftime('%Y-%m', date_time) = ?
        ORDER BY total_length_tenths DESC
        LIMIT ?
        """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minTenths.toString(),
                maxTenths.toString(),
                monthPrefix,
                limit.toString()
            )
        )

        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }

        cursor.close()
        db.close()
        return list
    }


}//----------------- END ---------------------
