package com.floraflow.app.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floraflow.app.R
import com.floraflow.app.data.IdentificationRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IdentGalleryAdapter(
    private val onDelete: (IdentificationRecord) -> Unit,
    private val onSetWallpaper: (IdentificationRecord) -> Unit,
    private val onShare: (IdentificationRecord) -> Unit
) : ListAdapter<IdentificationRecord, IdentGalleryAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IdentificationRecord>() {
            override fun areItemsTheSame(a: IdentificationRecord, b: IdentificationRecord) = a.id == b.id
            override fun areContentsTheSame(a: IdentificationRecord, b: IdentificationRecord) = a == b
        }
        private val DATE_FMT = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.record_photo)
        val name: TextView = view.findViewById(R.id.record_common_name)
        val scientific: TextView = view.findViewById(R.id.record_scientific_name)
        val confidence: TextView = view.findViewById(R.id.record_confidence)
        val dateTime: TextView = view.findViewById(R.id.record_datetime)
        val location: TextView = view.findViewById(R.id.record_location)
        val shareBtn: ImageView = view.findViewById(R.id.record_share_btn)
        val wallpaperBtn: ImageView = view.findViewById(R.id.record_wallpaper_btn)
        val deleteBtn: ImageView = view.findViewById(R.id.record_delete_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ident_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)

        Glide.with(holder.photo.context)
            .load(File(record.photoPath))
            .centerCrop()
            .placeholder(R.drawable.placeholder_plant)
            .into(holder.photo)

        holder.name.text = record.commonName
        holder.scientific.text = record.scientificName
        holder.confidence.text = "${record.confidence}% match"
        holder.dateTime.text = DATE_FMT.format(Date(record.timestampMs))

        if (!record.locationName.isNullOrBlank()) {
            holder.location.visibility = View.VISIBLE
            holder.location.text = "📍 ${record.locationName}"
        } else {
            holder.location.visibility = View.GONE
        }

        holder.shareBtn.setOnClickListener { onShare(record) }
        holder.wallpaperBtn.setOnClickListener { onSetWallpaper(record) }
        holder.deleteBtn.setOnClickListener { onDelete(record) }
    }
}
