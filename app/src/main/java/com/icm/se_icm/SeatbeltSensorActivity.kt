package com.icm.se_icm

import android.bluetooth.BluetoothGatt
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast

class SeatbeltSensorActivity : AppCompatActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val SERVICE_UUID = "0000180F-0000-1000-8000-00805F9B34FB" // Reemplázalo con el UUID correcto
    private val CHARACTERISTIC_UUID = "00002A19-0000-1000-8000-00805F9B34FB" // Reemplázalo con el UUID correcto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seatbelt_sensor)

        val services = intent.getStringArrayListExtra("services")

        services?.forEach { serviceInfo ->
            Log.d("BLE", serviceInfo)
        }

        val btnSendMessage = findViewById<Button>(R.id.btn_send_message)
        btnSendMessage.setOnClickListener {
            sendMessage("Hola BLE")  // Mensaje que se enviará
        }
    }

    fun setBluetoothGatt(gatt: BluetoothGatt) {
        bluetoothGatt = gatt
    }

    private fun sendMessage(message: String) {
        val gatt = bluetoothGatt ?: return

        val service = gatt.getService(java.util.UUID.fromString(SERVICE_UUID))
        val characteristic = service?.getCharacteristic(java.util.UUID.fromString(CHARACTERISTIC_UUID))

        if (characteristic != null) {
            characteristic.value = message.toByteArray()
            gatt.writeCharacteristic(characteristic)
            Toast.makeText(this, "Mensaje enviado: $message", Toast.LENGTH_SHORT).show()
            Log.d("BLE", "Mensaje enviado: $message")
        } else {
            Toast.makeText(this, "Característica no encontrada", Toast.LENGTH_SHORT).show()
            Log.e("BLE", "Característica no encontrada")
        }
    }
}