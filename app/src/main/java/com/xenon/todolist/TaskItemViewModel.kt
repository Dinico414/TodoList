package com.xenon.todolist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TaskItemViewModel : ViewModel() {
    val taskStatus = MutableLiveData<TaskStatusChange>()
    class TaskStatusChange(val type: TaskChangedType, val taskItem: TaskItem? = null, val idx: Int = -1, val idx2: Int = -1)
    enum class TaskChangedType {
        ADD, REMOVE, MOVED, UPDATE, MOVED_AND_UPDATED, OVERWRITTEN
    }

    private var maxTaskId = -1
    private var taskItems = ArrayList<TaskItem>()

    private var sortType = SortType.BY_COMPLETENESS

    enum class SortType {
        NONE, BY_COMPLETENESS, BY_CREATION_DATE, BY_DUE_DATE
    }

    fun getList(): ArrayList<TaskItem> {
        // returned list should not be modified
        return taskItems
    }
    fun setList(list: ArrayList<TaskItem>) {
//        taskItems = list
        taskItems.clear()
        taskItems.addAll(list)
        if (list.size > 0) {
            maxTaskId = list.maxBy { taskItem -> taskItem.id }.id
        }
        setSortType(sortType)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.OVERWRITTEN))
    }

    fun getSortType(): SortType {
        return sortType
    }
    fun setSortType(type: SortType) {
        if (type == sortType) {
            return
        }
        sortType = type

        when (type) {
            SortType.BY_COMPLETENESS -> taskItems.sortBy { taskItem -> if (taskItem.isCompleted()) 1 else 0 }
            SortType.BY_CREATION_DATE -> taskItems.sortBy { taskItem -> taskItem.createdDate }
            SortType.BY_DUE_DATE -> taskItems.sortBy { taskItem -> taskItem.dueTime }
            else -> {}
        }
        setList(taskItems)
    }

    private fun calculateItemPosition(taskItem: TaskItem, oldIdx: Int): Int {
        val newIdx: Int
        when (sortType) {
            SortType.BY_COMPLETENESS -> {
                var pivotIdx = taskItems.indexOfFirst { item -> item.isCompleted() && item != taskItem }
                if (pivotIdx > 0) pivotIdx -= 1
                else if (pivotIdx < 0) pivotIdx = taskItems.size - 1

                newIdx = if (taskItem.isCompleted() && oldIdx < pivotIdx) pivotIdx else oldIdx
            }
            SortType.BY_CREATION_DATE -> {
                var pivotIdx = taskItems.indexOfFirst { item ->
                    item.createdDate > taskItem.createdDate || item.dueTime == taskItem.dueTime && item != taskItem
                }
                if (pivotIdx > 0) pivotIdx -= 1
                else if (pivotIdx < 0) pivotIdx = taskItems.size - 1

                newIdx = if (oldIdx < pivotIdx) pivotIdx else oldIdx
            }
            SortType.BY_DUE_DATE -> {
                var pivotIdx = taskItems.indexOfFirst { item ->
                    item.dueTime > taskItem.dueTime || item.dueTime == taskItem.dueTime && item != taskItem
                }
                if (pivotIdx > 0) pivotIdx -= 1
                else if (pivotIdx < 0) pivotIdx = taskItems.size - 1

                newIdx = if (oldIdx < pivotIdx) pivotIdx else oldIdx
            }
            else -> {
                newIdx = oldIdx
            }
        }
        return newIdx
    }

    fun add(taskItem: TaskItem, idx: Int = -1) {
        val to = calculateItemPosition(taskItem, if (idx < 0) taskItems.size else idx)
        maxTaskId++
        taskItem.id = maxTaskId
        taskItems.add(to, taskItem)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.ADD, taskItem, to))
    }

    fun remove(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        if (idx < 0) return
        remove(idx)
    }

    fun remove(idx: Int) {
        val taskItem = taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, taskItem, idx))
    }

    fun moveAndUpdate(taskItem: TaskItem) {
        moveAndUpdate(taskItem, false)
    }

    fun moveAndUpdate(taskItem: TaskItem, completedStateChanged: Boolean) {
        // Updates taskItem and sets to correct position as per sorting
        val from = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        if (from < 0) return
        val to = if (completedStateChanged && sortType == SortType.BY_COMPLETENESS && !taskItem.isCompleted()) {
            // Move newly uncompleted items to top
            0
        } else {
            calculateItemPosition(taskItem, from)
        }
        if (from == to) {
            update(from)
            return
        }
        taskItems.add(to, taskItems.removeAt(from))
        taskStatus.postValue(TaskStatusChange(TaskChangedType.MOVED_AND_UPDATED, taskItem, from, to))
    }

    fun move(from: Int, to: Int): Boolean {
        if (sortType == SortType.BY_COMPLETENESS && taskItems[from].isCompleted() != taskItems[to].isCompleted()) {
            return false
        }
        else if (sortType == SortType.BY_CREATION_DATE && taskItems[from].createdDate != taskItems[to].createdDate) {
            return false
        }
        else if (sortType == SortType.BY_DUE_DATE && taskItems[from].dueTime != taskItems[to].dueTime) {
            return false
        }
        // Allow moving item
        taskItems.add(to, taskItems.removeAt(from))
        taskStatus.postValue(TaskStatusChange(TaskChangedType.MOVED, taskItems[from], from, to))
        return true
    }

    fun update(taskItem: TaskItem) {
        update(taskItems.indexOfFirst { item -> taskItem.id == item.id })
    }

    fun update(idx: Int) {
        taskStatus.postValue(TaskStatusChange(TaskChangedType.UPDATE, taskItems[idx], idx))
    }
}