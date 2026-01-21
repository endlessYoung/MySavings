package com.endlessyoung.mysavings.ui

import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.endlessyoung.mysavings.MoneyUtils
import com.endlessyoung.mysavings.R
import com.endlessyoung.mysavings.databinding.FragmentHomeBinding
import com.endlessyoung.mysavings.log.MySavingsLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var isAmountVisible = false
    private var realAmountText = ""

    private val sharedVm: SavingViewModel by activityViewModels()

    private lateinit var adapter: SavingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.savingsList.layoutManager = LinearLayoutManager(requireContext())
        adapter = SavingAdapter { item ->
            findNavController().navigate(R.id.action_homeFragment_to_SavingDetailFragment)
        }
        binding.savingsList.adapter = adapter
        binding.savingsList.setHasFixedSize(true)

        binding.tvBankTotal.text = "****"
        binding.ivToggleVisible.setOnClickListener {
            toggleAmount()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVm.savings.collect { list ->
                    MySavingsLog.d(TAG, "savings: $list, size: ${list.size}")
                    adapter.submitList(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.fab.setOnClickListener {
                    findNavController().navigate(R.id.action_homeFragment_to_AddSavingDialogFragment)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVm.totalAmount.collect { total ->
                    realAmountText = MoneyUtils.formatWithSymbol(total)

                    if (isAmountVisible) {
                        binding.tvBankTotal.text = realAmountText
                    }
                }
            }
        }

        binding.savingsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    binding.fab.shrink()
                } else {
                    binding.fab.extend()
                }
            }
        })

        attachSwipeToDelete()
    }

    private fun attachSwipeToDelete(){
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
        val background = Color.RED.toDrawable()

        val callback = object : ItemTouchHelper.SimpleCallback (
            0,
            ItemTouchHelper.LEFT
        ){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val pos = viewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = adapter.currentList[pos]
                    sharedVm.delete(item)

                    Toast.makeText(requireContext(), "已删除 ${item.bankName}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                deleteIcon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val top = itemView.top + iconMargin
                    val left = itemView.right - iconMargin - it.intrinsicWidth
                    val right = itemView.right - iconMargin
                    val bottom = top + it.intrinsicHeight

                    it.setBounds(left, top, right, bottom)
                    it.draw(c)
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.savingsList)
    }

    private fun toggleAmount() {
        isAmountVisible = !isAmountVisible

        binding.tvBankTotal.animate()
            .scaleX(0.8f).scaleY(0.8f).alpha(0f)
            .setDuration(120)
            .withEndAction {
                if (isAmountVisible) {
                    binding.tvBankTotal.text = realAmountText
                    binding.ivToggleVisible.setImageResource(R.drawable.ic_show)
                } else {
                    binding.tvBankTotal.text = "****"
                    binding.ivToggleVisible.setImageResource(R.drawable.ic_hide)
                }

                binding.tvBankTotal.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}