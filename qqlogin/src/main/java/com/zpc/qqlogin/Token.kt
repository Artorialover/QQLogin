package com.zpc.qqlogin

import android.content.Context
import com.tencent.mmkv.MMKV

class TokenStorage(val mmkv:MMKV): ITokenStorage {

    override fun save(token:String):Boolean {
        return mmkv.encode("qq_login",token)
    }

    override fun load():String? {
        return mmkv.decodeString("qq_login")
    }

    override fun remove() {
        mmkv.removeValueForKey("qq_login")
    }
}

class UserStorage(val mmkv:MMKV): ITokenStorage {

    override fun save(token:String):Boolean {
        return mmkv.encode("qq_user",token)
    }

    override fun load():String? {
        return mmkv.decodeString("qq_user")
    }

    override fun remove() {
        mmkv.removeValueForKey("qq_user")
    }
}

interface ITokenStorage{
    fun save(token:String):Boolean
    fun load():String?

    fun remove()
}