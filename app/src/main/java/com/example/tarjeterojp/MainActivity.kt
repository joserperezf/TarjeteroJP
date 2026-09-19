package com.example.tarjeterojp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import com.example.tarjeterojp.bluetooth.AppBluetoothManager
import com.example.tarjeterojp.data.SharedPreferencesManager
import com.example.tarjeterojp.ui.screens.DashboardScreen
import com.example.tarjeterojp.ui.screens.InboxScreen
import com.example.tarjeterojp.ui.screens.ProfileScreen
import com.example.tarjeterojp.ui.theme.TarjeteroJPTheme

private sealed interface TopLevelRoute {
    val icon: ImageVector
    val label: String
}
private data object ProfileRoute : TopLevelRoute { 
    override val icon = Icons.Default.AccountCircle 
    override val label = "Perfil"
}
private data object DashboardRoute : TopLevelRoute { 
    override val icon = Icons.Default.Bluetooth 
    override val label = "Tablero"
}
private data object InboxRoute : TopLevelRoute { 
    override val icon = Icons.Default.Inbox 
    override val label = "Bandeja"
}

private val TOP_LEVEL_ROUTES: List<TopLevelRoute> = listOf(ProfileRoute, DashboardRoute, InboxRoute)

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothManager: AppBluetoothManager
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bluetoothManager = AppBluetoothManager(this)
        sharedPreferencesManager = SharedPreferencesManager(this)

        setContent {
            TarjeteroJPTheme {
                val topLevelBackStack = remember { TopLevelBackStack<Any>(DashboardRoute) }
                
                LaunchedEffect(bluetoothManager) {
                    bluetoothManager.incomingCards.collect { card ->
                        val existingCards = sharedPreferencesManager.getReceivedCards().toMutableList()
                        existingCards.add(card)
                        sharedPreferencesManager.saveReceivedCards(existingCards)
                        Toast.makeText(this@MainActivity, "Tarjeta de ${card.name} recibida", Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                                val isSelected = topLevelRoute == topLevelBackStack.topLevelKey
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { topLevelBackStack.addTopLevel(topLevelRoute) },
                                    icon = { Icon(imageVector = topLevelRoute.icon, contentDescription = topLevelRoute.label) },
                                    label = { Text(topLevelRoute.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavDisplay(
                            backStack = topLevelBackStack.backStack,
                            onBack = { topLevelBackStack.removeLast() },
                            entryProvider = entryProvider {
                                entry<ProfileRoute> {
                                    ProfileScreen(sharedPreferencesManager = sharedPreferencesManager)
                                }
                                entry<DashboardRoute> {
                                    DashboardScreen(bluetoothManager = bluetoothManager)
                                }
                                entry<InboxRoute> {
                                    InboxScreen(sharedPreferencesManager = sharedPreferencesManager)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
    }
}

class TopLevelBackStack<T: Any>(startKey: T) {
    private var topLevelStacks : LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() = backStack.apply {
        clear()
        addAll(topLevelStacks.flatMap { it.value })
    }

    fun addTopLevel(key: T){
        if (topLevelStacks[key] == null){
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.apply {
                remove(key)?.let { put(key, it) }
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: T){
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun removeLast(){
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        topLevelStacks.remove(removedKey)
        if (topLevelStacks.isNotEmpty()) {
            topLevelKey = topLevelStacks.keys.last()
        }
        updateBackStack()
    }
}