package com.floraflow.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.floraflow.app.R
import com.floraflow.app.data.PreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    data class OnboardingPage(
        val emojiRes: Int,
        val titleRes: Int,
        val subtitleRes: Int
    )

    private lateinit var pager: ViewPager2
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: TextView
    private lateinit var dots: List<View>
    private lateinit var prefs: PreferencesManager

    private val pages by lazy {
        listOf(
            OnboardingPage(R.string.onboarding_page1_emoji, R.string.onboarding_page1_title, R.string.onboarding_page1_subtitle),
            OnboardingPage(R.string.onboarding_page2_emoji, R.string.onboarding_page2_title, R.string.onboarding_page2_subtitle),
            OnboardingPage(R.string.onboarding_page3_emoji, R.string.onboarding_page3_title, R.string.onboarding_page3_subtitle),
            OnboardingPage(R.string.onboarding_page4_emoji, R.string.onboarding_page4_title, R.string.onboarding_page4_subtitle)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        prefs = PreferencesManager(this)

        pager = findViewById(R.id.onboarding_pager)
        nextButton = findViewById(R.id.next_button)
        skipButton = findViewById(R.id.skip_button)
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        pager.adapter = OnboardingAdapter(pages)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                nextButton.text = when {
                    position == pages.size - 1 -> getString(R.string.onboarding_start)
                    else -> getString(R.string.onboarding_continue)
                }
                skipButton.visibility = if (position == pages.size - 1) View.INVISIBLE else View.VISIBLE
            }
        })

        nextButton.setOnClickListener {
            if (pager.currentItem < pages.size - 1) {
                pager.currentItem++
            } else {
                finishOnboarding()
            }
        }

        skipButton.setOnClickListener { finishOnboarding() }

        updateDots(0)
    }

    private fun updateDots(position: Int) {
        val density = resources.displayMetrics.density
        dots.forEachIndexed { index, dot ->
            val isActive = index == position
            val sizeDp = if (isActive) 10 else 7
            val lp = dot.layoutParams
            lp.width = (sizeDp * density).toInt()
            lp.height = (sizeDp * density).toInt()
            dot.layoutParams = lp
            dot.setBackgroundResource(if (isActive) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            prefs.setOnboardingComplete(true)
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    inner class OnboardingAdapter(private val items: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.page_emoji)
            val title: TextView = view.findViewById(R.id.page_title)
            val subtitle: TextView = view.findViewById(R.id.page_subtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val item = items[position]
            holder.emoji.text = getString(item.emojiRes)
            holder.title.text = getString(item.titleRes)
            holder.subtitle.text = getString(item.subtitleRes)
        }

        override fun getItemCount() = items.size
    }
}
