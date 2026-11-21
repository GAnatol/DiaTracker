package com.knz.diatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiabetesDao {
    @Insert
    suspend fun insertRecord(record: DiabetesRecord)

    @Update
    suspend fun updateRecord(record: DiabetesRecord)

    @Delete
    suspend fun deleteRecord(record: DiabetesRecord)

    @Query("SELECT * FROM diabetes_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DiabetesRecord>>
}