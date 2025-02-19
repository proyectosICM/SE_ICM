package com.icm.se_icm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.icm.se_icm.utils.BluetoothScanUtils
import com.icm.se_icm.utils.BluetoothUtils


class SelectDeviceActivity : AppCompatActivity() {
    private val REQUEST_PERMISSION_LOCATION = 1
    private val REQUEST_PERMISSION_BLUETOOTH = 2
    private val REQUEST_ENABLE_BLUETOOTH = 3

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var deviceListLayout: LinearLayout
    lateinit var progressBar: ProgressBar
    lateinit var stopScanText : TextView

    // Usamos un Set para almacenar direcciones MAC de dispositivos encontrados
    private val discoveredDevices = mutableSetOf<String>()

    private lateinit var prefs: SharedPreferences
    private var selectedSensor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        deviceListLayout = findViewById(R.id.deviceListLayout)
        progressBar = findViewById(R.id.progressBar)
        stopScanText = findViewById(R.id.stopScanText)

        BluetoothUtils.checkBluetooth(this, REQUEST_ENABLE_BLUETOOTH, REQUEST_PERMISSION_BLUETOOTH)
        BluetoothScanUtils.startBleScan(this)

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        selectedSensor = prefs.getString("selected_sensor", "")
        //Toast.makeText(this, "selected_sensor $selectedSensor", Toast.LENGTH_SHORT).show()
    }

    fun addDeviceToScrollView(device: BluetoothDevice, deviceName: String, deviceAddress: String, deviceType: Int) {
        // Verificar si la dirección MAC ya está en la lista de dispositivos descubiertos
        if (discoveredDevices.contains(deviceAddress)) {
            return // Ya está agregado, no lo agregamos nuevamente
        }

        // Agregar la dirección MAC a la lista para evitar duplicados
        discoveredDevices.add(deviceAddress)

        // Crear un nuevo CardView
        val cardView = CardView(this)
        val cardLayout = LinearLayout(this)
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(16, 16, 16, 16)
        cardLayout.setBackgroundColor(getResources().getColor(android.R.color.black))

        // Crear los elementos dentro del CardView
        val deviceNameTextView = TextView(this)
        deviceNameTextView.text = "Dispositivo: $deviceName"
        deviceNameTextView.textSize = 16f
        deviceNameTextView.setTextColor(getResources().getColor(android.R.color.white))

        val deviceAddressTextView = TextView(this)
        deviceAddressTextView.text = "MAC: $deviceAddress"
        deviceAddressTextView.textSize = 14f
        deviceAddressTextView.setTextColor(getResources().getColor(android.R.color.darker_gray))

        val deviceTypeTextView = TextView(this)
        deviceTypeTextView.text = "Type: $deviceType"
        deviceTypeTextView.textSize = 14f
        deviceTypeTextView.setTextColor(getResources().getColor(android.R.color.darker_gray))

        val selectButton = Button(this)
        selectButton.text = "Seleccionar"
        selectButton.setOnClickListener {
            // Crear un AlertDialog para confirmar la vinculación
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Vinculación de dispositivo")
            builder.setMessage("¿Desea vincularse a $deviceName?")

            // Botón positivo: Confirmar vinculación
            builder.setPositiveButton("Sí") { dialog, _ ->
                Toast.makeText(this, "Intentando vincularse a $deviceName...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                BluetoothUtils.connectToBleDevice(device, this, selectedSensor)
            }

            // Botón negativo: Cancelar
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            // Mostrar el diálogo
            builder.create().show()
        }

        // Agregar los elementos al layout del CardView
        cardLayout.addView(deviceNameTextView)
        cardLayout.addView(deviceAddressTextView)
        cardLayout.addView(deviceTypeTextView)
        cardLayout.addView(selectButton)

        // Agregar el layout al CardView
        cardView.addView(cardLayout)

        // Agregar el CardView al LinearLayout de la lista
        deviceListLayout.addView(cardView)
    }


    // Manejar la respuesta de la solicitud de activación de Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                //Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show()
                // Aquí puedes continuar con la búsqueda de dispositivos
                //BluetoothUtils.startBleScan(this)
            } else {
                Toast.makeText(this, "El Bluetooth no fue activado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Manejar la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_BLUETOOTH) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de Bluetooth concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "El permiso de Bluetooth no fue concedido", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener el escaneo BLE cuando la actividad se pausa
        BluetoothScanUtils.stopScan(this)
    }

    override fun onStop() {
        super.onStop()
        // Detener el escaneo BLE cuando la actividad se detiene
        BluetoothScanUtils.stopScan(this)
    }
}