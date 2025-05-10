package com.xenon.todolist.adapter

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
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.TodoList
import com.xenon.todolist.databinding.TodoListCellBinding

class TodoListAdapter(
    private val context: Context,
    private var taskListList: List<TodoList>,
    private val clickListener: TodoListClickListener?,
) : RecyclerView.Adapter<TodoListAdapter.TodoListViewHolder>() {
    enum class BindAction(val i: Int) {
        REBIND(0),
        CHECKED_STATE_CHANGED(1),
        ALL_UNCHECKED(2),
        SELECTED_STATE_CHANGED(4)
    }

    private var selectedItemPosition = -1
    private val checkedItems: ArrayList<TodoList> = ArrayList()

    private val listener = object : TodoListClickListener {
        override fun onItemEdited(taskList: TodoList, position: Int) {
            clickListener?.onItemEdited(taskList, position)
        }

        override fun onItemSelected(taskList: TodoList, position: Int) {
            clickListener?.onItemSelected(taskList, position)
            notifyItemRangeChanged(0, itemCount, true)
        }

        override fun onItemChecked(taskList: TodoList, position: Int, list: List<TodoList>) {
            if (taskList.checked) {
                checkedItems.add(taskList)
                if (checkedItems.size == 1)
                    notifyItemRangeChanged(0, itemCount, true)
            } else {
                checkedItems.remove(taskList)
                if (checkedItems.isEmpty())
                    notifyItemRangeChanged(0, itemCount, true)
            }
            clickListener?.onItemChecked(taskList, position, checkedItems)
        }
    }

    init {
        updateCheckedItemsList()
    }

    fun setTaskList(taskList: List<TodoList>) {
        this.taskListList = taskList
        updateCheckedItemsList()
    }

    fun selectItem(position: Int) {
        notifyItemChanged(selectedItemPosition, BindAction.SELECTED_STATE_CHANGED)
        notifyItemChanged(position, BindAction.SELECTED_STATE_CHANGED)
        selectedItemPosition = position
    }

    fun updateCheckedItemsList() {
        checkedItems.clear()
        for (list in taskListList)
            if (list.checked) checkedItems.add(list)
    }

    fun onItemRemoved(taskList: TodoList) {
        if (checkedItems.remove(taskList) && checkedItems.isEmpty()) {
            notifyItemRangeChanged(0, itemCount, true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoListViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TodoListCellBinding.inflate(from, parent, false)
        return TodoListViewHolder(parent.context, binding, listener)
    }

    override fun onBindViewHolder(holder: TodoListViewHolder, position: Int) {
        val item = taskListList[position]
        holder.bindTaskItem(item, position, position == selectedItemPosition)

        if (checkedItems.isNotEmpty()) {
            holder.setCheckboxState(true)
            holder.setChecked(checkedItems.contains(item))
        }
        else {
            holder.setCheckboxState(false)
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
        var action = 0
        payloads.forEach { payload ->
            val a = payload as? BindAction ?: BindAction.REBIND
            if (a.i == 0) {
                action = 0
                return@forEach
            }
            action = action.or(a.i)
        }
        Log.d("aaa", "$position $action ${taskListList[position].checked}")
        if (action == 0) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        if (action and BindAction.ALL_UNCHECKED.i > 0) {
            checkedItems.clear()
            holder.setCheckboxState(false)
            clickListener?.onItemChecked(taskListList[position], position, checkedItems)
        }
        if (action and BindAction.CHECKED_STATE_CHANGED.i > 0) {
            val i = checkedItems.indexOf(taskListList[position])
            if (taskListList[position].checked == (i < 0)) {
                if (i >= 0)
                    checkedItems.remove(taskListList[position])
                else
                    checkedItems.add(taskListList[position])
                clickListener?.onItemChecked(taskListList[position], position, checkedItems)
                if (checkedItems.isNotEmpty())
                    holder.setChecked(taskListList[position].checked)
                else
                    holder.setCheckboxState(false)
            }
        }
        if (action and BindAction.SELECTED_STATE_CHANGED.i > 0)
            holder.setSelected(selectedItemPosition == position)
    }

    override fun getItemCount(): Int = taskListList.size

    interface TodoListClickListener {
        fun onItemEdited(taskList: TodoList, position: Int)
        fun onItemSelected(taskList: TodoList, position: Int)
        fun onItemChecked(taskList: TodoList, position: Int, checkedItems: List<TodoList>)
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
                    clickListener.onItemChecked(taskList, position, emptyList())
                } else
                    clickListener.onItemSelected(taskList, position)
            }
            binding.taskCellContainer.setOnLongClickListener {
                if (!inCheckboxState) {
                    setChecked(true)
                    clickListener.onItemChecked(taskList, position, emptyList())
                    true
                } else {
                    false
                }
            }
            binding.editButton.setOnClickListener {
                clickListener.onItemEdited(taskList, position)
            }

            setSelected(selected)
        }

        fun setSelected(state: Boolean) {
            val color = if (state) {
                ContextCompat.getColor(context, R.color.primary)
            } else {
                ContextCompat.getColor(context, R.color.transparent)
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
                        R.color.primary
                    )
                )
            } else {
                val color = ContextCompat.getColor(
                    context,
                    R.color.textOnPrimary
                )
                val halfAlphaColor = ColorUtils.setAlphaComponent(color, (255 * 0.5).toInt())
                binding.selectedCheckbox.buttonTintList = ColorStateList.valueOf(halfAlphaColor)
            }
        }

        fun setCheckboxState(value: Boolean) {
            binding.selectedCheckbox.isVisible = value
            binding.editButton.isVisible = value
            if (!value) {
                taskList.checked = false
            }
            inCheckboxState = value
        }
    }
}