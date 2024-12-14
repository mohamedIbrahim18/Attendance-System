package com.example.newprojectattendancessystem.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newprojectattendancessystem.adapter.AttendanceAdapter
import com.example.newprojectattendancessystem.databinding.ActivityRecordedAttendanceBinding
import com.example.newprojectattendancessystem.local.AppDatabase
import kotlinx.coroutines.launch

class RecordedAttendanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordedAttendanceBinding
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordedAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userId = intent.getIntExtra("USER_ID", 0)

        db = AppDatabase.getInstance(applicationContext)

        loadAttendanceRecords(userId)
    }

    private fun loadAttendanceRecords(userId:Int) {
        lifecycleScope.launch {
            val records = db.attendanceRecordDao().getAttendanceRecordsForUser(userId)

            val adapter = AttendanceAdapter(records)
            binding.recyclerView.layoutManager = LinearLayoutManager(this@RecordedAttendanceActivity)
            binding.recyclerView.adapter = adapter
        }
    }
}
