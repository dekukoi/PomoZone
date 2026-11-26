package com.dekukoi.pomozone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        fun build(context: Context, inMemory: Boolean = false): AppDatabase {
            return if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            } else {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pomozone.db",
                ).build()
            }
        }
    }
}
