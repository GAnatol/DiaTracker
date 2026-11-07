package com.knz.diatracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SettingsSubSection {
    MAIN,
    SETTINGS,
    ABOUT
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: UserSettings,
    onGeneralChanged: (Boolean) -> Unit,
    onShortChanged: (Boolean) -> Unit,
    onLongChanged: (Boolean) -> Unit,
    onMixedChanged: (Boolean) -> Unit
) {
    var currentSubSection by remember { mutableStateOf(SettingsSubSection.MAIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2c3e6b))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSubSection != SettingsSubSection.MAIN) {
                    IconButton(onClick = { currentSubSection = SettingsSubSection.MAIN }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                }

                Text(
                    text = when (currentSubSection) {
                        SettingsSubSection.MAIN -> "Настройки"
                        SettingsSubSection.SETTINGS -> "Настройки и профиль"
                        SettingsSubSection.ABOUT -> "О приложении"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = if (currentSubSection == SettingsSubSection.MAIN) 8.dp else 0.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            when (currentSubSection) {
                SettingsSubSection.MAIN -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsMenuItem(
                            title = "Настройки и профиль (в доработке)",
                            subtitle = "Управление параметрами",
                            icon = Icons.Default.Settings,
                            onClick = { currentSubSection = SettingsSubSection.SETTINGS }
                        )

                        SettingsMenuItem(
                            title = "О приложении",
                            subtitle = "Информация, версия и описание",
                            icon = Icons.Default.Info,
                            onClick = { currentSubSection = SettingsSubSection.ABOUT }
                        )
                    }
                }

                SettingsSubSection.SETTINGS -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Отображаемые типы инсулина",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2c3e6b)
                            )

                            Text(
                                text = "Настройка отображения полей",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Spacer(Modifier.height(4.dp))

                            SettingSwitchRow(
                                title = "Инсулин\n(если не выбран конкретный)",
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

                            SettingSwitchRow(
                                title = "Короткий инсулин",
                                checked = settings.short,
                                onCheckedChange = { isChecked ->
                                    onShortChanged(isChecked)
                                    if (isChecked) {
                                        onGeneralChanged(false)
                                    }
                                }
                            )

                            SettingSwitchRow(
                                title = "Продленный инсулин",
                                checked = settings.long,
                                onCheckedChange = { isChecked ->
                                    onLongChanged(isChecked)
                                    if (isChecked) {
                                        onGeneralChanged(false)
                                    }
                                }
                            )

                            SettingSwitchRow(
                                title = "Смешанный инсулин",
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
                }

                SettingsSubSection.ABOUT -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DiaTracker",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2c3e6b)
                            )

                            Text(
                                text = "Версия 1.0.0",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Divider(color = Color.LightGray, thickness = 1.dp)

                            Text(
                                text = stringResource(R.string.about_description),
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.under_about),
                                fontSize = 13.sp,
                                color = Color(0xFF4c5e8b),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2c3e6b).copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF2c3e6b))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2c3e6b)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Открыть",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2c3e6b)
            )
        )
    }
}