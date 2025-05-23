package com.xenon.todolist.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.ConcurrentLinkedQueue

interface LiveListItem {
    /**
     * id gets auto-assigned by LiveListViewModel
     */
    var id: Int
}

@Suppress("unused")
open class LiveListViewModel<T: LiveListItem> : ViewModel() {
    val liveListEvent = MutableLiveData<ListEvent<T>>()
    val listEventQueue = ConcurrentLinkedQueue<ListEvent<T>>()
    class ListEvent<T>(val type: ListChangedType, val item: T? = null, val idx: Int = -1, val idx2: Int = -1, val payload: Any? = null)
    enum class ListChangedType {
        ADD, REMOVE, MOVED, UPDATE, MOVED_AND_UPDATED, OVERWRITTEN
    }

    private var maxTaskId = -1
    protected var items = ArrayList<T>()
    private var filteredItems = items
    private var currentFilter: ((T) -> Boolean)? = null

    private fun queueListEvent(event: ListEvent<T>) {
        listEventQueue.add(event)
        liveListEvent.postValue(event)
    }

    /**
     * returned list should not be modified
     */
    fun getList(): ArrayList<T> {
        return filteredItems
    }
    open fun setList(list: ArrayList<T>) {
        items = list
        for (i in 0 until list.size) {
            list[i].id = i
        }
        if (list.isNotEmpty()) {
            maxTaskId = list.maxBy { v -> v.id }.id
            sortItems()
        }
        filteredItems = items
        val f = currentFilter
        f?.apply { setListFilter(f) }
        queueListEvent(ListEvent(ListChangedType.OVERWRITTEN))
    }

    fun setListFilter(filter: (T) -> Boolean) {
        currentFilter = filter
        // TODO: Animate filter change
//        val prevFilteredItems = filteredItems
        filteredItems = items.filter(filter) as ArrayList<T>
        queueListEvent(ListEvent(ListChangedType.OVERWRITTEN))
    }

    fun clearFilter() {
        currentFilter = null
        filteredItems = items
    }

    protected open fun sortItems() {
    }

    protected open fun calculateItemPosition(item: T, currentIdx: Int): Int {
        return currentIdx
    }

    open fun add(item: T, idx: Int = -1) {
        val to = calculateItemPosition(item, if (idx < 0) items.size else idx)
        maxTaskId++
        item.id = maxTaskId
        items.add(to, item)

        val f = currentFilter
        f?.apply {
            if (!f.invoke(item)) return
            setListFilter(f)
        }

        queueListEvent(ListEvent(ListChangedType.ADD, item, to))
    }

    fun remove(item: T) {
        val idx = items.indexOfFirst { v -> item.id == v.id }
        if (idx < 0) return
        remove(idx)
    }

    open fun remove(idx: Int) {
        val item = items.removeAt(idx)
        val f = currentFilter
        f?.apply {
            if (!f.invoke(item)) return
            setListFilter(f)
        }
        queueListEvent(ListEvent(ListChangedType.REMOVE, item, idx))
    }

    /**
     * Updates item and sets to correct position as per calculateItemPosition
     * payload parameter may be used to pass message to
     * Recyclerview.Adapter.onBindViewHolder
     */
    open fun update(item: T, payload: Any? = null) {
        val from = items.indexOfFirst { v -> item.id == v.id }
        update(from, payload)
    }

    fun update(idx: Int, payload: Any? = null) {
        if (idx < 0) return
        val item = items[idx]
        val newIdx = calculateItemPosition(item, idx)
        if (idx == newIdx) {
            queueListEvent(ListEvent(ListChangedType.UPDATE, items[idx], idx, payload=payload))
            return
        }
        items.add(newIdx, items.removeAt(idx))
        queueListEvent(ListEvent(ListChangedType.MOVED_AND_UPDATED, item, idx, newIdx, payload=payload))
    }

    open fun move(from: Int, to: Int): Boolean {
        items.add(to, items.removeAt(from))
        queueListEvent(ListEvent(ListChangedType.MOVED, items[from], from, to))
        // Allow moving item
        return true
    }
}