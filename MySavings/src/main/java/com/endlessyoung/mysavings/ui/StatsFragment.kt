package com.endlessyoung.mysavings.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.databinding.FragmentStatsBinding
import com.endlessyoung.mysavings.ui.utils.AppColors
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import androidx.core.graphics.toColorInt
import androidx.navigation.fragment.findNavController
import com.endlessyoung.mysavings.log.MySavingsLog
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class StatsFragment : Fragment() {
    companion object {
        private const val TAG = "StatsFragment"
    }
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val sharedVm: SavingViewModel by activityViewModels()

    // 柱状图订阅管理
    private var barChartJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initChartStyles()
        setupBarChartInteraction()
        setupBarChartClick()

        // 观察饼图数据：储蓄分布
        viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.bankDistribution.collect { distribution ->
                if (distribution.isNotEmpty()) {
                    updatePieChart(distribution)
                }
            }
        }

        // 默认加载年度存款统计
        observeYearlyData()
    }

    private fun initChartStyles() {
        listOf(binding.pieChart, binding.barChart).forEach {
            it.description.isEnabled = false
            it.setNoDataText("暂无数据，快去记一笔吧")
            it.setNoDataTextColor(AppColors.TEXT_SECONDARY.toColorInt())
        }
    }

    private fun setupBarChartInteraction() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnYear -> {
                        binding.yearSpinner.visibility = View.GONE
                        observeYearlyData()
                    }
                    R.id.btnMonth -> {
                        binding.yearSpinner.visibility = View.VISIBLE
                        // 切换到月度模式时，立即开始观察
                        observeMonthlyData()
                    }
                }
            }
        }

        // 观察可用年份填充 Spinner
        viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.availableYears.collect { years ->
                if (years.isNotEmpty()) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        years.map { "${it}年" }
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.yearSpinner.adapter = adapter
                }
            }
        }

        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = sharedVm.availableYears.value[position]

                MySavingsLog.d(TAG, "selectedYear: $selectedYear")

                sharedVm.setFilterYear(selectedYear)

                if (binding.toggleGroup.checkedButtonId == R.id.btnMonth) {
                    observeMonthlyData()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBarChartClick() {
        binding.barChart.setOnTouchListener { v, event ->
            // 1. 只在手指抬起 (ACTION_UP) 时触发，防止滑动误触
            if (event.action == MotionEvent.ACTION_UP) {

                // 2. 获取点击坐标对应的 Highlight 对象
                // getHighlightByTouchPoint 会计算点击位置是否有具体数据
                val high = binding.barChart.getHighlightByTouchPoint(event.x, event.y)

                if (high != null) {
                    // 3. 精确判定点击范围：
                    // 默认情况下只要在 X 轴范围内就会触发。
                    // 如果你要求“必须点到着色部分”，可以判断点击的 Y 值是否小于等于柱子的高度值。
                    val entry = binding.barChart.data.getEntryForHighlight(high) as? BarEntry

                    // 获取点击位置相对于图表的 Y 轴数值
                    val touchY = binding.barChart.getTransformer(YAxis.AxisDependency.LEFT)
                        .getValuesByTouchPoint(event.x, event.y).y.toFloat()

                    // 只有当点击高度 <= 柱子实际高度时才跳转
                    if (entry != null && touchY <= entry.y * 1.1f) {
                        performDrillDown(entry)
                    }
                }
            }
            // 返回 false 以便图表继续处理缩放或滑动等其他手势
            false
        }
    }

    private fun performDrillDown(e: BarEntry) {
        val label = e.data.toString().toInt()
        val isYearly = binding.toggleGroup.checkedButtonId == R.id.btnYear

        val targetYear: Int
        val targetMonth: Int

        if (isYearly) {
            targetYear = label
            targetMonth = -1
        } else {
            targetYear = sharedVm.filterYear.value
            targetMonth = label
        }

        val bundle = Bundle().apply {
            putInt("year", targetYear)
            putInt("month", targetMonth)
        }
        findNavController().navigate(R.id.action_stats_to_recordList, bundle)
    }

    private fun observeYearlyData() {
        barChartJob?.cancel()
        barChartJob = viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.yearlySavings.collect { data ->
                updateBarChart(data, isYearly = true)
            }
        }
    }

    private fun observeMonthlyData() {
        barChartJob?.cancel()
        barChartJob = viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.monthlySavingsByYear.collect { data ->
                MySavingsLog.d(TAG, "monthlySavingsByYear: $data")
                updateBarChart(data, isYearly = false)
            }
        }
    }

    private fun updatePieChart(distribution: Map<String, BigDecimal>) {
        val entries = distribution.map { (bankName, totalAmount) ->
            PieEntry(totalAmount.toFloat(), bankName, totalAmount)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = AppColors.getChartColorPalette()
            sliceSpace = 3f
            selectionShift = 8f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = Color.parseColor(AppColors.TEXT_MAIN)
            valueLinePart1OffsetPercentage = 85f
            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.4f
            valueTextColor = Color.parseColor(AppColors.TEXT_MAIN)
        }

        binding.pieChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                        val amount = pieEntry?.data as? BigDecimal ?: BigDecimal.ZERO
                        return "${MoneyUtils.formatWithSymbol(amount)} (${String.format("%.1f", value)}%)"
                    }
                })
                setValueTextSize(11f)
                setValueTextColor(Color.parseColor(AppColors.TEXT_MAIN))
            }

            setUsePercentValues(true)
            setExtraOffsets(35f, 10f, 35f, 10f)
            holeRadius = 55f
            transparentCircleRadius = 60f
            centerText = "资产分布"
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor(AppColors.TEXT_MAIN))
            setEntryLabelColor(Color.parseColor(AppColors.TEXT_MAIN))
            setEntryLabelTextSize(11f)

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = Color.parseColor(AppColors.TEXT_MAIN)
                yOffset = 20f
                isWordWrapEnabled = true
            }
            animateY(1200, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateBarChart(data: Map<out Any, BigDecimal>, isYearly: Boolean) {
        val entries = data.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat(), entry.key)
        }

        val dataSet = BarDataSet(entries, if (isYearly) "年度存款 单位（万）" else "月度存款 单位（万）").apply {
            color = AppColors.PRIMARY_BLUE.toColorInt()
            valueTextColor = AppColors.TEXT_MAIN.toColorInt()
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return MoneyUtils.formatWithNumber(BigDecimal.valueOf(value.toDouble()))
                }
            }
        }

        binding.barChart.apply {
            this.data = BarData(dataSet).apply {
                barWidth = 0.5f
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = AppColors.TEXT_SECONDARY.toColorInt()
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        if (index < 0 || index >= entries.size) return ""
                        val key = entries[index].data.toString()
                        return if (isYearly) "${key}年" else "${key}月"
                    }
                }
            }

            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}