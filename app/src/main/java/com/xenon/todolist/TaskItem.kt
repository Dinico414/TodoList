package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.serialization.Serializable

@Serializable
data class TaskItem(
    var name: String,
    var desc: String,
    var dueTime: Long,
    var completedDate: Long,
) {
    var idx: Int = -1

    fun isCompleted() = completedDate >= 0
    private fun setCompleted(b: Boolean) {
        this.completedDate = if (b) System.currentTimeMillis() else -1
    }

    fun toggleCompleted() {
        setCompleted(!isCompleted())
    }

    fun imageResource(): Int = if (isCompleted()) R.drawable.checked else R.drawable.unchecked
    fun imageColor(context: Context): Int = if (isCompleted()) checked(context) else unchecked(context)

    private fun checked(context: Context) = ContextCompat.getColor(context, R.color.primary_attr)
    private fun unchecked(context: Context) = ContextCompat.getColor(context, R.color.color_on_surface_0_5)
}