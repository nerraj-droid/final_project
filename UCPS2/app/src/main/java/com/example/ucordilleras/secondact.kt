package com.example.ucordilleras


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar


class secondact : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondact)
        val button3 = findViewById<Button>(R.id.help)

        val mySnack = Snackbar.make(button3, "Help us improve by uploading images of diseases!", 5000)
        mySnack.show()

        val button = findViewById<Button>(R.id.mQuit)
        button.setOnClickListener {
            this.finishAffinity()
        }
        val button2 = findViewById<Button>(R.id.mStart)
        button2.setOnClickListener {
            val intent2 = Intent(this, MainActivity::class.java)
            startActivity(intent2)
        }


        button3.setOnClickListener {
            val intent3 = Intent(this, FbUpload::class.java)
            startActivity(intent3)
        }


    }

}








