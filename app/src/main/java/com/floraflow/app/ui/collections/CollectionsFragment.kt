package com.floraflow.app.ui.collections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R

class CollectionsFragment : Fragment() {

    private val viewModel: CollectionsViewModel by viewModels {
        CollectionsViewModelFactory((requireActivity().application as FloraFlowApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_collections, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.collections_recycler)
        val emptyState = view.findViewById<LinearLayout>(R.id.empty_state)
        val fab = view.findViewById<View>(R.id.fab_new_collection)
        val backBtn = view.findViewById<View>(R.id.back_button)

        backBtn.setOnClickListener { findNavController().navigateUp() }

        val adapter = CollectionsAdapter(
            onClick = { col ->
                findNavController().navigate(
                    R.id.action_collectionsFragment_to_collectionDetailFragment,
                    bundleOf("collectionId" to col.id, "collectionName" to "${col.emoji} ${col.name}")
                )
            },
            onLongClick = { col -> showDeleteDialog(col) },
            getCount = { id -> viewModel.collectionPlantCounts.value?.get(id) ?: 0 }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewModel.collections.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            viewModel.loadCounts()
        }

        viewModel.collectionPlantCounts.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }

        fab.setOnClickListener { showCreateDialog() }
    }

    private fun showCreateDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val emojiInput = EditText(ctx).apply {
            hint = getString(R.string.collection_emoji_hint)
            textSize = 22f
            setSingleLine()
            setText("🌿")
        }
        val nameInput = EditText(ctx).apply {
            hint = getString(R.string.collection_name_hint)
            textSize = 16f
            setSingleLine()
        }
        layout.addView(emojiInput)
        layout.addView(nameInput)

        AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.new_collection))
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createCollection(name, emojiInput.text.toString())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(col: com.floraflow.app.data.PlantCollection) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_collection))
            .setMessage(getString(R.string.delete_collection_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.deleteCollection(col) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
