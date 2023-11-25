package com.xenon.todolist

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xenon.todolist.databinding.FragmentNewTaskSheetBinding
import java.time.LocalTime
import java.util.Calendar

class NewTaskSheet(var mainActivity: MainActivity, var taskItem: TaskItem?) : BottomSheetDialogFragment()
{

    private lateinit var binding: FragmentNewTaskSheetBinding
    private var dueTime: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (taskItem != null)
        {
            binding.taskTitle.text = "Edit Task"
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem!!.name)
            binding.desc.text = editable.newEditable(taskItem!!.desc)
            if(taskItem!!.dueTime >= 0){
                dueTime = taskItem!!.dueTime
                updateTimeButtonText()
            }
        }
        else
        {
            binding.taskTitle.text = "New Task"
        }

        binding.saveButton.setOnClickListener {
            saveAction()
        }
        binding.timePickerButton.setOnClickListener{
            openTimePicker()
        }
    }
    private fun openTimePicker() {
        if(dueTime < 0)
            dueTime = System.currentTimeMillis()
        val listener = TimePickerDialog.OnTimeSetListener{ _, selectedHour, selectedMinute ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            if (cal.get(Calendar.HOUR) > selectedHour || cal.get(Calendar.HOUR) == selectedHour && cal.get(Calendar.MINUTE) >= selectedMinute) {
                cal.add(Calendar.DAY_OF_WEEK, 1)
            }
            cal.set(Calendar.MINUTE, selectedMinute)
            cal.set(Calendar.HOUR, selectedHour)
            dueTime = cal.timeInMillis

            updateTimeButtonText()
        }
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime
        val dialog = TimePickerDialog(activity, listener, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), true)
        dialog.setTitle("Task Due")
        dialog.show()
    }
    private fun updateTimeButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime
        binding.timePickerButton.text = String.format("%02d:%02d", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewTaskSheetBinding.inflate(inflater,container,false)
        return binding.root
    }

    private fun saveAction()
    {
        val name = binding.name.text.toString()
        val desc = binding.desc.text.toString()
        if(taskItem == null)
        {
            val newTask = TaskItem(name, desc, dueTime, -1)
            mainActivity.addTaskItem(newTask)
        }
        else
        {
            taskItem!!.name = name
            taskItem!!.desc = desc
            if (dueTime >= 0) taskItem!!.dueTime = dueTime
            mainActivity.updateTaskItem(taskItem!!)
        }
        binding.name.setText("")
        binding.desc.setText("")
        dismiss()
    }
}