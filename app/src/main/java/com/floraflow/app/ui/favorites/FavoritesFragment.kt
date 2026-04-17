package com.floraflow.app.ui.favorites

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.databinding.FragmentFavoritesBinding
import com.floraflow.app.ui.collections.CollectionsViewModel
import com.floraflow.app.ui.collections.CollectionsViewModelFactory
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        FavoritesViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi)
        )
    }

    private val collectionsViewModel: CollectionsViewModel by viewModels {
        CollectionsViewModelFactory((requireActivity().application as FloraFlowApp).database)
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

        adapter = FavoritesAdapter(
            onRemoveClick = { plant -> confirmRemoveFavorite(plant) },
            onLongClick = { plant -> showAddToCollectionDialog(plant) }
        )
        binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.favoritesRecycler.adapter = adapter

        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
            binding.emptyText.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
        }

        // Keep collections LiveData active so .value is populated for dialogs
        collectionsViewModel.collections.observe(viewLifecycleOwner) { /* kept active */ }

        binding.collectionsButton.setOnClickListener {
            findNavController().navigate(R.id.action_favorites_to_collections)
        }

        binding.badgesButton.setOnClickListener {
            findNavController().navigate(R.id.action_favorites_to_badges)
        }
    }

    private fun confirmRemoveFavorite(plant: DailyPlant) {
        AlertDialog.Builder(requireContext())
            .setTitle("Retirer des favoris ?")
            .setMessage("« ${plant.plantName} » sera retiré de vos favoris.")
            .setPositiveButton("Retirer") { _, _ -> viewModel.removeFavorite(plant) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAddToCollectionDialog(plant: DailyPlant) {
        val collections = collectionsViewModel.collections.value ?: emptyList()
        if (collections.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.collections_empty))
                .setPositiveButton("Créer une collection") { _, _ ->
                    findNavController().navigate(R.id.action_favorites_to_collections)
                }
                .setNegativeButton("Annuler", null)
                .show()
            return
        }

        val names = collections.map { "${it.emoji} ${it.name}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_to_collection))
            .setItems(names) { _, index ->
                val col = collections[index]
                lifecycleScope.launch {
                    collectionsViewModel.addPlantToCollection(plant.dateKey, col.id)
                }
            }
            .setNegativeButton("Annuler", null)
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
