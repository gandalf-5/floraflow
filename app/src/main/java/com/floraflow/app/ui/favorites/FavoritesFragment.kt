package com.floraflow.app.ui.favorites

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        FavoritesViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi)
        )
    }

    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FavoritesAdapter { plant -> confirmRemoveFavorite(plant) }
        binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.favoritesRecycler.adapter = adapter

        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
            binding.emptyText.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun confirmRemoveFavorite(plant: DailyPlant) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove from Favorites?")
            .setMessage("\"${plant.plantName}\" will be removed from your favorites.")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeFavorite(plant) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
