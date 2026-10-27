package com.knz.diatracker

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knz.diatracker.ui.theme.DiaTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaTrackerTheme {
/*                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }*/
            }
        }
    }
}

@Composable
fun Sliders(
/*    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    stepsCount: Int*/
) {
    // через канвас
/*    val value: Float
    val onValueChange: (Float) -> Unit
    val valueRange: ClosedFloatingPointRange<Float>
    val stepsCount: Int

    Box (
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
            Canvas(modifier = Modifier.size(300.dp).background(Color.Gray)) {
                val width = size.width
                val height = size.height
        }
    }*/

    var wholePart by remember { mutableStateOf(4f) }
    var decimalPart by remember { mutableStateOf(0.7f) }

    val total = String.format("%.1f", wholePart + decimalPart)

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            total + " ммоль/л",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Slider(
            value = wholePart,
            onValueChange = { wholePart = it },
            valueRange = 0f..20f,
            steps = 19 // Шаги между делениями
        )

        Slider(
            value = decimalPart,
            onValueChange = { decimalPart = it },
            valueRange = 0f..0.9f,
            steps = 8 // Шаги для десятых долей
        )
    }
}

@Composable
fun GlucoseRecordScreen() {
    Column (
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        Text(
            "Сахар",
            fontSize = 20.sp
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(60.dp),
                color = Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(
                    text = "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                }
            }
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(60.dp),
                color = Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(
                    text = "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                }
            }

            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(
                    text = "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                }
            }

            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(
                    text = "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "27 октября, 2026 г., 18:38",
            modifier = Modifier
                .border(
                shape = RoundedCornerShape(10.dp),
                width = 1.dp,
                color = Color.Black
                )
                .padding(horizontal = 25.dp),
            fontSize = 18.sp,
        )
        
        Spacer(Modifier.height(20.dp))

/*        Text(
            "0.0 ммоль/л",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )*/

        Sliders()

        Spacer(Modifier.height(10.dp))

        Row {
            Text(
                "до еды",
                modifier = Modifier
                    .background(
                        color = Color.Gray,
                        shape = RoundedCornerShape(10.dp)
                    ).padding(3.dp),
                color = Color.White
            )
            Spacer(Modifier.width(20.dp))
            Text(
                "после еды",
                modifier = Modifier
                    .background(
                        color = Color.Gray,
                        shape = RoundedCornerShape(10.dp)
                    ).padding(3.dp),
                color = Color.White
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "подтвердить",
            modifier = Modifier
                .background(Color(0xFF4c5e8b), shape = RoundedCornerShape(10.dp))
                .padding(3.dp),
            color = Color.White
        )
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column {
        Column(
            modifier = Modifier.background(Color.Gray).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            Text(
                "Мой дневник",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(410.dp),
                shape = RoundedCornerShape(15.dp),
                color = Color.White,
            ) {
                Box(
                ) {
                    GlucoseRecordScreen()
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        Column (
            modifier = Modifier.padding(10.dp)
        ) {
            Text("История заметок")

            val zametkiList = listOf(
                "Заметка 1", "Заметка 2", "Заметка 3", "Заметка 4", "Заметка 5", "Заметка 6"
            )

            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    items(zametkiList) { zametka ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = zametka,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                NavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                        label = { Text("Дневник") },
                        selected = true,
                        onClick = {  }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                        label = { Text("Настройки") },
                        selected = false,
                        onClick = {  }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    DiaTrackerTheme {
        MainScreen()
    }
}