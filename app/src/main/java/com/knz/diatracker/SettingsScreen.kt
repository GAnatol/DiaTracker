package com.knz.diatracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: UserSettings,
    onGeneralChanged: (Boolean) -> Unit,
    onShortChanged: (Boolean) -> Unit,
    onLongChanged: (Boolean) -> Unit,
    onMixedChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(30.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Настройки",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Готово",
                color = Color(0xFF4c5e8b),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Настройки инсулина:",
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(10.dp))

        SettingCheckboxRow(
            text = "Инсулин (по умолчанию)",
            checked = settings.general,
            onCheckedChange = { isChecked ->
                onGeneralChanged(isChecked)
                if (isChecked) {
                    onShortChanged(false)
                    onLongChanged(false)
                    onMixedChanged(false)
                }
            }
        )
        SettingCheckboxRow(
            text = "Инсулин короткий",
            checked = settings.short,
            onCheckedChange = { isChecked ->
                onShortChanged(isChecked)
                if (isChecked) {
                    onGeneralChanged(false)
                }
            }
        )
        SettingCheckboxRow(
            text = "Инсулин продленный",
            checked = settings.long,
            onCheckedChange = { isChecked ->
                onLongChanged(isChecked)
                if (isChecked) {
                    onGeneralChanged(false)
                }
            }
        )
        SettingCheckboxRow(
            text = "Инсулин смешанный",
            checked = settings.mixed,
            onCheckedChange = { isChecked ->
                onMixedChanged(isChecked)
                if (isChecked) {
                    onGeneralChanged(false)
                }
            }
        )
    }
}

@Composable
fun SettingCheckboxRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 5.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp)
    }
}