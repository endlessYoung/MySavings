package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.data.local.entity.PlanEntity
import com.endlessyoung.mysavings.databinding.DialogAddPlanBinding
import com.endlessyoung.mysavings.ui.base.BaseDialog
import com.google.android.material.datepicker.MaterialDatePicker
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddPlanDialogFragment : BaseDialog() {
    private var _binding: DialogAddPlanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavingViewModel by activityViewModels()

    // 默认选择当前系统时间
    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddPlanBinding.inflate(inflater, container, false)
        // 背景透明化处理，配合自定义的圆角背景 XML
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 设置默认日期回显
        binding.etPlanDate.setText(dateFormatter.format(Date(selectedDate)))

        // 2. 日期选择器逻辑
        binding.etPlanDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择计划日期")
                .setSelection(selectedDate)
                .setTheme(R.style.CustomDatePickerTheme)
                .build()

            datePicker.addOnPositiveButtonClickListener { utcTimestamp ->
                // MaterialDatePicker 返回的是 UTC 时间戳
                // 为了避免时区导致日期显示差一天，我们需要修正为本地时区时间戳
                val calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = utcTimestamp
                val localCalendar = java.util.Calendar.getInstance()
                localCalendar.set(calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH))

                selectedDate = localCalendar.timeInMillis
                binding.etPlanDate.setText(dateFormatter.format(Date(selectedDate)))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        // 3. 保存逻辑
        binding.btnSave.setOnClickListener {
            val title = binding.etPlanTitle.text.toString().trim()
            val amountStr = binding.etPlanAmount.text.toString().trim()

            // 基础校验
            if (title.isEmpty()) {
                binding.etPlanTitle.error = "请输入计划内容"
                return@setOnClickListener
            }
            if (amountStr.isEmpty()) {
                binding.etPlanAmount.error = "请输入金额"
                return@setOnClickListener
            }

            val plan = PlanEntity(
                title = title,
                amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                planDate = selectedDate,
                isRecurring = binding.switchRecurring.isChecked,
                isCompleted = false // 默认未完成
            )

            // 4. 调用 ViewModel 插入数据库
            viewModel.insertPlan(plan)

            Toast.makeText(requireContext(), "计划已添加", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}