package com.rijana.petcare.ui.care

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationStatus
import com.rijana.petcare.databinding.ItemMedicationBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MedicationListItem(
    val medication: Medication,
    val petName: String
)

class MedicationAdapter(
    private val onDelete: (Medication) -> Unit
) : ListAdapter<MedicationListItem, MedicationAdapter.MedicationViewHolder>(MedicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicationViewHolder(private val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MedicationListItem) {
            val med = item.medication
            binding.tvMedTitle.text = "${item.petName} · ${med.name}"
            binding.tvMedTime.text = formatTime(med.time)
            binding.tvMedSubtitle.text = buildString {
                append(med.dosage)
                med.instructions?.let { append(" · $it") }
            }

            val dateRange = if (med.endDate != null) {
                "${med.frequency} · ${formatDate(med.startDate)} - ${formatDate(med.endDate)}"
            } else {
                "${med.frequency} · Started ${formatDate(med.startDate)}"
            }
            binding.tvMedMeta.text = dateRange

            bindStatus(med)

            binding.tvMedDelete.setOnClickListener { onDelete(med) }
        }

        private fun bindStatus(med: Medication) {
            val context = binding.root.context
            when {
                med.status == MedicationStatus.COMPLETED -> {
                    binding.tvMedStatus.text = context.getString(R.string.completed)
                    binding.tvMedStatus.setTextColor(ContextCompat.getColor(context, R.color.color_success))
                }
                med.endDate == null -> {
                    binding.tvMedStatus.text = context.getString(R.string.ongoing)
                    binding.tvMedStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                else -> {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(med.endDate - System.currentTimeMillis())
                    if (daysLeft < 0) {
                        binding.tvMedStatus.text = context.getString(R.string.completed)
                        binding.tvMedStatus.setTextColor(ContextCompat.getColor(context, R.color.color_success))
                    } else {
                        binding.tvMedStatus.text = context.getString(R.string.days_left, daysLeft.toInt())
                        binding.tvMedStatus.setTextColor(ContextCompat.getColor(context, R.color.color_secondary))
                    }
                }
            }
        }

        private fun formatTime(rawTime: String): String = try {
            val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
        } catch (e: Exception) {
            rawTime
        }

        private fun formatDate(millis: Long): String =
            SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)
    }
}

private class MedicationDiffCallback : DiffUtil.ItemCallback<MedicationListItem>() {
    override fun areItemsTheSame(oldItem: MedicationListItem, newItem: MedicationListItem) =
        oldItem.medication.id == newItem.medication.id
    override fun areContentsTheSame(oldItem: MedicationListItem, newItem: MedicationListItem) =
        oldItem == newItem
}