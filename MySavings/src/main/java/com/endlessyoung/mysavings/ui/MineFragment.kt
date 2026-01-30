package com.endlessyoung.mysavings.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.endlessyoung.mysavings.databinding.FragmentMineBinding
import com.endlessyoung.mysavings.ui.utils.SettingsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MineFragment : Fragment() {
    companion object {
        private const val TAG = "MineFragment"
    }

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavingViewModel by activityViewModels()
    private val reminderAdapter = ReminderAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews()
        initListeners()
        observeData()
    }

    private fun initViews() {
        // Init RecyclerView
        binding.rvReminders.adapter = reminderAdapter
        
        // Load Settings
        val context = requireContext()
        
        // Theme
        binding.tvThemeModeSummary.text = SettingsManager.getThemeModeLabel(context)
        
        // Default Sort
        binding.tvDefaultSortSummary.text = SettingsManager.getDefaultSortLabel(context)
        
        // Hide Amount Switch
        binding.switchHideAmountOnStart.isChecked = SettingsManager.isAmountHiddenOnStart(context)
        
        // App Lock Switch
        binding.switchAppLock.isChecked = SettingsManager.isAppLockEnabled(context)
        
        // Version
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            binding.tvVersionName.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersionName.text = "v1.0.0"
        }
    }

    private fun initListeners() {
        val context = requireContext()
        
        // Theme Mode
        binding.rowThemeMode.setOnClickListener {
            val modes = arrayOf("跟随系统", "浅色模式", "深色模式")
            val currentMode = SettingsManager.getThemeMode(context)
            val checkedItem = when (currentMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> 1
                AppCompatDelegate.MODE_NIGHT_YES -> 2
                else -> 0
            }

            AlertDialog.Builder(context)
                .setTitle("选择主题模式")
                .setSingleChoiceItems(modes, checkedItem) { dialog, which ->
                    val selectedMode = when (which) {
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    SettingsManager.setThemeMode(context, selectedMode)
                    binding.tvThemeModeSummary.text = modes[which]
                    dialog.dismiss()
                    
                    // 重启 Activity 以应用主题 (虽然 setDefaultNightMode 通常会自动处理，但有时需要重建)
                    // activity?.recreate() // 可选，通常 AppCompatDelegate 会自动处理
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // Default Sort
        binding.rowDefaultSort.setOnClickListener {
            val options = arrayOf("按时间", "按金额")
            val currentSort = SettingsManager.getDefaultSort(context)
            
            AlertDialog.Builder(context)
                .setTitle("默认排序方式")
                .setSingleChoiceItems(options, currentSort) { dialog, which ->
                    SettingsManager.setDefaultSort(context, which)
                    binding.tvDefaultSortSummary.text = options[which]
                    
                    // Update ViewModel sort mode immediately if needed
                    val sortMode = if (which == SettingsManager.SORT_BY_AMOUNT) 
                        SavingSortMode.AMOUNT_DESC else SavingSortMode.DEFAULT
                    viewModel.setSortMode(sortMode)
                    
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // Hide Amount
        binding.switchHideAmountOnStart.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAmountHiddenOnStart(context, isChecked)
        }

        // App Lock
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAppLockEnabled(context, isChecked)
            if (isChecked) {
                Toast.makeText(context, "已开启应用锁，下次启动需验证", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Data Backup
        binding.rowExportData.setOnClickListener {
            Toast.makeText(context, "数据导出功能开发中...", Toast.LENGTH_SHORT).show()
        }
        
        binding.rowImportData.setOnClickListener {
            Toast.makeText(context, "数据导入功能开发中...", Toast.LENGTH_SHORT).show()
        }

        // Feedback
        binding.rowFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("developer@example.com"))
                putExtra(Intent.EXTRA_SUBJECT, "MySavings 反馈")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "未找到邮件应用", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Version
        binding.rowVersion.setOnClickListener {
            // Egg or check updates
        }
        
        // View All Reminders
        binding.btnViewAllReminders.setOnClickListener {
             // Navigate to Plan list or show all
             // For now just show toast or navigate to Home -> Plan tab if possible
             Toast.makeText(context, "请在主页查看所有计划", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        // Savings Stats
        lifecycleScope.launch {
            viewModel.savings.collectLatest { list ->
                binding.tvTotalRecords.text = list.size.toString()
                
                if (list.isNotEmpty()) {
                    val firstTime = list.minOf { it.startTime }
                    val now = System.currentTimeMillis()
                    val diff = now - firstTime
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                    binding.tvDaysUsed.text = days.toString()
                } else {
                    binding.tvDaysUsed.text = "0"
                }
            }
        }
        
        // Plan Stats and Reminders
        lifecycleScope.launch {
            viewModel.allPlans.collectLatest { plans ->
                val planList = plans ?: emptyList()
                binding.tvTotalPlans.text = planList.size.toString()
                
                // Show upcoming plans in reminder list (e.g. top 5 upcoming)
                val upcoming = planList.filter { it.planDate >= System.currentTimeMillis() }
                    .sortedBy { it.planDate }
                    .take(5)
                
                reminderAdapter.submitList(upcoming)
                
                // Show/Hide recycler view based on data
                if (upcoming.isEmpty()) {
                    binding.rvReminders.visibility = View.GONE
                    binding.btnViewAllReminders.visibility = View.GONE
                    // Optional: Show empty state text
                } else {
                    binding.rvReminders.visibility = View.VISIBLE
                    binding.btnViewAllReminders.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
