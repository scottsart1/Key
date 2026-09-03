package com.example.housekey.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [KeyEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KeyDatabase : RoomDatabase() {

    abstract fun keyDao(): KeyDao

    companion object {
        @Volatile
        private var instance: KeyDatabase? = null

        fun get(context: Context): KeyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeyDatabase::class.java,
                    "housekey.db",
                ).build().also { instance = it }
            }
    }
}
