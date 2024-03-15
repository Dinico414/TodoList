package com.xenon.todolist

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TaskListCellBinding

class TaskListAdapter(
    private val context: Context,
    var taskItems: List<TaskList>,
    private val clickListener: TaskListClickListener,
) : RecyclerView.Adapter<TaskListAdapter.TaskListViewHolder>() {

    var selectedItemPosition = -1
    val selectedItems: ArrayList<TaskList> = ArrayList()

    private enum class SelectedStateChanged {
        SELECTION_ACTIVE, SELECTION_INACTIVE
    }

    fun setInSelectionState(value: Boolean) {
        val state = if (value) SelectedStateChanged.SELECTION_ACTIVE else SelectedStateChanged.SELECTION_INACTIVE
        notifyItemRangeChanged(0, itemCount, state)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TaskListCellBinding.inflate(from, parent, false)
        return TaskListViewHolder(parent.context, binding, clickListener) { taskItem, selected ->
            if (selected) {
                selectedItems.add(taskItem)
                if (selectedItems.size == 1)
                    setInSelectionState(true)
            }
            else {
                selectedItems.remove(taskItem)
                if (selectedItems.size == 0)
                    setInSelectionState(false)
            }
        }
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        holder.bindTaskItem(taskItems[position], position, position == selectedItemPosition)

        if (selectedItems.size > 0) {
            holder.setSelected(selectedItems.contains(taskItems[position]))
        }

        val horizontalMarginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(horizontalMarginInPx, 0, horizontalMarginInPx, horizontalMarginInPx)
        holder.itemView.layoutParams = layoutParams
    }

    override fun onBindViewHolder(
        holder: TaskListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.lastOrNull() == null)
            super.onBindViewHolder(holder, position, payloads)
//        when (payloads.lastOrNull()) {
//            SelectedStateChanged.SELECTION_ACTIVE -> holder.setSelected(selectedItems.contains(taskItems[position]))
//            SelectedStateChanged.SELECTION_INACTIVE -> holder.setInSelectionState(false)
//            else -> super.onBindViewHolder(holder, position, payloads)
//        }
        if (selectedItems.size > 0)
            holder.setSelected(selectedItems.contains(taskItems[position]))
        else
            holder.setInSelectionState(false)
        holder.setEnabled(selectedItemPosition == position)
    }

    override fun getItemCount(): Int = taskItems.size

    interface TaskListClickListener {
        fun editTaskList(taskList: TaskList, position: Int)
        fun selectTaskList(taskList: TaskList, position: Int)
    }

    class TaskListViewHolder(
        private val context: Context,
        private val binding: TaskListCellBinding,
        private val clickListener: TaskListClickListener,
        private val onItemSelected: (taskList: TaskList, selected: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var selected: Boolean = false
        private var inSelectionState: Boolean = false

        fun setEnabled(state: Boolean) {
            if (state)
                binding.taskCellContainer.background.setTint(context.getColor(com.xenon.commons.accesspoint.R.color.surfaceBright))
            else
                binding.taskCellContainer.background.setTint(context.getColor(com.xenon.commons.accesspoint.R.color.surface))
        }

        fun setSelected(value: Boolean) {
            selected = value
            if (!inSelectionState) setInSelectionState(true)
            binding.selectedCheckbox.isEnabled = value
//            binding.taskCellContainer.background
        }

        fun setInSelectionState(value: Boolean) {
            binding.selectedCheckbox.isVisible = value
            if (!value)
                binding.selectedCheckbox.isEnabled = false
            inSelectionState = value
        }

        fun bindTaskItem(taskList: TaskList, position: Int, enabled: Boolean) {
            binding.name.text = taskList.name

            binding.taskCellContainer.setOnClickListener {
                if (inSelectionState) {
                    setSelected(!selected)
                    onItemSelected(taskList, selected)
                }
                else
                    clickListener.selectTaskList(taskList, position)
            }
            binding.taskCellContainer.setOnLongClickListener {
                if (!inSelectionState) {
                    setSelected(true)
                    onItemSelected(taskList, selected)
                    true
                }
                else {
                    false
                }
            }

            setEnabled(enabled)
        }
    }
}
