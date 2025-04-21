package com.xenon.todolist

import android.content.Context
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskItemCellBinding
import java.text.DateFormat
import java.util.Calendar

interface TaskItemClickListener {
    fun editTaskItem(taskItem: TaskItem)
    fun completeTaskItem(taskItem: TaskItem)
}

class TaskItemViewHolder(
    private val context: Context,
    private val binding: TaskItemCellBinding,
    private val clickListener: TaskItemClickListener
) : RecyclerView.ViewHolder(binding.root) {
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    fun bindTaskItem(taskItem: TaskItem) {
        binding.name.text = taskItem.name

        if (taskItem.isCompleted()) {
            binding.name.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            binding.dueTime.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.name.paintFlags = 0
            binding.dueTime.paintFlags = 0
        }

        binding.completeButton.setImageResource(taskItem.imageResource())
        binding.completeButton.setColorFilter(taskItem.imageColor(context))

        binding.completeButton.setOnClickListener {
            clickListener.completeTaskItem(taskItem)
        }
        binding.taskCellContainer.setOnClickListener {
            clickListener.editTaskItem(taskItem)
        }

        if (taskItem.dueTime >= 0) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = taskItem.dueTime
            val formattedTime = timeFormat.format(calendar.time)
            val formattedDate = dateFormat.format(calendar.time)
            binding.dueTime.text = "$formattedTime - $formattedDate"
        } else {
            binding.dueTime.text = ""
        }
    }
}