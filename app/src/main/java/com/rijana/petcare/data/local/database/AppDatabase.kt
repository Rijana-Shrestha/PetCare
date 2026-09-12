package com.rijana.petcare.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rijana.petcare.data.local.dao.*
import com.rijana.petcare.data.local.entity.*

@Database(
    entities = [
        User::class,
        Pet::class,
        Routine::class,
        RoutineCompletion::class,
        Medication::class,
        MedicationCompletion::class,
        VetAppointment::class,
        GroomingAppointment::class,
        Expense::class,
        SavedPlace::class,
        Contact::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineCompletionDao(): RoutineCompletionDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationCompletionDao(): MedicationCompletionDao
    abstract fun vetAppointmentDao(): VetAppointmentDao
    abstract fun groomingAppointmentDao(): GroomingAppointmentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS medication_completions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicationId INTEGER NOT NULL,
                        occurrenceDate INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL,
                        FOREIGN KEY(medicationId) REFERENCES medications(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_medication_completions_medicationId ON medication_completions(medicationId)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petcare.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}