package com.example.supportwebrctdemo

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.tencent.smtt.export.external.extension.interfaces.IX5WebChromeClientExtension
import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension
import com.tencent.smtt.export.external.interfaces.IX5WebViewBase
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.MediaAccessPermissionsCallback
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import java.io.File


class WebViewActivity : AppCompatActivity() {
    private var mAcceptType: String? = null
    private var mValueCallback: ValueCallback<Uri>? = null
    private lateinit var mWebview: WrapWebView
    private val REQUEST_CODE_SELECTIMAGE = 0x11
    private val REQUEST_CODE_VIDEO = 0x12
    private var selectImageTempUri: Uri? = null
    private var videoTempUri: Uri? = null


    companion object {
        fun start(context: Context, bundle: Bundle?) {
            var intent = Intent(context, WebViewActivity::class.java)
            if (bundle != null) {
                intent.putExtras(bundle)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        mWebview = findViewById<WrapWebView>(R.id.webView)
        initWebViewSetting()
        initView()
    }

    private fun initView() {
        val bundle = intent.extras
        if (bundle != null) {
            val url = bundle.getString("url")
            val ua = bundle.getString("ua")
            val webSettings = mWebview.settings
            webSettings.userAgentString =
                webSettings.userAgentString + "?${ua}"
            if (!url.isNullOrEmpty()) {
                mWebview.loadUrl(url)
            }

        }
    }

    private fun initWebViewSetting() {
        val webSettings = mWebview.settings
        webSettings.javaScriptEnabled = true
        //支持网页弹对话框
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true

        webSettings.allowFileAccess = false

        mWebview.webChromeClientExtension = x5WebChromeClientExtension

        mWebview.webChromeClient = object : WebChromeClient() {
            override fun openFileChooser(
                callback: ValueCallback<Uri>?,
                acceptType: String,
                captureType: String
            ) {
//                //重置
                mValueCallback?.onReceiveValue(null)
//                mValueCallback = null
                mValueCallback = callback
                mAcceptType = acceptType
                //TODO 权限申请
                XXPermissions.with(this@WebViewActivity)
                    .permission(Permission.CAMERA)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                            showFileChooser()
                        }

                        override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                            super.onDenied(permissions, never)
                        }
                    })
            }
        }
    }

    private fun showFileChooser(
    ) {
       val tempDir= createTempOutputFile("fileChooseTempDir")
        when (mAcceptType) {
            "video/*" -> {
                //录像
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                //限制时长
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)

                //保存路径
                val tempVideoFile = File(tempDir, "video_${System.currentTimeMillis()}.mp4")
                videoTempUri = Uri.fromFile(tempVideoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoTempUri)
                //开启摄像机
                startActivityForResult(intent, REQUEST_CODE_VIDEO)

            }
            "image/*" -> {
                //拍照相册
                selectImage(tempDir)
            }
            else -> {
                //拍照相册
                selectImage(tempDir)
            }
        }
    }

    private fun createTempOutputFile(childFile: String?): File {
        var f = File(externalCacheDir, childFile)
        if (!f.exists()) {
            f.mkdirs()
        }
        //每次请清空
        var fileList = f.listFiles()
        fileList.forEach {
            it.delete()
        }
        return f
    }

    private fun selectImage(
        tempDir:File
    ) {
        //相册、文件管理
        val albumIntent = Intent(Intent.ACTION_GET_CONTENT)
        albumIntent.addCategory(Intent.CATEGORY_OPENABLE)
        albumIntent.type = "image/*"
        //拍照
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.resolveActivity(packageManager)?.let {

            //如果存在则删除

            //TODO 这里文件名每次生成需要不一样，不然webview不会上传
            val selectImageTempFile = File(tempDir, "image_${System.currentTimeMillis()}.jpg")

            //否则使用Uri.fromFile(file)方法获取Uri
            selectImageTempUri = Uri.fromFile(selectImageTempFile)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectImageTempUri)
//            }
        }
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.select_file))
        chooserIntent.putExtra(Intent.EXTRA_INTENT, albumIntent)
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Intent>(cameraIntent))

        startActivityForResult(
            chooserIntent,
            REQUEST_CODE_SELECTIMAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECTIMAGE -> {
                var results: Uri? = null
                // Check that the response is a good one
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        // If there is not data, then we may have taken a photo
                        if (selectImageTempUri != null) {
                            results = selectImageTempUri
                        }
                    } else {
                        val dataString = data.dataString
                        if (dataString != null) {
                            results = (Uri.parse(dataString))
                        }
                    }
                }
                mValueCallback?.onReceiveValue(results)
                mValueCallback = null
                selectImageTempUri = null
            }
            REQUEST_CODE_VIDEO -> {
                var results: Uri? = null
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        if (videoTempUri != null) {
                            results = videoTempUri
                        }
                    } else {
                        val dataString = data.dataString
                        if (dataString != null) {
                            results = (Uri.parse(dataString))
                        }
                    }
                }
                mValueCallback?.onReceiveValue(results)
                mValueCallback = null
                videoTempUri = null
            }
            else -> {
            }
        }
    }

    var x5WebChromeClientExtension = object : IX5WebChromeClientExtension {
        override fun getVideoLoadingProgressView(): View? {
            return null
        }

        override fun onAllMetaDataFinished(
            p0: IX5WebViewExtension?,
            p1: java.util.HashMap<String, String>?
        ) {

        }

        override fun onBackforwardFinished(p0: Int) {
        }

        override fun onHitTestResultForPluginFinished(
            p0: IX5WebViewExtension?,
            p1: IX5WebViewBase.HitTestResult?,
            p2: Bundle?
        ) {
        }

        override fun onHitTestResultFinished(
            p0: IX5WebViewExtension?,
            p1: IX5WebViewBase.HitTestResult?
        ) {
        }

        override fun onPromptScaleSaved(p0: IX5WebViewExtension?) {
        }

        override fun onPromptNotScalable(p0: IX5WebViewExtension?) {
        }

        override fun onAddFavorite(
            p0: IX5WebViewExtension?,
            p1: String?,
            p2: String?,
            p3: JsResult?
        ): Boolean {
            return false
        }

        override fun onPrepareX5ReadPageDataFinished(
            p0: IX5WebViewExtension?,
            p1: java.util.HashMap<String, String>?
        ) {
        }

        override fun onSavePassword(
            p0: String?,
            p1: String?,
            p2: String?,
            p3: Boolean,
            p4: Message?
        ): Boolean {
            return false
        }

        override fun onSavePassword(
            p0: android.webkit.ValueCallback<String>?,
            p1: String?,
            p2: String?,
            p3: String?,
            p4: String?,
            p5: String?,
            p6: Boolean
        ): Boolean {
            return false
        }

        override fun onX5ReadModeAvailableChecked(p0: java.util.HashMap<String, String>?) {
        }

        override fun addFlashView(p0: View?, p1: ViewGroup.LayoutParams?) {
        }

        override fun h5videoRequestFullScreen(p0: String?) {
        }

        override fun h5videoExitFullScreen(p0: String?) {
        }

        override fun requestFullScreenFlash() {
        }

        override fun exitFullScreenFlash() {
        }

        override fun jsRequestFullScreen() {
        }

        override fun jsExitFullScreen() {
        }

        override fun acquireWakeLock() {
        }

        override fun releaseWakeLock() {
        }

        override fun getApplicationContex(): Context? {
            return null
        }

        override fun onPageNotResponding(p0: Runnable?): Boolean {
            return false
        }

        override fun onMiscCallBack(p0: String?, p1: Bundle?): Any? {
            return null
        }

        override fun openFileChooser(
            p0: android.webkit.ValueCallback<Array<Uri>>?,
            p1: String?,
            p2: String?
        ) {
            Log.d("xxx", "openFileChooser1111")
        }

        override fun onPrintPage() {
        }

        override fun onColorModeChanged(p0: Long) {
        }

        override fun onPermissionRequest(
            origin: String,
            resource: Long,
            callback: MediaAccessPermissionsCallback
        ): Boolean {
            var cameraRequest =
                (resource and MediaAccessPermissionsCallback.BITMASK_RESOURCE_VIDEO_CAPTURE) != 0L

            var microphoneRequest =
                (resource and MediaAccessPermissionsCallback.BITMASK_RESOURCE_AUDIO_CAPTURE) != 0L


            var permissionList = arrayListOf<String>()
            if (cameraRequest) {
                permissionList.add(Permission.CAMERA)
            }
            if (microphoneRequest) {
                permissionList.add(Permission.RECORD_AUDIO)
            }
            Log.d("xxx", "cameraRequest:" + cameraRequest)
            XXPermissions.with(this@WebViewActivity)
                .permission(permissionList.toTypedArray())
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                        if (all) {
                            val flag =
                                resource or MediaAccessPermissionsCallback.ALLOW_AUDIO_CAPTURE
                            callback.invoke(origin, flag, true);
                        }
                    }
                })
            return true;

        }

        override fun getX5WebChromeClientInstance(): Any? {
            return null
        }
    }
}