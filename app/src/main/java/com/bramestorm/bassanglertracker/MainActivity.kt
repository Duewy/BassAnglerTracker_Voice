package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.training.UserTrainingIndex

class MainActivity : AppCompatActivity() {

    private lateinit var btnUserPage:Button
    private lateinit var btnSetUp11:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openUserPage = findViewById<Button>(R.id.btnUserPage)
        openUserPage.setOnClickListener{
            val intent1 = Intent(this, UserTrainingIndex::class.java)
            startActivity(intent1)
        }
        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp11)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }


    }// `````````` END On Create  ``````````````````````


}// !!!!!!!!!!!!!!! END MainActivity !!!!!!!!!!!!!!!!!!!!!!!
