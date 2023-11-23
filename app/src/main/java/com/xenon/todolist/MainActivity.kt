package com.xenon.todolist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TaskItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Move view initialization here
        val todoListRecycleView: RecyclerView = findViewById(R.id.todoListRecycleView)
        val noTasksTextView: TextView = findViewById(R.id.noTasks)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(null).show(supportFragmentManager, "newTaskTag")
        }

        setRecyclerView()

        // Rest of your code
    }

    private fun setRecyclerView() {
        val mainActivity = this
        taskViewModel.taskItems.observe(this) { taskItems ->
            binding.todoListRecycleView.apply {
                layoutManager = LinearLayoutManager(applicationContext)
                adapter = taskItems?.let { TaskItemAdapter(it, mainActivity) }
            }

            // Update visibility based on the item count
            if (taskItems?.isEmpty() == true) {
                binding.noTasks.visibility = View.VISIBLE
            } else {
                binding.noTasks.visibility = View.GONE
            }
        }
    }

    override fun editTaskItem(taskItem: TaskItem) {
        NewTaskSheet(taskItem).show(supportFragmentManager, "newTaskTag")
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskViewModel.toggleCompleted(taskItem)
    }
}
