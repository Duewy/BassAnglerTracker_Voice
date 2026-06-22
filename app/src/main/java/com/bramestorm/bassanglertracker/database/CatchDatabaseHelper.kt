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
import com.bramestorm.bassanglertracker.MeasurementMode
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// !!!!!!!!!!!!! Set the Version of Upgrades so the DataBase follows.  !!!!!!!!!!!! +++++++ Added POUNDS to list +++++++
class CatchDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, "catch_database.db", null, 9) {

    private val prefs by lazy { context.getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE) }

    companion object {
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_TOTAL_WEIGHT_OZ = "total_weight_oz"                                // Stored in pounds ounces: 34 = 2 lbs and 2 oz
        private const val COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS = "total_weight_hundredth_pounds"    // Stored in hundredths of pounds: 225 = 2.25 lbs
        private const val COLUMN_TOTAL_LENGTH_QUARTERS = "total_length_quarters"                    // Stored in 4ths of inch:  18 = 2 inches and 2/4 inch
        private const val COLUMN_TOTAL_WEIGHT_KG = "total_weight_hundredth_kg"                      // Stored in hundredths of Kgs : 255 = 2.55 Kgs
        private const val COLUMN_TOTAL_LENGTH_TENTHS = "total_length_tenths"                        // Stored in millimeters: 135 = 13.5 Cm
        private const val COLUMN_CATCH_TYPE = "catch_type"                                          // FunDay or Tournament
        private const val COLUMN_MARKER_TYPE = "marker_type"
        private const val COLUMN_CLIP_COLOR = "clip_color"                                          // Culling Clip Colors the catches were saved with.
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
            $COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_QUARTERS INTEGER DEFAULT 0,
            $COLUMN_TOTAL_WEIGHT_KG INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_TENTHS INTEGER DEFAULT 0,
            $COLUMN_CATCH_TYPE TEXT NOT NULL,
            $COLUMN_MARKER_TYPE TEXT,
            $COLUMN_CLIP_COLOR TEXT
        )
    """.trimIndent()
        db.execSQL(createCatchesTable)

        // 🌟 Insert the “Three Brothers / Kingston” legend catch 🦈
        insertSampleLegendCatch(db)
    }

    private fun columnExists(db: SQLiteDatabase, column: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_NAME)", null)
        cursor.use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == column) return true
            }
        }
        return false
    }


    // Insert a single “legend” catch near Three Brothers Islands (Kingston)
    // Smallmouth, 59.5 cm, Fun Day, July 1st 2007, pre-cormorant era.
    private fun insertSampleLegendCatch(db: SQLiteDatabase) {
        try {
            // Avoid duplicates if this ever gets called again for some reason
            val checkCursor = db.rawQuery(
                """
    SELECT COUNT(*) FROM $TABLE_NAME
    WHERE $COLUMN_DATE_TIME = ?
    AND $COLUMN_SPECIES = ?
    AND $COLUMN_LATITUDE = ?
    AND $COLUMN_LONGITUDE = ?
    """.trimIndent(),
                arrayOf(
                    "2007-07-01 12:00:00",
                    "Small Mouth",
                    "44.206096",
                    "-76.625115"
                )
            )

            var alreadyExists = false
            if (checkCursor.moveToFirst()) {
                alreadyExists = checkCursor.getInt(0) > 0
            }
            checkCursor.close()

            if (alreadyExists) {
                Log.d("DB_INIT", "Legend sample catch already exists, skipping insert.")
                return
            }

            val values = ContentValues().apply {
                put(COLUMN_DATE_TIME, "2007-07-01 12:00:00")
                put(COLUMN_LATITUDE, 44.206096)
                put(COLUMN_LONGITUDE, -76.625115)
                put(COLUMN_SPECIES, "Small Mouth")

                // 59.5 cm → stored as tenths of cm = 595
                put(COLUMN_TOTAL_LENGTH_TENTHS, 595)

                put(COLUMN_CATCH_TYPE, "Fun Day")
                put(COLUMN_MARKER_TYPE, "Legend")
                // No clip color needed, but you *could* set "BLUE" or similar
            }

            val rowId = db.insert(TABLE_NAME, null, values)
            if (rowId == -1L) {
                Log.e("DB_INIT", "❌ Failed to insert legend sample catch.")
            } else {
                Log.d("DB_INIT", "✅ Legend sample catch inserted with ID=$rowId")
            }
        } catch (e: Exception) {
            Log.e("DB_INIT", "❌ Error inserting legend sample catch: ${e.message}")
        }
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO ${TABLE_NAME}_old;")
            onCreate(db)
            db.execSQL("""
            INSERT INTO $TABLE_NAME (
                $COLUMN_ID,
                $COLUMN_DATE_TIME,
                $COLUMN_LATITUDE,
                $COLUMN_LONGITUDE,
                $COLUMN_SPECIES,
                $COLUMN_TOTAL_WEIGHT_OZ,
                $COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS,
                $COLUMN_TOTAL_WEIGHT_KG,
                $COLUMN_TOTAL_LENGTH_QUARTERS,
                $COLUMN_TOTAL_LENGTH_TENTHS,
                $COLUMN_MARKER_TYPE,
                $COLUMN_CATCH_TYPE,
                $COLUMN_CLIP_COLOR
            )
            SELECT
                $COLUMN_ID,
                $COLUMN_DATE_TIME,
                $COLUMN_LATITUDE,
                $COLUMN_LONGITUDE,
                $COLUMN_SPECIES,
                $COLUMN_TOTAL_WEIGHT_OZ,
                $COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS,
                $COLUMN_TOTAL_WEIGHT_KG,
                $COLUMN_TOTAL_LENGTH_QUARTERS,
                $COLUMN_TOTAL_LENGTH_TENTHS,
                $COLUMN_MARKER_TYPE,
                $COLUMN_CATCH_TYPE,
                $COLUMN_CLIP_COLOR
            FROM ${TABLE_NAME}_old;
        """.trimIndent())
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_NAME}_old;")
        }

        if (oldVersion < 7) {
            // Only add columns *not already present in onCreate()* or if you plan to modify them
            if (!columnExists(db, "length_decimal_tenth_cm"))  {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN length_decimal_tenth_cm INTEGER DEFAULT 0;")
            }
        }

        if (oldVersion < 8) {
            if (!columnExists(db, "total_weight_hundredth_pounds"))  {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN total_weight_hundredth_pounds INTEGER DEFAULT 0;")
            }
        }

        if (oldVersion < 9) {
            Log.d("DB_UPGRADE", "🔧 Upgraded to version 9 — no schema changes.") // Just to track changes
        }

        if (oldVersion > 9 ){
            Log.d("DB_UPGRADE", "🔧 Upgraded to version 10 — no schema changes.")
            // When Updated Add Information to this line...
        }

    }
    //=== END on Up Grade ================================


    fun insertCatch(catch: CatchItem): Boolean {
        val db = this.writableDatabase

        try {
            val values = ContentValues().apply {
                put(COLUMN_DATE_TIME, catch.dateTime)
                put(COLUMN_SPECIES, catch.species)
                put(COLUMN_TOTAL_WEIGHT_OZ, catch.totalWeightOz)
                put(COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS, catch.totalWeightHundredthPounds)
                put(COLUMN_TOTAL_LENGTH_QUARTERS, catch.totalLengthQuarters)
                put(COLUMN_TOTAL_LENGTH_TENTHS, catch.totalLengthTenths)
                put(COLUMN_TOTAL_WEIGHT_KG, catch.totalWeightHundredthKg)
                put(COLUMN_CATCH_TYPE, catch.catchType)
                put(COLUMN_MARKER_TYPE, catch.markerType)
                put(COLUMN_CLIP_COLOR, catch.clipColor)
            }

            val rowId = db.insert(TABLE_NAME, null, values)

            if (rowId == -1L) {
                Log.e("DB_ERROR", "❌ Failed to insert catch.")
                return false
            }

            Log.d("DB_DEBUG", "✅ Catch inserted with ID: $rowId")
            updateLastCatchTime()

            // ✅ Respect the user's GPS setting instead of forcing it ON
            val gpsEnabled = prefs.getBoolean("GPS_ENABLED", false)
            Log.d("GPS_DEBUG", "GPS_ENABLED at insertCatch time = $gpsEnabled")

            if (gpsEnabled) {
                // Try to get GPS after short delay (let sensors/wifi settle)
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
                }, 3000L)
            } else {
                Log.d("GPS_DEBUG", "GPS disabled by user; not requesting location for catch ID=$rowId")
            }

            return true

        } catch (e: Exception) {
            Log.e("DB_ERROR", "❌ insertCatch error: ${e.message}")
            return false
        }
        // ✅ REMOVED db.close() from finally block — GPS callback fires 3 seconds later
        //    and needs the DB connection alive. SQLiteOpenHelper manages the lifecycle.
    }
    //==== END === Insert Catch ============================================

    private fun updateLastCatchTime() {
        val prefs = context.getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("LAST_CATCH_TIME", System.currentTimeMillis()).apply()
    }

    fun getLastCatchTimeMillis(): Long {
        val prefs = context.getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE)
        return prefs.getLong("LAST_CATCH_TIME", System.currentTimeMillis())
    }


    fun getCatchesForToday(catchType: String, todaysDate: String): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()

        val cursor: Cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE strftime('%Y-%m-%d', $COLUMN_DATE_TIME) = ?
            AND $COLUMN_CATCH_TYPE = ?
            ORDER BY $COLUMN_DATE_TIME DESC
            """.trimIndent(),
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

    private fun updateCatchGPS(catchId: Int, lat: Double, lon: Double) {
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
        newWeightPounds: Int? = null,
        newWeightKg: Int? = null,
        newLengthQuarters: Int? = null,
        newLengthCm: Int? = null,
        species: String,
        clipColor: String? = null,
        markerType: String? = null
    )
    {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_SPECIES, species)
        if (newWeightOz != null) values.put(COLUMN_TOTAL_WEIGHT_OZ, newWeightOz)
        if (newWeightPounds!= null) values.put(COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS, newWeightPounds)
        if (newWeightKg != null) values.put(COLUMN_TOTAL_WEIGHT_KG, newWeightKg)
        if (newLengthQuarters != null) values.put(COLUMN_TOTAL_LENGTH_QUARTERS, newLengthQuarters)
        if (newLengthCm != null) values.put(COLUMN_TOTAL_LENGTH_TENTHS, newLengthCm)
        if (clipColor != null) values.put("clip_color", clipColor)
        if (markerType != null) values.put("marker_type", markerType)
        db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    // Information of VC Questions
    private fun parseCatch(cursor: Cursor): CatchItem {
        return CatchItem(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME)),
            species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES)),
            totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_OZ))
                .takeIf { it > 0 },
            totalWeightHundredthPounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS))
                .takeIf { it > 0 },
            totalLengthQuarters = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_QUARTERS))
                .takeIf { it > 0 },
            totalLengthTenths = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_TENTHS))
                .takeIf { it > 0 },
            totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_KG))
                .takeIf { it > 0 },
            catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE)),
            markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)),
            clipColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIP_COLOR)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
        )
    }

    // ---- GPS helpers: insist on fresh + accurate locations (global) ----

    private fun isLocationFreshAndAccurate(loc: android.location.Location): Boolean {
        val maxAgeMs = 40_000L    // 40 seconds old
        val maxAccM  = 15f        // 15 meters ≈ 49 ft accuracy (The GPS Accuracy will try for 1 meter but in cloudy days may need 15 meter window)

        val ageMs = System.currentTimeMillis() - loc.time
        val accurate = loc.hasAccuracy() && loc.accuracy <= maxAccM

        Log.d("GPS_DEBUG", "📐 Location check: age=${ageMs}ms, accuracy=${loc.accuracy}m, maxAcc=${maxAccM}m, pass=${ageMs in 0..maxAgeMs && accurate}")

        return ageMs in 0..maxAgeMs && accurate
    }

    /**
     * Get a single good fix, or null if we can’t get one quickly/accurately.
     * Call this from insertCatch when GPS is enabled.
     */
    private fun getLastKnownLocation(callback: (android.location.Location?) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GPS_DEBUG", "❌ No location permission, cannot get GPS.")
            callback(null)
            return
        }

        fused.lastLocation
            .addOnSuccessListener { last ->
                if (last != null && isLocationFreshAndAccurate(last)) {
                    Log.d("GPS_DEBUG", "✅ Using lastLocation: ${last.latitude}, ${last.longitude}, acc=${last.accuracy}")
                    callback(last)
                } else {
                    Log.d("GPS_DEBUG", "ℹ️ lastLocation is null or not good enough, requesting fresh update...")

                    val req = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        1_000L
                    ).apply {
                        setMinUpdateIntervalMillis(500L)
                        setMaxUpdates(1)
                    }.build()


                    fused.requestLocationUpdates(
                        req,
                        object : com.google.android.gms.location.LocationCallback() {
                            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                val fresh = result.lastLocation
                                if (fresh != null && isLocationFreshAndAccurate(fresh)) {
                                    Log.d("GPS_DEBUG", "📡 Fresh GPS: ${fresh.latitude}, ${fresh.longitude}, acc=${fresh.accuracy}")
                                    callback(fresh)
                                } else {
                                    Log.w("GPS_DEBUG", "⚠️ Fresh GPS not accurate enough or null.")
                                    callback(null)
                                }
                                fused.removeLocationUpdates(this)
                            }
                        },
                        Looper.getMainLooper()
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("GPS_DEBUG", "❌ Failed to get location: ${e.message}")
                callback(null)
            }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    //---------- Google Map Query for Pins on map ------------

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
        if (species.isNotBlank() && species.lowercase() != "all") {
            whereClauses.add("REPLACE(LOWER(species), ' ', '') = ?")
            args.add(species.lowercase().replace(" ", ""))
        }

        // Optional catchType filter
        if (catchType.isNotBlank() && catchType.lowercase() != "all") {
            whereClauses.add("LOWER(catch_type) = ?")
            args.add(catchType.lowercase())
        }

        // Apply default date range if not provided
        val today = getCurrentDateTime().substringBefore(" ")
        val from = fromDate.ifBlank { today }
        val to = toDate.ifBlank { today }

        whereClauses.add("$COLUMN_DATE_TIME BETWEEN ? AND ?")
        args.add(from)
        args.add(to)

        // Measurement filter
        if (measurementType.isNotBlank() && measurementType.lowercase() != "all") {
            when (measurementType.lowercase()) {
                "weightkg", "kg" -> {
                    whereClauses.add("$COLUMN_TOTAL_WEIGHT_KG BETWEEN ? AND ?")
                    args.add((minValue * 100).toInt().toString())
                    args.add((maxValue * 100).toInt().toString())
                }
                "weight", "lbs", "lb" -> {
                    whereClauses.add("$COLUMN_TOTAL_WEIGHT_OZ BETWEEN ? AND ?")
                    args.add((minValue * 16).toInt().toString())
                    args.add((maxValue * 16).toInt().toString())
                }
                "weightpounds", "pounds", "pound" -> {
                    whereClauses.add("$COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS BETWEEN ? AND ?")
                    args.add((minValue * 100).toInt().toString())
                    args.add((maxValue * 100).toInt().toString())
                }
                "lengthcm", "cm" -> {
                    whereClauses.add("$COLUMN_TOTAL_LENGTH_TENTHS BETWEEN ? AND ?")
                    args.add((minValue * 10).toInt().toString())
                    args.add((maxValue * 10).toInt().toString())
                }
                "length", "inches", "in" -> {
                    whereClauses.add("$COLUMN_TOTAL_LENGTH_QUARTERS BETWEEN ? AND ?")
                    args.add((minValue * 4).toInt().toString())
                    args.add((maxValue * 4).toInt().toString())
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
    } // END getFilteredCatchesWithLocationAdvanced

    /**
     * Returns the number of catches of a given type since a timestamp.
     * Used for hot streak detection (e.g., catches in last 15 minutes).
     */
    fun getCatchCountSince(catchType: String, sinceMillis: Long): Int {
        val sinceDateTime = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
        ).format(java.util.Date(sinceMillis))

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM catches WHERE catch_type = ? AND date_time >= ?",
            arrayOf(catchType, sinceDateTime)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getLastNCatchesWithLocation(limit: Int): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM $TABLE_NAME
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY $COLUMN_DATE_TIME DESC
        LIMIT ?
        """.trimIndent(),
            arrayOf(limit.toString())
        )

        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }

        cursor.close()
        db.close()
        return list
    }


    // for Map Searches TOP 5 of Length or Weight in set Species...

    // Top 5 by lbs/oz within a date range
    fun getTopCatchesByLbsWithinDateRange(
        species: String,
        minOz: Int,
        maxOz: Int,
        fromDate: String,
        toDate: String,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER($COLUMN_SPECIES) = ?
              AND $COLUMN_TOTAL_WEIGHT_OZ BETWEEN ? AND ?
              AND strftime('%Y-%m-%d', $COLUMN_DATE_TIME) BETWEEN ? AND ?
            ORDER BY $COLUMN_TOTAL_WEIGHT_OZ DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minOz.toString(),
                maxOz.toString(),
                fromDate,
                toDate,
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


    // for Map Searches TOP 5 of Length or Weight in set Species...
    fun getTopCatchesByPoundsWithinDateRange(
        species: String,
        minHundredthsPounds: Int,
        maxHundredthsPounds: Int,
        fromDate: String,
        toDate: String,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER($COLUMN_SPECIES) = ?
              AND $COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS BETWEEN ? AND ?
              AND strftime('%Y-%m-%d', $COLUMN_DATE_TIME) BETWEEN ? AND ?
            ORDER BY $COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minHundredthsPounds.toString(),
                maxHundredthsPounds.toString(),
                fromDate,
                toDate,
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

    // for Map Searches TOP 5 of Length or Weight in set Species...

    fun getTopCatchesByKgWithinDateRange(
        species: String,
        minHundredthsKg: Int,
        maxHundredthsKg: Int,
        fromDate: String,
        toDate: String,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER($COLUMN_SPECIES) = ?
              AND $COLUMN_TOTAL_WEIGHT_KG BETWEEN ? AND ?
              AND strftime('%Y-%m-%d', $COLUMN_DATE_TIME) BETWEEN ? AND ?
            ORDER BY $COLUMN_TOTAL_WEIGHT_KG DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minHundredthsKg.toString(),
                maxHundredthsKg.toString(),
                fromDate,
                toDate,
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

    // for Map Searches TOP 5 of Length or Weight in set Species...
    fun getTopCatchesByInchesWithinDateRange(
        species: String,
        minQuarters: Int,
        maxQuarters: Int,
        fromDate: String,
        toDate: String,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER($COLUMN_SPECIES) = ?
              AND $COLUMN_TOTAL_LENGTH_QUARTERS BETWEEN ? AND ?
              AND strftime('%Y-%m-%d', $COLUMN_DATE_TIME) BETWEEN ? AND ?
            ORDER BY $COLUMN_TOTAL_LENGTH_QUARTERS DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minQuarters.toString(),
                maxQuarters.toString(),
                fromDate,
                toDate,
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

    // for Map Searches TOP 5 of Length or Weight in set Species...
    fun getTopCatchesByCmWithinDateRange(
        species: String,
        minTenths: Int,
        maxTenths: Int,
        fromDate: String,
        toDate: String,
        limit: Int
    ): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME
            WHERE LOWER($COLUMN_SPECIES) = ?
              AND $COLUMN_TOTAL_LENGTH_TENTHS BETWEEN ? AND ?
              AND strftime('%Y-%m-%d', $COLUMN_DATE_TIME) BETWEEN ? AND ?
            ORDER BY $COLUMN_TOTAL_LENGTH_TENTHS DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                species.lowercase(),
                minTenths.toString(),
                maxTenths.toString(),
                fromDate,
                toDate,
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





    //----------------------- GET MOTIVATIONAL MESSAGE INFORMATION ---------------------------------
    fun getCatchById(catchId: Int): CatchItem? {
        val db = readableDatabase
        val cursor = db.query(
            "catches",
            null,
            "id = ?",
            arrayOf(catchId.toString()),
            null,
            null,
            null
        )

        var catchItem: CatchItem? = null
        if (cursor.moveToFirst()) {
            catchItem = parseCatch(cursor)
        }

        cursor.close()
        return catchItem
    }

    //----------------------- GET Top Tournament Catches INFORMATION ---------------------------------
    fun getTopTournamentCatches(catchType: String, measurementMode: MeasurementMode, limit: Int): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // ── Pick the correct column to sort by based on measurement mode ──
        val orderColumn = when (measurementMode) {
            MeasurementMode.LBS_OZ -> COLUMN_TOTAL_WEIGHT_OZ
            MeasurementMode.POUNDS -> COLUMN_TOTAL_WEIGHT_HUNDREDTH_POUNDS
            MeasurementMode.KG     -> COLUMN_TOTAL_WEIGHT_KG
            MeasurementMode.INCHES -> COLUMN_TOTAL_LENGTH_QUARTERS
            MeasurementMode.CM     -> COLUMN_TOTAL_LENGTH_TENTHS
        }

        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_NAME 
            WHERE strftime('%Y-%m-%d', $COLUMN_DATE_TIME) = ?
              AND $COLUMN_CATCH_TYPE = ?
            ORDER BY $orderColumn DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(today, catchType, limit.toString())
        )

        while (cursor.moveToNext()) {
            catchList.add(parseCatch(cursor))
        }

        Log.d("DB_DEBUG", "🔎 getTopTournamentCatches: catchType=$catchType, mode=$measurementMode, limit=$limit, found=${catchList.size}")

        cursor.close()
        db.close()
        return catchList
    }//------------- END -- GET Top Tournament Catches INFORMATION  ---------------------------


    //============ Functions for Various Data Retrieval  =================================

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


    fun getAllCatchesExcludingPractice(): List<CatchItem> {
        val db = readableDatabase
        val list = mutableListOf<CatchItem>()
        val cursor = db.rawQuery(
            "SELECT * FROM catches WHERE LOWER(catch_type) != 'practice'",
            null
        )
        while (cursor.moveToNext()) {
            list.add(parseCatch(cursor))
        }
        cursor.close()
        db.close()
        return list
    }

}//----------------- END Catch Database Helper---------------------