package com.endlessyoung.mysavings.ui.utils

import android.graphics.Color
import androidx.core.graphics.toColorInt

object AppColors {
    // 主题色 - 现代简洁蓝
    const val PRIMARY_BLUE = "#0066FF"
    const val PRIMARY_DARK = "#003D99"
    const val PRIMARY_LIGHT = "#F0F5FF"

    // 功能色 - 现代化调色板
    const val SUCCESS_GREEN = "#10B981"
    const val WARNING_ORANGE = "#F59E0B"
    const val ERROR_RED = "#EF4444"
    const val INFO_CYAN = "#06B6D4"
    const val PURPLE_ACCENT = "#8B5CF6"
    const val PINK_ACCENT = "#EC4899"

    // 背景和表面色
    const val BACKGROUND_WHITE = "#FFFFFF"
    const val BACKGROUND_LIGHT = "#F9FAFB"
    const val BACKGROUND_SECONDARY = "#F3F4F6"
    
    // 文本色 - 清晰的层级
    const val TEXT_PRIMARY = "#1F2937"
    const val TEXT_SECONDARY = "#6B7280"
    const val TEXT_TERTIARY = "#9CA3AF"
    const val TEXT_HINT = "#D1D5DB"
    
    // 分割线和边框
    const val DIVIDER_GRAY = "#E5E7EB"
    const val BORDER_GRAY = "#D1D5DB"

    fun getChartColorPalette(): List<Int> {
        return listOf(
            PRIMARY_BLUE.toColorInt(),
            SUCCESS_GREEN.toColorInt(),
            WARNING_ORANGE.toColorInt(),
            ERROR_RED.toColorInt(),
            PURPLE_ACCENT.toColorInt(),
            INFO_CYAN.toColorInt(),
            PINK_ACCENT.toColorInt()
        )
    }
}