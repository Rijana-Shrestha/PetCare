package com.rijana.petcare.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.rijana.petcare.R

object SuccessToast {
    fun show(context: Context, message: String) {
        val view = LayoutInflater.from(context).inflate(R.layout.toast_success, null)
        view.findViewById<TextView>(R.id.tvToastMessage).text = message
        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            @Suppress("DEPRECATION")
            this.view = view
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 180)
            show()
        }
    }
}