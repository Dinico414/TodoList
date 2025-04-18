package com.xenon.todolist

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.TodoListCellBinding

class TodoListAdapter(
    private val context: Context,
    private var taskListList: List<TodoList>,
    private val clickListener: TodoListClickListener?,
) : RecyclerView.Adapter<TodoListAdapter.TodoListViewHolder>() {

    var selectedItemPosition = -1
    private val checkedItems: ArrayList<TodoList> = ArrayList()

    init {
        updateCheckedItemsList()
    }

    fun setTaskList(taskList: List<TodoList>) {
        this.taskListList = taskList
        updateCheckedItemsList()
    }

    fun updateCheckedItemsList() {
        for (list in taskListList)
            if (list.checked) checkedItems.add(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoListViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TodoListCellBinding.inflate(from, parent, false)
        return TodoListViewHolder(parent.context, binding, object : TodoListClickListener {
            override fun editTodoList(taskList: TodoList, position: Int) {
                clickListener?.editTodoList(taskList, position)
            }
            override fun selectTodoList(taskList: TodoList, position: Int) {
                clickListener?.selectTodoList(taskList, position)
                notifyItemRangeChanged(0, itemCount, true)
            }

            override fun onItemChecked(taskList: TodoList, position: Int) {
                clickListener?.onItemChecked(taskList, position)
                if (taskList.checked) {
                    checkedItems.add(taskList)
                    if (checkedItems.size == 1)
                        notifyItemRangeChanged(0, itemCount, true)
                } else {
                    checkedItems.remove(taskList)
                    if (checkedItems.isEmpty())
                        notifyItemRangeChanged(0, itemCount, true)
                }
            }
        })
    }

    override fun onBindViewHolder(holder: TodoListViewHolder, position: Int) {
        holder.bindTaskItem(taskListList[position], position, position == selectedItemPosition)

        if (checkedItems.isNotEmpty()) {
            holder.setCheckboxState(true)
            holder.setChecked(checkedItems.contains(taskListList[position]))
        }

        holder.setSelected(selectedItemPosition == position)

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
        holder: TodoListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.lastOrNull() == null) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        else if (checkedItems.isNotEmpty())
            holder.setChecked(checkedItems.contains(taskListList[position]))
        else
            holder.setCheckboxState(false)
        holder.setSelected(selectedItemPosition == position)
    }

    override fun getItemCount(): Int = taskListList.size

    interface TodoListClickListener {
        fun editTodoList(taskList: TodoList, position: Int)
        fun selectTodoList(taskList: TodoList, position: Int)
        fun onItemChecked(taskList: TodoList, position: Int)
    }

    class TodoListViewHolder(
        private val context: Context,
        private val binding: TodoListCellBinding,
        private val clickListener: TodoListClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var taskList: TodoList
        private var inCheckboxState: Boolean = false

        fun bindTaskItem(taskList: TodoList, position: Int, selected: Boolean) {
            this.taskList = taskList
            binding.name.text = taskList.name

            binding.taskCellContainer.setOnClickListener {
                if (inCheckboxState) {
                    setChecked(!taskList.checked)
                    clickListener.onItemChecked(taskList, position)
                } else
                    clickListener.selectTodoList(taskList, position)
            }
            binding.taskCellContainer.setOnLongClickListener {
                if (!inCheckboxState) {
                    setChecked(true)
                    clickListener.onItemChecked(taskList, position)
                    true
                } else {
                    false
                }
            }

            setSelected(selected)
        }

        fun setSelected(state: Boolean) {
            val color = if (state) {
                ContextCompat.getColor(context, com.xenon.commons.accesspoint.R.color.primary)
            } else {
                ContextCompat.getColor(context, com.xenon.commons.accesspoint.R.color.transparent)
            }
            val alpha = if (state) 0.5f else 0.0f
            val tintedColor = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            binding.taskCellContainer.background.setTint(tintedColor)
        }

        fun setChecked(value: Boolean) {
            taskList.checked = value
            if (!inCheckboxState) setCheckboxState(true)
            binding.selectedCheckbox.isEnabled = value
            binding.selectedCheckbox.isChecked = value
            if (value) {
                binding.selectedCheckbox.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        com.xenon.commons.accesspoint.R.color.primary
                    )
                )
            } else {
                val color = ContextCompat.getColor(
                    context,
                    com.xenon.commons.accesspoint.R.color.textOnPrimary
                )
                val halfAlphaColor = ColorUtils.setAlphaComponent(color, (255 * 0.5).toInt())
                binding.selectedCheckbox.buttonTintList = ColorStateList.valueOf(halfAlphaColor)
            }
        }

        fun setCheckboxState(value: Boolean) {
            binding.selectedCheckbox.isVisible = value
            if (!value) {
                taskList.checked = false
                binding.selectedCheckbox.isEnabled = false
            }
            inCheckboxState = value
        }
    }
}
