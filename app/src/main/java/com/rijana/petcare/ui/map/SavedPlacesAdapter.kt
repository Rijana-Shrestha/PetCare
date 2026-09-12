package com.rijana.petcare.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.databinding.ItemSavedPlaceBinding

class SavedPlacesAdapter(
    private val onPlaceClick: (SavedPlace) -> Unit,
    private val onDeleteClick: (SavedPlace) -> Unit
) : ListAdapter<SavedPlace, SavedPlacesAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemSavedPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(private val binding: ItemSavedPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: SavedPlace) {
            binding.tvPlaceName.text = place.name
            binding.tvPlaceType.text = place.type.name
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar(Char::uppercase)
            binding.tvPlaceAddress.text = place.address ?: "No address saved"

            binding.root.setOnClickListener { onPlaceClick(place) }
            binding.ivDelete.setOnClickListener { onDeleteClick(place) }
        }
    }
}

private class PlaceDiffCallback : DiffUtil.ItemCallback<SavedPlace>() {
    override fun areItemsTheSame(oldItem: SavedPlace, newItem: SavedPlace) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: SavedPlace, newItem: SavedPlace) = oldItem == newItem
}