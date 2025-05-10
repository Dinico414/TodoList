package com.xenon.todolist.viewmodel

import androidx.lifecycle.MutableLiveData
import com.xenon.todolist.TodoList
import com.xenon.todolist.adapter.TodoListAdapter

class TodoListViewModel : LiveListViewModel<TodoList>() {
    val selectedIdx = MutableLiveData<Int>(0)

    fun uncheckAll() {
        getList().forEachIndexed { i, item ->
            item.checked = false
            update(i, TodoListAdapter.BindAction.ALL_UNCHECKED)
        }
    }

    override fun add(item: TodoList, idx: Int) {
        super.add(item, idx)
        val newIdx = if (idx < 0) getList().size - 1 else idx
        selectedIdx.postValue(newIdx)
    }

    override fun remove(idx: Int) {
        super.remove(idx)
        val selIdx = selectedIdx.value
        selIdx?.apply {
            if (idx == selIdx) {
                selectedIdx.postValue(0)
            }
            else if (idx < selIdx) {
                selectedIdx.postValue(selIdx - 1)
            }
        }
    }
}