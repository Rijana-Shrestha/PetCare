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
import java.util.Calendar
import java.util.Locale

data class GroomingAppointmentListItem(
    val appointment: GroomingAppointment,
    val petName: String
)

class GroomingAppointmentAdapter(
    private val onDelete: (GroomingAppointment) -> Unit
) : ListAdapter<
        GroomingAppointmentListItem,
        GroomingAppointmentAdapter.ViewHolder
        >(GroomingDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GroomingAppointmentListItem) {

            val appt = item.appointment
            val context = binding.root.context

            binding.tvApptTitle.text =
                appt.type

            binding.tvApptPetDate.text =
                "${item.petName} · ${formatDate(appt.date)}"

            binding.tvApptTimeProvider.text =
                "${formatTime(appt.time)} · ${appt.salonName}"


            val isCompleted =
                appt.status == AppointmentStatus.COMPLETED ||
                        isAppointmentPast(
                            appt.date,
                            appt.time
                        )

            if (isCompleted) {

                binding.tvApptStatus.text =
                    context.getString(R.string.completed)

                binding.tvApptStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_success
                    )
                )

            } else {

                binding.tvApptStatus.text =
                    context.getString(R.string.upcoming)

                binding.tvApptStatus.setBackgroundResource(
                    R.drawable.bg_status_upcoming
                )

                binding.tvApptStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_secondary
                    )
                )
            }

            binding.tvApptDelete.setOnClickListener {
                onDelete(appt)
            }
        }

        private fun isAppointmentPast(
            dateMillis: Long,
            timeString: String
        ): Boolean {

            return try {

                val appointmentCalendar =
                    Calendar.getInstance().apply {

                        timeInMillis = dateMillis

                        set(
                            Calendar.HOUR_OF_DAY,
                            0
                        )

                        set(
                            Calendar.MINUTE,
                            0
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }

                val parsedTime =
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).parse(timeString)

                if (parsedTime != null) {

                    val timeCalendar =
                        Calendar.getInstance().apply {
                            timeInMillis =
                                parsedTime.time
                        }

                    appointmentCalendar.set(
                        Calendar.HOUR_OF_DAY,
                        timeCalendar.get(
                            Calendar.HOUR_OF_DAY
                        )
                    )

                    appointmentCalendar.set(
                        Calendar.MINUTE,
                        timeCalendar.get(
                            Calendar.MINUTE
                        )
                    )
                }

                appointmentCalendar.timeInMillis <
                        System.currentTimeMillis()

            } catch (e: Exception) {

                dateMillis <
                        Calendar.getInstance().apply {

                            set(
                                Calendar.HOUR_OF_DAY,
                                0
                            )

                            set(
                                Calendar.MINUTE,
                                0
                            )

                            set(
                                Calendar.SECOND,
                                0
                            )

                            set(
                                Calendar.MILLISECOND,
                                0
                            )

                        }.timeInMillis
            }
        }

        private fun formatDate(
            millis: Long
        ): String {

            return SimpleDateFormat(
                "MMM d, yyyy",
                Locale.getDefault()
            ).format(millis)
        }

        private fun formatTime(
            rawTime: String
        ): String {

            return try {

                val parsed =
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).parse(rawTime)

                SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(parsed!!)

            } catch (e: Exception) {

                rawTime
            }
        }
    }
}

private class GroomingDiffCallback :
    DiffUtil.ItemCallback<GroomingAppointmentListItem>() {

    override fun areItemsTheSame(
        oldItem: GroomingAppointmentListItem,
        newItem: GroomingAppointmentListItem
    ): Boolean {

        return oldItem.appointment.id ==
                newItem.appointment.id
    }

    override fun areContentsTheSame(
        oldItem: GroomingAppointmentListItem,
        newItem: GroomingAppointmentListItem
    ): Boolean {

        return oldItem == newItem
    }
}