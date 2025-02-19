package com.icm.se_icm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun openSensorConfiguration(view: View?) {
        val intent = Intent(
            this, ConfigSensorActivity::class.java
        )
        startActivity(intent)
    }

    fun other(view: View?){
        Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
    }
}