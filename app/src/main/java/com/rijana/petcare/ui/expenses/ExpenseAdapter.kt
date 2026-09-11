package com.rijana.petcare.ui.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.Expense
import com.rijana.petcare.data.local.entity.ExpenseCategory
import com.rijana.petcare.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Locale

data class ExpenseListItem(val expense: Expense, val petName: String)

class ExpenseAdapter : ListAdapter<ExpenseListItem, ExpenseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExpenseListItem) {
            val expense = item.expense
            val context = binding.root.context

            binding.tvExpenseCategory.text = categoryLabel(expense.category)
            binding.tvExpensePetDate.text = "${item.petName} · ${formatDate(expense.date)}"
            binding.tvExpenseAmount.text = "$${"%.2f".format(expense.amount)}"

            binding.tvCategoryIcon.text = categoryLabel(expense.category).take(1)
            binding.tvCategoryIcon.background.setTint(
                ContextCompat.getColor(context, categoryColor(expense.category))
            )
        }

        private fun categoryLabel(category: ExpenseCategory): String =
            category.name.lowercase().replaceFirstChar(Char::uppercase)

        private fun categoryColor(category: ExpenseCategory): Int = when (category) {
            ExpenseCategory.FOOD -> R.color.color_secondary
            ExpenseCategory.GROOMING -> R.color.color_primary
            ExpenseCategory.VETERINARY -> R.color.color_danger
            ExpenseCategory.MEDICATION -> R.color.color_secondary
            ExpenseCategory.TOYS -> R.color.color_success
            ExpenseCategory.OTHER -> R.color.text_secondary
        }

        private fun formatDate(millis: Long) = SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
    override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem) =
        oldItem.expense.id == newItem.expense.id
    override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem) =
        oldItem == newItem
}