package com.xenon.todolist

import com.xenon.todolist.viewmodel.LiveListItem
import kotlinx.serialization.Serializable

@Serializable
data class TodoList(
    var name: String,
    var items: ArrayList<TaskItem>,
    var createdTime: Long
) : LiveListItem {
    override var id: Int = -1
    var checked: Boolean = false
}