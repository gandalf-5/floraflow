package com.floraflow.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.floraflow.app.R
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val title = findViewById<TextView>(R.id.splash_title)
        val subtitle = findViewById<TextView>(R.id.splash_subtitle)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        val fadeInDelay = AnimationUtils.loadAnimation(this, R.anim.fade_in_up_delay)

        logo.startAnimation(fadeIn)
        title.startAnimation(fadeIn)
        subtitle.startAnimation(fadeInDelay)

        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                val onboardingDone = PreferencesManager(this@SplashActivity)
                    .onboardingComplete.first()
                val target = if (onboardingDone) {
                    Intent(this@SplashActivity, MainActivity::class.java)
                } else {
                    Intent(this@SplashActivity, OnboardingActivity::class.java)
                }
                startActivity(target)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }, 1700)
    }
}
