package com.floraflow.app.ui.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R

class BadgesFragment : Fragment() {

    private val viewModel: BadgesViewModel by viewModels {
        BadgesViewModelFactory((requireActivity().application as FloraFlowApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_badges, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.badges_recycler)
        val progressText = view.findViewById<TextView>(R.id.badges_progress_text)
        val backBtn = view.findViewById<View>(R.id.back_button)

        backBtn.setOnClickListener { findNavController().navigateUp() }

        val adapter = BadgesAdapter()
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        viewModel.badges.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val earned = items.count { it.earned }
            val total = items.size
            progressText.text = getString(R.string.badges_progress, earned, total)
        }
    }
}
