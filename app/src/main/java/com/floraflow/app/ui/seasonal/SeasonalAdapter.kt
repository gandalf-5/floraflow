package com.floraflow.app.ui.seasonal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.databinding.ItemSeasonPlantCardBinding
import com.floraflow.app.databinding.ItemSeasonSectionBinding
import com.google.android.material.chip.Chip

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

            try {
                seasonCard.setCardBackgroundColor(Color.parseColor(section.color))
            } catch (e: Exception) { }

            // Hide season badge — not relevant for ecosystem view
            inSeasonBadge.visibility = android.view.View.GONE

            if (section.plants.isNotEmpty()) {
                val ctx = root.context
                plantCount.text = ctx.resources.getQuantityString(
                    R.plurals.seasonal_discovered_count, section.plants.size, section.plants.size
                )
                plantCount.visibility = android.view.View.VISIBLE
                plantsRecycler.visibility = android.view.View.VISIBLE
                curatedChipGroup.visibility = android.view.View.GONE

                plantsRecycler.layoutManager = LinearLayoutManager(
                    ctx, LinearLayoutManager.HORIZONTAL, false
                )
                plantsRecycler.adapter = SeasonPlantAdapter(section.plants, onPlantClick)
            } else {
                // Show curated plant names as chips
                plantCount.text = root.context.getString(
                    R.string.explore_region_count, section.curatedNames.size
                )
                plantCount.visibility = android.view.View.VISIBLE
                plantsRecycler.visibility = android.view.View.GONE

                curatedChipGroup.removeAllViews()
                section.curatedNames.forEach { name ->
                    val chip = Chip(root.context).apply {
                        text = name
                        isClickable = false
                        isCheckable = false
                        setChipBackgroundColorResource(android.R.color.transparent)
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                        chipStrokeWidth = 1f
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
                        setTextColor(Color.WHITE)
                    }
                    curatedChipGroup.addView(chip)
                }
                curatedChipGroup.visibility = android.view.View.VISIBLE
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
                scientificName.visibility = android.view.View.VISIBLE
            } else {
                scientificName.visibility = android.view.View.GONE
            }
            Glide.with(root)
                .load(plant.imageUrlRegular)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_plant)
                .into(plantImage)
            root.setOnClickListener { onClick(plant) }
        }
    }

    override fun getItemCount() = plants.size
}
