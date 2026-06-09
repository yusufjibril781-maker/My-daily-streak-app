package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE dateString = :dateString LIMIT 1")
    fun getRecordForDate(dateString: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE dateString = :dateString LIMIT 1")
    suspend fun getRecordForDateSuspend(dateString: String): DailyRecord?

    @Query("SELECT * FROM daily_records ORDER BY dateString ASC")
    fun getAllRecords(): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecord)

    @Update
    suspend fun updateRecord(record: DailyRecord)

    @Delete
    suspend fun deleteRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records")
    suspend fun clearAllRecords()
}
