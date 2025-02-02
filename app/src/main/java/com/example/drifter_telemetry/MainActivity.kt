package com.example.drifter_telemetry

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private val deviceAddress = "A0:DD:6C:03:9E:42" // Replace with your ESP32 Bluetooth MAC address
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID

    // Declare views
    private lateinit var gForceView: GForceView
    private lateinit var leftWheelLine: View
    private lateinit var rightWheelLine: View
    private lateinit var rotationalRateGauge: RotationalRateGaugeView
    private lateinit var armedTextView: TextView
    private lateinit var steeringModeTextView: TextView
    private lateinit var driveModeTextView: TextView
    private lateinit var motor1RpmBar: ProgressBar
    private lateinit var motor2RpmBar: ProgressBar
    private lateinit var motor3RpmBar: ProgressBar
    private lateinit var motor4RpmBar: ProgressBar
    private lateinit var batteryStatusTextView: TextView
    private lateinit var batteryVoltageTextView: TextView

    private val REQUEST_ENABLE_BT = 1
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val updateIntervalMs: Long = 100
    private val buffer = StringBuilder()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_ENABLE_BT
            )
        } else {
            initializeBluetooth()
        }

        // Initialize views
        initializeViews()

        // Start fetching data
        startFetchingData()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth()
            } else {
                Log.e("MainActivity", "Bluetooth permissions denied")
            }
        }
    }

    private fun initializeViews() {
        gForceView = findViewById(R.id.gForceView)
        leftWheelLine = findViewById(R.id.leftWheelLine)
        rightWheelLine = findViewById(R.id.rightWheelLine)
        rotationalRateGauge = findViewById(R.id.rotationalRateGauge)
        armedTextView = findViewById(R.id.armedTextView)
        steeringModeTextView = findViewById(R.id.steeringModeTextView)
        driveModeTextView = findViewById(R.id.driveModeTextView)
        motor1RpmBar = findViewById(R.id.motor1RpmBar)
        motor2RpmBar = findViewById(R.id.motor2RpmBar)
        motor3RpmBar = findViewById(R.id.motor3RpmBar)
        motor4RpmBar = findViewById(R.id.motor4RpmBar)
        batteryStatusTextView = findViewById(R.id.batteryStatusTextView)
        batteryVoltageTextView = findViewById(R.id.batteryVoltageTextView)
    }

    private fun initializeBluetooth() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val device: BluetoothDevice? = pairedDevices?.find { it.address == deviceAddress }

        if (device == null) {
            Log.e("MainActivity", "Device with address $deviceAddress is not paired")
            return
        }

        Log.d("MainActivity", "Attempting to connect to device: $deviceAddress")
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            Log.d("MainActivity", "Bluetooth connected")
        } catch (e: IOException) {
            Log.e("MainActivity", "Failed to connect Bluetooth: ${e.message}")
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                closeException.printStackTrace()
            }
        }
    }

    private fun startFetchingData() {
        ioScope.launch {
            try {
                while (isActive) {
                    inputStream?.let {
                        val buffer = ByteArray(1024)
                        val bytesRead = it.read(buffer)
                        if (bytesRead > 0) {
                            val jsonData = String(buffer, 0, bytesRead)
                            processReceivedData(jsonData)
                        }
                    } ?: run {
                        Log.e("MainActivity", "InputStream is not initialized")
                        delay(1000) // Wait for a second before retrying
                    }
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Error reading Bluetooth data: ${e.message}")
            }
        }
    }

    private fun processReceivedData(data: String) {
        buffer.append(data)

        var startIndex = 0
        var endIndex: Int

        while (true) {
            endIndex = buffer.indexOf("\n", startIndex)
            if (endIndex == -1) break

            val jsonObject = buffer.substring(startIndex, endIndex).trim()
            if (jsonObject.isNotEmpty()) {
                try {
                    val json = JSONObject(jsonObject)
                    ioScope.launch {
                        delay(updateIntervalMs)
                        withContext(Dispatchers.Main) {
                            updateUI(json)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to parse JSON data: ${e.message}")
                }
            }
            startIndex = endIndex + 1
        }

        // Remove processed data from buffer
        buffer.delete(0, startIndex)
    }

    private fun updateUI(jsonObject: JSONObject) {
        if (jsonObject == null) return

        try {
            val gForceX = jsonObject.getDouble("g_force_x").toFloat()
            val gForceY = jsonObject.getDouble("g_force_y").toFloat()
            val steeringArray = jsonObject.getJSONArray("steering_values")
            val leftSteeringAngle = steeringArray.getDouble(0).toFloat()
            val rightSteeringAngle = steeringArray.getDouble(1).toFloat()
            val rotationalRate = jsonObject.getDouble("rotational_rate").toFloat()
            val armed = jsonObject.getString("arm_enabled")
            val steeringMode = jsonObject.getString("steering_mode")
            val driveMode = jsonObject.getString("drive_mode")
            val motorRpmArray = jsonObject.getJSONArray("motor_rpm")
            val motor1Rpm = motorRpmArray.getInt(0)
            val motor2Rpm = motorRpmArray.getInt(1)
            val motor3Rpm = motorRpmArray.getInt(2)
            val motor4Rpm = motorRpmArray.getInt(3)
            val batteryStatus = jsonObject.getString("battery_status")
            val batteryVoltage = jsonObject.getDouble("battery_voltage").toFloat()

            gForceView.updateGForce(gForceX, gForceY)
            leftWheelLine.rotation = leftSteeringAngle
            rightWheelLine.rotation = rightSteeringAngle
            rotationalRateGauge.updateRotationalRate(rotationalRate)
            armedTextView.text = armed
            steeringModeTextView.text = steeringMode
            driveModeTextView.text = driveMode
            motor1RpmBar.progress = motor1Rpm
            motor2RpmBar.progress = motor2Rpm
            motor3RpmBar.progress = motor3Rpm
            motor4RpmBar.progress = motor4Rpm
            batteryStatusTextView.text = batteryStatus
            batteryVoltageTextView.text = "%.2fV".format(batteryVoltage)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating UI: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel() // Cancel all coroutines
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}