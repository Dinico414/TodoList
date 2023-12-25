package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.xenon.commons.accesspoint.R
import kotlinx.serialization.Serializable

@Serializable
data class TaskItem(
    var id: Int,
    var name: String,
    var desc: String,
    var dueTime: Long,
    var completedDate: Long,
) {

    fun isCompleted() = completedDate >= 0
    private fun setCompleted(b: Boolean) {
        this.completedDate = if (b) System.currentTimeMillis() else -1
    }

    fun toggleCompleted() {
        setCompleted(!isCompleted())
    }

    fun imageResource(): Int =
        if (isCompleted()) com.xenon.todolist.R.drawable.checked else com.xenon.todolist.R.drawable.unchecked

    fun imageColor(context: Context): Int =
        if (isCompleted()) checked(context) else unchecked(context)

    private fun checked(context: Context) = ContextCompat.getColor(context, R.color.primary)
    private fun unchecked(context: Context): Int {
        val originalColor = ContextCompat.getColor(context, R.color.checkbox)

        return ColorUtils.setAlphaComponent(originalColor, (255 * 0.5f).toInt())
    }
}