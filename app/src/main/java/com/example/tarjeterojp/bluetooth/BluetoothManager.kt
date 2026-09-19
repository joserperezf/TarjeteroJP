package com.example.tarjeterojp.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.tarjeterojp.data.Card
import com.example.tarjeterojp.data.Profile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission") // Permissions handled by UI/Activity
class AppBluetoothManager(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val _discoveredDevices = MutableSharedFlow<BluetoothDevice>()
    val discoveredDevices: SharedFlow<BluetoothDevice> = _discoveredDevices.asSharedFlow()

    private val _incomingCards = MutableSharedFlow<Card>()
    val incomingCards: SharedFlow<Card> = _incomingCards.asSharedFlow()

    private val _bondStates = MutableSharedFlow<Pair<BluetoothDevice, Int>>()
    val bondStates: SharedFlow<Pair<BluetoothDevice, Int>> = _bondStates.asSharedFlow()

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    private val NAME = "TarjeteroJP"

    private var serverJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        coroutineScope.launch {
                            _discoveredDevices.emit(it)
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    device?.let {
                        coroutineScope.launch {
                            _bondStates.emit(it to bondState)
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter?.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }

    fun pairDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond()
        }
    }

    fun startServer() {
        if (serverJob?.isActive == true) return
        serverJob = coroutineScope.launch(Dispatchers.IO) {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                var shouldLoop = true
                while (shouldLoop) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        shouldLoop = false
                        null
                    }
                    socket?.let {
                        manageConnectedSocket(it)
                        serverSocket?.close()
                        shouldLoop = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendProfile(device: BluetoothDevice, profile: Profile) {
        coroutineScope.launch(Dispatchers.IO) {
            bluetoothAdapter?.cancelDiscovery()
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                socket.connect()

                val jsonAdapter = moshi.adapter(Profile::class.java)
                val jsonStr = jsonAdapter.toJson(profile)

                val outStream: OutputStream = socket.outputStream
                outStream.write(jsonStr.toByteArray())
                outStream.flush()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    socket?.close()
                } catch (closeException: Exception) {
                    closeException.printStackTrace()
                }
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inStream: InputStream = socket.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int
                val sb = StringBuilder()

                while (true) {
                    try {
                        bytes = inStream.read(buffer)
                        if (bytes == -1) break
                        val readMessage = String(buffer, 0, bytes)
                        sb.append(readMessage)
                    } catch (e: Exception) {
                        break
                    }
                }
                
                val receivedJson = sb.toString()
                if (receivedJson.isNotEmpty()) {
                    val jsonAdapter = moshi.adapter(Profile::class.java)
                    val profile = jsonAdapter.fromJson(receivedJson)
                    profile?.let {
                        val card = Card(it.name, it.phone, it.email)
                        _incomingCards.emit(card)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Restart server for the next connection
                startServer()
            }
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        serverJob?.cancel()
    }
}