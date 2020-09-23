package com.example.ucordilleras

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class analyze : AppCompatActivity() {
    private var txtView: TextView? =  null
    private var mProgress: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze)
        mProgress = findViewById<View>(R.id.splash_screen_progress_bar) as ProgressBar
        txtView = findViewById<View>(R.id.load) as TextView
        //Splash Screen duration
        Thread(Runnable {
            doWork()
            startApp()
            finish()
        }).start()
    }
    private fun doWork() {
        var progress = 0
        var basis = 0
        val messages = listOf("Processing...", "Image Detected...", "Extracting Region...", "Pooling Layers...", "Classifying...", "Finishing...")
        while (progress < 5) {
            try {
                Thread.sleep(1000) //10000
                mProgress!!.progress = progress
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(progress == basis){
                runOnUiThread {
                    txtView?.setText(messages.get(basis))
                }
            }
            basis += 1
            progress += 1
        }
    }



    private fun startApp() {
        finish()
    }
    }
