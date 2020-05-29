package com.example.bricklist

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    var archiveChoice :Int? = 0
    lateinit  var tl: TableLayout
    lateinit  var tr: TableRow
    lateinit var DATABASE_PATH: String
    var DATABASE_NAME = "BrickList.db"
    lateinit var db: SQLiteDatabase

    val SETTINGS_REQUEST_CODE=10
    val ADD_REQUEST_CODE=11
    val PROJECT_REQUEST_CODE=12
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fab.setOnClickListener { clickAddProject() }
        DATABASE_PATH = this.getExternalFilesDir(null).toString() + "/"
        initDataBase()
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        if (prefs.getString("database_file", "") == ""){
            prefs.edit().putString("database_file", DATABASE_PATH+DATABASE_NAME).apply()
        }

//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        updateMainScreen()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings){
            clickSettings()
        }
        return true
    }
    fun clickSettings(){
        val intent = Intent(this, SettingsActivity::class.java)
        val dbpath = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("database_file", "")
        intent.putExtra("dbpath", dbpath)
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    fun clickAddProject(){
        val intent = Intent(this, AddActivity::class.java)
        startActivityForResult(intent, ADD_REQUEST_CODE)
    }

    fun clickOpenProject(name : String, id : Int){
        val intent = Intent(this, ProjectActivity::class.java)
        intent.putExtra("setName", name)
        intent.putExtra("setID", id)
        startActivityForResult(intent, PROJECT_REQUEST_CODE)
    }

    fun deleteProject(id: Int){
        val dbpath = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("database_file", "")
        val db = dbpath?.let { SQLiteDatabase.openDatabase(it,null, 0) }
        db?.delete("INVENTORIES", "ID=$id", null)
        db?.delete("INVENTORIESPARTS", "INVENTORYID=$id", null)
        db?.close()
        updateMainScreen()
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    fun updateMainScreen(){
        tl = findViewById(R.id.projectsTable)
        tl.removeAllViews()
        val dbpath = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("database_file", "")
        val showArchived = PreferenceManager.getDefaultSharedPreferences(baseContext).getBoolean("archive", false)
        db = SQLiteDatabase.openDatabase(dbpath!!,null, 0)
        val query = "SELECT * FROM Inventories"
        val cursor = db.rawQuery(query, null)
        while(cursor.moveToNext()) {
            if (cursor.getInt(2) == 1 || showArchived) {
                val id = Integer.parseInt((cursor.getString(0)))
                val name = cursor.getString(1)
                tr = layoutInflater.inflate(R.layout.tablerow, null) as TableRow
                val but: Button = tr.findViewById<Button>(R.id.tableCell1)
                but.text = name
                tr.id = id
                but.setOnClickListener { clickOpenProject(name, id) }
                val delBut: Button = tr.findViewById(R.id.delBut)
                delBut.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete this entry?")
                    builder.setPositiveButton("Yes") { _, _ ->
                        deleteProject(id)
                    }
                    builder.setNegativeButton(android.R.string.no, null)
                    builder.create().show()
                }
                tl.addView(tr)
            }
        }
        cursor.close()
        db.close()
    }

    fun initDataBase() {
        val file = File(DATABASE_PATH, DATABASE_NAME)
        if (!file.exists()) {
            val dbpath = DATABASE_PATH+DATABASE_NAME
            val out = FileOutputStream(dbpath)
            val ins = assets.open(DATABASE_NAME)
            val buf = ByteArray(1024)
            var len: Int = ins.read(buf)
            while (len > 0) {
                out.write(buf, 0, len)
                len = ins.read(buf)
            }
            ins.close()
            out.flush()
            out.close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(((requestCode==ADD_REQUEST_CODE)
            && (resultCode == Activity.RESULT_OK)) or
            ((requestCode==SETTINGS_REQUEST_CODE)
                    && (resultCode == Activity.RESULT_OK)) or
            ((requestCode==PROJECT_REQUEST_CODE)
                    && (resultCode == Activity.RESULT_OK))){
            updateMainScreen()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
