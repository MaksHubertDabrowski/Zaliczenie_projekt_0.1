package com.example.zaliczenie_projekt_01.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Catch::class], version = 2, exportSchema = false) // Version incremented to 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchDao(): CatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mushroom_hunter_db"
                ).fallbackToDestructiveMigration() // Added for simplicity in development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}