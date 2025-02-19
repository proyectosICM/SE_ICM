package com.icm.se_icm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.util.UUID

class WeightSensorActivity : AppCompatActivity() {

    private var messageCharacteristicUUID: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_sensor)

        val services = intent.getStringArrayListExtra("services")

        services?.forEach { serviceInfo ->
            Log.d("BLE", serviceInfo)

            // Buscar características con escritura y notificación
            if (serviceInfo.contains("ESCRITURA") && serviceInfo.contains("NOTIFICACIÓN")) {
                val uuidPattern = Regex("[0-9a-fA-F-]{36}")  // Buscar UUIDs en el string
                val match = uuidPattern.findAll(serviceInfo).toList()

                if (match.size >= 2) {  // Asegurar que haya al menos 2 UUIDs (servicio y característica)
                    val characteristicUUID = match[1].value  // El segundo UUID es la característica
                    messageCharacteristicUUID = UUID.fromString(characteristicUUID)
                    Log.d("BLE", "Característica válida para mensajes encontrada: $messageCharacteristicUUID")
                }
            }
        }

        if (messageCharacteristicUUID == null) {
            Log.e("BLE", "No se encontró una característica válida para envío de mensajes")
        }
    }
}