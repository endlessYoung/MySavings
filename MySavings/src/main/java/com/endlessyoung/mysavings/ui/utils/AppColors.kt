package com.endlessyoung.mysavings.ui.utils

import android.graphics.Color
import androidx.core.graphics.toColorInt

object AppColors {
    const val PRIMARY_BLUE = "#1677FF"
    const val PRIMARY_DARK = "#1976D2"

    const val SUCCESS_GREEN = "#66BB6A"
    const val WARNING_ORANGE = "#FFA726"
    const val ERROR_RED = "#EF5350"
    const val INFO_CYAN = "#26C6DA"
    const val PURPLE_ACCENT = "#AB47BC"

    const val BACKGROUND_LIGHT = "#F8F9FA"
    const val TEXT_MAIN = "#263238"
    const val TEXT_SECONDARY = "#90A4AE"
    const val DIVIDER_GRAY = "#EEEEEE"

    fun getChartColorPalette(): List<Int> {
        return listOf(
            PRIMARY_BLUE.toColorInt(),
            SUCCESS_GREEN.toColorInt(),
            WARNING_ORANGE.toColorInt(),
            ERROR_RED.toColorInt(),
            PURPLE_ACCENT.toColorInt(),
            INFO_CYAN.toColorInt()
        )
    }
}