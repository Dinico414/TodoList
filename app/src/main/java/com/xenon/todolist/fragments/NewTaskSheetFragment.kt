package com.xenon.todolist.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.TaskItem
import com.xenon.todolist.databinding.FragmentNewTaskSheetBinding
import com.xenon.todolist.viewmodel.TaskItemViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

@Suppress("DEPRECATION")
class NewTaskSheetFragment : BottomSheetDialogFragment() {
    companion object {
        private lateinit var taskItemViewModel: TaskItemViewModel
        private lateinit var taskItem: TaskItem
        private var newTask = false

        fun getInstance(taskItemViewModel: TaskItemViewModel, taskItem: TaskItem?): NewTaskSheetFragment {
            Companion.taskItemViewModel = taskItemViewModel
            newTask = taskItem == null
            val curTime = System.currentTimeMillis()
            Companion.taskItem = taskItem ?: TaskItem(0, "", "", curTime, curTime, -1, ArrayList())
            return NewTaskSheetFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.XenonFloatingBottomSheetDialogTheme)
    }


    private lateinit var binding: FragmentNewTaskSheetBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!newTask) {
            binding.taskTitle.text = getString(com.xenon.todolist.R.string.edit_task)
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem.name)
            binding.desc.text = editable.newEditable(taskItem.desc)
            if (taskItem.dueTime >= 0) {
                updateTimeButtonText()
                updateDateButtonText()
            }
        } else {
            binding.taskTitle.text = getString(com.xenon.todolist.R.string.new_task)
        }

        binding.saveButton.setOnClickListener {
            dismiss()
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
        binding.saveButton.isEnabled = binding.name.text?.isNotEmpty() == true
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
        cal.timeInMillis = taskItem.dueTime

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

            taskItem.dueTime = cal.timeInMillis
            updateTimeButtonText()
        }

        picker.show(childFragmentManager, "TAG")
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = taskItem.dueTime

        val constraintsBuilder = CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraintsBuilder.build())
            .setTitleText(getString(com.xenon.todolist.R.string.select_date))
            .setSelection(cal.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val time = Instant.ofEpochMilli(taskItem.dueTime).atZone(ZoneId.of("UTC")).toLocalTime()
            val date = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.of("UTC")).toLocalDate()
            taskItem.dueTime = time.atDate(date).toInstant(OffsetDateTime.now().offset).toEpochMilli()
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
        cal.timeInMillis = taskItem.dueTime

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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        saveAction()
    }

    private fun saveAction() {
        val name = binding.name.text.toString()
        val desc = binding.desc.text.toString()

        if (newTask) {
            taskItemViewModel.add(taskItem)
        } else {
            taskItem.name = name
            taskItem.desc = desc
            taskItemViewModel.update(taskItem)
        }
        binding.name.setText("")
        binding.desc.setText("")
    }
}