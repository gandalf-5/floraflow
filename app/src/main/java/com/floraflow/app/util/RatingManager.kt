package com.floraflow.app.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

object RatingManager {

    private const val PREFS              = "rating_prefs"
    private const val KEY_POSITIVE       = "positive_events"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private const val MIN_EVENTS         = 3
    private const val MIN_DAYS           = 30L

    fun recordPositiveEvent(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = sp.getInt(KEY_POSITIVE, 0) + 1
        sp.edit().putInt(KEY_POSITIVE, count).apply()
    }

    fun requestReviewIfAppropriate(activity: Activity) {
        val sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = sp.getInt(KEY_POSITIVE, 0)
        val lastMs = sp.getLong(KEY_LAST_PROMPT_MS, 0L)
        val daysSince = (System.currentTimeMillis() - lastMs) / 86_400_000L

        val neverAsked = lastMs == 0L
        if (count >= MIN_EVENTS && (neverAsked || daysSince >= MIN_DAYS)) {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { req ->
                if (req.isSuccessful) {
                    manager.launchReviewFlow(activity, req.result).addOnCompleteListener {
                        sp.edit()
                            .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
                            .putInt(KEY_POSITIVE, 0)
                            .apply()
                    }
                }
            }
        }
    }
}
