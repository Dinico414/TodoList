package com.xenon.todolist

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
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
            TypedValue.COMPLEX_UNIT_DIP,
            10.toFloat(),
            context.resources.displayMetrics
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
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    fun bindTaskItem(taskItem: TaskItem) {
        binding.name.text = taskItem.name

        if (taskItem.isCompleted()) {
            binding.name.alpha = 0.5f
            binding.dueTime.alpha = 0.5f

            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            binding.taskCellContainer.background.colorFilter = filter
        } else {
            binding.name.paintFlags = 0
            binding.dueTime.paintFlags = 0
            binding.name.alpha = 1.0f
            binding.dueTime.alpha = 1.0f
            binding.taskCellContainer.background.clearColorFilter()
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

            // Check if both time and date are present
            val hasTime =
                calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0
            val hasDate =
                calendar.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR) ||
                        calendar.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance()
                    .get(Calendar.DAY_OF_YEAR)

            binding.dueTime.text = when {
                hasTime && hasDate -> "$formattedTime\n$formattedDate" // Both time and date
                hasTime -> formattedTime // Only time
                hasDate -> formattedDate // Only date
                else -> "" // Neither (shouldn't happen if dueTime >= 0, but handle for safety)
            }
        } else {
            binding.dueTime.text = ""
        }
    }
}