package com.xenon.todolist

import android.util.Log
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

    fun move(taskItem: TaskItem, to: Int) {
        Log.d("tttt", "taskItem: ${taskItem.id} $taskItem")
        move(taskItems.indexOfFirst { item -> taskItem.id == item.id }, to)
    }

    fun move(from: Int, to: Int) {
        Log.d("ajaj", "$from $to")
        if (from == to) {
            update(from)
            return
        }
        Log.d("aa", "before: $taskItems")
        taskItems.add(to, taskItems.removeAt(from))
        Log.d("aa", "after: $taskItems")
        taskStatus.postValue(TaskStatusChange(TaskChangedType.MOVED, null, from, to))
    }

    fun update(taskItem: TaskItem) {
        update(taskItems.indexOfFirst { item -> taskItem.id == item.id })
    }

    fun update(idx: Int) {
        taskStatus.postValue(TaskStatusChange(TaskChangedType.UPDATE, taskItems[idx], idx))
    }
}