package com.xenon.todolist.activities

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

open class BaseActivity : AppCompatActivity() {

    fun adjustBottomMargin(layoutMain: View?) {
        adjustBottomMargin(layoutMain, null)
    }

    fun adjustBottomMargin(layoutMain: View?, floatingButton: ExtendedFloatingActionButton?) {
        if (layoutMain == null)
            return
        val layoutParams = layoutMain.layoutParams as MarginLayoutParams
        val navigationBarHeight = getNavigationBarHeight()

        val targetMargin = if (navigationBarHeight == 0) {
            15.dpToPx()
        } else {
            0
        }

        layoutParams.bottomMargin = targetMargin
        layoutMain.layoutParams = layoutParams

        if (floatingButton != null) {
            val fabMargin = if (navigationBarHeight == 0) {
                14.dpToPx()
            } else {
                14.dpToPx()
            }
            setNewTaskButtonMargin(floatingButton, fabMargin)
        }
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0 // Return 0 as default when navigation bar height is not found
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
