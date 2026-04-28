package com.floraflow.app.ui.collections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.util.CollectionShareUtil
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class CollectionDetailFragment : Fragment() {

    private lateinit var viewModel: CollectionDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_collection_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val collectionId = arguments?.getLong("collectionId") ?: 0L
        val headerTitle = arguments?.getString("collectionName") ?: ""

        val factory = CollectionDetailViewModelFactory(
            (requireActivity().application as FloraFlowApp).database,
            collectionId
        )
        val vm: CollectionDetailViewModel by viewModels { factory }
        viewModel = vm

        val titleView  = view.findViewById<TextView>(R.id.collection_title)
        val subtitleView = view.findViewById<TextView>(R.id.collection_subtitle)
        val recycler   = view.findViewById<RecyclerView>(R.id.detail_recycler)
        val emptyState = view.findViewById<LinearLayout>(R.id.empty_state)
        val backBtn    = view.findViewById<View>(R.id.back_button)
        val shareBtn   = view.findViewById<View>(R.id.share_button)
        val addFab     = view.findViewById<ExtendedFloatingActionButton>(R.id.add_plants_fab)

        titleView.text = headerTitle
        backBtn.setOnClickListener { findNavController().navigateUp() }

        val adapter = CollectionDetailAdapter(
            onRemove = { plant ->
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.remove_from_collection))
                    .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.removePlant(plant) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        // Hide/show FAB based on scroll position
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8) addFab.shrink() else if (dy < -8) addFab.extend()
            }
        })

        viewModel.collectionWithPlants.observe(viewLifecycleOwner) { result ->
            val plants = result?.plants ?: emptyList()
            adapter.submitList(plants)
            emptyState.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
            subtitleView.text = getString(R.string.collection_plant_count, plants.size)

            shareBtn.setOnClickListener {
                if (result != null) CollectionShareUtil.share(requireContext(), result)
            }
        }

        // Load available plants for picker whenever collection changes
        viewModel.collectionWithPlants.observe(viewLifecycleOwner) {
            viewModel.loadAvailablePlants()
        }

        addFab.setOnClickListener { showAddPlantPicker() }
    }

    private fun showAddPlantPicker() {
        val available = viewModel.availablePlants.value ?: emptyList()

        if (available.isEmpty()) {
            // Check if it's because there are no discovered plants at all
            val allInCollection = viewModel.collectionWithPlants.value?.plants?.isNotEmpty() == true
            val msg = if (allInCollection)
                getString(R.string.all_plants_in_collection)
            else
                getString(R.string.no_plants_discovered_yet)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            return
        }

        val names = available.map { p ->
            buildString {
                append(p.plantName)
                if (!p.scientificName.isNullOrBlank()) append("\n${p.scientificName}")
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_plant_to_collection))
            .setItems(names) { _, idx -> addPlantAndConfirm(available[idx]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addPlantAndConfirm(plant: DailyPlant) {
        viewModel.addPlant(plant)
        Toast.makeText(
            requireContext(),
            getString(R.string.plant_added_to_collection, plant.plantName),
            Toast.LENGTH_SHORT
        ).show()
    }
}
