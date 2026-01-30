package com.endlessyoung.mysavings.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.databinding.ItemReminderCardBinding
import com.endlessyoung.mysavings.ui.utils.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter : ListAdapter<PlanEntity, ReminderAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReminderViewHolder(private val binding: ItemReminderCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())

        fun bind(plan: PlanEntity) {
            binding.tvReminderTitle.text = plan.title
            binding.tvReminderDate.text = dateFormat.format(Date(plan.planDate))

            val now = System.currentTimeMillis()
            val diff = plan.planDate - now
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()

            binding.tvDaysLeft.apply {
                when {
                    days < 0 -> {
                        text = "已逾期 ${-days} 天"
                        backgroundTintList = ColorStateList.valueOf(AppColors.ERROR_RED.toColorInt())
                    }
                    days == 0 -> {
                        text = "今天"
                        backgroundTintList = ColorStateList.valueOf(AppColors.WARNING_ORANGE.toColorInt())
                    }
                    else -> {
                        text = "还有 ${days + 1} 天"
                        backgroundTintList = ColorStateList.valueOf(AppColors.PRIMARY_BLUE.toColorInt())
                    }
                }
            }
        }
    }

    class ReminderDiffCallback : DiffUtil.ItemCallback<PlanEntity>() {
        override fun areItemsTheSame(oldItem: PlanEntity, newItem: PlanEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PlanEntity, newItem: PlanEntity) = oldItem == newItem
    }
}
