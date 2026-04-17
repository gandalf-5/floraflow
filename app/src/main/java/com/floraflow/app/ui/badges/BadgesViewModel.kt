package com.floraflow.app.ui.badges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.util.BadgeDefinition
import com.floraflow.app.util.BadgeManager
import kotlinx.coroutines.launch

data class BadgeUiItem(
    val definition: BadgeDefinition,
    val earned: Boolean,
    val earnedAtMs: Long = 0L
)

class BadgesViewModel(private val db: AppDatabase) : ViewModel() {

    private val _badges = MutableLiveData<List<BadgeUiItem>>(emptyList())
    val badges: LiveData<List<BadgeUiItem>> = _badges

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val earned = db.userBadgeDao().getAllEarnedBadges().associateBy { it.badgeId }
            val items = BadgeManager.ALL_BADGES.map { def ->
                val earnedBadge = earned[def.id]
                BadgeUiItem(
                    definition = def,
                    earned = earnedBadge != null,
                    earnedAtMs = earnedBadge?.earnedAt ?: 0L
                )
            }
            _badges.value = items
        }
    }
}
