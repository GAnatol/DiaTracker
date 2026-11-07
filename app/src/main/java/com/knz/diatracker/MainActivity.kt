package com.knz.diatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knz.diatracker.ui.theme.DiaTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaTrackerTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val settings by viewModel.uiState.collectAsState()
    val currentScreen = viewModel.currentScreen

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF2c3e6b), // Задаем цвет фона всего Scaffold (под цвет шапки)
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF2c3e6b)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                    label = { Text("Главная") },
                    selected = currentScreen == Screen.MAIN,
                    onClick = { viewModel.navigateTo(Screen.MAIN) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF2c3e6b),
                        indicatorColor = Color(0xFF2c3e6b)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Статистика") },
                    label = { Text("Статистика") },
                    selected = currentScreen == Screen.STATS,
                    onClick = { viewModel.navigateTo(Screen.STATS) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF2c3e6b),
                        indicatorColor = Color(0xFF2c3e6b)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") },
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF2c3e6b),
                        indicatorColor = Color(0xFF2c3e6b)
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            color = Color.Transparent
        ) {
            when (currentScreen) {
                Screen.MAIN -> {
                    MainScreen(
                        onOpenSettings = { viewModel.navigateTo(Screen.SETTINGS) }
                    )
                }
                Screen.STATS -> {
                    StatsScreen(settings)
                }
                Screen.SETTINGS -> {
                    SettingsScreen(
                        onBack = { viewModel.navigateTo(Screen.MAIN) },
                        settings = settings,
                        onGeneralChanged = { viewModel.toggleGeneralInsulin(it) },
                        onShortChanged = { viewModel.toggleShortInsulin(it) },
                        onLongChanged = { viewModel.toggleLongInsulin(it) },
                        onMixedChanged = { viewModel.toggleMixedInsulin(it) }
                    )
                }
                else -> {}
            }
        }
    }
}