package com.rijana.petcare.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.databinding.ItemUpcomingAppointmentBinding
import com.rijana.petcare.viewmodel.UpcomingAppointmentItem
import java.text.SimpleDateFormat
import java.util.Locale

data class UpcomingListItem(val appointment: UpcomingAppointmentItem, val petName: String)

class UpcomingAppointmentAdapter :
    ListAdapter<UpcomingListItem, UpcomingAppointmentAdapter.ViewHolder>(UpcomingAppointmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpcomingAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemUpcomingAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UpcomingListItem) {
            binding.tvUpcomingTitle.text = "${item.petName}'s ${item.appointment.title}"
            binding.tvUpcomingSubtitle.text = item.appointment.category
            binding.tvUpcomingTime.text = "${formatDate(item.appointment.date)}, ${formatTime(item.appointment.time)}"
        }

        private fun formatDate(millis: Long) = SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)

        private fun formatTime(rawTime: String): String = try {
            val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
        } catch (e: Exception) {
            rawTime
        }
    }
}

private class UpcomingAppointmentDiffCallback : DiffUtil.ItemCallback<UpcomingListItem>() {
    override fun areItemsTheSame(oldItem: UpcomingListItem, newItem: UpcomingListItem) =
        oldItem.appointment.id == newItem.appointment.id
    override fun areContentsTheSame(oldItem: UpcomingListItem, newItem: UpcomingListItem) =
        oldItem == newItem
}