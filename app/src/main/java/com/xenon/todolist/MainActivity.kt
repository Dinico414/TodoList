package com.xenon.todolist

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xenon.todolist.databinding.ActivityMainBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity(), TaskItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var taskItems = ArrayList<TaskItem>()
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(this, null).show(supportFragmentManager, "newTaskTag")
        }

        sharedPref = getPreferences(Context.MODE_PRIVATE)

        loadTaskItems()
        setRecyclerView()
    }

    private fun loadTaskItems() {
        val json = sharedPref.getString("taskItems", "[]")
        taskItems = json?.let { Json.decodeFromString(it) }!!
    }

    private fun saveTaskItems() {
        val json = Json.encodeToString(taskItems)
        with(sharedPref.edit()) {
            putString("taskItems", json)
            apply()
        }
    }

    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(taskItems, mainActivity)
        }

        onTaskItemsChanged()
    }

    override fun editTaskItem(taskItem: TaskItem) {
        NewTaskSheet(this, taskItem).show(supportFragmentManager, "newTaskTag")
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskItem.toggleCompleted()
        updateTaskItem(taskItem)
    }

    fun addTaskItem(taskItem: TaskItem) {
        taskItems.add(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemInserted(taskItems.size - 1)
        onTaskItemsChanged()
    }

    fun updateTaskItem(taskItem: TaskItem) {
        val idx = if (taskItem.idx >= 0) taskItem.idx else taskItems.indexOf(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemChanged(idx)
        saveTaskItems()
    }

    private fun onTaskItemsChanged() {
        saveTaskItems()
        if (taskItems.isEmpty()) {
            binding.noTasks.visibility = View.VISIBLE
        } else {
            binding.noTasks.visibility = View.GONE
        }
    }
}
