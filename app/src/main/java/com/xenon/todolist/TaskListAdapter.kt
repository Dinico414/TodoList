package com.xenon.todolist

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskItemCellBinding
import com.xenon.todolist.databinding.TaskListCellBinding

class TaskListAdapter(
    private val context: Context,
    var taskItems: List<TaskList>,
    private val clickListener: TaskListClickListener,
) : RecyclerView.Adapter<TaskListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TaskListCellBinding.inflate(from, parent, false)
        return TaskListViewHolder(parent.context, binding, clickListener)
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
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
