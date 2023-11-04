package com.xenon.todolist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TaskViewModel: ViewModel()
{
    var taskItems = MutableLiveData<MutableList<TaskItem>?>()

    init {
        taskItems.value = mutableListOf()
    }

    fun addTaskItem(newTask: TaskItem)
    {
        val list = taskItems.value
        list!!.add(newTask)
        taskItems.postValue(list)
    }

    fun updateTaskItem(id: UUID, name: String, desc: String, dueTime: LocalTime?)
    {
        val list = taskItems.value
        val task = list!!.find { it.id == id }!!
        task.name = name
        task.desc = desc
        task.dueTime = dueTime
        taskItems.postValue(list)
    }

    fun setCompleted(taskItem: TaskItem, enabled: Boolean)
    {
        val list = taskItems.value
        val task = list!!.find { it.id == taskItem.id }!!
        if (task.completedDate == null && enabled)
            task.completedDate = LocalDate.now()
        else if (!enabled)
            task.completedDate = null
        taskItems.postValue(list)
    }

    fun toggleCompleted(taskItem: TaskItem) {
        setCompleted(taskItem, !taskItem.isCompleted())
    }
}