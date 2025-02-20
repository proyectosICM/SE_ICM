package com.icm.se_icm

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.content.ContextCompat
import java.util.UUID

class WeightSensorActivity : AppCompatActivity() {

    private var messageCharacteristicUUID: UUID? = null
    private var bluetoothGatt: BluetoothGatt? = BleManager.bluetoothGatt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_sensor)

        val services = intent.getStringArrayListExtra("services")

        val btnSendMessage = findViewById<Button>(R.id.btn_send_message)
        btnSendMessage.setOnClickListener {
            sendMessage("Mensaje de prueba")  // Envía un mensaje cuando se haga clic
            Log.e("BLE", "Mensaje de prueba enviados")
        }

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

    fun sendMessage(message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLE", "Permiso de BLUETOOTH_CONNECT no concedido")
            return
        }

        if (bluetoothGatt == null) {
            Log.e("BLE", "BluetoothGatt no está conectado")
            return
        }

        val characteristicUUID = messageCharacteristicUUID ?: run {
            Log.e("BLE", "UUID de la característica no encontrado")
            return
        }

        val characteristic = findCharacteristic(characteristicUUID)
        if (characteristic == null) {
            Log.e("BLE", "Característica no encontrada en el dispositivo")
            return
        }

        characteristic.value = message.toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false

        if (success) {
            Log.d("BLE", "Mensaje enviado con éxito: $message")
        } else {
            Log.e("BLE", "Error al enviar el mensaje")
        }
    }

    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        bluetoothGatt?.services?.forEach { service: BluetoothGattService ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.uuid == uuid) {
                    return characteristic
                }
            }
        }
        return null
    }
}