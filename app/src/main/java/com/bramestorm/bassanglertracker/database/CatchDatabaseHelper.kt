package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 6
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
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
            $COLUMN_SPECIES TEXT NOT NULL,
            $COLUMN_TOTAL_WEIGHT_OZ INTEGER,
            $COLUMN_TOTAL_LENGTH_8THS INTEGER,
            $COLUMN_TOTAL_WEIGHT_KG INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_TENTHS INTEGER,
            $COLUMN_CATCH_TYPE TEXT NOT NULL,
            $COLUMN_MARKER_TYPE TEXT,
            $COLUMN_CLIP_COLOR TEXT
        )
    """.trimIndent()
        db.execSQL(createCatchesTable)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {  // 🚨 Ensure this runs only if upgrading from an older version

            // Step 1: Rename the old table
            db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO old_$TABLE_NAME")

            // Step 2: Create the new table with the correct schema
            db.execSQL(
                """
            CREATE TABLE $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date_time TEXT NOT NULL,
                species TEXT NOT NULL,
                total_weight_oz INTEGER,
                total_length_8ths INTEGER,
                total_weight_hundredth_kg INTEGER DEFAULT 0,  -- ✅ Ensure this column is included
                total_length_tenths INTEGER,
                catch_type TEXT NOT NULL,
                marker_type TEXT,
                clip_color TEXT
            )
        """.trimIndent()
            )

            // Step 3: Copy data from the old table to the new table
            db.execSQL(
                """
            INSERT INTO $TABLE_NAME (id, date_time, species, total_weight_oz, total_length_8ths, total_length_tenths, catch_type, marker_type, clip_color)
            SELECT id, date_time, species, total_weight_oz, total_length_8ths, total_length_tenths, catch_type, marker_type, clip_color FROM old_$TABLE_NAME
        """.trimIndent()
            )

            // Step 4: Drop the old table
            db.execSQL("DROP TABLE old_$TABLE_NAME")
        }
    }


    fun insertCatch(catch: CatchItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, catch.dateTime) // ✅ Correctly maps dateTime -> date_time
            put(COLUMN_SPECIES, catch.species)
            put(COLUMN_TOTAL_WEIGHT_OZ, catch.totalWeightOz)
            put(COLUMN_TOTAL_LENGTH_8THS, catch.totalLengthA8th)
            put(COLUMN_TOTAL_LENGTH_TENTHS, catch.lengthDecimalTenthCm)
            put(COLUMN_TOTAL_WEIGHT_KG, catch.totalWeightHundredthKg)
            put(COLUMN_CATCH_TYPE, catch.catchType)
            put(COLUMN_MARKER_TYPE, catch.markerType)
            put(COLUMN_CLIP_COLOR, catch.clipColor)
        }

        val result = db.insert(TABLE_NAME, null, values)
        db.close()

        return result != -1L // ✅ Returns `true` if insert was successful
    }



    fun getAllCatches(): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Get today's date
        val cursor = db.rawQuery(
            "SELECT * FROM catches WHERE date_time LIKE ? AND catchType = ? ORDER BY totalWeightOz DESC",
            arrayOf("$todayDate%", "lbsOzs")
        )



        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_OZ))
            val totalLengthA8th = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_8THS))
            val totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_KG))
            val lengthDecimalTenthCm = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_TENTHS))
            val catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE))
            val markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)) ?: "Unknown"
            val clipColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIP_COLOR))

            catches.add(
                CatchItem(
                    id = id,
                    dateTime = dateTime,
                    species = species,
                    totalWeightOz = totalWeightOz,
                    totalLengthA8th = totalLengthA8th,
                    totalWeightHundredthKg = totalWeightHundredthKg,
                    lengthDecimalTenthCm = lengthDecimalTenthCm,
                    catchType = catchType,
                    markerType = markerType,
                    clipColor = clipColor
                )
            )
        }
        cursor.close()
        db.close()
        return catches
    }

    fun getCatchCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) // ✅ Now correctly retrieves catch count
        }
        cursor.close()
        return count
    }

    fun getCatchesForToday(catchType: String, todayDate: String): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()

        val cursor = db.rawQuery(
            "SELECT * FROM catches WHERE dateTime LIKE ? AND catchType = ? ORDER BY totalWeightOz DESC",
            arrayOf("$todayDate%", catchType)
        )

        if (cursor.moveToFirst()) {
            do {
                val catch = CatchItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow("dateTime")),
                    species = cursor.getString(cursor.getColumnIndexOrThrow("species")),
                    totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow("totalWeightOz")),
                    totalLengthA8th = null,
                    lengthDecimalTenthCm = null,
                    totalWeightHundredthKg = null,
                    catchType = cursor.getString(cursor.getColumnIndexOrThrow("catchType")),
                    markerType = cursor.getString(cursor.getColumnIndexOrThrow("markerType")),
                    clipColor = null
                )
                catchList.add(catch)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return catchList
    }


    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    fun logAllTournamentCatches() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM catches WHERE catchType = 'kgs'", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val species = cursor.getString(cursor.getColumnIndexOrThrow("species"))
                val totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow("totalWeightHundredthKg"))

                Log.d("TournamentKgsDebug", "DB Entry: ID=$id, Species=$species, Weight=${totalWeightHundredthKg} hundredth kg")
            } while (cursor.moveToNext())
        } else {
            Log.e("TournamentKgsDebug", "Database is empty for Tournament Kgs!")
        }

        cursor.close()
    }

}
