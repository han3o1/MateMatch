package com.mp.matematch.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsRepository {

    private const val PREFERENCES_NAME = "MateMatchSettings"
    private const val KEY_PUSH_ENABLED = "isPushEnabled"
    private const val KEY_FEED_VIEW_MODE = "feedViewMode"
    private const val KEY_LAST_FILTER_CITY = "lastFilterCity"
    private const val KEY_LAST_FILTER_BUILDING = "lastFilterBuilding"
    const val VIEW_MODE_CARD = "CARD"
    const val VIEW_MODE_LIST = "LIST"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    // 푸시 알림 설정
    fun setPushEnabled(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit {
            putBoolean(KEY_PUSH_ENABLED, isEnabled)
        }
    }
    fun isPushEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PUSH_ENABLED, true)
    }

    // 피드 뷰 모드
    fun setFeedViewMode(context: Context, mode: String) {
        getPrefs(context).edit {
            putString(KEY_FEED_VIEW_MODE, mode)
        }
    }
    fun getFeedViewMode(context: Context): String {
        return getPrefs(context).getString(KEY_FEED_VIEW_MODE, VIEW_MODE_CARD) ?: VIEW_MODE_CARD
    }

    // 마지막으로 설정한 필터 저장
    fun setLastFilter(context: Context, city: String, buildingType: String) {
        getPrefs(context).edit {
            putString(KEY_LAST_FILTER_CITY, city)
            putString(KEY_LAST_FILTER_BUILDING, buildingType)
        }
    }
    fun getLastFilter(context: Context): Pair<String, String> {
        val prefs = getPrefs(context)
        val city = prefs.getString(KEY_LAST_FILTER_CITY, "") ?: ""
        val building = prefs.getString(KEY_LAST_FILTER_BUILDING, "") ?: ""
        // Pair 객체로 두 값을 한 번에 반환
        return Pair(city, building)
    }
}