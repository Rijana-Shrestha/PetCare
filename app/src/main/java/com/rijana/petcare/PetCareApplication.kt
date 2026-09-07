package com.rijana.petcare

import android.app.Application
import com.rijana.petcare.data.local.database.AppDatabase

class PetCareApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}