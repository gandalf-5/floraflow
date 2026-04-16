package com.floraflow.app.ui.seasonal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.databinding.FragmentSeasonalBinding

class SeasonalFragment : Fragment() {

    private var _binding: FragmentSeasonalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SeasonalViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        SeasonalViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeasonalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.sectionsRecycler.layoutManager = LinearLayoutManager(requireContext())

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.loadingGroup.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.sections.observe(viewLifecycleOwner) { sections ->
            if (sections.isEmpty()) {
                binding.emptyGroup.visibility = View.VISIBLE
                binding.sectionsRecycler.visibility = View.GONE
            } else {
                binding.emptyGroup.visibility = View.GONE
                binding.sectionsRecycler.visibility = View.VISIBLE
                binding.sectionsRecycler.adapter = SeasonalAdapter(sections) { plant ->
                    // Navigate back to discovery context
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
