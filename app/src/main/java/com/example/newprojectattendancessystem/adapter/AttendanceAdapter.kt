package com.example.newprojectattendancessystem.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newprojectattendancessystem.R
import com.example.newprojectattendancessystem.local.model.AttendanceRecord

class AttendanceAdapter(private val records: List<AttendanceRecord>) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.attendanceMessage)
        val timeTextView: TextView = itemView.findViewById(R.id.attendanceTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_record, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = records[position]
        holder.messageTextView.text = record.message
        holder.timeTextView.text = record.time
    }

    override fun getItemCount(): Int = records.size
}
