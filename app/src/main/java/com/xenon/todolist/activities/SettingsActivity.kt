package com.xenon.todolist.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import com.xenon.todolist.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adjustBottomMargin(binding.layoutMain)
        setupViews()
    }

    private fun setupViews() {
        binding.languageSelectionValue.text = Locale.getDefault().displayLanguage
        binding.languageSelectionHolder.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
            }
            else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }
}