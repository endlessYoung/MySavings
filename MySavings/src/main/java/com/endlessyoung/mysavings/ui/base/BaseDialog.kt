package com.endlessyoung.mysavings.ui.base

import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

abstract class BaseDialog : DialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            val displayMetrics = resources.displayMetrics

            params.width = (displayMetrics.widthPixels * 0.92).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params

            window.setGravity(Gravity.CENTER)

            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    protected fun closeDialog() {
        dismiss()
    }
}