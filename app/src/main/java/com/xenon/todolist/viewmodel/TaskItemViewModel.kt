package com.xenon.todolist.viewmodel

import com.xenon.todolist.TaskItem

class TaskItemViewModel : LiveListViewModel<TaskItem>() {
    private var sortType = SortType.BY_COMPLETENESS

    enum class SortType {
        NONE, BY_COMPLETENESS, BY_CREATION_DATE, BY_DUE_DATE
    }

    override fun sortItems() {
        super.sortItems()
        sortListBySortType(items, sortType)
    }

    fun getSortType(): SortType {
        return sortType
    }
    fun setSortType(type: SortType) {
        if (type == sortType) {
            return
        }
        sortType = type
        sortListBySortType(items, type)
        setList(items)
    }

    private fun sortListBySortType(list: ArrayList<TaskItem>, type: SortType) {
        when (type) {
            SortType.BY_COMPLETENESS -> list.sortBy { taskItem -> if (taskItem.isCompleted()) 1 else 0 }
            SortType.BY_CREATION_DATE -> list.sortBy { taskItem -> taskItem.createdDate }
            SortType.BY_DUE_DATE -> list.sortBy { taskItem -> taskItem.dueTime }
            else -> {}
        }
    }

    override fun calculateItemPosition(item: TaskItem, currentIdx: Int): Int {
        val newIdx: Int
        when (sortType) {
            SortType.BY_COMPLETENESS -> {
                var pivotIdx = items.indexOfFirst { v -> v.isCompleted() && v != item }
                if (pivotIdx > 0) pivotIdx -= 1
                if (pivotIdx < 0) pivotIdx = items.size - 1

                newIdx = if (item.isCompleted() && currentIdx < pivotIdx) pivotIdx else if (pivotIdx < currentIdx - 1) pivotIdx + 1 else currentIdx
            }
            SortType.BY_CREATION_DATE -> {
                var pivotIdx = items.indexOfFirst { v ->
                    v.createdDate > item.createdDate || v.dueTime == item.dueTime && v != item
                }
                if (pivotIdx > 0) pivotIdx -= 1
                else if (pivotIdx < 0) pivotIdx = items.size - 1

                newIdx = if (currentIdx < pivotIdx) pivotIdx else currentIdx
            }
            SortType.BY_DUE_DATE -> {
                var pivotIdx = items.indexOfFirst { v ->
                    v.dueTime > item.dueTime || v.dueTime == item.dueTime && v != item
                }
                if (pivotIdx > 0) pivotIdx -= 1
                else if (pivotIdx < 0) pivotIdx = items.size - 1

                newIdx = if (currentIdx < pivotIdx) pivotIdx else currentIdx
            }
            else -> {
                newIdx = currentIdx
            }
        }
        return newIdx
    }

    override fun move(from: Int, to: Int): Boolean {
        if (sortType == SortType.BY_COMPLETENESS && items[from].isCompleted() != items[to].isCompleted()) {
            return false
        }
        else if (sortType == SortType.BY_CREATION_DATE && items[from].createdDate != items[to].createdDate) {
            return false
        }
        else if (sortType == SortType.BY_DUE_DATE && items[from].dueTime != items[to].dueTime) {
            return false
        }
        return super.move(from, to)
    }
}