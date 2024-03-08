package com.xenon.todolist

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskListCellBinding

class TaskListAdapter(
    private val context: Context,
    var taskItems: List<TaskList>,
    private val clickListener: TaskListClickListener,
) : RecyclerView.Adapter<TaskListAdapter.TaskListViewHolder>() {

    var selectedItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TaskListCellBinding.inflate(from, parent, false)
        return TaskListViewHolder(parent.context, binding, clickListener)
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        holder.bindTaskItem(taskItems[position], position, position == selectedItemPosition)

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

    interface TaskListClickListener {
        fun editTaskList(taskList: TaskList, position: Int)
        fun selectTaskList(taskList: TaskList, position: Int)
    }

    class TaskListViewHolder(
        private val context: Context,
        private val binding: TaskListCellBinding,
        private val clickListener: TaskListClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindTaskItem(taskList: TaskList, position: Int, enabled: Boolean) {
            binding.name.text = taskList.name

            binding.taskCellContainer.setOnClickListener {
                clickListener.selectTaskList(taskList, position)
            }

            if (enabled)
                binding.taskCellContainer.background.setTint(context.getColor(com.xenon.commons.accesspoint.R.color.surfaceBright))
            else
                binding.taskCellContainer.background.setTint(context.getColor(com.xenon.commons.accesspoint.R.color.surface))
        }
    }
}
