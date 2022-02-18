package com.zpc.qqlogin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tencent.connect.UserInfo
import com.tencent.connect.common.Constants
import com.tencent.mmkv.MMKV
import com.tencent.tauth.DefaultUiListener
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import org.json.JSONObject
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class BaseLoginActivity:AppCompatActivity() {

    val APP_ID="101992433"
    val PROVIDER_AUTHORITIES="com.tencent.login.myfileprovider"
    lateinit var mUiListener:IUiListener
    lateinit var context: Context;
    lateinit var mTencent: Tencent;

    lateinit var tokenTool:ITokenStorage
    lateinit var userTool:ITokenStorage
    lateinit var mmkv: MMKV;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context=this;

        //重要
        Tencent.setIsPermissionGranted(true,Build.MODEL);
        Tencent.resetTargetAppInfoCache()
        Tencent.resetQQAppInfoCache()
        Tencent.resetTimAppInfoCache()

        mTencent = Tencent.createInstance(APP_ID, applicationContext, PROVIDER_AUTHORITIES)
        LogUtil.d("qq install: ${mTencent.isQQInstalled(this)}")
        //初始化token存储工具
        MMKV.initialize(this)
        mmkv=MMKV.defaultMMKV();
        tokenTool= TokenStorage(mmkv)
        userTool= UserStorage(mmkv)
        //检查缓存
        checkHasQQLoginCache()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Tencent.onActivityResultData(requestCode, resultCode, data,mUiListener)
        if (requestCode == Constants.REQUEST_API) {
            if (resultCode == Constants.REQUEST_LOGIN) {
                Tencent.handleResultData(data, mUiListener)
            }
        }

    }

    //check cache
    private fun checkHasQQLoginCache(){
        val token = tokenTool.load()
        token?.apply {
            val gson = Gson()
            val token1 = gson.fromJson(this, QQLogin::class.java)
            mTencent.setAccessToken(token1.access_token,token1.expires_in.toString())
            mTencent.openId=token1.openid
        }
    }

    protected suspend fun checkQQLoginValid():QQResult= suspendCoroutine {

        mTencent.checkLogin(object : DefaultUiListener() {
            override fun onComplete(response: Any?) {
                if(response==null){
                    it.resume(QQResult(-1,"check login response is null"))
                    return
                }
                val jsonResp = response as JSONObject

                if(jsonResp.optInt("ret", -1)==0){
                    val jsonObject: String? = tokenTool.load()
                    if (jsonObject == null) {
                        //登录失败
                        it.resume(QQResult(-1,"token is null"))
                    } else {
                        //已经登录
                        it.resume(QQResult(0,"success"))
                    }
                }else {
                    //登录已过期，请重新登录
                    it.resume(QQResult(-1,"login expired"))
                }
            }

            override fun onError(p0: UiError?) {
                //登录已过期，请重新登录
                it.resume(QQResult(-1,p0?.errorMessage?:"error"))
            }

            override fun onCancel() {
                //取消登录
                it.resume(QQResult(-1,"cancel"))
            }

        })
    }

    inner class MyUiListener(val trigger:Continuation<QQLoginResult>):IUiListener{
        override fun onComplete(response: Any?) {
            if (response == null) {
                trigger.resume(QQLoginResult(-1,null,"login response is null"))
                return
            }
            val jsonResponse = response as JSONObject
            if (jsonResponse.length() == 0) {
                trigger.resume(QQLoginResult(-1,null,"login response is null"))
                return
            }

            tokenTool.save(response.toString())
            //登陆成功
            checkHasQQLoginCache()
            //获取信息
            getQQInfo(context)
        }

        override fun onError(p0: UiError?) {
            trigger.resume(QQLoginResult(-1,null,p0?.errorMessage?:"error"))
        }

        override fun onCancel() {
            trigger.resume(QQLoginResult(-1,null,"cancel"))
        }

        override fun onWarning(p0: Int) {
            trigger.resume(QQLoginResult(-1,null,"warning $p0"))
        }

        private fun getQQInfo(context: Context){
            val qqToken = mTencent.qqToken
            val info = UserInfo(context,qqToken)
            info.getUserInfo(object :IUiListener{
                override fun onComplete(response: Any?){

                    if(response!=null){
                        //保存信息
                        userTool.save(response.toString())
                        //
                        val gson = Gson()
                        val user = gson.fromJson(response.toString(), QQInfo::class.java)
                        trigger.resume(QQLoginResult(0,user,"success"))
                    }else{
                        trigger.resume(QQLoginResult(-1,null,"get user information fail"))
                    }
                }

                override fun onError(p0: UiError?) {
                    trigger.resume(QQLoginResult(-1,null,p0?.errorMessage?:"error"))
                }

                override fun onCancel() {
                    trigger.resume(QQLoginResult(-1,null,"getQQInfo cancel"))
                }

                override fun onWarning(p0: Int) {
                    trigger.resume(QQLoginResult(-1,null,"getQQInfo warning $p0"))
                }
            })
        }

    }

    protected suspend fun startLoginQQ():QQLoginResult= suspendCoroutine {
        if(mTencent.isSessionValid){
            it.resume(QQLoginResult(-1,null,"session valid"))
        }else{
            mUiListener=MyUiListener(it)
            val login = mTencent.login(this, "all", mUiListener)
            when(login){
                0 ->{
                    //正常登录
                }

                1 -> {
                    //开始登录
                }

                -1 -> {
                    //it.resume(QQLoginResult(-1,null,"login error -1"))
                }

                2 -> {
                    //it.resume(QQLoginResult(-2,null,"need download QQ"))
                }

                else -> {
                    //it.resume(QQLoginResult(-1,null,"login error $login"))
                }
            }
        }

    }

    protected fun getLocalUserInfo():QQInfo?{
        val load = userTool.load()

        return load?.let {
            return Gson().fromJson(load,QQInfo::class.java)
        }
    }

    protected fun logoutQQ(){
        mTencent.logout(this)
        tokenTool.remove()
        userTool.remove()
    }
}