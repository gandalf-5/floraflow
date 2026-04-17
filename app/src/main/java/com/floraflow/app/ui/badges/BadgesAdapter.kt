package com.floraflow.app.ui.badges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.floraflow.app.R

class BadgesAdapter : ListAdapter<BadgeUiItem, BadgesAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BadgeUiItem>() {
            override fun areItemsTheSame(a: BadgeUiItem, b: BadgeUiItem) =
                a.definition.id == b.definition.id
            override fun areContentsTheSame(a: BadgeUiItem, b: BadgeUiItem) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.badge_card)
        val emoji: TextView = view.findViewById(R.id.badge_emoji)
        val name: TextView = view.findViewById(R.id.badge_name)
        val desc: TextView = view.findViewById(R.id.badge_desc)
        val status: TextView = view.findViewById(R.id.badge_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context
        holder.emoji.text = item.definition.emoji
        holder.name.text = ctx.getString(item.definition.nameResId)
        holder.desc.text = ctx.getString(item.definition.descResId)

        if (item.earned) {
            holder.card.background = ContextCompat.getDrawable(ctx, R.drawable.bg_badge_earned)
            holder.emoji.alpha = 1f
            holder.name.alpha = 1f
            holder.status.text = ctx.getString(R.string.badge_earned_label)
            holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
        } else {
            holder.card.background = ContextCompat.getDrawable(ctx, R.drawable.bg_badge_locked)
            holder.emoji.alpha = 0.4f
            holder.name.alpha = 0.5f
            holder.status.text = ctx.getString(R.string.badge_locked_label)
            holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.text_tertiary))
        }
    }
}
