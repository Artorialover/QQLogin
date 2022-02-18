# QQLogin
接入官方SDK,实现QQ账号登录的功能(分享等其他功能未实现)

### 官方文档
https://wiki.connect.qq.com/qq%e7%99%bb%e5%bd%95

### 参考文章
[Android QQ 登录接入详细介绍](https://mp.weixin.qq.com/s/5y7dAmKir4usPC2bMr4jAA)

### 遇到的坑
1.官方文档太差，所以参考别的文章

2.在Android 11及以上，使用Tencent.isQQInstalled()会返回false,原因是读取安装应用列表被限制，解决方法参考[Android 上的软件包可见性过滤](https://developer.android.com/training/package-visibility).
~~~
//使用queries标签
<manifest>
...
  <queries>
        <package android:name="com.tencent.mobileqq"/>
  </queries>
...
</manifest>

~~~

### 封装的几个简单的api
使用MMKV保存了Token信息和从QQ获取到的用户信息

checkQQLoginValid() 检查登录状态，如果无需登录(返回code为0)，可以直接从MMKV获取本地用户信息。如果code不为0，则需要开始请求使用QQ登录

getLocalUserInfo() 直接从MMKV获取本地用户信息

startLoginQQ() 开始请求用QQ账户登录

logoutQQ() 登出账号，清空本地缓存

以上都封装在了/qqlogin这个模块里，简单使用参考com.zpc.qqdemo.MainActivity里的写法


### 其他
要申请自己的appid，审核可能需要几天，在com.zpc.qqlogin.BaseLoginActivity里替换原来的APP_ID。另外<data android:scheme=""/>这个便签内容也换下
