package com.mamafua.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mamafua.app.data.local.dao.JobDao
import com.mamafua.app.data.local.dao.NotificationDao
import com.mamafua.app.data.local.dao.UserDao
import com.mamafua.app.data.local.entity.JobEntity
import com.mamafua.app.data.local.entity.NotificationEntity
import com.mamafua.app.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, JobEntity::class, NotificationEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao
    abstract fun notificationDao(): NotificationDao
}
