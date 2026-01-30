package com.endlessyoung.mysavings.ui.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object SettingsManager {
    private const val PREF_NAME = "mysavings_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_HIDE_AMOUNT = "hide_amount"
    private const val KEY_APP_LOCK = "app_lock"
    private const val KEY_DEFAULT_SORT = "default_sort"

    // Sort modes
    const val SORT_BY_TIME = 0
    const val SORT_BY_AMOUNT = 1

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Theme Mode
    fun getThemeMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getThemeModeLabel(context: Context): String {
        return when (getThemeMode(context)) {
            AppCompatDelegate.MODE_NIGHT_NO -> "浅色模式"
            AppCompatDelegate.MODE_NIGHT_YES -> "深色模式"
            else -> "跟随系统"
        }
    }

    // Hide Amount
    fun isAmountHiddenOnStart(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HIDE_AMOUNT, false)
    }

    fun setAmountHiddenOnStart(context: Context, hidden: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIDE_AMOUNT, hidden).apply()
    }

    // App Lock
    fun isAppLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_APP_LOCK, false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    }

    // Default Sort
    fun getDefaultSort(context: Context): Int {
        return getPrefs(context).getInt(KEY_DEFAULT_SORT, SORT_BY_TIME)
    }

    fun setDefaultSort(context: Context, sortMode: Int) {
        getPrefs(context).edit().putInt(KEY_DEFAULT_SORT, sortMode).apply()
    }

    fun getDefaultSortLabel(context: Context): String {
        return when (getDefaultSort(context)) {
            SORT_BY_AMOUNT -> "按金额"
            else -> "按时间"
        }
    }
    
    // Initialize theme on app start
    fun initTheme(context: Context) {
        val mode = getThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
