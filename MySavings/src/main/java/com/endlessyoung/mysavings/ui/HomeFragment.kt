package com.endlessyoung.mysavings.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.CalendarContract
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.databinding.DialogActionMenuBinding
import com.endlessyoung.mysavings.databinding.FragmentHomeBinding
import com.endlessyoung.mysavings.log.MySavingsLog
import com.endlessyoung.mysavings.ui.utils.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 使用 activityViewModels 保证与 DialogFragment 共享同一个 ViewModel 实例
    private val sharedVm: SavingViewModel by activityViewModels()

    private lateinit var adapter: SavingAdapter

    private var isAmountVisible = true

    private var isPlanExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        
        // Load initial visibility preference
        val hideOnStart = SettingsManager.isAmountHiddenOnStart(requireContext())
        isAmountVisible = !hideOnStart

        setupRecyclerView()
        setupListeners()
        observeData()
        attachSwipeToDelete()
        initPlanDashboard()
    }

    private fun setupRecyclerView() {
        // 初始化适配器，点击列表项跳转详情
        adapter = SavingAdapter { item ->
            findNavController().navigate(R.id.action_homeFragment_to_SavingDetailFragment)
        }

        binding.savingsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
            setHasFixedSize(true)

            // 滚动监听：向上滑动收缩 FAB，向下滑动展开 FAB
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        binding.fab.shrink()
                    } else {
                        binding.fab.extend()
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        // 资产显示/隐藏切换
        binding.ivToggleVisible.setOnClickListener {
            toggleAmountVisibility()
        }

        // 悬浮按钮弹出操作菜单
        binding.fab.setOnClickListener {
            showActionMenu()
        }

        // 顶部 toolbar（在 Activity 里）上的搜索和排序，只在 HomeFragment 里生效
        val activity = requireActivity()
        val spSort = activity.findViewById<Spinner>(R.id.spSort)
        val etSearch = activity.findViewById<EditText>(R.id.etSearch)
//        val btnSearch = activity.findViewById<ImageView>(R.id.btnSearch)

        // 设置 Spinner 选项
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_mode_labels,
            R.layout.item_spinner_selected
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
            spSort.adapter = adapter
        }

        // 根据当前排序模式设置默认选择
        spSort.setSelection(
            when (sharedVm.sortMode.value) {
                SavingSortMode.DEFAULT -> 0
                SavingSortMode.AMOUNT_DESC -> 1
                SavingSortMode.RATE_DESC -> 2
            },
            false
        )

        spSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val mode = when (position) {
                    0 -> SavingSortMode.DEFAULT
                    1 -> SavingSortMode.AMOUNT_DESC
                    2 -> SavingSortMode.RATE_DESC
                    else -> SavingSortMode.DEFAULT
                }
                if (sharedVm.sortMode.value != mode) {
                    sharedVm.setSortMode(mode)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        // 搜索输入实时过滤
        etSearch.addTextChangedListener { text ->
            sharedVm.setSearchQuery(text?.toString().orEmpty())
        }

//        btnSearch.setOnClickListener {
//            sharedVm.setSearchQuery(etSearch.text?.toString().orEmpty())
//        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. 订阅资产核心数据流：总资产、存款合计、公积金合计
                launch {
                    combine(
                        sharedVm.totalAssets,       // 由 GetTotalAssetsUseCase 提供
                        sharedVm.totalSavingAmount, // 仅银行存款
                        sharedVm.totalFundAmount    // 仅公积金
                    ) { total, saving, fund ->
                        Triple(total, saving, fund)
                    }.collect { (total, saving, fund) ->
                        updateFinancialCards(total, saving, fund)
                    }
                }

                // 2. 订阅存款列表数据（带搜索和排序）
                launch {
                    sharedVm.filteredSavings.collect { list ->
                        MySavingsLog.d("HomeFragment", "更新列表数据，条数: ${list.size}")
                        adapter.submitList(list)
                        // 更新资产明细的统计数量
                        binding.tvItemCount.text = "共${list.size}项"
                    }
                }

                // 3. 支出计划
                setupCollapsibleDashboard()
            }
        }
    }

    private fun setupCollapsibleDashboard() {
        val planAdapter = ExpandedPlanAdapter { plan ->
            // 点击具体计划同步日历
            syncToCalendar(plan)
        }
        binding.rvExpandedPlans.adapter = planAdapter
        binding.rvExpandedPlans.layoutManager = LinearLayoutManager(requireContext())

        // 观察数据并计算汇总
        viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.allPlans.collect { plans ->
                val total = plans?.sumOf { it.amount }
                binding.tvPlanSummary.text =
                    "本月计划支出: ${MoneyUtils.formatWithSymbol(total)} (${plans?.size}笔)"
                // 更新计划数量角标
                val count = plans?.size ?: 0
                binding.tvPlanCount.text = "${count}项"
                
                planAdapter.submitList(plans)

                // 无数据自动隐藏整个卡片
                binding.planDashboardCard.visibility =
                    if (plans?.isEmpty() == true) View.GONE else View.VISIBLE
            }
        }

        // 切换折叠逻辑
        binding.layoutPlanHeader.setOnClickListener {
            isPlanExpanded = !isPlanExpanded

            binding.layoutPlanDetails.visibility = if (isPlanExpanded) View.VISIBLE else View.GONE

            TransitionManager.beginDelayedTransition(binding.homeRoot)

            binding.ivExpandArrow.animate()
                .rotation(if (isPlanExpanded) 180f else 0f)
                .setDuration(300)
                .start()
        }
    }

    private fun initPlanDashboard() {
        val planAdapter = ExpandedPlanAdapter { plan -> syncToCalendar(plan) }

        binding.rvExpandedPlans.apply {
            adapter = planAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // 添加内置分割线
            addItemDecoration(
                DrawableDividerDecoration(
                    requireContext(),
                    R.drawable.divider_fancy
                )
            )
        }
    }

    private fun syncToCalendar(plan: com.endlessyoung.mysavings.data.local.entity.PlanEntity) {
        try {
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "支出提醒: ${plan.title}")
                .putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "预计金额: ${MoneyUtils.formatWithSymbol(plan.amount)}"
                )
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, plan.planDate)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, plan.planDate + 3600000)

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法调起日历", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFinancialCards(total: BigDecimal, saving: BigDecimal, fund: BigDecimal) {
        if (isAmountVisible) {
            binding.tvNetWorthAmount.text = MoneyUtils.formatWithSymbol(total)
            binding.tvBankTotal.text = "存款: ${MoneyUtils.formatWithSymbol(saving)}"
            binding.tvFundTotal.text = "公积金: ${MoneyUtils.formatWithSymbol(fund)}"
            binding.ivToggleVisible.setImageResource(R.drawable.ic_visibility)
        } else {
            binding.tvNetWorthAmount.text = "******"
            binding.tvBankTotal.text = "存款: ******"
            binding.tvFundTotal.text = "公积金: ******"
            binding.ivToggleVisible.setImageResource(R.drawable.ic_visibility_off)
        }
    }


    private fun toggleAmountVisibility() {
        isAmountVisible = !isAmountVisible
        
        // 更新适配器中的隐私模式状态
        adapter.isPrivacyMode = !isAmountVisible
        adapter.notifyDataSetChanged()
        
        val planAdapter = binding.rvExpandedPlans.adapter as? ExpandedPlanAdapter
        planAdapter?.let {
            it.isPrivacyMode = !isAmountVisible
            it.notifyDataSetChanged()
        }

        binding.allPropertiesCard.animate()
            .alpha(0.6f)
            .setDuration(120)
            .withEndAction {
                // 动画执行一半时更新文本
                updateFinancialCards(
                    sharedVm.totalAssets.value,
                    sharedVm.totalSavingAmount.value,
                    sharedVm.totalFundAmount.value
                )
                binding.allPropertiesCard.animate().alpha(1f).setDuration(120).start()
            }.start()
    }

    private fun showActionMenu() {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogActionMenuBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.apply {
            // 导航至：新增存款
            btnNavAddSaving.setOnClickListener {
                bottomSheet.dismiss()
                findNavController().navigate(R.id.action_homeFragment_to_AddSavingDialogFragment)
            }
            // 导航至：更新公积金
            btnNavUpdateFund.setOnClickListener {
                bottomSheet.dismiss()
                findNavController().navigate(R.id.action_homeFragment_to_AddFundDialogFragment)
            }
            // 导航至：新增支出计划
            btnNavAddPlan.setOnClickListener {
                bottomSheet.dismiss()
                findNavController().navigate(R.id.action_homeFragment_to_AddPlanDialogFragment)
            }
            // 导航至：智能录入
            btnNavSmartImport.setOnClickListener {
                bottomSheet.dismiss()
                findNavController().navigate(R.id.action_homeFragment_to_SmartImportDialogFragment)
            }
        }
        bottomSheet.show()
    }

    private fun attachSwipeToDelete() {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
        // 更柔和的 Gmail 风格红色背景
        val background = Color.parseColor("#F44336").toDrawable()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f * resources.displayMetrics.scaledDensity
        }

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                r: RecyclerView,
                v: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = adapter.currentList[pos]
                    sharedVm.deleteSaving(item)
                    Toast.makeText(
                        requireContext(),
                        "已移除 ${item.bankName} 记录",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onChildDraw(
                c: Canvas, r: RecyclerView, v: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = v.itemView

                if (dX < 0) {
                    // 向左滑动时，计算滑动进度，控制背景和文字透明度
                    val rawProgress = -dX / itemView.width
                    val swipeProgress = minOf(rawProgress * 1.8f, 1f) // 提前拉满

                    // 让一开始就更鲜艳：设置一个最低透明度
                    val minAlpha = 80
                    val alpha = (minAlpha + (255 - minAlpha) * swipeProgress)
                        .toInt()
                        .coerceIn(minAlpha, 255)

                    background.alpha = alpha
                    background.setBounds(
                        itemView.right + dX.toInt(), itemView.top,
                        itemView.right, itemView.bottom
                    )
                    background.draw(c)

                    deleteIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val top = itemView.top + iconMargin
                        val left = itemView.right - iconMargin - it.intrinsicWidth
                        val right = itemView.right - iconMargin
                        val bottom = top + it.intrinsicHeight
                        it.setBounds(left, top, right, bottom)
                        it.alpha = alpha
                        it.draw(c)

                        // 绘制“删除”文字，靠近图标，居中对齐
                        val label = "删除"
                        textPaint.alpha = alpha
                        val textWidth = textPaint.measureText(label)
                        val marginPx = (8 * itemView.resources.displayMetrics.density)
                        val textX = left - marginPx - textWidth
                        val textY =
                            itemView.top + itemView.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                        c.drawText(label, textX, textY, textPaint)
                    }
                }

                // 保持 item 自身的位移效果，让滑动手感接近 Gmail
                super.onChildDraw(c, r, v, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.savingsList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}