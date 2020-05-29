package com.example.bricklist

import android.os.AsyncTask
import android.util.Log
import java.net.MalformedURLException

class Project(var projectID: Int?, var projectName: String?, var active: Int?, var lastAccessed: Int?){
    class Brick(var brickID: String?, var quantity: Int?, var quantityInSet: Int?, var type : String?, var colorId: Int?, var extra: Int?){
        var itemName : String? = null
        var itemColor : String? = null
        var photo : ByteArray? = null
        var idFromDB : Int? = null
    }

    var inventoryItems: MutableList<Brick> = mutableListOf()

}