package com.floraflow.app.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentQuizBinding
import com.google.android.material.button.MaterialButton

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        QuizViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.openAiApi),
            PreferencesManager(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizUiState.Loading -> showLoading()
                is QuizUiState.Ready -> showQuiz(state)
                is QuizUiState.Unavailable -> showUnavailable()
            }
        }
    }

    private fun showLoading() {
        binding.loadingGroup.visibility = View.VISIBLE
        binding.quizGroup.visibility = View.GONE
        binding.unavailableGroup.visibility = View.GONE
    }

    private fun showUnavailable() {
        binding.loadingGroup.visibility = View.GONE
        binding.quizGroup.visibility = View.GONE
        binding.unavailableGroup.visibility = View.VISIBLE
        binding.retryButton.setOnClickListener { viewModel.retry() }
    }

    private fun showQuiz(state: QuizUiState.Ready) {
        binding.loadingGroup.visibility = View.GONE
        binding.unavailableGroup.visibility = View.GONE
        binding.quizGroup.visibility = View.VISIBLE

        binding.questionText.text = state.quiz.question
        binding.answersContainer.removeAllViews()

        state.quiz.options.forEachIndexed { index, option ->
            val btn = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = option
                isAllCaps = false
                textSize = 14f
                setPadding(48, 24, 48, 24)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 16) }
                layoutParams = lp

                when {
                    state.revealed && index == state.quiz.correct -> {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_correct))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        strokeColor = null
                        isEnabled = false
                    }
                    state.revealed && index == state.selected && index != state.quiz.correct -> {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_wrong))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        strokeColor = null
                        isEnabled = false
                    }
                    state.revealed -> {
                        alpha = 0.5f
                        isEnabled = false
                    }
                    else -> {
                        setOnClickListener { viewModel.selectAnswer(index) }
                    }
                }
            }
            binding.answersContainer.addView(btn)
        }

        if (state.revealed) {
            binding.explanationCard.visibility = View.VISIBLE
            binding.explanationText.text = state.quiz.explanation
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.content_fade_in)
            binding.explanationCard.startAnimation(fadeIn)
            val icon = if (state.selected == state.quiz.correct) "✅ Correct!" else "❌ Incorrect"
            binding.resultLabel.text = icon
            binding.resultLabel.visibility = View.VISIBLE
        } else {
            binding.explanationCard.visibility = View.GONE
            binding.resultLabel.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
