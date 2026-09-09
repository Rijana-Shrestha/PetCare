package com.rijana.petcare.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.databinding.ItemPetHomeBinding
import java.util.concurrent.TimeUnit

class HomePetAdapter(
    private val onPetClick: (Pet) -> Unit
) : ListAdapter<Pet, HomePetAdapter.PetViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(private val binding: ItemPetHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: Pet) {
            val context = binding.root.context
            binding.tvPetName.text = pet.name
            binding.tvPetAge.text = calculateAge(context, pet.dateOfBirth)
            binding.tvPetWeight.text = context.getString(R.string.pet_weight_format, pet.weightKg)

            Glide.with(binding.ivPetPhoto)
                .load(pet.photoUri)
                .placeholder(R.drawable.circle_avatar_placeholder)
                .error(R.drawable.circle_avatar_placeholder)
                .centerCrop()
                .into(binding.ivPetPhoto)

            binding.root.setOnClickListener { onPetClick(pet) }
        }

        private fun calculateAge(context: Context, dateOfBirthMillis: Long): String {
            val ageMillis = System.currentTimeMillis() - dateOfBirthMillis
            val years = TimeUnit.MILLISECONDS.toDays(ageMillis) / 365
            return if (years < 1) {
                context.getString(R.string.pet_age_less_than_year)
            } else {
                context.getString(R.string.pet_age_years, years.toInt())
            }
        }
    }
}

private class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
    override fun areItemsTheSame(oldItem: Pet, newItem: Pet) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Pet, newItem: Pet) = oldItem == newItem
}