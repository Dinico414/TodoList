package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import com.xenon.commons.accesspoint.R
import kotlinx.serialization.Serializable

@Serializable
data class TaskItem(
    var id: Int,
    var name: String,
    var desc: String,
    var dueTime: Long,
    var createdDate: Long,
    var completedDate: Long,
    val children: ArrayList<TaskItem>
) {

    fun isCompleted() = completedDate >= 0
    private fun setCompleted(b: Boolean) {
        this.completedDate = if (b) System.currentTimeMillis() else -1
    }

    fun toggleCompleted() {
        setCompleted(!isCompleted())
    }

    fun imageResource(): Int =
        if (isCompleted()) R.drawable.checked else R.drawable.unchecked

    fun imageColor(context: Context): Int =
        if (isCompleted()) checked(context) else unchecked(context)

    private fun checked(context: Context) = ContextCompat.getColor(context, R.color.primary)
    private fun unchecked(context: Context) = ContextCompat.getColor(context, R.color.checkbox)

}