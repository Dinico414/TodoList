package com.xenon.todolist

import android.content.Context
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskItemCellBinding
import com.xenon.todolist.databinding.TaskListCellBinding
import java.text.SimpleDateFormat
import java.util.Locale

interface TaskListClickListener {
    fun editTaskList(taskList: TaskList)
    fun selectTaskList(taskList: TaskList)
}
class TaskListViewHolder(
    private val context: Context,
    private val binding: TaskListCellBinding,
    private val clickListener: TaskListClickListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bindTaskItem(taskList: TaskList) {
        binding.name.text = taskList.name

        binding.taskCellContainer.setOnClickListener {
            clickListener.selectTaskList(taskList)
        }
    }
}