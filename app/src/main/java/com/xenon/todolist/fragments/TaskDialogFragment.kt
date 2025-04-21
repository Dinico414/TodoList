package com.xenon.todolist.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.xenon.todolist.R
import com.xenon.todolist.TaskItem
import com.xenon.todolist.databinding.FragmentTaskDialogBinding
import com.xenon.todolist.viewmodel.TaskItemViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

@Suppress("DEPRECATION")
class TaskDialogFragment : DialogFragment() {
    companion object {
        private lateinit var taskItemViewModel: TaskItemViewModel
        private lateinit var taskItem: TaskItem
        private var newTask = false

        fun getInstance(taskItemViewModel: TaskItemViewModel, taskItem: TaskItem?): TaskDialogFragment {
            Companion.taskItemViewModel = taskItemViewModel
            newTask = taskItem == null
            val curTime = Instant.now().toEpochMilli()
            Companion.taskItem = taskItem ?: TaskItem(0, "", "", -1, -1, curTime, -1, ArrayList())
            return TaskDialogFragment()
        }
    }

    private lateinit var binding: FragmentTaskDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentTaskDialogBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        if (!newTask) {
            binding.taskTitle.text = getString(R.string.edit_task)
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem.name)
            binding.desc.text = editable.newEditable(taskItem.desc)
            if (taskItem.dueTime >= 0) {
                updateTimeButtonText()
                updateDateButtonText()
            }
        } else {
            binding.taskTitle.text = getString(R.string.new_task)
        }

        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }
        binding.datePickerButton.setOnClickListener {
            openDatePicker()
        }

        binding.name.addTextChangedListener { text ->
            binding.saveButton.isEnabled = text.toString().trim().isNotEmpty()
            taskItem.name = text.toString()
        }

        binding.desc.addTextChangedListener { text ->
            taskItem.desc = text.toString()
        }

        binding.saveButton.setOnClickListener {
            dismiss()
        }
        binding.saveButton.isEnabled = binding.name.text.toString().trim().isNotEmpty()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        saveAction()
    }

    private fun openTimePicker() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        var dueTime = if(taskItem.dueTime > 0) taskItem.dueTime else Instant.now().toEpochMilli()
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.task_due))
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            cal.timeInMillis = 0
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)

            taskItem.dueTime = cal.timeInMillis
            updateTimeButtonText()
        }

        picker.show(childFragmentManager, "TAG")
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        var dueTime = if(taskItem.dueDate > 0) taskItem.dueDate else MaterialDatePicker.todayInUtcMilliseconds()
        cal.timeInMillis = dueTime

        val constraintsBuilder = CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraintsBuilder.build())
            .setTitleText(getString(R.string.select_date))
            .setSelection(cal.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val time = Instant.ofEpochMilli(dueTime).atZone(ZoneId.systemDefault()).toLocalTime()
            val date = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault()).toLocalDate()
            taskItem.dueDate = time.atDate(date).toInstant(OffsetDateTime.now().offset).toEpochMilli()
            updateDateButtonText()
        }

        datePicker.show(childFragmentManager, "DATE_PICKER_TAG")
    }

    private fun updateTimeButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = taskItem.dueTime

        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val formattedTime = timeFormat.format(cal.time)

        binding.timePickerButton.text = formattedTime
    }

    private fun updateDateButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = taskItem.dueDate

        val dateFormat = DateFormat.getDateFormat(requireContext())
        val formattedDate = dateFormat.format(cal.time)

        binding.datePickerButton.text = formattedDate
    }

    private fun saveAction() {
        val name = binding.name.text.toString().trim()
        val desc = binding.desc.text.toString().trim()

        if (name.isNotEmpty()) {
            taskItem.name = name
        }
        taskItem.desc = desc

        if (!newTask) {
            taskItemViewModel.update(taskItem)
        } else if (name.isNotEmpty()) {
            taskItemViewModel.add(taskItem)
        }
    }
}