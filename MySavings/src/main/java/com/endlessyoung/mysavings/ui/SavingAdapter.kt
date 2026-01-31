package com.endlessyoung.mysavings.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.domain.model.SavingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.endlessyoung.mysavings.MoneyUtils
import com.google.android.material.card.MaterialCardView


class SavingAdapter(
    private val onClick: ((SavingItem) -> Unit)? = null
) : ListAdapter<SavingItem, SavingAdapter.VH>(Diff) {

    var isPrivacyMode: Boolean = false

    companion object {
        private const val DAY = 24 * 60 * 60 * 1000L
    }

    object Diff : DiffUtil.ItemCallback<SavingItem>() {
        override fun areItemsTheSame(o: SavingItem, n: SavingItem): Boolean {
            return o.startTime == n.startTime && o.bankName == n.bankName && o.amount == n.amount
        }

        override fun areContentsTheSame(o: SavingItem, n: SavingItem): Boolean = o == n
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings, parent, false)
        return VH(view, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), isPrivacyMode)
    }

    class VH(
        itemView: View,
        private val onClick: ((SavingItem) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvBank = itemView.findViewById<TextView>(R.id.tvBank)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
        private val tvRate = itemView.findViewById<TextView>(R.id.tvRate)
        private val tvEndTime = itemView.findViewById<TextView>(R.id.tvEndTime)
        private val tvRemainingDays = itemView.findViewById<TextView>(R.id.tvRemainingDays)
        private val tvDays = itemView.findViewById<TextView>(R.id.tvDays)
        private val tvInterest = itemView.findViewById<TextView>(R.id.tvInterest)
        private val tvTotalAmount = itemView.findViewById<TextView>(R.id.tvTotalAmount)
        private val viewBankColorTag = itemView.findViewById<View>(R.id.viewBankColorTag)

        private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        @SuppressLint("SetTextI18n")
        fun bind(item: SavingItem, isPrivacyMode: Boolean) {
            val now = System.currentTimeMillis()
            val remain = (item.endTime - now).coerceAtLeast(0)
            val days = remain / DAY
            val total = item.amount.add(item.interest)
            val year = item.year
            val bankColor = getBankColor(item.bankName)

            tvBank.text = item.bankName
            
            if (isPrivacyMode) {
                tvAmount.text = "******"
                tvInterest.text = "+ ******"
                tvTotalAmount.text = "******"
            } else {
                tvAmount.text = MoneyUtils.formatWithSymbol(item.amount)
                tvInterest.text = "+ ${MoneyUtils.formatWithSymbol(item.interest)}"
                tvTotalAmount.text = MoneyUtils.formatWithSymbol(total)
            }
            
            tvRate.text = "利率 ${item.interestRate}%"
            tvEndTime.text = "到期 ${sdf.format(Date(item.endTime))}"
            tvDays.text = "$year 年期"
            tvRemainingDays.text = "剩余 $days 天"

            tvDays.setTextColor(
                if (days <= 7) Color.RED else "#4CAF50".toColorInt()
            )

            viewBankColorTag.setBackgroundColor(bankColor.toColorInt())

            val bgColor = when {
                days <= 0 -> "#FFEBEE"
                days <= 7 -> "#FFF3E0"
                else -> "#FFFFFF"
            }

            (itemView as? MaterialCardView)?.setCardBackgroundColor(bgColor.toColorInt())

//            progress?.let {
//                val total = (item.endTime - (item.endTime - 365 * DAY)).coerceAtLeast(1)
//                it.max = total.toInt()
//                it.progress = (total - remain).toInt().coerceAtLeast(0)
//            }

            itemView.setOnClickListener { onClick?.invoke(item) }
        }

        private fun getBankColor(bankName: String): String {
            return when {
                bankName.contains("招商") -> "#E91E63"
                bankName.contains("建设") -> "#003399"
                bankName.contains("农业") -> "#008573"
                bankName.contains("工商") -> "#C62828"
                bankName.contains("中国银行") -> "#B71C1C"
                bankName.contains("交通") -> "#003DA5"
                bankName.contains("邮储") -> "#007D32"
                else -> "#B0BEC5"
            }
        }

        private fun getBankBg(bankName: String): Drawable? {
            val resId = when {
                bankName.contains("招商") ->  R.drawable.bg_xingye
                bankName.contains("建设") -> R.drawable.bg_xingye
                bankName.contains("农业") -> R.drawable.bg_xingye
                bankName.contains("工商") -> R.drawable.bg_xingye
                bankName.contains("中国银行") -> R.drawable.bg_xingye
                bankName.contains("交通") -> R.drawable.bg_xingye
                bankName.contains("邮储") -> R.drawable.bg_xingye
                bankName.contains("兴业") -> R.drawable.bg_xingye
                else -> R.drawable.bg_xingye
            }

            return ContextCompat.getDrawable(itemView.context, resId)?.mutate()?.apply {
                alpha = 35
            }
        }
    }
}

