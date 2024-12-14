package com.example.newprojectattendancessystem.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.newprojectattendancessystem.local.model.AttendanceRecord

@Dao
interface AttendanceRecordDao {

    @Insert
    suspend fun insert(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceRecords(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId")
    suspend fun getAttendanceRecordsForUser(userId: Int): List<AttendanceRecord>
}
