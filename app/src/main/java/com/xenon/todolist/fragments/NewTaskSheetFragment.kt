package com.xenon.todolist.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.TaskItem
import com.xenon.todolist.databinding.FragmentNewTaskSheetBinding
import com.xenon.todolist.viewmodel.TaskItemViewModel
import java.util.Calendar
import java.util.TimeZone

@Suppress("DEPRECATION")
class NewTaskSheetFragment : BottomSheetDialogFragment() {
    companion object {
        private var taskItemViewModel: TaskItemViewModel? = null
        private var taskItem: TaskItem? = null
        fun getInstance(taskItemViewModel: TaskItemViewModel, taskItem: TaskItem?): NewTaskSheetFragment {
            Companion.taskItemViewModel = taskItemViewModel
            Companion.taskItem = taskItem
            return NewTaskSheetFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.XenonFloatingBottomSheetDialogTheme)
    }


    private lateinit var binding: FragmentNewTaskSheetBinding
    private var dueTime: Long = -1
    private var dueDate: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (taskItem != null) {
            binding.taskTitle.text = getString(com.xenon.todolist.R.string.edit_task)
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem!!.name)
            binding.desc.text = editable.newEditable(taskItem!!.desc)
            if (taskItem!!.dueTime >= 0) {
                dueTime = taskItem!!.dueTime
                updateTimeButtonText()
                updateDateButtonText()
            }
        } else {
            binding.taskTitle.text = getString(com.xenon.todolist.R.string.new_task)
        }

        binding.saveButton.setOnClickListener {
            saveAction()
        }
        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }
        binding.datePickerButton.setOnClickListener {
            openDatePicker()
        }
        binding.name.addTextChangedListener { text ->
            binding.saveButton.isEnabled = text.toString().trim().isNotEmpty()
        }
        binding.saveButton.isEnabled = binding.name.text?.isNotEmpty() ?: false
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val screenWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density

        val screenWidthDp = (screenWidth / density).toInt()
        val minMargin =25 //dp
        val maxMargin = 56 // dp

        val dynamicMarginDp = if (screenWidthDp > 640) {
            ((screenWidthDp - 640) / 2).coerceAtLeast(maxMargin)
        } else {
            minMargin
        }

        val layoutParams = binding.cardView.layoutParams as ViewGroup.MarginLayoutParams

        val dynamicMarginPx = (dynamicMarginDp * density).toInt()


        layoutParams.marginStart = dynamicMarginPx
        layoutParams.marginEnd = dynamicMarginPx
        binding.cardView.layoutParams = layoutParams
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (dialog is BottomSheetDialog) {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }


    private fun openTimePicker() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val cal = Calendar.getInstance()
        if (taskItem != null && taskItem!!.dueTime > 0) {
            cal.timeInMillis = taskItem?.dueTime ?: System.currentTimeMillis()
        }

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText(getString(com.xenon.todolist.R.string.task_due))
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()


        picker.addOnPositiveButtonClickListener {
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)

            dueTime = cal.timeInMillis
            updateTimeButtonText()
        }

        picker.show(childFragmentManager, "TAG")
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (taskItem != null && taskItem!!.dueTime > 0) {
            cal.timeInMillis = taskItem?.dueTime ?: System.currentTimeMillis()
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(com.xenon.todolist.R.string.select_date))
            .setSelection(cal.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            cal.timeInMillis = selectedDate
            dueDate = cal.timeInMillis
            updateDateButtonText()
        }

        datePicker.show(childFragmentManager, "DATE_PICKER_TAG")
    }


    private fun updateTimeButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime

        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val formattedTime = timeFormat.format(cal.time)

        binding.timePickerButton.text = formattedTime
    }

    private fun updateDateButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueDate

        val dateFormat = DateFormat.getDateFormat(requireContext())
        val formattedDate = dateFormat.format(cal.time)

        binding.datePickerButton.text = formattedDate
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

        val cal = Calendar.getInstance()
        if (dueDate > 0) {
            cal.timeInMillis = dueDate
        }
        if (dueTime > 0) {
            val timeCal = Calendar.getInstance()
            timeCal.timeInMillis = dueTime
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        }

        val finalDueTime = if (dueDate > 0 || dueTime > 0) cal.timeInMillis else -1

        if (taskItem == null) {
            val newTask = TaskItem(0, name, desc, finalDueTime, System.currentTimeMillis(), -1, ArrayList())
            taskItemViewModel?.add(newTask)
        } else {
            taskItem!!.name = name
            taskItem!!.desc = desc
            Log.d("", "updated to $finalDueTime")
            taskItem!!.dueTime = finalDueTime
            taskItemViewModel?.update(taskItem!!)
        }
        binding.name.setText("")
        binding.desc.setText("")
        dismiss()
    }

}