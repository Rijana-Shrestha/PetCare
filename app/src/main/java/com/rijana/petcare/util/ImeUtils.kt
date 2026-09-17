package com.rijana.petcare.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applyImeBottomPadding(maxPaddingDp: Int = 250) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val maxPaddingPx = (maxPaddingDp * v.resources.displayMetrics.density).toInt()
        val padding = imeInsets.bottom.coerceAtMost(maxPaddingPx)
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, padding)
        insets
    }
}