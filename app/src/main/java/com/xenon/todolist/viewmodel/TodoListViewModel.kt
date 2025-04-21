package com.xenon.todolist.viewmodel

import androidx.lifecycle.MutableLiveData
import com.xenon.todolist.TodoList
import com.xenon.todolist.TodoListAdapter

class TodoListViewModel : LiveListViewModel<TodoList>() {
    val selectedIdx = MutableLiveData<Int>(0)

    fun uncheckAll() {
        getList().forEachIndexed { i, item ->
            item.checked = false
            update(i, TodoListAdapter.BindAction.ALL_UNCHECKED)
        }
    }
}