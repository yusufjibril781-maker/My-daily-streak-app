package com.example.data

import kotlinx.coroutines.flow.Flow

class DailyRecordRepository(private val dailyRecordDao: DailyRecordDao) {
    val allRecords: Flow<List<DailyRecord>> = dailyRecordDao.getAllRecords()

    fun getRecordForDate(dateString: String): Flow<DailyRecord?> {
        return dailyRecordDao.getRecordForDate(dateString)
    }

    suspend fun getRecordForDateSuspend(dateString: String): DailyRecord? {
        return dailyRecordDao.getRecordForDateSuspend(dateString)
    }

    suspend fun insertRecord(record: DailyRecord) {
        dailyRecordDao.insertRecord(record)
    }

    suspend fun updateRecord(record: DailyRecord) {
        dailyRecordDao.updateRecord(record)
    }

    suspend fun clearAllRecords() {
        dailyRecordDao.clearAllRecords()
    }
}
