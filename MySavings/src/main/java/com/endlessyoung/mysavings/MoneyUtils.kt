package com.endlessyoung.mysavings

import java.math.BigDecimal

object MoneyUtils {

    private val WAN = BigDecimal("10000")
    private val YI = BigDecimal("100000000")

    fun formatSmart(amount: BigDecimal?): String {
        if (amount == null) return "0"

        val normalized = amount.stripTrailingZeros()

        return when {
            normalized >= YI -> {
                format(normalized.divide(YI)) + " 亿"
            }
            normalized >= WAN -> {
                format(normalized.divide(WAN)) + " 万"
            }
            else -> format(normalized)
        }
    }

    fun formatWithSymbol(amount: BigDecimal?): String {
        return "¥ ${formatSmart(amount)}"
    }

    fun formatWithNumber(amount: BigDecimal?): String {
        return formatSmart(amount).dropLast(1)
    }

    private fun format(value: BigDecimal): String {
        val v = value.stripTrailingZeros()
        val plain = v.toPlainString()

        val parts = plain.split(".")
        val intPart = parts[0]
        val decPart = parts.getOrNull(1)

        val intFormatted = formatThousands(intPart)

        return if (decPart.isNullOrEmpty()) {
            intFormatted
        } else {
            "$intFormatted.$decPart"
        }
    }

    private fun formatThousands(intPart: String): String {
        val sb = StringBuilder()
        var count = 0
        for (i in intPart.length - 1 downTo 0) {
            sb.append(intPart[i])
            count++
            if (count == 3 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString()
    }
}

