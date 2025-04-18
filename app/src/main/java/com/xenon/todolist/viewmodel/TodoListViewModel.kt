package com.xenon.todolist.viewmodel

import androidx.lifecycle.MutableLiveData
import com.xenon.todolist.TodoList

class TodoListViewModel : LiveListViewModel<TodoList>() {
    val selectedIdx = MutableLiveData<Int>(0)
}