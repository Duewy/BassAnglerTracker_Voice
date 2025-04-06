package com.bramestorm.bassanglertracker

import android.app.Activity
import android.os.Bundle
import android.widget.Button

class PopupMapQuery: Activity() {

    private lateinit var btnDayMap :Button
    private lateinit var btnSpeciesMapQuery: Button
    private lateinit var btnTypeOfDayMapQuery :Button
    private lateinit var btnSizesMapQuery: Button
    private lateinit var btnMeasurementsMapQuery: Button
    private lateinit var btnGetQueryMap : Button
    private lateinit var btnMainPgMapQuery : Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_map_query)

    }
}