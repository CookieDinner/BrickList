package com.example.bricklist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_add.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class AddActivity : AppCompatActivity() {

    var db : SQLiteDatabase? = null
    var lastCorrectId: String = ""
    var xml : String = ""
    var dbpath : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        dbpath = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("database_file", "")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addButton.isEnabled = false
        loadDb()
        idText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(editable: Editable?) { addButton.isEnabled = false }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        idText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                idText.removeTextChangedListener(this)
                val regex = """[^0-9]""".toRegex()
                val matched = regex.containsMatchIn(input = editable.toString())
                when {
                    matched -> {
                        idText.setText(lastCorrectId)
                        idText.setSelection(idText.text.toString().length)
                    }
                    editable.toString().length > 6 -> {
                        idText.setText(lastCorrectId)
                        idText.setSelection(idText.text.toString().length)
                    }
                    editable.toString().isNotEmpty() -> {
                        lastCorrectId = editable.toString()
                        idText.setSelection(idText.text.toString().length)
                    }
                }
                idText.addTextChangedListener(this) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun finish() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)
        super.finish()
    }

    fun onClickCheck(v: View){
        if (idText.text.isNotEmpty() and nameText.text.isNotEmpty()) {
            if(checkQuery("SELECT * FROM Inventories WHERE id=${idText.text} OR Name='${nameText.text}'")){
                Log.i("test", "starting download")
                DownloadXML(baseContext).execute(idText.text.toString())
                //Próbowałem dodać delay/sprawdzanie czy wciąż jest pobierane ale wątek ui nie poznalał
                addButton.isEnabled = true
            }
        }
        else
            Snackbar.make(lay, "Fields can't be empty!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
    }

    fun onClickAdd(v: View){
        val values = ContentValues()
        val projectId = idText.text.toString()
        values.put("ID", projectId)
        values.put("NAME", nameText.text.toString())
        values.put("ACTIVE", 1)
        values.put("LASTACCESSED", 0)
        db?.insert("INVENTORIES", null, values)

        val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            InputSource(StringReader(xml)))
        xmlDoc.documentElement.normalize()
        val bricks: NodeList = xmlDoc.getElementsByTagName("ITEM")
        for(i in 0 until bricks.length){
            val brickNode: Node = bricks.item(i)
            if(brickNode.nodeType == Node.ELEMENT_NODE){
                val elem = brickNode as Element
                val children = elem.childNodes

                var currentType:String? = null
                var currentItemID:String? = null
                var currentQuantity:Int? = null
                var currentColor:Int? = null
                var currentExtra:String? = null
                var currentAlternate:String? = null
                for (j in 0 until children.length - 1){
                    val node = children.item(j)
                    if(node is Element){
                        when(node.nodeName){
                            "ITEMTYPE" -> currentType = node.textContent
                            "ITEMID" -> currentItemID = node.textContent
                            "QTY" -> currentQuantity = node.textContent.toInt()
                            "COLOR" -> currentColor = node.textContent.toInt()
                            "EXTRA" -> currentExtra = node.textContent
                            "ALTERNATE" -> currentAlternate = node.textContent
                        }
                    }
                }
                if(currentAlternate == "N" && currentType != "M"){
                    val brickValues = ContentValues()
                    val extraid = runQueryForResult("SELECT MAX(id) FROM INVENTORIESPARTS", 0)
                    if (extraid != null)
                        brickValues.put("ID", 1+extraid.toInt())
                    else
                        brickValues.put("ID", i)
                    brickValues.put("INVENTORYID", projectId)
                    brickValues.put("TYPEID", runQueryForResult(
                        "SELECT * FROM ItemTypes WHERE Code='$currentType'",0))
                    brickValues.put("ITEMID", currentItemID)
                    brickValues.put("QUANTITYINSET", currentQuantity)
                    brickValues.put("QUANTITYINSTORE", 0)
                    brickValues.put("COLORID", currentColor)
                    brickValues.put("EXTRA", currentExtra)
                    db?.insert("INVENTORIESPARTS", null, brickValues)
                    val tItemID: String? = runQueryForResult("select id from Parts where Code='$currentItemID'", 0)
                    val tColorID: String? = runQueryForResult("select id from Colors where Code=$currentColor", 0)
                    val code = runQueryForResult("SELECT CODE FROM CODES WHERE ITEMID=$tItemID AND COLORID=$tColorID", 0)

                    if(checkIfImageExists(tItemID, tColorID, db)) {
                        if (code != null)
                            DownloadImage(code).execute(
                                "https://www.lego.com/service/bricks/5/2/$code",
                                tItemID,
                                tColorID
                            )
                        DownloadImage(code).execute(
                            "http://img.bricklink.com/P/$currentColor/$currentItemID.gif",
                            tItemID,
                            tColorID
                        )
                        DownloadImage(code).execute(
                            "https://www.bricklink.com/PL/$currentItemID.jpg",
                            tItemID,
                            tColorID
                        )
                    }
                }
            }
        }
        db?.close()
        finish()
    }

    fun checkQuery(query: String): Boolean {
        val cursor = db?.rawQuery(query, null)
        if (cursor != null) {
            if(cursor.moveToFirst()){
                Snackbar.make(lay, "Project already exists!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
                cursor.close()
                return false
            }
            cursor.close()
            return true
        }
        return false
    }

    fun runQueryForResult(query: String, column: Int): String? {
        val cursor = db?.rawQuery(query, null)
        var result : String? = null
        if (cursor != null) {
            if (cursor.moveToFirst()){
                result = cursor.getString(column)
            }
        }
        cursor?.close()
        return result
    }
    fun loadDb(){
        db = dbpath?.let { SQLiteDatabase.openDatabase(it,null, 0) }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == android.R.id.home) {
            finish()
            true
        } else
            super.onOptionsItemSelected(item)
    }

    @SuppressLint("StaticFieldLeak")
    private inner class DownloadXML(val baseContext: Context) : AsyncTask<String, Int, String>(){

        override fun doInBackground(vararg params: String?): String {
            try {
                val urlText = PreferenceManager.getDefaultSharedPreferences(baseContext)
                    .getString("prefix","")
                Log.i("test", "URL: $urlText${params[0]}.xml")
                val url = URL(urlText+params[0]+".xml")
                Log.i("test", "1")
                val connection = url.openConnection()
                Log.i("test", "2")
                connection.connect()
                Log.i("test", "3")

                val lengthOfFile = connection.contentLength
                val isStream = url.openStream()
                val data = ByteArray(1024)
                var total: Long = 0
                var progress = 0
                var finXML = ""

                var count = isStream.read(data)
                while (count != -1){
                    total += count.toLong()
                    val progress_temp = total.toInt() * 100 / lengthOfFile
                    if (progress_temp % 10 == 0 && progress != progress_temp)
                        progress = progress_temp
                    finXML += String(data.copyOfRange(0, count))
                    count = isStream.read(data)
                }
                isStream.close()
                xml = finXML
            } catch (e : MalformedURLException) {
                e.printStackTrace()
                return "Malformed URL"
            }catch (e : IOException){
                e.printStackTrace()
                return "IO Exception"
            }
            Log.i("test", "download finished")
            return "Success"
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class DownloadImage(val code : String?) : AsyncTask<String, Int, String>(){

        val tempdb = dbpath?.let { SQLiteDatabase.openDatabase(it,null, 0) }
        override fun doInBackground(vararg params: String?): String {
            try {
                val urlText = params[0]
                Log.i("test", "URL: $urlText")
                val url = URL(urlText)
                Log.i("test", "1")
                val connection = url.openConnection()
                Log.i("test", "2")
                connection.connect()
                Log.i("test", "3")

                val lengthOfFile = connection.contentLength
                val isStream = url.openStream()
                val data = ByteArray(1024)
                var total: Long = 0
                var progress = 0
                var finImage = ArrayList<Byte>()

                var count = isStream.read(data)
                while (count != -1){
                    total += count.toLong()
                    val progress_temp = total.toInt() * 100 / lengthOfFile
                    if (progress_temp % 10 == 0 && progress != progress_temp)
                        progress = progress_temp
                    finImage.addAll(data.copyOfRange(0, count).toList())
                    count = isStream.read(data)
                }
                isStream.close()
                if(checkIfImageExists(params[1], params[2], tempdb)) {
                    if (code != null)
                        saveImageToDb(finImage, params[1], params[2], tempdb)
                    else
                        saveImageToDbButWithMoreFunctionalityThatFixesStuff(
                            finImage,
                            params[1],
                            params[2],
                            tempdb
                        )
                }
            } catch (e : Exception) {
                e.printStackTrace()
                return "Exception"
            }
            Log.i("test", "download finished")
            return "Success"
        }
    }
    fun saveImageToDb(img: ArrayList<Byte>, itemID: String?, colorID: String?, tempdb: SQLiteDatabase?){
        val values = ContentValues().apply {
            put("Image", img.toByteArray())
        }
        tempdb?.update("Codes", values, "ITEMID= ? AND COLORID= ?", arrayOf(itemID,colorID))
    }
    fun saveImageToDbButWithMoreFunctionalityThatFixesStuff(img: ArrayList<Byte>, itemID: String?, colorID: String?, tempdb: SQLiteDatabase?){
        val values = ContentValues()
        values.put("ItemID", itemID)
        values.put("ColorID", colorID)
        values.put("Image", img.toByteArray())
        tempdb?.insert("Codes", null, values)
    }
    fun checkIfImageExists(itemID: String?, colorID: String?, tempdb : SQLiteDatabase?) : Boolean{
        val query = "select Image from Codes where ItemID=$itemID and ColorID=$colorID"
        val cursor = tempdb?.rawQuery(query, null)
        if (cursor != null) {
            if(cursor.moveToFirst()){
                val img = cursor.getBlob(0)
                cursor.close()
                return img == null
            }
        }
        return true
    }
}
