package com.example.zaliczenie_projekt_01.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "catches")
data class Catch(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val imagePath: String?,
    val latitude: Double,
    val longitude: Double,
    val date: Long,
    val species: String?,
    val weight: Double? = null // Added weight field
) : Serializable
