package com.rijana.petcare.data.local.database

import androidx.room.TypeConverter
import com.rijana.petcare.data.local.entity.*

class Converters {

    @TypeConverter
    fun fromPetType(value: PetType): String = value.name
    @TypeConverter
    fun toPetType(value: String): PetType = PetType.valueOf(value)

    @TypeConverter
    fun fromGender(value: Gender): String = value.name
    @TypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)

    @TypeConverter
    fun fromTaskRepeat(value: TaskRepeat): String = value.name
    @TypeConverter
    fun toTaskRepeat(value: String): TaskRepeat = TaskRepeat.valueOf(value)

    @TypeConverter
    fun fromMedicationType(value: MedicationType): String = value.name
    @TypeConverter
    fun toMedicationType(value: String): MedicationType = MedicationType.valueOf(value)

    @TypeConverter
    fun fromMedicationStatus(value: MedicationStatus): String = value.name
    @TypeConverter
    fun toMedicationStatus(value: String): MedicationStatus = MedicationStatus.valueOf(value)

    @TypeConverter
    fun fromAppointmentStatus(value: AppointmentStatus): String = value.name
    @TypeConverter
    fun toAppointmentStatus(value: String): AppointmentStatus = AppointmentStatus.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name
    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)

    @TypeConverter
    fun fromPlaceType(value: PlaceType): String = value.name
    @TypeConverter
    fun toPlaceType(value: String): PlaceType = PlaceType.valueOf(value)
}