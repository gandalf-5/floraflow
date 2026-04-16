package com.floraflow.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        HistoryViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi)
        )
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter { plant -> viewModel.toggleFavorite(plant) }
        binding.historyRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.historyRecycler.adapter = adapter

        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
            binding.emptyText.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
