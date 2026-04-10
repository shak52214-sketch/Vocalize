package com.vocalize.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.vocalize.app.data.local.dao.CategoryDao
import com.vocalize.app.data.local.dao.MemoDao
import com.vocalize.app.data.local.dao.PlaylistDao
import com.vocalize.app.data.local.dao.ReminderDao
import com.vocalize.app.data.local.dao.TagDao
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.data.local.entity.PlaylistEntity
import com.vocalize.app.data.local.entity.PlaylistMemoCrossRef
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.data.local.entity.TagEntity
import com.vocalize.app.data.local.entity.MemoTagCrossRef
import com.vocalize.app.data.local.entity.MemoCategoryCrossRef
import com.vocalize.app.data.local.entity.ReminderEntity

class Converters {
    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.valueOf(value)
}

@Database(
    entities = [
        MemoEntity::class,
        CategoryEntity::class,
        PlaylistEntity::class,
        PlaylistMemoCrossRef::class,
        TagEntity::class,
        MemoTagCrossRef::class,
        MemoCategoryCrossRef::class,
        ReminderEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun tagDao(): TagDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocalize_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
