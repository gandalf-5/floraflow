package com.floraflow.app.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.floraflow.app.R
import com.floraflow.app.databinding.FragmentPremiumBottomSheetBinding
import com.floraflow.app.databinding.ItemPremiumRowBinding
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

        data class RowData(
            val rowBinding: ItemPremiumRowBinding,
            val icon: String,
            val labelRes: Int,
            val freeRes: Int,
            val premiumRes: Int
        )

        val rows = listOf(
            RowData(binding.rowDiscovery,     "🌿", R.string.feat_daily_discovery,     R.string.check,             R.string.check),
            RowData(binding.rowIdentify,      "🔬", R.string.feat_identifications,      R.string.free_id_limit,     R.string.unlimited),
            RowData(binding.rowStoryBasic,    "📖", R.string.feat_story_etymology,      R.string.check,             R.string.check),
            RowData(binding.rowStoryFull,     "🔮", R.string.feat_story_full,           R.string.cross,             R.string.check),
            RowData(binding.rowWallpaper,     "🖼️", R.string.feat_auto_wallpaper,       R.string.cross,             R.string.check),
            RowData(binding.rowIdentWallpaper,"🌿", R.string.feat_ident_wallpaper,      R.string.cross,             R.string.check),
            RowData(binding.rowGallery,       "📚", R.string.feat_gallery,              R.string.free_gallery_limit,R.string.unlimited),
            RowData(binding.rowPdf,           "📄", R.string.feat_pdf_export,           R.string.cross,             R.string.check),
        )

        rows.forEach { row ->
            row.rowBinding.rowIcon.text         = row.icon
            row.rowBinding.rowLabel.text        = getString(row.labelRes)
            row.rowBinding.rowFreeValue.text    = getString(row.freeRes)
            row.rowBinding.rowPremiumValue.text = getString(row.premiumRes)
        }

        // Style cross values in red-ish, check values in green
        rows.forEach { row ->
            val freeText = row.rowBinding.rowFreeValue.text.toString()
            if (freeText == getString(R.string.cross)) {
                row.rowBinding.rowFreeValue.alpha = 0.35f
            }
        }

        binding.premiumCtaButton.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
