package com.xenon.todolist.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xenon.todolist.SharedPreferenceManager
import com.xenon.todolist.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val themeTitleList = arrayOf("Light", "Dark", "System")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adjustBottomMargin(binding.layoutMain)
        setupViews()

        val themeSelectionValue = findViewById<TextView>(com.xenon.todolist.R.id.theme_selection_value)

        val sharedPreferenceManager = SharedPreferenceManager(this)
        var checkedTheme = sharedPreferenceManager.theme
        themeSelectionValue.text = themeTitleList[sharedPreferenceManager.theme]

        val themeDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Select Theme")
            .setPositiveButton("OK") { _, _ ->
                sharedPreferenceManager.theme = checkedTheme
                AppCompatDelegate.setDefaultNightMode(sharedPreferenceManager.themeFlag[checkedTheme])
                themeSelectionValue.text = themeTitleList[checkedTheme]
            }
            .setSingleChoiceItems(themeTitleList, checkedTheme) { _, which ->
                checkedTheme = which
            }
            .setCancelable(false)

        binding.themeSelectionHolder.setOnClickListener {
            themeDialog.show()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        binding.languageSelectionValue.text = Locale.getDefault().displayLanguage
        binding.languageSelectionHolder.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }
}
