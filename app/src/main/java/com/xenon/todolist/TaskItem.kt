package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.viewmodel.LiveListItem
import kotlinx.serialization.Serializable
import java.text.DateFormat

private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

// Define the Importance enum
enum class Importance(val value: Int) {
    NO_IMPORTANCE(2),
    HIGH_IMPORTANCE(1),
    HIGHEST_IMPORTANCE(0)
}

@Serializable
data class TaskItem(
    override var id: Int,
    var name: String,
    var desc: String,
    var dueTime: Long = -1,
    var dueDate: Long = -1,
    var createdDate: Long,
    var completedDate: Long = -1,
    var importance: Importance = Importance.NO_IMPORTANCE,
    var steps: Int = 0,
    var files: Int = 0,
    var notification: Int = 0,
    var moreOptionsExpanded: Boolean = false,
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

    val dueDateString: String
        get() = dateFormat.format(dueDate)

    val dueTimeString: String
        get() = timeFormat.format(dueTime)

    val description: String
        get() = desc.ifEmpty { "No description" }

    fun isHighImportance() = importance == Importance.HIGH_IMPORTANCE
    fun isHighestImportance() = importance == Importance.HIGHEST_IMPORTANCE
}