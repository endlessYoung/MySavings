package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.endlessyoung.mysavings.data.local.entity.FundEntity
import com.endlessyoung.mysavings.databinding.DialogAddFundBinding
import com.endlessyoung.mysavings.ui.base.BaseDialog
import java.math.BigDecimal

class AddFundDialogFragment : BaseDialog() {

    private var _binding: DialogAddFundBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddFundBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            // 1. 获取输入数据
            val ownerName = binding.etOwnerName.text.toString().trim()
            val balanceStr = binding.etFundBalance.text.toString()
            val monthlyStr = binding.etMonthlyPay.text.toString()

            if (balanceStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入当前余额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val balance = balanceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val monthly = monthlyStr.toBigDecimalOrNull() ?: BigDecimal.ZERO

            // 2. 构建 FundEntity 实体
            val fundEntity = FundEntity(
                ownerName = ownerName,
                totalBalance = balance,
                monthlyAmount = monthly,
                lastUpdateTime = System.currentTimeMillis()
            )

            // 3. 调用 ViewModel 写入数据库
            viewModel.updateFund(fundEntity)

            Toast.makeText(requireContext(), "${ownerName}的公积金数据已同步", Toast.LENGTH_SHORT)
                .show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}