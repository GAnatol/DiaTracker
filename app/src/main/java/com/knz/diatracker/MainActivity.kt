package com.knz.diatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knz.diatracker.ui.theme.DiaTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaTrackerTheme {
                MainApp(viewModel())
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val settings by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (viewModel.currentScreen) {
            Screen.MAIN -> MainScreen({ viewModel.navigateTo(Screen.SETTINGS) })
            Screen.SETTINGS -> SettingsScreen(
                onBack = { viewModel.navigateTo(Screen.MAIN) },
                settings = settings,
                onGeneralChanged = { viewModel.toggleGeneralInsulin(it) },
                onShortChanged = { viewModel.toggleShortInsulin(it) },
                onLongChanged = { viewModel.toggleLongInsulin(it) },
                onMixedChanged = { viewModel.toggleMixedInsulin(it) }
            )
            else -> {}
        }
    }
}

data class MeasurementType(
    val title: String,
    val unit: String,
    val minWhole: Float,
    val maxWhole: Float,
    val wholeSteps: Int,
    val hasDecimal: Boolean
)


@Composable
fun StatsScreen(){
    Column {
        Column(
            modifier = Modifier.background(Color.Gray).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            Row{
                Text(
                    "Статистика",
                    modifier = Modifier,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            Row (
                modifier = Modifier.border(
                    shape = RoundedCornerShape(10.dp),
                    width = 1.dp,
                    color = Color.Black
                ).padding(10.dp)
            ) {
                Text(
                    "День",
                    fontSize = 18.sp
                    )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Неделя",
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Месяц",
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(200.dp))


        }
    }
}

@Composable
fun TasksScreen(){
    Column {
        Column(
            modifier = Modifier.background(Color.Gray).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(30.dp))

            Row {
                Text(
                    "Цели",
                    modifier = Modifier,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )


            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    DiaTrackerTheme {
    }
}