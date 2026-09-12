package com.rijana.petcare.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.databinding.ItemRoutineBinding
import com.rijana.petcare.viewmodel.MedicationOccurrence
import java.text.SimpleDateFormat
import java.util.Locale

data class MedicationTodayItem(val occurrence: MedicationOccurrence, val petName: String)

class MedicationTodayAdapter(
    private val onToggle: (MedicationOccurrence) -> Unit
) : ListAdapter<MedicationTodayItem, MedicationTodayAdapter.ViewHolder>(MedicationTodayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicationTodayItem) {
            val med = item.occurrence.medication
            binding.tvRoutineTitle.text = "${item.petName} · ${med.name}"
            binding.tvRoutineSubtitle.text = med.dosage
            binding.tvRoutineTime.text = formatTime(med.time)
            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = item.occurrence.isCompleted
            binding.cbDone.setOnCheckedChangeListener { _, _ -> onToggle(item.occurrence) }
        }

        private fun formatTime(rawTime: String): String = try {
            val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
        } catch (e: Exception) {
            rawTime
        }
    }
}

private class MedicationTodayDiffCallback : DiffUtil.ItemCallback<MedicationTodayItem>() {
    override fun areItemsTheSame(oldItem: MedicationTodayItem, newItem: MedicationTodayItem) =
        oldItem.occurrence.medication.id == newItem.occurrence.medication.id
    override fun areContentsTheSame(oldItem: MedicationTodayItem, newItem: MedicationTodayItem) =
        oldItem == newItem
}