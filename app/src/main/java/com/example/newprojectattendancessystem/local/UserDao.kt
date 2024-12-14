package com.example.newprojectattendancessystem.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.newprojectattendancessystem.local.model.User

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>
    @Query("SELECT * FROM user_table WHERE email= :email")
    suspend fun getUserByEmail(email: String): User
}