package com.xenon.todolist

import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.xenon.todolist.databinding.FragmentNewTaskSheetBinding
import java.util.Calendar

class NewTaskSheet(private var mainActivity: MainActivity, private var taskItem: TaskItem?) :
    BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FloatingBottomSheetDialogTheme)
    }


    private lateinit var binding: FragmentNewTaskSheetBinding
    private var dueTime: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (taskItem != null) {
            binding.taskTitle.text = getString(R.string.edit_task)
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem!!.name)
            binding.desc.text = editable.newEditable(taskItem!!.desc)
            if (taskItem!!.dueTime >= 0) {
                dueTime = taskItem!!.dueTime
                updateTimeButtonText()
            }
        } else {
            binding.taskTitle.text = getString(R.string.new_task)
        }

        binding.saveButton.setOnClickListener {
            saveAction()
        }
        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }
    }

    private fun openTimePicker() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val cal = Calendar.getInstance()

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.task_due))
            .build()

        picker.addOnPositiveButtonClickListener {
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)

            if (cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_WEEK, 1)
            }

            dueTime = cal.timeInMillis
            updateTimeButtonText()
        }

        picker.show(childFragmentManager, "TAG")
    }


    private fun updateTimeButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime

        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val formattedTime = timeFormat.format(cal.time)

        binding.timePickerButton.text = formattedTime
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun saveAction() {
        val name = binding.name.text.toString()
        val desc = binding.desc.text.toString()
        if (taskItem == null) {
            val newTask = TaskItem(name, desc, dueTime, -1)
            mainActivity.addTaskItem(newTask)
        } else {
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