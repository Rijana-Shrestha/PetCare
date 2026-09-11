package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.ExpenseDao
import com.rijana.petcare.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getExpenses(ownerId: Long, petId: Long?): Flow<List<Expense>> =
        expenseDao.getExpenses(ownerId, petId)

    fun getTotalForMonth(ownerId: Long, monthStart: Long, monthEnd: Long, petId: Long?): Flow<Float> =
        expenseDao.getTotalForMonth(ownerId, monthStart, monthEnd, petId)

    suspend fun addExpense(expense: Expense): Long = expenseDao.insert(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)
}