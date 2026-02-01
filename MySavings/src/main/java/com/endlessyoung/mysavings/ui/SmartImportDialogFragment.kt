package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.endlessyoung.mysavings.databinding.DialogSmartImportBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.view.isVisible

class SmartImportDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSmartImportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSmartImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnScanImage.setOnClickListener {
            // TODO: Implement image picker
            Toast.makeText(requireContext(), "功能开发中：图片识别", Toast.LENGTH_SHORT).show()
        }

        binding.btnPasteText.setOnClickListener {
            toggleTextInput()
        }

        binding.btnAnalyze.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotEmpty()) {
                // TODO: Implement text analysis
                Toast.makeText(requireContext(), "正在解析文本: $text", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleTextInput() {
        if (binding.etInputText.isVisible) {
            binding.etInputText.visibility = View.GONE
            binding.btnAnalyze.visibility = View.GONE
        } else {
            binding.etInputText.visibility = View.VISIBLE
            binding.btnAnalyze.visibility = View.VISIBLE
            binding.etInputText.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}