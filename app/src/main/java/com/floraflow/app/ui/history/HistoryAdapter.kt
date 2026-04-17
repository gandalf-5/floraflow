package com.floraflow.app.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val onFavoriteClick: (DailyPlant) -> Unit
) : ListAdapter<DailyPlant, HistoryAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DailyPlant>() {
            override fun areItemsTheSame(a: DailyPlant, b: DailyPlant) = a.dateKey == b.dateKey
            override fun areContentsTheSame(a: DailyPlant, b: DailyPlant) = a == b
        }
        private val INPUT_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val OUTPUT_FMT = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
        fun formatDate(dateKey: String): String = try {
            val date = INPUT_FMT.parse(dateKey)
            if (date != null) OUTPUT_FMT.format(date) else dateKey
        } catch (_: Exception) { dateKey }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.item_image)
        val name: TextView = view.findViewById(R.id.item_plant_name)
        val date: TextView = view.findViewById(R.id.item_date)
        val scientific: TextView = view.findViewById(R.id.item_scientific_name)
        val favoriteBtn: ImageButton = view.findViewById(R.id.item_favorite_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = getItem(position)
        holder.name.text = plant.plantName
        holder.date.text = formatDate(plant.dateKey)
        if (!plant.scientificName.isNullOrBlank()) {
            holder.scientific.visibility = View.VISIBLE
            holder.scientific.text = plant.scientificName
        } else {
            holder.scientific.visibility = View.GONE
        }
        holder.favoriteBtn.setImageResource(
            if (plant.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
        holder.favoriteBtn.setOnClickListener { onFavoriteClick(plant) }
        Glide.with(holder.image.context)
            .load(plant.imageUrlRegular)
            .centerCrop()
            .placeholder(R.drawable.placeholder_plant)
            .into(holder.image)
    }
}
