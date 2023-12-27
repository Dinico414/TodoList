package com.xenon.todolist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Collections

class TaskItemViewModel : ViewModel() {
    val taskStatus = MutableLiveData<TaskStatusChange>()
    class TaskStatusChange(val type: TaskChangedType, val taskItem: TaskItem? = null, val idx: Int = -1, val idx2: Int = -1)
    enum class TaskChangedType {
        ADD, REMOVE, MOVED, UPDATE, OVERWRITTEN
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
        taskStatus.postValue(TaskStatusChange(TaskChangedType.ADD, taskItem, _idx))
    }

    fun remove(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        if (idx < 0)
            return
        taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, taskItem, idx))
    }

    fun remove(idx: Int) {
        val taskItem = taskItems.removeAt(idx)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.REMOVE, taskItem, idx))
    }

    fun move(from: Int, to: Int) {
//        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        Collections.swap(taskItems, from, to)
        taskStatus.postValue(TaskStatusChange(TaskChangedType.MOVED, null, from, to))
    }

    fun update(taskItem: TaskItem) {
        val idx = taskItems.indexOfFirst { item -> taskItem.id == item.id }
        taskItems[idx] = taskItem
        taskStatus.postValue(TaskStatusChange(TaskChangedType.UPDATE, taskItem, idx))
    }
}