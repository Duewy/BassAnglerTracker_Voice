package com.bramestorm.bassanglertracker.database

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.bramestorm.bassanglertracker.CatchItem
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

    // **************** - INSERT CATCH -  *****************************************

    fun insertCatch(catch: CatchItem): Boolean {

        val db = this.writableDatabase
        return try {
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
                // Add GPS coordinates if enabled
                val gpsEnabled = prefs.getBoolean("GPS_ENABLED", false)
                if (gpsEnabled) {
                    val location = getLastKnownLocation()
                    location?.let {
                        put(COLUMN_LATITUDE,catch.latitude)
                        put(COLUMN_LONGITUDE,catch.longitude)
                    }
                }
            }
            Log.d("DB_DEBUG", "🚀 insertCatch() called ")

            val success = db.insert(TABLE_NAME, null, values)
            success != -1L
        } catch (e: Exception) {
            Log.e("DB_ERROR", "❌ Error inserting catch: ${e.message}")
            false
        } finally {

            db.close() // ✅ Make sure to close the database
        }
    }


    // ************ - GET ALL CATCHES FOR LIST VIEWS  *******************************

    fun getCatchesForToday(catchType: String, todaysDate: String): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()

        Log.d("DB_DEBUG", "🔍 This is todaysDate in CatchDatabase: $todaysDate")
        Log.d("DB_DEBUG", "Fetching catches for today: $todaysDate, catchType: $catchType")

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM catches WHERE strftime('%Y-%m-%d', $COLUMN_DATE_TIME) = ? AND $COLUMN_CATCH_TYPE = ? ORDER BY $COLUMN_TOTAL_WEIGHT_OZ DESC",
            arrayOf(todaysDate, catchType)
        )

        if (cursor.moveToFirst()) {
            do {
                val catch = CatchItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME)),
                    species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES)),
                    totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_OZ)),            //Weight in Lbs and ounces
                    totalLengthA8th = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_8THS)),        //Length in Inches and 8ths
                    totalLengthTenths = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_TENTHS)),    //Length in millimeters
                    totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_KG)),   //Weight in 0.00 Kgs
                    catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE)),                  // Lbs, Inches, Cms, Kgs
                    markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)),                //Tournament Culling Limits
                    clipColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIP_COLOR))                   // Culling Clip colors from SoftLock
                )

                catchList.add(catch)
            } while (cursor.moveToNext())
        } else {
            Log.d("DB_DEBUG", "No data found for today: $todaysDate")
        }
        cursor.close()
        db.close()
        return catchList
    }


// ______________ EDIT Existing CATCH _______________________________

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

        values.put("species", species) // Always update species

        // ✅ Update the correct weight/length field based on non-null values
        if (newWeightOz != null) values.put("total_weight_oz", newWeightOz)
        if (newWeightKg != null) values.put("total_weight_hundredth_kg", newWeightKg)
        if (newLengthA8ths != null) values.put("total_length_8ths", newLengthA8ths)
        if (newLengthCm != null) values.put("total_length_tenths", newLengthCm)

        Log.d(
            "DB_DEBUG",
            "🔄 Updating ID=$catchId, " +
                    "New WeightOz=${newWeightOz ?: "N/A"}, " +
                    "New WeightKg=${newWeightKg ?: "N/A"}, " +
                    "New LengthA8ths=${newLengthA8ths ?: "N/A"}, " +
                    "New LengthCm=${newLengthCm ?: "N/A"}, " +
                    "New Species=$species"
        )

        val rowsUpdated = db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(catchId.toString()))

        if (rowsUpdated > 0) {
            Log.d("DB_DEBUG", "✅ Catch Updated Successfully: ID=$catchId")
        } else {
            Log.e("DB_DEBUG", "⚠️ Update FAILED for ID=$catchId")
        }

        db.close()
    }


    //_______________ DELETE CATCH FROM DATA BASE  _______________________

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    private var resetPoint: String? = null

    fun setTournamentResetPoint() {
        resetPoint = getCurrentDateTime() // Store current timestamp
        Log.d("DB_DEBUG", "✅ Tournament Reset Point Set: $resetPoint")
    }

    fun getTournamentResetPoint(): String? {
        return resetPoint
    }

    // $$$$$$$$$$$$ Get Last Known Location  $$$$$$$$$$$$$$$$$$$$$$$$$$$$

    private fun getLastKnownLocation(): android.location.Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers = locationManager.getProviders(true)
        for (provider in providers.reversed()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) return location
        }
        return null
    }

    //++++++++++++++++ Date and Time  +++++++++++++++++++++++++++++
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }


}//~~~~~~~~~~~~~~~~~ END of CatchDatabaseHelper.kt ~~~~~~~~~~~~~~~~~~~~~~~~~~
