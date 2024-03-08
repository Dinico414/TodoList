package com.xenon.todolist

import com.xenon.todolist.viewmodel.LiveListItem
import kotlinx.serialization.Serializable

@Serializable
data class TaskList(
    override var id: Int,
    var name: String,
    var items: ArrayList<TaskItem>,
    var createdTime: Long
) : LiveListItem {
}