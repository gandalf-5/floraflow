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
            monthRange.text = section.monthRange

            try { seasonCard.setCardBackgroundColor(Color.parseColor(section.color)) } catch (e: Exception) { }

            inSeasonBadge.visibility = if (section.isCurrentSeason) View.VISIBLE else View.GONE

            seasonFact.text = section.seasonFact

            if (section.plants.isNotEmpty()) {
                val ctx = root.context
                val count = section.plants.size
                plantCount.text = ctx.resources.getQuantityString(
                    R.plurals.seasonal_discovered_count, count, count
                )
                plantCount.visibility = View.VISIBLE
                discoveredLabel.visibility = View.VISIBLE
                plantsRecycler.visibility = View.VISIBLE

                plantsRecycler.layoutManager = LinearLayoutManager(
                    ctx, LinearLayoutManager.HORIZONTAL, false
                )
                plantsRecycler.adapter = SeasonPlantAdapter(section.plants, onPlantClick)
            } else {
                plantCount.visibility = View.GONE
                discoveredLabel.visibility = View.GONE
                plantsRecycler.visibility = View.GONE
            }

            if (section.curatedPlants.isNotEmpty()) {
                curatedLabel.visibility = View.VISIBLE
                curatedRecycler.visibility = View.VISIBLE
                curatedRecycler.layoutManager = LinearLayoutManager(
                    root.context, LinearLayoutManager.HORIZONTAL, false
                )
                curatedRecycler.adapter = CuratedPlantAdapter(section.curatedPlants)
            } else {
                curatedLabel.visibility = View.GONE
                curatedRecycler.visibility = View.GONE
            }
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

class CuratedPlantAdapter(
    private val plants: List<CuratedPlant>
) : RecyclerView.Adapter<CuratedPlantAdapter.CuratedViewHolder>() {

    inner class CuratedViewHolder(val binding: ItemSeasonPlantCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuratedViewHolder {
        val binding = ItemSeasonPlantCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CuratedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CuratedViewHolder, position: Int) {
        val plant = plants[position]
        with(holder.binding) {
            plantName.text = plant.name
            scientificName.visibility = View.GONE
            favoriteIcon.visibility = View.GONE
            root.isClickable = false
            root.isFocusable = false

            if (!plant.imageUrl.isNullOrBlank()) {
                Glide.with(plantImage.context)
                    .load(plant.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_plant)
                    .into(plantImage)
            } else {
                plantImage.setImageResource(R.drawable.placeholder_plant)
            }
        }
    }

    override fun getItemCount() = plants.size
}
