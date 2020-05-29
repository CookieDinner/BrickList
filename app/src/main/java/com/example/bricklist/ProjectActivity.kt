package com.example.bricklist

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_project.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ProjectActivity : AppCompatActivity() {

    lateinit  var tl: TableLayout
    lateinit  var tr: TableRow
    var db: SQLiteDatabase? = null
    var projectname : String? = null
    var projectID : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        projectname = intent.extras?.getString("setName")
        projectID = intent.extras?.getInt("setID")
        val dbpath = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("database_file", "")
        db = dbpath?.let { SQLiteDatabase.openDatabase(it,null, 0) }
        supportActionBar?.title = "$projectname - Set nr. $projectID"
        updateScreen()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.refresh, menu)
        return true
    }

    override fun finish() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)
        super.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            (item.itemId == android.R.id.home) -> {
                finish()
                true
            }
            (item.itemId == R.id.action_refresh) -> {
                updateScreen()
                true
            }
            (item.itemId == R.id.action_export) -> {
                exportToXML()
                true
            }
                else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    fun updateScreen(){
        if (db != null) {
            val query = "SELECT * FROM Inventories WHERE Name='$projectname'"
            val cursor = db?.rawQuery(query, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    tl = findViewById(R.id.bricksTable)
                    tl.removeAllViews()
                    val query2 = "SELECT * FROM INVENTORIESPARTS WHERE INVENTORYID=$projectID"
                    val cursor2 = db?.rawQuery(query2, null)
                    if (cursor2 != null) {
                        while (cursor2.moveToNext()) {
                            try {
                                tr = layoutInflater.inflate(R.layout.brickrow, null) as TableRow
                                val descriptionText: TextView =
                                    tr.findViewById<TextView>(R.id.brickDescription)
                                val brickQty: TextView = tr.findViewById<TextView>(R.id.brickQty)
                                //template: name\ncolor[id]\nqty_cur of qty_total
                                //name - Parts - identified by CODE, name in NAME
                                //color - Colors - identified by CODE, color in NAME
                                val brickID = cursor2.getString(3)
                                val colorID = cursor2.getInt(6)
                                val qtyStored = cursor2.getInt(5)
                                val qtySet = cursor2.getInt(4)
                                val colorName =
                                    runQueryForResult("SELECT * FROM COLORS WHERE CODE=$colorID", 2)
                                var brickName = runQueryForResult(
                                    "SELECT * FROM PARTS WHERE CODE='$brickID'",
                                    3
                                )
                                if (brickName != null)
                                    if (brickName.length >= 40)
                                        brickName = brickName.substring(0, 40) + "..."
                                descriptionText.text = "$brickName\n$colorName[$brickID]"
                                descriptionText.requestLayout()
                                brickQty.text = "$qtyStored of $qtySet"
                                if (qtyStored == qtySet)
                                    ((brickQty.parent as ViewGroup).parent as ViewGroup).setBackgroundColor(Color.parseColor("#adffc3"))
                                val tItemID: String? = runQueryForResult("select id from Parts where Code='$brickID'", 0)
                                val tColorID: String? = runQueryForResult("select id from Colors where Code=$colorID", 0)
                                val img = getImageFromDb(tItemID, tColorID)
                                val brickImg: ImageView =
                                    tr.findViewById<ImageView>(R.id.brickImage)
                                if (img != null) {
                                    val bmp = BitmapFactory.decodeByteArray(img, 0, img.size)
                                    if (bmp != null)
                                        brickImg.setImageBitmap(
                                            Bitmap.createScaledBitmap(bmp, 120, 120, false))
                                }
                                val addButton: Button = tr.findViewById<Button>(R.id.addBrickButton)
                                val remButton: Button = tr.findViewById<Button>(R.id.removeBrickButton)
                                addButton.setOnClickListener {
                                    var curQty = runQueryForResult(
                                        "SELECT * FROM INVENTORIESPARTS WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID",
                                        5
                                    )?.toInt()
                                    val maxQty = runQueryForResult(
                                        "SELECT * FROM INVENTORIESPARTS WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID",
                                        4
                                    )?.toInt()
                                    if (curQty != null && maxQty != null) {
                                        curQty += 1
                                        if(curQty <= maxQty) {
                                            db?.execSQL("UPDATE INVENTORIESPARTS SET QUANTITYINSTORE=$curQty WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID")
                                            val textview = ((((remButton.parent as ViewGroup).parent as ViewGroup)
                                                .getChildAt(1) as ViewGroup)
                                                .getChildAt(1) as TextView)
                                            textview.text = "$curQty of $maxQty"
                                            if (curQty == maxQty)
                                                ((textview.parent as ViewGroup).parent as ViewGroup).setBackgroundColor(Color.parseColor("#adffc3"))
                                        }
                                    }
                                }
                                remButton.setOnClickListener {
                                    var curQty = runQueryForResult(
                                        "SELECT * FROM INVENTORIESPARTS WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID",
                                        5
                                    )?.toInt()
                                    val maxQty = runQueryForResult(
                                        "SELECT * FROM INVENTORIESPARTS WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID",
                                        4
                                    )?.toInt()
                                    curQty = curQty?.minus(1)
                                    if (curQty!! >= 0) {
                                        db?.execSQL("UPDATE INVENTORIESPARTS SET QUANTITYINSTORE=$curQty WHERE INVENTORYID=$projectID AND ITEMID='$brickID' AND COLORID=$colorID")
                                        val textview = ((((remButton.parent as ViewGroup).parent as ViewGroup)
                                            .getChildAt(1) as ViewGroup)
                                            .getChildAt(1) as TextView)
                                        textview.text = "$curQty of $maxQty"
                                        if (curQty != maxQty)
                                            ((textview.parent as ViewGroup).parent as ViewGroup).setBackgroundColor(Color.parseColor("#FFFFFF"))
                                    }
                                }
                                tl.addView(tr)
                            } catch (e: SQLiteException) {

                            }
                        }
                    }
                    cursor2?.close()
                }
            }
            cursor?.close()
        }
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

    fun getImageFromDb(itemID : String?, colorID : String?) : ByteArray?{
        val query = "SELECT IMAGE FROM CODES WHERE ITEMID=$itemID AND COLORID=$colorID"
        val cursor = db?.rawQuery(query, null)
        var result : ByteArray? = null
        if (cursor != null) {
            if (cursor.moveToFirst()){
                result = cursor.getBlob(0)
            }
        }
        cursor?.close()
        return result
    }
    fun exportToXML() {
        val docBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()

        val rootElement: Element = doc.createElement("INVENTORY")

        val query = "select INVENTORYID, Code, ItemID, COLORID, QuantityInSet-QuantityInStore " +
                "from InventoriesParts LEFT JOIN ItemTypes ON ItemTypes.id = InventoriesParts.typeid " +
                "where InventoryID=$projectID"
        val cursor = db?.rawQuery(query, null)
        if (cursor != null) {
            Log.i("test", "starting")
            while(cursor.moveToNext()){
                if(cursor.getInt(4) > 0) {
                    val item = doc.createElement("ITEM")
                    val itemType = doc.createElement("ITEMTYPE")
                    val itemID = doc.createElement("ITEMID")
                    val color = doc.createElement("COLOR")
                    val qtyFilled = doc.createElement("QTYFILLED")

                    itemType.appendChild(doc.createTextNode(cursor.getString(1)))
                    itemID.appendChild(doc.createTextNode(cursor.getString(2)))
                    color.appendChild(doc.createTextNode(cursor.getString(3)))
                    qtyFilled.appendChild(doc.createTextNode(cursor.getString(4)))

                    item.appendChild(itemType)
                    item.appendChild(itemID)
                    item.appendChild(color)
                    item.appendChild(qtyFilled)

                    rootElement.appendChild(item)
                }
            }
            //doc.appendChild(rootElement)
            cursor.close()
            val transformer: Transformer = TransformerFactory.newInstance().newTransformer()

            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

            val path= this.getExternalFilesDir(null)
            val outDir = File(path, "XMLS")
            outDir.mkdir()

            val file = File(outDir, "$projectID-$projectname.xml")

            transformer.transform(DOMSource(rootElement), StreamResult(file))
        }
        val values = ContentValues().apply {
            put("Active", 0)
        }
        db?.update("INVENTORIES", values, "ID= ?", arrayOf(projectID.toString()))
        Snackbar.make(projectLayout, "Saved the xml file to ${this.getExternalFilesDir(null).toString()}/XMLS/$projectID-$projectname.xml",
            Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }
}
