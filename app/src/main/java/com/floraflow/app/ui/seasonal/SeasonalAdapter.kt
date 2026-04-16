package com.floraflow.app.ui.seasonal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.databinding.ItemSeasonPlantCardBinding
import com.floraflow.app.databinding.ItemSeasonSectionBinding

class SeasonalAdapter(
    private val sections: List<SeasonalSection>,
    private val onPlantClick: (DailyPlant) -> Unit
) : RecyclerView.Adapter<SeasonalAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(val binding: ItemSeasonSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSeasonSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        with(holder.binding) {
            seasonEmoji.text = section.emoji
            seasonName.text = section.season
            plantCount.text = "${section.plants.size} plant${if (section.plants.size > 1) "s" else ""}"
            try { seasonCard.setCardBackgroundColor(Color.parseColor(section.color)) } catch (e: Exception) { }
            val plantAdapter = SeasonPlantAdapter(section.plants, onPlantClick)
            plantsRecycler.layoutManager = LinearLayoutManager(
                root.context, LinearLayoutManager.HORIZONTAL, false
            )
            plantsRecycler.adapter = plantAdapter
        }
    }

    override fun getItemCount() = sections.size
}

class SeasonPlantAdapter(
    private val plants: List<DailyPlant>,
    private val onClick: (DailyPlant) -> Unit
) : RecyclerView.Adapter<SeasonPlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(val binding: ItemSeasonPlantCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemSeasonPlantCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        with(holder.binding) {
            plantName.text = plant.plantName
            if (!plant.scientificName.isNullOrBlank()) {
                scientificName.text = plant.scientificName
                scientificName.visibility = View.VISIBLE
            } else {
                scientificName.visibility = View.GONE
            }
            favoriteIcon.visibility = if (plant.isFavorite) View.VISIBLE else View.GONE
            Glide.with(plantImage.context)
                .load(plant.imageUrlRegular)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_plant)
                .into(plantImage)
            root.setOnClickListener { onClick(plant) }
        }
    }

    override fun getItemCount() = plants.size
}
