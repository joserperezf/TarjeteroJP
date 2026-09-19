package com.example.tarjeterojp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tarjeterojp.bluetooth.AppBluetoothManager
import com.example.tarjeterojp.data.Profile
import com.example.tarjeterojp.data.SharedPreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen(
    bluetoothManager: AppBluetoothManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { SharedPreferencesManager(context) }
    
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var pairedDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    
    val makeVisibleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Handled visibility request
    }

    LaunchedEffect(bluetoothManager, permissionsGranted) {
        if (permissionsGranted) {
            pairedDevices = bluetoothManager.getPairedDevices()
        }
        bluetoothManager.discoveredDevices.collect { device ->
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device)
            }
        }
    }
    
    LaunchedEffect(bluetoothManager) {
        bluetoothManager.bondStates.collect { (_, state) ->
            if (state == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(context, "Dispositivo vinculado", Toast.LENGTH_SHORT).show()
                pairedDevices = bluetoothManager.getPairedDevices()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (!permissionsGranted) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Otorgar permisos de Bluetooth")
                }
            }
            return@Column
        }

        Text(
            text = "Tablero Bluetooth",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedButton(
                onClick = {
                    Toast.makeText(context, "Buscando...", Toast.LENGTH_SHORT).show()
                    discoveredDevices.clear()
                    bluetoothManager.startDiscovery()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Rounded.BluetoothSearching, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Buscar dispositivos")
            }

            ElevatedButton(
                onClick = {
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    makeVisibleLauncher.launch(discoverableIntent)
                    bluetoothManager.startServer()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hacer visible")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dispositivos Vinculados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pairedDevices) { device ->
                DeviceCard(
                    device = device,
                    onClick = {
                        val profile = sharedPrefs.getProfile() ?: Profile("Anónimo", "", "")
                        Toast.makeText(context, "Enviando tarjeta...", Toast.LENGTH_SHORT).show()
                        bluetoothManager.sendProfile(device, profile)
                        // In a real app we might want to track success, for now assume sent
                        Toast.makeText(context, "Tarjeta enviada exitosamente", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dispositivos Encontrados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(discoveredDevices) { device ->
                DeviceCard(
                    device = device,
                    onClick = {
                        bluetoothManager.pairDevice(device)
                        Toast.makeText(context, "Vinculando...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceCard(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Dispositivo Desconocido",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Conectar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}