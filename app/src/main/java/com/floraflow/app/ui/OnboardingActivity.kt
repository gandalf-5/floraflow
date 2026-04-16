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
        val emoji: String,
        val title: String,
        val subtitle: String
    )

    private val pages = listOf(
        OnboardingPage(
            "🌿",
            "Daily Botanical Discovery",
            "A new plant every single day — with over 400 species from every corner of the world. More than a full year of unique daily discoveries."
        ),
        OnboardingPage(
            "🧬",
            "AI Botanical Insights",
            "GPT-4 powered insights reveal the hidden science behind each plant — its native habitat, evolutionary history, and surprising uses."
        ),
        OnboardingPage(
            "📷",
            "Identify Any Plant",
            "Point your camera at any plant for instant AI identification. Discover what's growing in your garden, on a hike, or anywhere in the world."
        )
    )

    private lateinit var pager: ViewPager2
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: TextView
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        prefs = PreferencesManager(this)

        pager = findViewById(R.id.onboarding_pager)
        nextButton = findViewById(R.id.next_button)
        skipButton = findViewById(R.id.skip_button)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        pager.adapter = OnboardingAdapter(pages)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                nextButton.text = if (position == pages.size - 1) "Get Started" else "Continue"
            }
        })

        nextButton.setOnClickListener {
            if (pager.currentItem < pages.size - 1) {
                pager.currentItem++
            } else {
                finishOnboarding()
            }
        }

        skipButton.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateDots(position: Int) {
        val dots = listOf(dot1, dot2, dot3)
        dots.forEachIndexed { index, dot ->
            val size = if (index == position) 10 else 8
            val dpSize = (size * resources.displayMetrics.density).toInt()
            val lp = dot.layoutParams
            lp.width = dpSize
            lp.height = dpSize
            dot.layoutParams = lp
            dot.setBackgroundResource(
                if (index == position) R.drawable.dot_active else R.drawable.dot_inactive
            )
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
            holder.emoji.text = item.emoji
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle
        }

        override fun getItemCount() = items.size
    }
}
