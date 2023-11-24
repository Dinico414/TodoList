package com.xenon.todolist

import android.content.Context
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TaskItem(
    var name: String,
    var desc: String,
    var dueTime: LocalTime?,
    var completedDate: LocalDate?,
    var id: UUID = UUID.randomUUID(),
)
{
    var idx: Int = -1

    fun isCompleted() = completedDate != null
    fun setCompleted(b: Boolean) {
        this.completedDate = if(b) LocalDate.now() else null
    }
    fun toggleCompleted() {
        setCompleted(!isCompleted())
    }

    fun imageResource(): Int = if(isCompleted()) R.drawable.checked else R.drawable.unchecked
    fun imageColor(context: Context): Int = if(isCompleted()) purple(context) else black(context)

    private fun purple(context: Context) = ContextCompat.getColor(context, R.color.purple_500)
    private fun black(context: Context) = ContextCompat.getColor(context, R.color.black)
}