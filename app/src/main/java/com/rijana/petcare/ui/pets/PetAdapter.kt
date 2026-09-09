package com.rijana.petcare.ui.pets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rijana.petcare.R
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.databinding.ItemPetBinding
import java.util.concurrent.TimeUnit

class PetAdapter(
    private val onPetClick: (Pet) -> Unit
) : ListAdapter<Pet, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(private val binding: ItemPetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: Pet) {
            binding.tvPetName.text = pet.name
            binding.tvPetBreed.text = pet.breed
            binding.tvPetDetails.text = "${calculateAge(pet.dateOfBirth)}    ${pet.weightKg} kg"

            Glide.with(binding.ivPetPhoto)
                .load(pet.photoUri)
                .placeholder(R.drawable.circle_avatar_placeholder)
                .error(R.drawable.circle_avatar_placeholder)
                .circleCrop()
                .into(binding.ivPetPhoto)

            binding.root.setOnClickListener { onPetClick(pet) }
        }

        private fun calculateAge(dateOfBirthMillis: Long): String {
            val ageMillis = System.currentTimeMillis() - dateOfBirthMillis
            val years = TimeUnit.MILLISECONDS.toDays(ageMillis) / 365
            return if (years < 1) "<1 yr" else "$years yrs"
        }
    }
}

private class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
    override fun areItemsTheSame(oldItem: Pet, newItem: Pet) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Pet, newItem: Pet) = oldItem == newItem
}