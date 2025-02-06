package com.emm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emm.data.scrap.ExampleDao
import com.emm.data.scrap.ExampleEntity
import com.emm.data.scrap.WordContentDao
import com.emm.data.scrap.WordContentEntity
import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity

@Database(
    entities = [
        WordEntity::class,
        WordContentEntity::class,
        ExampleEntity::class
    ],
    version = 6,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun exampleDao(): ExampleDao

    abstract fun wordContentDao(): WordContentDao

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