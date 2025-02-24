package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItemInches
import android.util.Log


class CatchDatabaseHelperInches(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_LENGTH_INCHES = "lengthInches"
        private const val COLUMN_LENGTH_A8TH= "lengtha8th"
        private const val COLUMN_LENGTH_DECIMAL = "lengthInDec"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
        CREATE TABLE catches (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date_time TEXT NOT NULL,
            species TEXT NOT NULL,
            lengtha8th INTEGER NOT NULL,
            lengthInches INTEGER NOT NULL,
            lengthInDec REAL NOT NULL  -- ✅ Ensure this column exists
        )
    """.trimIndent()

        db.execSQL(createTableQuery)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB_Upgrade", "Upgrading database from version $oldVersion to $newVersion...")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")  // ✅ Delete the old table
        onCreate(db)  // ✅ Recreate the table with correct columns
    }


    fun insertCatch(dateTime: String, species: String, lengtha8th: Int, lengthInches: Int, lengthInDec: Float) {
        val values = ContentValues().apply {
            put("date_time", dateTime)
            put("species", species)
            put("lengtha8th", lengtha8th)
            put("lengthInches", lengthInches)
            put("lengthInDec", lengthInDec)
        }

        val db = writableDatabase
        val result = db.insert("catches", null, values)
        db.close()

        if (result == -1L) {
            Log.e("DB_Error", "Failed to insert catch: $species, $lengthInDec.$lengtha8th inches")
        } else {
            Log.d("DB_Success", "Inserted catch: $species, $lengthInDec.$lengtha8th inches")
        }
    }



    fun getAllCatches(): List<CatchItemInches> {
        val catches = mutableListOf<CatchItemInches>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM catches ORDER BY date_time DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"))
            val species = cursor.getString(cursor.getColumnIndexOrThrow("species"))
            val lengthInches = cursor.getInt(cursor.getColumnIndexOrThrow("lengthInches"))
            val lengtha8th = cursor.getInt(cursor.getColumnIndexOrThrow("lengtha8th"))
            val lengthInDec = cursor.getFloat(cursor.getColumnIndexOrThrow("lengthInDec"))

            val catchItem = CatchItemInches(id, dateTime, species, lengtha8th, lengthInches, lengthInDec)
            catches.add(catchItem)

            Log.d("DB_Debug", "Fetched Catch: $catchItem")
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
}
