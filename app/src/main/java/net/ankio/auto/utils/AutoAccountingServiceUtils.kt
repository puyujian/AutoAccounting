/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.utils

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.common.config.AccountingConfig
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * AutoAccountingServiceUtils
 * 自动记账服务调用工具
 */
class AutoAccountingServiceUtils(mContext: Context) : CoroutineScope by MainScope(){

    private var requestsUtils = RequestsUtils(mContext)

    private val host = "http://127.0.0.1"

    private val headers = HashMap<String, String>()

    companion object{
        private const val PORT = 52045
        // 将isServerStart转换为挂起函数
        suspend fun isServerStart(mContext: Context): Boolean = withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                RequestsUtils(mContext).get("http://127.0.0.1:$PORT/",
                    headers = hashMapOf("Authorization" to getToken()),
                    onSuccess = { _, code ->
                        continuation.resume(code == 200)
                    },
                    onError = {
                        Logger.i("http://127.0.0.1:$PORT/ 请求错误$it")
                        continuation.resume(false)
                    })
            }
        }

        fun getToken(): String {
           return get("token")
        }
        /**
         * 获取文件内容
         */
        fun get(name: String): String {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${BuildConfig.APPLICATION_ID}/cache/shell/${name}.txt"
            val file = File(path)
            if(file.exists()){
                return file.readText().trim()
            }
            return ""
        }

        fun delete(name: String) {
            val path =  Environment.getExternalStorageDirectory().path+"/Android/data/${BuildConfig.APPLICATION_ID}/cache/shell/${name}.txt"
            val file = File(path)
            if(file.exists()){
                file.delete()
            }
        }
    }

    init {
        headers["Authorization"] = getToken()
        launch {
            if(!isServerStart(mContext)){
                throw AutoServiceException("Server error")
            }
        }

    }

    /**
     * 获取请求地址
     */
    private fun getUrl(path:String): String {
        return "$host:$PORT$path"
    }

    /**
     * 请求错误
     */
    private fun onError(string: String){
        Logger.i("自动记账服务错误：${string}")
    }

    /**
     * 获取数据
     */
    fun get(name: String, onSuccess: (String) -> Unit){
        requestsUtils.get(getUrl("/get"),
            query = hashMapOf("name" to name),
            headers = headers,
            onSuccess = { bytearray,code ->
                        if(code==200){
                            onSuccess(String(bytearray).trim())
                        }else{
                            onError(String(bytearray).trim())
                        }
        },
            onError = this::onError)
    }

    /**
     * 设置数据
     */
    fun set(name: String, value: String){
        requestsUtils.post(getUrl("/set"),
            query = hashMapOf("name" to name),
            data = hashMapOf("raw" to value),
            headers = headers,
            contentType = RequestsUtils.TYPE_RAW,
            onSuccess = { bytearray, code ->
                if(code!=200){
                    onError(String(bytearray).trim())
                }
        },onError = this::onError)
    }

    /**
     * 设置App记录数据
     */
    fun putData(value: String){
        requestsUtils.post(getUrl("/data"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers,
            onSuccess = { bytearray,code ->
                if(code!=200){
                    onError(String(bytearray).trim())
                }
        },onError = this::onError)
    }

    /**
     * 获取记录的数据
     */
    fun getData(onSuccess: (String) -> Unit){
        get("data",onSuccess)
    }

    /**
     * 设置App记录日志
     */
    fun putLog(value: String){
        requestsUtils.post(getUrl("/log"),
            data = hashMapOf("raw" to value),
            contentType = RequestsUtils.TYPE_RAW,
            headers = headers,
            onSuccess = { bytearray,code ->
                if(code!=200){
                    onError(String(bytearray).trim())
                }
        },onError = this::onError)
    }

    /**
     * 获取App记录的日志
     */
    fun getLog(onSuccess: (String) -> Unit){
        get("log",onSuccess)
    }
    /**
     * 获取配置
     */
    fun config(autoAccountingConfig: (AccountingConfig)->Unit){
        runCatching {
            get("bookAppConfig") {
                val config = Gson().fromJson(it, AccountingConfig::class.java)
                if(config==null){
                    set("bookAppConfig",Gson().toJson(AccountingConfig()))
                    autoAccountingConfig(AccountingConfig())
                }else{
                    autoAccountingConfig(config)
                }

            }
        }.onFailure {
            Logger.e("获取配置失败",it)
            autoAccountingConfig(AccountingConfig())
        }
    }
}