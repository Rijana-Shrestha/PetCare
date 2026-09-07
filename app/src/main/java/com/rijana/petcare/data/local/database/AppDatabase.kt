package com.rijana.petcare.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rijana.petcare.data.local.dao.*
import com.rijana.petcare.data.local.entity.*

@Database(
    entities = [
        User::class,
        Pet::class,
        Routine::class,
        RoutineCompletion::class,
        Medication::class,
        VetAppointment::class,
        GroomingAppointment::class,
        Expense::class,
        SavedPlace::class,
        Contact::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineCompletionDao(): RoutineCompletionDao
    abstract fun medicationDao(): MedicationDao
    abstract fun vetAppointmentDao(): VetAppointmentDao
    abstract fun groomingAppointmentDao(): GroomingAppointmentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petcare.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}