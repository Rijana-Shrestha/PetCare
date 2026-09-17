package com.rijana.petcare.ui.care

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.databinding.ItemRoutineBinding
import com.rijana.petcare.viewmodel.RoutineOccurrence
import java.text.SimpleDateFormat
import java.util.Locale

data class RoutineListItem(
    val occurrence: RoutineOccurrence,
    val petName: String
)

class RoutineAdapter(
    private val onToggle: (RoutineOccurrence) -> Unit
) : ListAdapter<RoutineListItem, RoutineAdapter.RoutineViewHolder>(RoutineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoutineViewHolder(private val binding: ItemRoutineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoutineListItem) {
            val routine = item.occurrence.routine
            binding.tvRoutineTitle.text = "${item.petName} · ${routine.taskName}"
            binding.tvRoutineSubtitle.text = routine.durationMinutes
                ?.let { "$it minutes" }
                ?: routine.taskType
            binding.tvRoutineTime.text = "Today ${formatTime(routine.time)}"

            val context = binding.root.context
            binding.tvTypeIcon.text = routine.taskType.take(1).uppercase()
            binding.tvTypeIcon.background.setTint(
                ContextCompat.getColor(context, taskTypeColor(routine.taskType))
            )

            // Clear the old listener before setting isChecked, so restoring
            // state on a recycled row doesn't fire a spurious toggle.
            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = item.occurrence.isCompleted
            binding.cbDone.setOnCheckedChangeListener { _, _ ->
                onToggle(item.occurrence)
            }
        }

        private fun formatTime(rawTime: String): String {
            return try {
                val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
            } catch (e: Exception) {
                rawTime
            }
        }

        private fun taskTypeColor(taskType: String): Int {
            val t = taskType.lowercase()
            return when {
                t.contains("feed") || t.contains("food") -> R.color.color_secondary
                t.contains("play") -> R.color.color_primary
                t.contains("walk") -> R.color.color_success
                t.contains("groom") -> R.color.color_primary
                t.contains("vet") -> R.color.color_danger
                else -> R.color.text_secondary
            }
        }
    }
}

private class RoutineDiffCallback : DiffUtil.ItemCallback<RoutineListItem>() {
    override fun areItemsTheSame(oldItem: RoutineListItem, newItem: RoutineListItem) =
        oldItem.occurrence.routine.id == newItem.occurrence.routine.id

    override fun areContentsTheSame(oldItem: RoutineListItem, newItem: RoutineListItem) =
        oldItem == newItem
}