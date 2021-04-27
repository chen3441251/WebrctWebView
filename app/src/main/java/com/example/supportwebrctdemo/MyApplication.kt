package com.example.supportwebrctdemo

import android.app.Application
import android.util.Log
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk


/**
 * author : chenchao
 * time   : 4/27/21
 * project：SupportWebrctDemo
 * des    ：
 */
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // 在调用TBS初始化、创建WebView之前进行如下配置
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = mutableMapOf<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)

        QbSdk.initX5Environment(this,object :QbSdk.PreInitCallback{
            override fun onCoreInitFinished() {
                Log.d("xxx","onCoreInitFinished")
            }

            override fun onViewInitFinished(p0: Boolean) {
                Log.d("xxx","onViewInitFinished")            }
        })
    }
}