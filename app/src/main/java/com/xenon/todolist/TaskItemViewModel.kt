package com.xenon.todolist

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Collections

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
        BY_ID, BY_COMPLETENESS
    }

    fun getList(): ArrayList<TaskItem> {
        // returned list should not be modified
        return taskItems
    }
    fun setList(list: ArrayList<TaskItem>) {
        taskItems = list
        if (list.size > 0) {
            maxTaskId = list.maxBy { taskItem -> taskItem.id }.id
        }
        setSortType(sortType)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.OVERWRITTEN))
    }

    fun setSortType(type: SortType) {
        if (type == SortType.BY_COMPLETENESS) {
            taskItems.sortBy { taskItem -> if (taskItem.isCompleted()) 1 else 0 }
        }
        sortType = type
    }

    fun add(taskItem: TaskItem, idx: Int = -1) {
        var _idx = if (idx < 0) { taskItems.size } else { idx }
        if (sortType == SortType.BY_COMPLETENESS) {
            for ((i, item) in taskItems.reversed().withIndex()) {
                if (taskItem.isCompleted() && taskItems.size - i <= _idx) {
                    break
                }
                if (!item.isCompleted()) {
                    _idx = if (taskItem.isCompleted()) {
                        maxOf(taskItems.size - i, _idx)
                    } else {
                        minOf(taskItems.size - i, _idx)
                    }
                    break
                }
            }
        }
        maxTaskId++
        taskItem.id = maxTaskId
        taskItems.add(_idx, taskItem)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.ADD, taskItem, _idx))
    }

    fun remove(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        if (idx < 0)
            return
        remove(idx)
    }

    fun remove(idx: Int) {
        val taskItem = taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, taskItem, idx))
    }

    fun moveAndUpdate(taskItem: TaskItem) {
        // Updates taskItem and sets to correct position as per sorting
        val from = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        var to = 0
        if (sortType == SortType.BY_COMPLETENESS) {
            if (taskItem.isCompleted()) {
                for ((i, item) in taskItems.reversed().withIndex()) {
                    if (!item.isCompleted() || item == taskItem) {
                        to = taskItems.size - i - 1
                        break
                    }
                }
            }
            else {
                to = 0
            }
        }
        else {
            to = taskItems.size - 1
        }
        if (from == to) {
            update(from)
            return
        }
        taskItems.add(to, taskItems.removeAt(from))
        taskStatus.postValue(TaskStatusChange(TaskChangedType.MOVED_AND_UPDATED, taskItem, from, to))
    }

    fun move(from: Int, to: Int): Boolean {
        if (sortType == SortType.BY_COMPLETENESS) {
            if (taskItems[from].isCompleted() != taskItems[to].isCompleted()) {
                return false
            }
        }
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