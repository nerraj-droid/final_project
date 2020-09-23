package com.example.ucordilleras

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId


class loginpagee : AppCompatActivity() {
    private var inputEmail: EditText? = null
    private var inputPassword: EditText? = null
    private var mAuth: FirebaseAuth? = null
    private var progressBar: ProgressBar? = null
    private var btnSignup: Button? = null
    private var btnLogin: Button? = null
    private var btnReset: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(this.toString(), "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }

                })


        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance()
        if (mAuth!!.currentUser != null) {
            startActivity(Intent(this@loginpagee, secondact::class.java))
            finish()
        }


        // set the view now
        setContentView(R.layout.activity_loginpagee)
        inputEmail = findViewById<EditText>(R.id.email) as EditText
        inputPassword = findViewById<EditText>(R.id.password) as EditText
        btnSignup = findViewById<Button>(R.id.sign_up_button) as Button
        btnLogin = findViewById<Button>(R.id.sign_in_button) as Button
        btnReset = findViewById<Button>(R.id.btn_reset_password) as Button

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance()
        btnSignup!!.setOnClickListener {
            startActivity(Intent(this@loginpagee, registerpage::class.java))
        }
        btnReset!!.setOnClickListener {
            startActivity(Intent(this@loginpagee, ResetPasswordActivity::class.java))
        }
        btnLogin!!.setOnClickListener {
            val email = inputEmail!!.text.toString()
            val password = inputPassword!!.text.toString()
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(applicationContext, "Enter email address!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(applicationContext, "Enter password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar?.visibility = View.VISIBLE

            //authenticate user
            mAuth!!.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@loginpagee) { task ->
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        progressBar?.visibility = View.GONE
                        if (!task.isSuccessful) {
                            // there was an error
                            if (password.length < 6) {
                                inputPassword!!.error = getString(R.string.minimum_password)
                            } else {
                                Toast.makeText(this@loginpagee, getString(R.string.auth_failed), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val intent = Intent(this@loginpagee, secondact::class.java)
                            startActivity(intent)
                            finish()
                        }
        }




        }


    }



}