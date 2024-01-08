package com.xenon.todolist.activities

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlin.math.max

open class BaseActivity : AppCompatActivity() {
    fun adjustBottomMargin(layoutMain: View) {
        adjustBottomMargin(layoutMain, null)
    }

    fun adjustBottomMargin(layoutMain: View, floatingButton: ExtendedFloatingActionButton?) {
        val layoutParams = layoutMain.layoutParams as MarginLayoutParams
        val desiredMargin = layoutParams.leftMargin

        val navigationBarHeight = getNavigationBarHeight()
        val targetMargin = max(0, desiredMargin - navigationBarHeight)

        layoutParams.bottomMargin = targetMargin
        layoutMain.layoutParams = layoutParams

        if (floatingButton != null) {
            setNewTaskButtonMargin(floatingButton, 2 * targetMargin - 1)
        }
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun setNewTaskButtonMargin(button: ExtendedFloatingActionButton, margin: Int) {
        val layoutParams = button.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.bottomMargin = margin
        button.layoutParams = layoutParams
    }

    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}