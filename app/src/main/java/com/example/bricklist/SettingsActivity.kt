package com.example.bricklist

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import java.io.File


class SettingsActivity : AppCompatActivity() {

    var prefListener : OnSharedPreferenceChangeListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbpath = intent.extras?.getString("dbpath")
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        Log.i("test", "t1")
        prefListener = OnSharedPreferenceChangeListener{ sharedPreferences: SharedPreferences, key: String ->
            Log.i("test", "i")
            if (key == "database_file") {
                Log.i("test", "data")
                if (!File(sharedPreferences.getString(key, "")!!).exists()) {
                    Snackbar.make(window.decorView.findViewById(android.R.id.content), "File doesn't exist! Returning to default.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                    sharedPreferences.edit()
                        .putString("database_file", dbpath).apply()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.settings, SettingsFragment()).commit()
                    registerListener()
                }
            }
        }
        PreferenceManager.getDefaultSharedPreferences(baseContext).registerOnSharedPreferenceChangeListener(prefListener)

    }

    fun registerListener(){
        PreferenceManager.getDefaultSharedPreferences(baseContext).registerOnSharedPreferenceChangeListener(prefListener)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }


    override fun finish() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)
        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == android.R.id.home) {
            finish()
            true
        } else
            super.onOptionsItemSelected(item)
    }

}