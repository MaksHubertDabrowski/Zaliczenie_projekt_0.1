package com.example.zaliczenie_projekt_01.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CatchDao {
    @Insert
    suspend fun insertCatch(fishCatch: Catch)

    @Query("SELECT * FROM catches")
    suspend fun getAllCatches(): List<Catch>

    @Query("SELECT * FROM catches WHERE id = :catchId")
    suspend fun getCatchById(catchId: Int): Catch?
}