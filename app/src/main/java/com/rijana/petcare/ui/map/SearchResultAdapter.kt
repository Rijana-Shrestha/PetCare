package com.rijana.petcare.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rijana.petcare.R
import com.rijana.petcare.data.network.NominatimResult
import com.rijana.petcare.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private val onBookmarkClick: (NominatimResult, Int) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private val items = mutableListOf<NominatimResult>()
    private val savedPositions = mutableSetOf<Int>()

    fun submitResults(results: List<NominatimResult>) {
        items.clear()
        items.addAll(results)
        savedPositions.clear()
        notifyDataSetChanged()
    }

    fun markSaved(position: Int) {
        savedPositions.add(position)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position, savedPositions.contains(position))
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: NominatimResult, position: Int, isSaved: Boolean) {
            val parts = result.display_name.split(",")
            binding.tvResultName.text = parts.firstOrNull()?.trim() ?: result.display_name
            binding.tvResultAddress.text = result.display_name

            binding.ivBookmark.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            binding.ivBookmark.setOnClickListener {
                if (!isSaved) onBookmarkClick(result, position)
            }
        }
    }
}