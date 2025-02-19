package com.icm.se_icm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class ConfigSensorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_sensor)

        findViewById<CardView>(R.id.card_seat_belt)?.setOnClickListener { openSelectDevice(it) }
        findViewById<CardView>(R.id.card_weight)?.setOnClickListener { openSelectDevice(it) }
        findViewById<CardView>(R.id.card_other)?.setOnClickListener { other(it) }
    }

    fun openSelectDevice(view: View?) {
        val sensorType = view?.tag as? String ?: return

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        prefs.edit().putString("selected_sensor", sensorType).apply()

        startActivity(Intent(this, SelectDeviceActivity::class.java))
    }

    fun other(view: View?){
        Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
    }
}