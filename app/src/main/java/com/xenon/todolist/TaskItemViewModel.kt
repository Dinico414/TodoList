package com.xenon.todolist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TaskItemViewModel : ViewModel() {
    val taskStatus = MutableLiveData<TaskStatusChange>()
    class TaskStatusChange(val type: TaskChangedType, val idx: Int = -1, val taskItem: TaskItem? = null)
    enum class TaskChangedType {
        ADD, REMOVE, UPDATE, OVERWRITTEN
    }

    private var maxTaskId = -1
    private var taskItems = ArrayList<TaskItem>()
    fun getList(): ArrayList<TaskItem> {
        // returned list should not be modified
        return taskItems
    }
    fun setList(list: ArrayList<TaskItem>) {
        taskItems = list
        if (list.size > 0) {
            maxTaskId = list.maxBy { taskItem -> taskItem.id }.id
        }
        taskStatus.postValue(TaskStatusChange(TaskChangedType.OVERWRITTEN))
    }

    fun add(taskItem: TaskItem, idx: Int = -1) {
        val _idx = if (idx < 0) { taskItems.size } else { idx }
        maxTaskId++
        taskItem.id = maxTaskId
        taskItems.add(_idx, taskItem)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.ADD, _idx, taskItem))
    }

    fun remove(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        if (idx < 0)
            return
        taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, idx, taskItem))
    }

    fun remove(idx: Int) {
        val taskItem = taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, idx, taskItem))
    }

    fun update(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        taskItems[idx] = taskItem
        taskStatus.postValue(TaskStatusChange(TaskChangedType.UPDATE, idx, taskItem))
    }
}