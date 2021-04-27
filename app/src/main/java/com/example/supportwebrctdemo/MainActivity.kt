package com.example.supportwebrctdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val et = findViewById<EditText>(R.id.et_webview)
        et.hint = "http://www.script-tutorials.com/demos/199/index.html"
        val etUa = findViewById<EditText>(R.id.et_ua)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            var bundle = Bundle()
            var url = et.hint.toString()
            if (!et.text.toString().trim().isNullOrEmpty()) {
                url = et.text.toString().trim()
            }
            bundle.putString("url", url)
            bundle.putString("ua", etUa.text.toString().trim())
            WebViewActivity.start(this, bundle)
        }


    }
}