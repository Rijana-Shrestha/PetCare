package com.rijana.petcare.data.local.entity

enum class PetType {
    DOG, CAT, RABBIT, PARROT, OTHER
}

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class TaskRepeat {
    EVERYDAY, WEEKDAYS, CUSTOM
}

enum class MedicationType {
    TABLET, LIQUID, INJECTION, TOPICAL, OTHER
}

enum class MedicationStatus {
    ACTIVE, COMPLETED
}

enum class AppointmentStatus {
    UPCOMING, COMPLETED
}

enum class ExpenseCategory {
    FOOD, GROOMING, VETERINARY, MEDICATION, TOYS, OTHER
}

enum class PlaceType {
    VET_CLINIC, GROOMING_SALON, DOG_PARK, PET_SUPPLY_STORE, ANIMAL_SHELTER
}