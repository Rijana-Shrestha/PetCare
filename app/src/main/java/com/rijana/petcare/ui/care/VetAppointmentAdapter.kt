package com.rijana.petcare.ui.care

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.AppointmentStatus
import com.rijana.petcare.data.local.entity.VetAppointment
import com.rijana.petcare.databinding.ItemAppointmentBinding
import java.text.SimpleDateFormat
import java.util.Locale

data class VetAppointmentListItem(val appointment: VetAppointment, val petName: String)

class VetAppointmentAdapter(
    private val onDelete: (VetAppointment) -> Unit
) : ListAdapter<VetAppointmentListItem, VetAppointmentAdapter.ViewHolder>(VetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VetAppointmentListItem) {
            val appt = item.appointment
            val context = binding.root.context

            binding.tvApptTitle.text = appt.type
            binding.tvApptPetDate.text = "${item.petName} · ${formatDate(appt.date)}"
            binding.tvApptTimeProvider.text = "${formatTime(appt.time)} · ${appt.clinicName}"

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

private class VetDiffCallback : DiffUtil.ItemCallback<VetAppointmentListItem>() {
    override fun areItemsTheSame(oldItem: VetAppointmentListItem, newItem: VetAppointmentListItem) =
        oldItem.appointment.id == newItem.appointment.id
    override fun areContentsTheSame(oldItem: VetAppointmentListItem, newItem: VetAppointmentListItem) =
        oldItem == newItem
}