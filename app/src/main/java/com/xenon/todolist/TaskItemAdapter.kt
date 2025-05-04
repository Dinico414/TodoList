package com.xenon.todolist

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskItemCellBinding
import java.text.DateFormat
import java.util.Calendar

class TaskItemAdapter(
    private val context: Context,
    var taskItems: List<TaskItem>,
    private val clickListener: TaskItemClickListener,
) : RecyclerView.Adapter<TaskItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskItemViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TaskItemCellBinding.inflate(from, parent, false)
        return TaskItemViewHolder(parent.context, binding, clickListener)
    }

    override fun onBindViewHolder(holder: TaskItemViewHolder, position: Int) {
        holder.bindTaskItem(taskItems[position])

        val horizontalMarginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10.toFloat(), context.resources.displayMetrics
        ).toInt()

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(horizontalMarginInPx, 0, horizontalMarginInPx, horizontalMarginInPx)
        holder.itemView.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = taskItems.size
}

interface TaskItemClickListener {
    fun editTaskItem(taskItem: TaskItem)
    fun completeTaskItem(taskItem: TaskItem)
}

class TaskItemViewHolder(
    private val context: Context,
    private val binding: TaskItemCellBinding,
    private val clickListener: TaskItemClickListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bindTaskItem(taskItem: TaskItem) {
        binding.name.text = taskItem.name

        if (taskItem.isCompleted()) {
            binding.name.alpha = 0.5f
            binding.dueTime.alpha = 0.5f
            ViewCompat.setBackgroundTintList(
                binding.taskCellContainer, ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context, com.xenon.commons.accesspoint.R.color.surfaceContainerHighest
                    )
                )
            )
        } else {
            binding.name.alpha = 1f
            binding.dueTime.alpha = 1f
            ViewCompat.setBackgroundTintList(
                binding.taskCellContainer, ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context, com.xenon.commons.accesspoint.R.color.secondaryContainer
                    )
                )
            )
        }

        binding.completeButton.setImageResource(taskItem.imageResource())
        binding.completeButton.setColorFilter(taskItem.imageColor(context))

        binding.completeButton.setOnClickListener {
            clickListener.completeTaskItem(taskItem)
        }
        binding.taskCellContainer.setOnClickListener {
            clickListener.editTaskItem(taskItem)
        }

        if (taskItem.dueDateTime >= 0) {
            val calender = Calendar.getInstance()
            calender.timeInMillis = taskItem.dueTime

            val hasTime =
                taskItem.dueTime > 0 && (calender.get(Calendar.HOUR_OF_DAY) != 0 || calender.get(
                    Calendar.MINUTE
                ) != 0)

            calender.timeInMillis = taskItem.dueDate
            val hasDate =
                taskItem.dueDate > 0 && (calender.get(Calendar.YEAR) != Calendar.getInstance()
                    .get(Calendar.YEAR) || calender.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance()
                    .get(Calendar.DAY_OF_YEAR))

            binding.dueTime.text = when {
                hasTime && hasDate -> "${taskItem.dueTimeString}\n${taskItem.dueDateString}" // Both time and date
                hasTime -> taskItem.dueTimeString // Only time
                hasDate -> taskItem.dueDateString // Only date
                else -> "" // Neither (shouldn't happen if dueTime >= 0, but handle for safety)
            }
        } else {
            binding.dueTime.text = ""
        }
    }
}