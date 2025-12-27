package com.igino.tastiera

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.concurrent.Executors

@SuppressLint("MissingPermission") // Permissions are checked before use
class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    private lateinit var scanLayout: LinearLayout
    private lateinit var gamepadLayout: ConstraintLayout
    private lateinit var scanButton: Button
    private lateinit var devicesListView: ListView
    private lateinit var listAdapter: ArrayAdapter<String>
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceNameList = mutableListOf<String>()

    private val executor = Executors.newSingleThreadExecutor()

    private val buttonStates = BooleanArray(16) // For 16 buttons
    private var dpadState: Int = 8 // Neutral position for D-pad

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false) &&
                    permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false) &&
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        } else {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        }

        if (allGranted) {
            startBluetoothOperations()
        } else {
            Log.e(TAG, "Permissions not granted")
        }
    }

    private val requestEnableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startBluetoothOperations()
        } else {
            Log.e(TAG, "Bluetooth not enabled by user")
        }
    }

    private val hidDeviceCallback: BluetoothHidDevice.Callback? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                super.onAppStatusChanged(pluggedDevice, registered)
                Log.d(TAG, "onAppStatusChanged: registered=$registered, device=$pluggedDevice")
            }

            override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                super.onConnectionStateChanged(device, state)
                Log.d(TAG, "onConnectionStateChanged: device=$device, state=$state")
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    hostDevice = device
                    runOnUiThread {
                        scanLayout.visibility = View.GONE
                        gamepadLayout.visibility = View.VISIBLE
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    hostDevice = null
                    runOnUiThread {
                        scanLayout.visibility = View.VISIBLE
                        gamepadLayout.visibility = View.GONE
                    }
                }
            }
        }
    } else {
        null
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                Log.i(TAG, "HID Device profile connected")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        "Android Gamepad",
                        "Gamepad for Android TV",
                        "Android",
                        0x08.toByte(), // Subclass: Gamepad
                        HidReportConstants.GAMEPAD_REPORT_DESCRIPTOR
                    )
                    hidDevice?.registerApp(sdp, null, null, executor, hidDeviceCallback!!)
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i(TAG, "HID Device profile disconnected")
                hidDevice = null
            }
        }
    }

    private val deviceDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let {
                    val deviceName = it.name ?: "Unknown"
                    if (it.name != null && !deviceList.any { d -> d.address == it.address }) {
                        deviceList.add(it)
                        deviceNameList.add(deviceName)
                        listAdapter.notifyDataSetChanged()
                        Log.i(TAG, "Found device: $deviceName - ${it.address}")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scanLayout = findViewById(R.id.scan_layout)
        gamepadLayout = findViewById(R.id.gamepad_layout)
        scanButton = findViewById(R.id.scan_button)
        devicesListView = findViewById(R.id.devices_list_view)
        listAdapter = ArrayAdapter(this, R.layout.list_item_device, deviceNameList)
        devicesListView.adapter = listAdapter

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            val device = deviceList[position]
            connectToDevice(device)
        }

        scanButton.setOnClickListener {
            startScan()
        }

        // Re-enable all buttons
        findViewById<Button>(R.id.button_a).setOnTouchListener(getButtonTouchListener(0))
        findViewById<Button>(R.id.button_b).setOnTouchListener(getButtonTouchListener(1))
        findViewById<Button>(R.id.button_x).setOnTouchListener(getButtonTouchListener(2))
        findViewById<Button>(R.id.button_y).setOnTouchListener(getButtonTouchListener(3))

        findViewById<Button>(R.id.button_up).setOnTouchListener(getDpadTouchListener(0)) // Up
        findViewById<Button>(R.id.button_down).setOnTouchListener(getDpadTouchListener(4)) // Down
        findViewById<Button>(R.id.button_left).setOnTouchListener(getDpadTouchListener(6)) // Left
        findViewById<Button>(R.id.button_right).setOnTouchListener(getDpadTouchListener(2)) // Right

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionRequest.launch(requiredPermissions)

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceDiscoveryReceiver, filter)

        startBluetoothOperations()
    }

    private fun startBluetoothOperations() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
            return
        }

        bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private fun startScan() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        deviceList.clear()
        deviceNameList.clear()
        listAdapter.notifyDataSetChanged()
        Log.d(TAG, "Starting discovery...")
        bluetoothAdapter.startDiscovery()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to ${device.name}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            hidDevice?.connect(device)
        }
    }

    private fun getButtonTouchListener(buttonIndex: Int): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> buttonStates[buttonIndex] = true
                MotionEvent.ACTION_UP -> {
                    buttonStates[buttonIndex] = false
                    view.performClick()
                }
            }
            sendGamepadState()
            true
        }
    }

    private fun getDpadTouchListener(direction: Int): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            dpadState = if (event.action == MotionEvent.ACTION_DOWN) direction else 8 // 8 is neutral
            sendGamepadState()
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            true
        }
    }

    private fun sendGamepadState() {
        hostDevice?.let { device ->
            var buttonBits = 0
            for (i in buttonStates.indices) {
                if (buttonStates[i]) {
                    buttonBits = buttonBits or (1 shl i)
                }
            }

            // Report size is 7 bytes:
            // 2 bytes for buttons
            // 1 byte for D-Pad
            // 4 bytes for analog sticks (X, Y, Z, Rz)
            val report = ByteArray(7)
            report[0] = (buttonBits and 0xFF).toByte()
            report[1] = (buttonBits shr 8 and 0xFF).toByte()
            report[2] = dpadState.toByte()
            report[3] = 0 // Neutral X
            report[4] = 0 // Neutral Y
            report[5] = 0 // Neutral Z
            report[6] = 0 // Neutral Rz

            val reportString = report.joinToString { "%02X".format(it) }
            Log.d(TAG, "Attempting to send report: $reportString")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (hidDevice?.sendReport(device, HidReportConstants.REPORT_ID, report) == true) {
                    Log.d(TAG, "Report sent successfully.")
                } else {
                    Log.e(TAG, "Failed to send report.")
                }
            }
        } ?: Log.w(TAG, "sendGamepadState called but no host device connected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceDiscoveryReceiver)
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        hidDevice?.let { proxy ->
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
