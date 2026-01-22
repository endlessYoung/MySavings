package com.endlessyoung.mysavings.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.domain.model.SavingItem
import com.google.android.material.datepicker.MaterialDatePicker
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.drawable.toDrawable
import com.endlessyoung.mysavings.databinding.DialogAddSavingBinding
import com.endlessyoung.mysavings.ui.base.BaseDialog

class AddSavingDialogFragment : BaseDialog() {

    private var _binding: DialogAddSavingBinding? = null
    private val binding get() = _binding!!

    // 默认选择今天
    private var selectedStartTime: Long = System.currentTimeMillis()
    private val vm: SavingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddSavingBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBankSpinner()
        setupDatePicker()
        setupRealtimeCalculation()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { save() }

        displaySelectedDate()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            val displayMetrics = resources.displayMetrics
            params.width = (displayMetrics.widthPixels * 0.92).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params

            window.setGravity(Gravity.CENTER)
        }
    }

    private fun setupBankSpinner() {
        val banks = arrayOf("工商银行", "建设银行", "农业银行", "招商银行", "恒丰银行", "兴业银行", "邮政银行", "交通银行")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, banks)
        binding.spinnerBank.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.etStartDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择起始日期")
                .setSelection(selectedStartTime)
                .setTheme(R.style.CustomDatePickerTheme)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedStartTime = selection
                displaySelectedDate()
                calculatePreview()
            }
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }
    }

    private fun displaySelectedDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.etStartDate.setText(sdf.format(Date(selectedStartTime)))
    }

    private fun setupRealtimeCalculation() {
        binding.etAmount.doAfterTextChanged { calculatePreview() }
        binding.etRate.doAfterTextChanged { calculatePreview() }
        binding.etDuration.doAfterTextChanged { calculatePreview() }
    }

    @SuppressLint("SetTextI18n")
    private fun calculatePreview() {
        val amountStr = binding.etAmount.text?.toString() ?: ""
        val rateStr = binding.etRate.text?.toString() ?: ""
        val durationStr = binding.etDuration.text?.toString() ?: ""

        if (amountStr.isBlank() || rateStr.isBlank() || durationStr.isBlank()) {
            binding.tvPreviewTotal.text = "¥ 0.00"
            return
        }

        runCatching {
            val amount = amountStr.toBigDecimal().multiply(BigDecimal("10000"))
            val rate = rateStr.toBigDecimal().divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
            val years = durationStr.toBigDecimal()

            val interest = amount.multiply(rate).multiply(years)
            val total = amount.add(interest)

            binding.tvPreviewTotal.text = MoneyUtils.formatWithSymbol(total.setScale(2, RoundingMode.HALF_UP))
        }.onFailure {
            binding.tvPreviewTotal.text = "¥ 0.00"
        }
    }

    private fun save() {
        val amountStr = binding.etAmount.text.toString()
        val rateStr = binding.etRate.text.toString()
        val durationStr = binding.etDuration.text.toString()

        if (amountStr.isEmpty() || rateStr.isEmpty() || durationStr.isEmpty()) {
            // 这里可以加一个抖动动画或 Toast 提示
            return
        }

        val amount = amountStr.toBigDecimal().multiply(BigDecimal("10000"))
        val rate = rateStr.toBigDecimal()
        val years = durationStr.toInt()

        // 使用 Calendar 根据选中的开始时间精确计算到期日
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedStartTime
        calendar.add(Calendar.YEAR, years)
        val endTime = calendar.timeInMillis

        vm.insertSaving(
            SavingItem(
                bankName = binding.spinnerBank.selectedItem.toString(),
                amount = amount,
                interestRate = rate,
                year = years,
                interest = amount * rate * BigDecimal(years) / BigDecimal(100),
                startTime = selectedStartTime, // 使用选中的时间
                endTime = endTime
            )
        )
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}