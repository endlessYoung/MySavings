package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.databinding.FragmentRecordListBinding
import com.endlessyoung.mysavings.databinding.FragmentRecordListItemBinding
import com.endlessyoung.mysavings.databinding.ItemRecordHeaderBinding
import com.endlessyoung.mysavings.domain.model.SavingItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TYPE_HEADER = 0
private const val TYPE_ITEM = 1

class RecordListFragment : Fragment() {
    private var _binding: FragmentRecordListBinding? = null
    private val binding get() = _binding!!

    private val sharedVm: SavingViewModel by activityViewModels()
    private val recordAdapter by lazy { RecordAdapter() }

    private var year: Int = 0
    private var month: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            year = it.getInt("year")
            month = it.getInt("month")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置动态标题
        binding.recordListTitle.text = if (month == -1) "${year}年 存款明细" else "${year}年${month}月 存款明细"

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }

        // 观察数据
        viewLifecycleOwner.lifecycleScope.launch {
            sharedVm.getGroupedSavings(year, month).collectLatest { groupedMap ->
                if (groupedMap.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    recordAdapter.updateData(groupedMap)
                }
            }
        }
    }

    // 2. 移除 inner class 里的 companion object
    private inner class RecordAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val displayList = mutableListOf<Any>()

        fun updateData(groupedMap: Map<String, List<SavingItem>>) {
            displayList.clear()
            groupedMap.forEach { (label, items) ->
                displayList.add(label)
                displayList.addAll(items)
            }
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (displayList[position] is String) TYPE_HEADER else TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(ItemRecordHeaderBinding.inflate(inflater, parent, false))
            } else {
                ItemViewHolder(FragmentRecordListItemBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val data = displayList[position]
            when (holder) {
                is HeaderViewHolder -> {
                    holder.binding.tvHeaderLabel.text = data as String
                }
                is ItemViewHolder -> {
                    val item = data as SavingItem
                    holder.binding.apply {
                        itemBank.text = item.bankName
                        itemAmount.text = MoneyUtils.formatWithSymbol(item.amount)
                        itemRate.text = "年化 ${item.interestRate}%"
                        itemInterest.text = "预计收益 ${MoneyUtils.formatWithSymbol(item.interest)}"
                    }
                }
            }
        }

        override fun getItemCount(): Int = displayList.size

        inner class HeaderViewHolder(val binding: ItemRecordHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        inner class ItemViewHolder(val binding: FragmentRecordListItemBinding) : RecyclerView.ViewHolder(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}