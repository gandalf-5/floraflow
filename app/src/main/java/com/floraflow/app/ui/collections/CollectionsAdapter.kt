package com.floraflow.app.ui.collections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.floraflow.app.R
import com.floraflow.app.data.PlantCollection

class CollectionsAdapter(
    private val onClick: (PlantCollection) -> Unit,
    private val onLongClick: (PlantCollection) -> Unit,
    private val getCount: (Long) -> Int
) : ListAdapter<PlantCollection, CollectionsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PlantCollection>() {
            override fun areItemsTheSame(a: PlantCollection, b: PlantCollection) = a.id == b.id
            override fun areContentsTheSame(a: PlantCollection, b: PlantCollection) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.collection_emoji)
        val name: TextView = view.findViewById(R.id.collection_name)
        val count: TextView = view.findViewById(R.id.collection_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val col = getItem(position)
        holder.emoji.text = col.emoji
        holder.name.text = col.name
        val cnt = getCount(col.id)
        holder.count.text = holder.itemView.context.getString(R.string.collection_plant_count, cnt)
        holder.itemView.setOnClickListener { onClick(col) }
        holder.itemView.setOnLongClickListener { onLongClick(col); true }
    }
}
