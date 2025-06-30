package com.emm.data.deprecated

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emm.data.deprecated.word.WordDao
import com.emm.data.deprecated.word.WordEntity
import com.emm.data.deprecated.wordcontent.ExampleDao
import com.emm.data.deprecated.wordcontent.ExampleEntity
import com.emm.data.deprecated.wordcontent.WordContentDao
import com.emm.data.deprecated.wordcontent.WordContentEntity

@Database(
    entities = [
        WordEntity::class,
        WordContentEntity::class,
        ExampleEntity::class
    ],
    version = 9,
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
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}