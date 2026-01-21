package com.endlessyoung.mysavings.ui

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.endlessyoung.mysavings.databinding.FragmentHomeBinding
import com.endlessyoung.mysavings.databinding.FragmentMineBinding

class MineFragment: Fragment() {
    companion object {
        private const val TAG = "MineFragment"
    }

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }


}