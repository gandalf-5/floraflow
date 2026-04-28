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
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentFavoritesBinding
import com.floraflow.app.ui.collections.CollectionsViewModel
import com.floraflow.app.ui.collections.CollectionsViewModelFactory
import com.floraflow.app.ui.premium.PremiumBottomSheetFragment
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        FavoritesViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi),
            PreferencesManager(requireContext())
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

        viewModel.isPremium.observe(viewLifecycleOwner) { premium ->
            binding.favoritesFreeBanner.visibility = if (premium) View.GONE else View.VISIBLE
        }

        viewModel.totalFavCount.observe(viewLifecycleOwner) { total ->
            val isPrem = viewModel.isPremium.value ?: false
            if (!isPrem) {
                val limit = PreferencesManager.FREE_FAVORITES_LIMIT
                binding.favoritesFreeBannerText.text = getString(
                    R.string.favorites_free_limit_banner,
                    minOf(total, limit),
                    limit
                )
            }
        }

        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
            binding.emptyText.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
        }

        collectionsViewModel.collections.observe(viewLifecycleOwner) { /* kept active */ }

        binding.collectionsButton.setOnClickListener {
            findNavController().navigate(R.id.action_favorites_to_collections)
        }

        binding.badgesButton.setOnClickListener {
            findNavController().navigate(R.id.action_favorites_to_badges)
        }

        binding.favoritesFreeBanner.setOnClickListener {
            PremiumBottomSheetFragment.newInstance()
                .show(parentFragmentManager, PremiumBottomSheetFragment.TAG)
        }
    }

    private fun confirmRemoveFavorite(plant: DailyPlant) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.favorites_remove_title))
            .setMessage(getString(R.string.favorites_remove_message, plant.plantName))
            .setPositiveButton(getString(R.string.favorites_remove_confirm)) { _, _ ->
                viewModel.removeFavorite(plant)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddToCollectionDialog(plant: DailyPlant) {
        val collections = collectionsViewModel.collections.value ?: emptyList()
        if (collections.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.collections_empty))
                .setPositiveButton(getString(R.string.favorites_create_collection)) { _, _ ->
                    findNavController().navigate(R.id.action_favorites_to_collections)
                }
                .setNegativeButton(android.R.string.cancel, null)
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
            .setNegativeButton(android.R.string.cancel, null)
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
