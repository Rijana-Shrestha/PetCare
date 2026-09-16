package com.rijana.petcare.util

import android.annotation.SuppressLint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import com.rijana.petcare.R

@SuppressLint("ClickableViewAccessibility")
fun EditText.setupPasswordToggle() {
    var isPasswordVisible = false

    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            // Check if touch target hit the right end drawable (eye icon)
            val drawableEnd = compoundDrawables[2]
            if (drawableEnd != null && event.rawX >= (right - compoundDrawables[2].bounds.width() - paddingEnd)) {
                isPasswordVisible = !isPasswordVisible

                if (isPasswordVisible) {
                    transformationMethod = HideReturnsTransformationMethod.getInstance()
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_visible, 0)
                } else {
                    transformationMethod = PasswordTransformationMethod.getInstance()
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_hidden, 0)
                }

                // Keep focus and retain cursor at end of text
                setSelection(text?.length ?: 0)
                return@setOnTouchListener true
            }
        }
        false
    }
}