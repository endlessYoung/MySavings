package com.endlessyoung.mysavings.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.databinding.ItemPlanExpandedBinding
import androidx.core.graphics.toColorInt

class ExpandedPlanAdapter(private val onSyncClick: (PlanEntity) -> Unit) :
    ListAdapter<PlanEntity, ExpandedPlanAdapter.PlanViewHolder>(PlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanExpandedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanViewHolder(private val binding: ItemPlanExpandedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: PlanEntity) {
            binding.tvPlanTitle.text = plan.title
            binding.tvPlanAmount.text = MoneyUtils.formatWithSymbol(plan.amount)

            val diff = plan.planDate - System.currentTimeMillis()
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()

            // 根据紧急程度切换颜色
            when {
                days < 0 -> {
                    binding.tvPlanDays.text = "已逾期"
                    binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    binding.tvPlanDays.backgroundTintList = ColorStateList.valueOf("#FFEBEE".toColorInt())
                    binding.tvPlanDays.setTextColor(Color.RED)
                }
                days == 0 -> {
                    binding.tvPlanDays.text = "今天"
                    binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf("#FF9800".toColorInt())
                }
                else -> {
                    binding.tvPlanDays.text = "${days + 1}天后"
                    binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf("#4CAF50".toColorInt())
                }
            }
        }
    }

    class PlanDiffCallback : DiffUtil.ItemCallback<PlanEntity>() {
        override fun areItemsTheSame(oldItem: PlanEntity, newItem: PlanEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PlanEntity, newItem: PlanEntity) = oldItem == newItem
    }
}