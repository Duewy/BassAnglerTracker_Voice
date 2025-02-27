package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.util.Log
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem

class CatchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 5 // 🔺 Incremented version to trigger schema update
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        // Weight fields
        private const val COLUMN_WEIGHT_LBS = "weight_lbs"
        private const val COLUMN_WEIGHT_OZ = "weight_oz"
        private const val COLUMN_WEIGHT_DECIMAL = "weight_decimal"
        // Length fields
        private const val COLUMN_LENGTH_A8TH = "length_a8th"
        private const val COLUMN_LENGTH_INCHES = "length_inches"
        private const val COLUMN_LENGTH_DECIMAL = "length_decimal"
        // Type indicator ("weight" or "length")
        private const val COLUMN_CATCH_TYPE = "catch_type"
        private const val COLUMN_MARKER_TYPE = "marker_type" // ✅ Stores Bass type

    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCatchesTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_SPECIES TEXT NOT NULL,
                $COLUMN_WEIGHT_LBS INTEGER,
                $COLUMN_WEIGHT_OZ INTEGER,
                $COLUMN_WEIGHT_DECIMAL REAL,
                $COLUMN_LENGTH_A8TH INTEGER NULL,  -- ✅ Allow NULL values
                $COLUMN_LENGTH_INCHES INTEGER,
                $COLUMN_LENGTH_DECIMAL REAL,
                $COLUMN_CATCH_TYPE TEXT NOT NULL, -- Catch type column
                $COLUMN_MARKER_TYPE TEXT  
            )
        """.trimIndent()
        db.execSQL(createCatchesTable)

        val createTournamentTable = """
            CREATE TABLE IF NOT EXISTS tournament_catches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                marker_type TEXT NOT NULL,
                catch_limit INTEGER NOT NULL,
                unit_system TEXT NOT NULL,
                culling_enabled INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTournamentTable) // ✅ Ensures tournament table exists
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {  // 🔺 Only update if upgrading from an older version
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LENGTH_A8TH INTEGER NULL") // ✅ Fixes constraint error
            } catch (e: Exception) {
                Log.e("CatchDatabaseHelper", "Error upgrading database: ${e.message}")
            }
        }
    }

    fun insertCatch(catch: CatchItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, catch.dateTime)
            put(COLUMN_SPECIES, catch.species)
            put(COLUMN_WEIGHT_LBS, catch.weightLbs)
            put(COLUMN_WEIGHT_OZ, catch.weightOz)
            put(COLUMN_WEIGHT_DECIMAL, catch.weightDecimal)
            put(COLUMN_LENGTH_INCHES, catch.lengthInches)
            put(COLUMN_LENGTH_DECIMAL, catch.lengthDecimal)
            put(COLUMN_CATCH_TYPE, catch.catchType)
            put(COLUMN_MARKER_TYPE, catch.markerType)

            // ✅ Allow NULL for lengthA8th
            if (catch.lengthA8th != null) {
                put(COLUMN_LENGTH_A8TH, catch.lengthA8th)
            } else {
                putNull(COLUMN_LENGTH_A8TH)
            }
        }

        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun getAllCatches(): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE_TIME DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val weightLbs = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_LBS))
            val weightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_OZ))
            val weightDecimal = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_DECIMAL))
            val lengthA8th = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_A8TH)))
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_A8TH)) else null
            val lengthInches = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_INCHES))
            val lengthDecimal = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_DECIMAL))
            val catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE))
            val markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)) ?: "Unknown"

            catches.add(
                CatchItem(
                    id = id,
                    dateTime = dateTime,
                    species = species,
                    weightLbs = weightLbs,
                    weightOz = weightOz,
                    weightDecimal = weightDecimal,
                    lengthA8th = lengthA8th,
                    lengthInches = lengthInches,
                    lengthDecimal = lengthDecimal,
                    catchType = catchType,
                    markerType = markerType
                )
            )
        }
        cursor.close()
        db.close()
        return catches
    }

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

    fun insertTournamentCatch(markerType: String, catchLimit: Int, unitSystem: String, isCullingEnabled: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MARKER_TYPE, markerType) // ✅ Save Bass type (Large or Small Mouth)
            put("catch_limit", catchLimit)
            put("unit_system", unitSystem)
            put("culling_enabled", if (isCullingEnabled) 1 else 0)
        }
        val result = db.insert("tournament_catches", null, values)
        db.close()
        return result != -1L
    }
}
