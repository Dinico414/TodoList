package com.xenon.todolist.viewmodel

import com.xenon.todolist.TaskItem

class TaskItemViewModel : LiveListViewModel<TaskItem>() {
    private var sortType = SortType.BY_COMPLETENESS
    private var sortDirection = SortDirection.ASCENDING

    enum class SortType {
        NONE, BY_COMPLETENESS, BY_CREATION_DATE, BY_DUE_DATE, BY_NAME,  BY_IMPORTANCE
    }

    enum class SortDirection {
        ASCENDING, DESCENDING
    }

    override fun sortItems() {
        super.sortItems()
        sortListBySortType(items, sortType, sortDirection)
    }

    fun getSortType(): SortType {
        return sortType
    }

    fun getSortDirection(): SortDirection {
        return sortDirection
    }

    fun setSortType(type: SortType, direction: SortDirection) {
        if (type == sortType && direction == sortDirection) {
            return
        }
        sortType = type
        sortDirection = direction
        sortListBySortType(items, type, direction)
        setList(items)
    }

    private fun sortListBySortType(list: ArrayList<TaskItem>, type: SortType, direction: SortDirection) {
        when (type) {
            SortType.BY_COMPLETENESS -> {
                list.sortBy { taskItem -> if (taskItem.isCompleted()) 1 else 0 }
                if (direction == SortDirection.DESCENDING) {
                    list.reverse()
                }
            }
            SortType.BY_CREATION_DATE -> {
                list.sortBy { taskItem -> taskItem.createdDate }
                if (direction == SortDirection.DESCENDING) {
                    list.reverse()
                }
            }
            SortType.BY_DUE_DATE -> {
                list.sortBy { taskItem -> taskItem.dueDateTime }
                if (direction == SortDirection.DESCENDING) {
                    list.reverse()
                }
            }
            SortType.BY_NAME -> {
                list.sortBy { taskItem -> taskItem.name }
                if (direction == SortDirection.DESCENDING) {
                    list.reverse()
                }
            }
            SortType.BY_IMPORTANCE -> {
                list.sortBy { taskItem -> taskItem.importance.value }
                if (direction == SortDirection.DESCENDING) {
                    list.reverse()
                }
            }
            else -> {}
        }
    }

    override fun calculateItemPosition(item: TaskItem, currentIdx: Int): Int {
        val newIdx: Int
        when (sortType) {
            SortType.BY_COMPLETENESS -> {
                var pivotIdx = if (sortDirection == SortDirection.ASCENDING) {
                    items.indexOfFirst { v -> v.isCompleted() && v != item }
                } else {
                    items.indexOfLast { v -> !v.isCompleted() && v != item }
                }

                if (pivotIdx < 0) pivotIdx = if (sortDirection == SortDirection.ASCENDING) items.size else 0

                newIdx = if (sortDirection == SortDirection.ASCENDING) {
                    if (item.isCompleted() && currentIdx < pivotIdx) pivotIdx - 1
                    else if (!item.isCompleted() && currentIdx >= pivotIdx) pivotIdx
                    else currentIdx
                } else {
                    if (!item.isCompleted() && currentIdx > pivotIdx) pivotIdx + 1
                    else if (item.isCompleted() && currentIdx <= pivotIdx) pivotIdx
                    else currentIdx
                }
            }
            SortType.BY_CREATION_DATE -> {
                var pivotIdx = if (sortDirection == SortDirection.ASCENDING) {
                    items.indexOfFirst { v ->
                        v.createdDate > item.createdDate || v.dueDateTime == item.dueDateTime && v != item
                    }
                } else {
                    items.indexOfLast { v ->
                        v.createdDate < item.createdDate || v.dueDateTime == item.dueDateTime && v != item
                    }
                }

                if (pivotIdx < 0) pivotIdx = if (sortDirection == SortDirection.ASCENDING) items.size - 1 else 0
                if (pivotIdx > 0 && sortDirection == SortDirection.ASCENDING) pivotIdx -= 1

                newIdx = if (sortDirection == SortDirection.ASCENDING) {
                    if (currentIdx < pivotIdx) pivotIdx else currentIdx
                } else {
                    if (currentIdx > pivotIdx) pivotIdx else currentIdx
                }
            }
            SortType.BY_DUE_DATE -> {
                var pivotIdx = if (sortDirection == SortDirection.ASCENDING) {
                    items.indexOfFirst { v ->
                        v.dueTime > item.dueTime || v.dueDateTime == item.dueDateTime && v != item
                    }
                } else {
                    items.indexOfLast { v ->
                        v.dueTime < item.dueTime || v.dueDateTime == item.dueDateTime && v != item
                    }
                }
                if (pivotIdx < 0) pivotIdx = if (sortDirection == SortDirection.ASCENDING) items.size - 1 else 0
                if (pivotIdx > 0 && sortDirection == SortDirection.ASCENDING) pivotIdx -= 1


                newIdx = if (sortDirection == SortDirection.ASCENDING) {
                    if (currentIdx < pivotIdx) pivotIdx else currentIdx
                } else {
                    if (currentIdx > pivotIdx) pivotIdx else currentIdx
                }
            }
            SortType.BY_NAME -> {
                var pivotIdx = if (sortDirection == SortDirection.ASCENDING) {
                    items.indexOfFirst { v ->
                        v.name > item.name || v.dueDateTime == item.dueDateTime && v != item
                    }
                } else {
                    items.indexOfLast { v ->
                        v.name < item.name || v.dueDateTime == item.dueDateTime && v != item
                    }
                }

                if (pivotIdx < 0) pivotIdx = if (sortDirection == SortDirection.ASCENDING) items.size - 1 else 0
                if (pivotIdx > 0 && sortDirection == SortDirection.ASCENDING) pivotIdx -= 1

                newIdx = if (sortDirection == SortDirection.ASCENDING) {
                    if (currentIdx < pivotIdx) pivotIdx else currentIdx
                } else {
                    if (currentIdx > pivotIdx) pivotIdx else currentIdx
                }
            }
            SortType.BY_IMPORTANCE -> {
                var pivotIdx = if (sortDirection == SortDirection.ASCENDING) {
                    items.indexOfFirst { v ->
                        v.importance > item.importance || v.dueDateTime == item.dueDateTime && v != item
                    }
                } else {
                    items.indexOfLast { v ->
                        v.importance < item.importance || v.dueDateTime == item.dueDateTime && v != item
                    }
                }
                if (pivotIdx < 0) pivotIdx = if (sortDirection == SortDirection.ASCENDING) items.size - 1 else 0
                if (pivotIdx > 0 && sortDirection == SortDirection.ASCENDING) pivotIdx -= 1

                newIdx = if (sortDirection == SortDirection.ASCENDING) {
                    if (currentIdx < pivotIdx) pivotIdx else currentIdx
                } else {
                    if (currentIdx > pivotIdx) pivotIdx else currentIdx
                }
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
        else if (sortType == SortType.BY_NAME && items[from].name != items[to].name) {
            return false
        }
        else if (sortType == SortType.BY_IMPORTANCE && items[from].importance != items[to].importance) {
            return false
        }
        return super.move(from, to)
    }
}