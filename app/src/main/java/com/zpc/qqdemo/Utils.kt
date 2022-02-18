package com.zpc.qqdemo

import android.content.Context
import android.util.Log
import android.widget.Toast


object LogUtil{
    fun d(message:String){
        Log.d("QAQ",message)
    }
}

fun String.showToast(context:Context){
    Toast.makeText(context,this, Toast.LENGTH_SHORT).show()
}