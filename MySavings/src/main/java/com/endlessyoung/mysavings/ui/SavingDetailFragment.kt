package com.endlessyoung.mysavings.ui

import androidx.fragment.app.Fragment
import java.math.BigDecimal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.databinding.FragmentSavingDetailBinding
import com.endlessyoung.mysavings.domain.model.SavingItem
import java.text.SimpleDateFormat
import java.util.*

class SavingDetailFragment : Fragment() {

    private var _binding: FragmentSavingDetailBinding? = null
    private val binding get() = _binding!!

    private var savingItem: SavingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savingItem = arguments?.getParcelable("savingItem", SavingItem::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savingItem?.let { bindData(it) }
    }

    private fun bindData(item: SavingItem) {
        // 金额
        binding.tvDetailAmount.text = MoneyUtils.formatWithSymbol(item.amount)

        // 年化利率
        binding.tvDetailRate.text = "${item.interestRate}%"

        // 存期
        binding.tvDetailDuration.text = "${item.year} 年期"

        // 日均利息
        val dailyIncome = item.interest.divide(item.year.toBigDecimal() * 365.toBigDecimal(), 2, BigDecimal.ROUND_HALF_UP)
        binding.tvDailyIncome.text = MoneyUtils.formatWithSymbol(dailyIncome)

        // 到期利息
        binding.tvTotalInterest.text = MoneyUtils.formatWithSymbol(item.interest)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.tvStartTime.text = sdf.format(Date(item.startTime))
        binding.tvEndTime.text = sdf.format(Date(item.endTime))

        val now = System.currentTimeMillis()
        val progress = ((now - item.startTime).toFloat() / (item.endTime - item.startTime) * 100).toInt()
        binding.detailProgress.progress = progress.coerceIn(0, 100)

//        binding.tvMemo.text = item.memo ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
