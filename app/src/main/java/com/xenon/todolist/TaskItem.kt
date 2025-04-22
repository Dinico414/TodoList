package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.viewmodel.LiveListItem
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TaskItem(
    override var id: Int,
    var name: String,
    var desc: String,
    var dueTime: Long = -1,
    var dueDate: Long = -1,
    var createdDate: Long,
    var completedDate: Long = -1,
    val children: ArrayList<TaskItem> = ArrayList()

) : LiveListItem {

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
        if (isCompleted()) checkedColor(context) else uncheckedColor(context)

    private fun checkedColor(context: Context) = ContextCompat.getColor(context, R.color.primary)
    private fun uncheckedColor(context: Context) = ContextCompat.getColor(context, R.color.checkbox)

    val dueDateTime: Long
        get() = dueTime + dueDate
}