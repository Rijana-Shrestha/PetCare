package com.rijana.petcare.ui.care

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.AppointmentStatus
import com.rijana.petcare.data.local.entity.GroomingAppointment
import com.rijana.petcare.databinding.ItemAppointmentBinding
import java.text.SimpleDateFormat
import java.util.Locale

data class GroomingAppointmentListItem(val appointment: GroomingAppointment, val petName: String)

class GroomingAppointmentAdapter(
    private val onDelete: (GroomingAppointment) -> Unit
) : ListAdapter<GroomingAppointmentListItem, GroomingAppointmentAdapter.ViewHolder>(GroomingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GroomingAppointmentListItem) {
            val appt = item.appointment
            val context = binding.root.context

            binding.tvApptTitle.text = appt.type
            binding.tvApptPetDate.text = "${item.petName} · ${formatDate(appt.date)}"
            binding.tvApptTimeProvider.text = "${formatTime(appt.time)} · ${appt.salonName}"

            if (appt.status == AppointmentStatus.UPCOMING) {
                binding.tvApptStatus.text = context.getString(R.string.upcoming)
                binding.tvApptStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                binding.tvApptStatus.setTextColor(ContextCompat.getColor(context, R.color.color_secondary))
            } else {
                binding.tvApptStatus.text = context.getString(R.string.completed)
                binding.tvApptStatus.setBackgroundResource(R.drawable.bg_status_completed)
                binding.tvApptStatus.setTextColor(ContextCompat.getColor(context, R.color.color_primary))
            }

            binding.tvApptDelete.setOnClickListener { onDelete(appt) }
        }

        private fun formatDate(millis: Long) = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis)

        private fun formatTime(rawTime: String): String = try {
            val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
        } catch (e: Exception) {
            rawTime
        }
    }
}

private class GroomingDiffCallback : DiffUtil.ItemCallback<GroomingAppointmentListItem>() {
    override fun areItemsTheSame(oldItem: GroomingAppointmentListItem, newItem: GroomingAppointmentListItem) =
        oldItem.appointment.id == newItem.appointment.id
    override fun areContentsTheSame(oldItem: GroomingAppointmentListItem, newItem: GroomingAppointmentListItem) =
        oldItem == newItem
}