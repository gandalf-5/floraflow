package com.floraflow.app.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.floraflow.app.R
import com.floraflow.app.databinding.FragmentPremiumBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PremiumBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPremiumBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "PremiumBottomSheet"
        fun newInstance() = PremiumBottomSheetFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Feature rows: icon, label, free value, premium value
        val rows = listOf(
            Row(binding.rowDiscovery,    "🌿", R.string.feat_daily_discovery,      R.string.check, R.string.check),
            Row(binding.rowIdentify,     "🔬", R.string.feat_identifications,       R.string.free_id_limit, R.string.unlimited),
            Row(binding.rowStoryBasic,   "📖", R.string.feat_story_etymology,       R.string.check, R.string.check),
            Row(binding.rowStoryFull,    "🔮", R.string.feat_story_full,            R.string.cross,  R.string.check),
            Row(binding.rowWallpaper,    "🖼️", R.string.feat_auto_wallpaper,        R.string.cross,  R.string.check),
            Row(binding.rowIdentWallpaper,"🌿",R.string.feat_ident_wallpaper,       R.string.cross,  R.string.check),
            Row(binding.rowGallery,      "📚", R.string.feat_gallery,               R.string.free_gallery_limit, R.string.unlimited),
            Row(binding.rowPdf,          "📄", R.string.feat_pdf_export,            R.string.cross,  R.string.check),
        )

        rows.forEach { row ->
            row.rootView.findViewById<TextView>(R.id.row_icon).text = row.icon
            row.rootView.findViewById<TextView>(R.id.row_label).text = getString(row.labelRes)
            row.rootView.findViewById<TextView>(R.id.row_free_value).text = getString(row.freeRes)
            row.rootView.findViewById<TextView>(R.id.row_premium_value).text = getString(row.premiumRes)
        }

        binding.premiumCtaButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class Row(
        val rootView: View,
        val icon: String,
        val labelRes: Int,
        val freeRes: Int,
        val premiumRes: Int
    )
}
