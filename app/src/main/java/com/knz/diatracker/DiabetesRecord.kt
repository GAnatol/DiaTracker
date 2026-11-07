package com.knz.diatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diabetes_records")
data class DiabetesRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,          // Точное время создания записи (для сортировки)
    val dateTimeText: String,     // Красивая строка даты/времени для отображения
    val mealType: String?,        // "до еды", "после еды" или null

    val bloodSugar: Float?,       // Сахар
    val breadUnits: Float?,       // Еда (ХЕ)
    val generalInsulin: Float?,   // Общий инсулин
    val shortInsulin: Float?,     // Короткий
    val longInsulin: Float?,      // Продленный
    val mixedInsulin: Float?,     // Смешанный
    val weight: Float?,           // Вес

    val note: String?,            // Текст заметки
    val activityName: String?,    // Название активности ("Бег", "Ходьба" и т.д.)
    val activityMinutes: Float?   // Время активности в минутах
)