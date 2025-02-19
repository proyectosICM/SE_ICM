package com.icm.se_icm.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.icm.se_icm.SelectDeviceActivity

object BluetoothScanUtils {
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
     * Inicia el escaneo de dispositivos de tipo desconocido, registra un BroadcastReceiver para
     * recibir dispositivos encontrados, y pasa la información del dispositivo a la actividad correspondiente.
     * Detiene el escaneo después de 20 segundos.
     */
    fun startBluetoothScanForType0(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as androidx.appcompat.app.AppCompatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        if (isScanning) {
            return
        }

        Toast.makeText(context, "Iniciando scaneo", Toast.LENGTH_SHORT).show()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Registrar el BroadcastReceiver al iniciar el escaneo
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(deviceFoundReceiver, filter)

        // Crear y guardar el ScanCallback
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val device = it.device
                    if(device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN){
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Pasar la información del dispositivo a la actividad
                            if (context is SelectDeviceActivity) {
                                context.addDeviceToScrollView(device, device.name ?: "Desconocido", device.address, device.type)
                            }
                        } else {
                            Toast.makeText(context, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(context, "Error en la búsqueda de dispositivos BLE", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothLeScanner.startScan(scanCallback)

        isScanning = true

        Looper.myLooper()?.let { looper ->
            android.os.Handler(looper).postDelayed({
                stopScan(context)
            }, 20000)
        }
    }

    /**
     * Inicia la búsqueda de dispositivos Bluetooth cercanos, incluyendo dispositivos de tipo clásico, BLE y Dual-mode.
     * Registra un BroadcastReceiver para recibir los dispositivos encontrados. Esta función no discrimina por tipo de dispositivo,
     * por lo que buscará todos los dispositivos Bluetooth disponibles en las cercanías.
     */
    fun startBluetoothScan(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as androidx.appcompat.app.AppCompatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.startDiscovery()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(deviceFoundReceiver, filter)
    }

    /**
     * Inicia el escaneo de dispositivos BLE y Dual-mode, registra un BroadcastReceiver para recibir
     * los dispositivos encontrados y pasa la información a la actividad correspondiente.
     * Detiene el escaneo automáticamente después de 20 segundos.
     */
    fun startScanForBleAndDualDevices(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as androidx.appcompat.app.AppCompatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        if (isScanning) {
            return
        }

        Toast.makeText(context, "Iniciando scaneo BLE/Dual-mode", Toast.LENGTH_SHORT).show()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Registrar el BroadcastReceiver al iniciar el escaneo
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(deviceFoundReceiver, filter)

        // Crear y guardar el ScanCallback
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val device = it.device
                    if (device.type == BluetoothDevice.DEVICE_TYPE_LE || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Pasar la información del dispositivo a la actividad
                            if (context is SelectDeviceActivity) {
                                context.addDeviceToScrollView(device, device.name ?: "Desconocido", device.address, device.type)
                            }
                        }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(context, "Error en la búsqueda de dispositivos BLE", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothLeScanner.startScan(scanCallback)

        isScanning = true

        Looper.myLooper()?.let { looper ->
            android.os.Handler(looper).postDelayed({
                stopScan(context)
            }, 20000)
        }
    }

    /**
     * Inicia el escaneo de dispositivos BLE (Bluetooth Low Energy), registra un BroadcastReceiver para
     * recibir los dispositivos encontrados, y pasa la información del dispositivo a la actividad correspondiente.
     * Detiene el escaneo automáticamente después de 20 segundos.
     */
    fun startBleScan(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as androidx.appcompat.app.AppCompatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        if (isScanning) {
            return // No iniciar si ya se está escaneando
        }

        Toast.makeText(context, "Iniciando scaneo BLE", Toast.LENGTH_SHORT).show()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Registrar el BroadcastReceiver al iniciar el escaneo
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(deviceFoundReceiver, filter)

        // Crear y guardar el ScanCallback
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val device = it.device

                    // Verificar si el dispositivo es BLE
                    if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Pasar la información del dispositivo a la actividad
                            if (context is SelectDeviceActivity) {
                                context.addDeviceToScrollView(device,device.name ?: "Desconocido", device.address, device.type
                                )
                            }
                        } else {
                            Toast.makeText(context, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(context, "Error en la búsqueda de dispositivos BLE", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothLeScanner.startScan(scanCallback)

        isScanning = true

        Looper.myLooper()?.let { looper ->
            android.os.Handler(looper).postDelayed({
                stopScan(context)
            }, 20000)
        }
    }

    /**
     * Detiene el escaneo de dispositivos Bluetooth.
     * Verifica que el escaneo esté en curso y que los permisos necesarios estén concedidos.
     * Si todo está correcto, detiene el escaneo, actualiza la interfaz de usuario (ocultando el ProgressBar y mostrando el texto correspondiente)
     * y restablece el estado de `isScanning` a `false`. Si se produce un error debido a permisos, se muestra un mensaje de error.
     */
    fun stopScan(context: Context) {
        if (!isScanning || scanCallback == null) {
            return // Si no se está escaneando o el callback no está disponible, no hacer nada
        }

        // Verificar permisos antes de detener el escaneo
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permisos de Bluetooth no concedidos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (isScanning) {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

                bluetoothLeScanner.stopScan(scanCallback)

                // Ocultar el ProgressBar
                val activity = context as SelectDeviceActivity
                activity.progressBar.visibility = ProgressBar.INVISIBLE
                activity.stopScanText.visibility = TextView.VISIBLE

                isScanning = false
                Toast.makeText(context, "Escaneo detenido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error al detener el escaneo BLE: Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }
}