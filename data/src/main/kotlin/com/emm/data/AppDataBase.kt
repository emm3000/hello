package com.emm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity

@Database(
    entities = [WordEntity::class],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "database-name_1"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}