package com.icm.se_icm.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.icm.se_icm.ConfigSensorActivity
import com.icm.se_icm.SeatbeltSensorActivity
import com.icm.se_icm.SelectDeviceActivity
import com.icm.se_icm.WeightSensorActivity
import java.util.logging.Handler

object BluetoothUtils {

    private var isScanning = false
    private var scanCallback: ScanCallback? = null

    /**
     * Escucha eventos de dispositivos Bluetooth detectados y, si el permiso está concedido,
     * extrae su información y la pasa a la actividad para que la muestre en la interfaz.
     */
    private val deviceFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    val deviceName = device.name ?: "Desconocido"
                    val deviceAddress = device.address
                    val deviceType = device.type

                    if (context is SelectDeviceActivity) {
                        context.addDeviceToScrollView(device, deviceName, deviceAddress, deviceType)
                    }
                } else {
                    Toast.makeText(context, "Permiso de Bluetooth no concedido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Verifica si el dispositivo soporta Bluetooth, si está habilitado y si tiene los permisos necesarios.
     * si no se cumplen estas condiciones, solicita al usuario habilitar Bluetooth o conceder permisos.
     * */
    fun checkBluetooth(context: Context, enableRequestCode: Int, permissionRequestCode: Int) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth no soportado en este dispositivo", Toast.LENGTH_SHORT).show()
            (context as? androidx.appcompat.app.AppCompatActivity)?.finish()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                (context as? androidx.appcompat.app.AppCompatActivity)?.startActivityForResult(enableBtIntent, enableRequestCode)
            } else {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    // Request Bluetooth and location permissions
                    ActivityCompat.requestPermissions(
                        context as androidx.appcompat.app.AppCompatActivity,
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        permissionRequestCode
                    )
                } else {
                    // Permissions granted, proceed with BLE scanning
                }
            }
        }
    }

    fun pairDeviceClasic(device: BluetoothDevice, context: Context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permiso de Bluetooth no concedido", Toast.LENGTH_SHORT).show()
                return
            }

            val method = device.javaClass.getMethod("createBond") // Llamar al método de emparejamiento
            method.invoke(device)

            Toast.makeText(context, "Intentando vincularse con ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al vincular el dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra un mensaje emergente con un mensaje dado.
     * @param context El contexto de la aplicación.
     * @param message El mensaje que se mostrará en el AlertDialog.
     */
    private fun showDialogOnMainThread(context: Context, message: String) {
        // Asegurarse de que el código se ejecute en el hilo principal
        (context as? Activity)?.runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(message)
                .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }
    }

    fun connectToBleDevice(device: BluetoothDevice, context: Context, selectedSensor: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showDialogOnMainThread(context, "Permiso de Bluetooth no concedido")
                    return
                }
            }

            device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BLE", "Conectado a ${device.name}")
                        showDialogOnMainThread(context, "Conectado a ${device.name}")
                        gatt?.discoverServices()
                        // val intent = Intent(context, SeatbeltSensorActivity::class.java)
                        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK  // Esto es necesario si el contexto no es una actividad
                        // context.startActivity(intent)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("BLE", "Desconectado de ${device.name}")
                        showDialogOnMainThread(context, "Desconectado de ${device.name}")
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                        Log.d("BLE", "Servicios descubiertos en ${gatt.device.name}")



                        val serviceList = ArrayList<String>()
                        for (service in gatt.services) {
                            val serviceInfo = StringBuilder("Servicio: ${service.uuid}")

                            for (characteristic in service.characteristics) {
                                serviceInfo.append("\n  ├── Característica: ${characteristic.uuid}")
                                serviceInfo.append("\n  │     Propiedades: ${getCharacteristicProperties(characteristic.properties)}")

                                for (descriptor in characteristic.descriptors) {
                                    serviceInfo.append("\n  │     ├── Descriptor: ${descriptor.uuid}")
                                }
                            }

                            serviceList.add(serviceInfo.toString())
                        }

                        // Pasar los datos a la otra actividad


                        if(selectedSensor.equals("seat_belt")){
                            val intent = Intent(context, SeatbeltSensorActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            intent.putStringArrayListExtra("services", serviceList)
                            context.startActivity(intent)
                        }

                        if(selectedSensor.equals("weight")){
                            val intent = Intent(context, WeightSensorActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            intent.putStringArrayListExtra("services", serviceList)
                            context.startActivity(intent)
                        }

                    } else {
                        Log.e("BLE", "Error al descubrir servicios, código: $status")
                    }
                }
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
            showDialogOnMainThread(context, "Error de permisos Bluetooth")
        }
    }

    /*
    *
    * for (service in gatt.services) {
                            Log.d("BLE", "Servicio encontrado: UUID=${service.uuid}")

                            for (characteristic in service.characteristics) {
                                Log.d("BLE", "  ├── Característica: UUID=${characteristic.uuid}")
                                Log.d("BLE", "  │     Propiedades: ${getCharacteristicProperties(characteristic.properties)}")

                                for (descriptor in characteristic.descriptors) {
                                    Log.d("BLE", "  │     ├── Descriptor: UUID=${descriptor.uuid}")
                                }
                            }
                        }
    *  */
    /**
     * Función auxiliar para interpretar las propiedades de la característica
     */
    private fun getCharacteristicProperties(properties: Int): String {
        val props = mutableListOf<String>()
        if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("LECTURA")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("ESCRITURA")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("NOTIFICACIÓN")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("INDICACIÓN")
        return props.joinToString(", ")
    }
}
