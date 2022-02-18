package com.zpc.qqdemo

import android.os.Bundle

import com.zpc.qqdemo.databinding.ActivityMainBinding
import com.zpc.qqlogin.BaseLoginActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 参考：https://mp.weixin.qq.com/s/5y7dAmKir4usPC2bMr4jAA
 */
class MainActivity : BaseLoginActivity() {



    lateinit var binding: ActivityMainBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater);
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener {
            logout()
        }
        login()
    }

    fun login(){
        GlobalScope.launch {
            val ret = checkQQLoginValid()
            LogUtil.d("current state: ${ret.toString()}")
            if(ret.code==0){
                //已经登录，之后获取用户信息
                LogUtil.d("already login!")
                val localUserInfo = getLocalUserInfo()
                localUserInfo?.let {
                    LogUtil.d("current user: ${it.toString()}")
                }
            }else{
                //非登录状态
                LogUtil.d("start login!")
                val result = startLoginQQ()
                LogUtil.d("login result: ${result.toString()}")
            }
        }
    }


    fun logout(){
        logoutQQ()
    }




}
