package com.example.ucordilleras


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class registerpage : AppCompatActivity() {
    private var inputEmail: EditText? = null
    private var inputPassword: EditText? = null
    private var btnSignIn: Button? = null
    private var btnSignUp: Button? = null
    private var btnResetPassword: Button? = null
    private var progressBar: ProgressBar? = null
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registerpage)

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance()

        btnSignIn = findViewById<Button>(R.id.sign_in_button) as Button
        btnSignUp = findViewById<Button>(R.id.sign_up_button) as Button
        inputEmail = findViewById<EditText>(R.id.email) as EditText
        inputPassword = findViewById<EditText>(R.id.password) as EditText
        progressBar = findViewById<ProgressBar>(R.id.progressBar) as ProgressBar
        btnResetPassword = findViewById<Button>(R.id.btn_reset_password) as Button
        btnResetPassword!!.setOnClickListener {
            startActivity(Intent(this@registerpage, ResetPasswordActivity::class.java))
        }

        btnSignIn!!.setOnClickListener {
            finish()
        }
        btnSignUp!!.setOnClickListener {
            val email = inputEmail!!.text.toString().trim { it <= ' ' }
            val password = inputPassword!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(applicationContext, "Enter email address!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(applicationContext, "Enter password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(applicationContext, "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar!!.visibility = View.VISIBLE
            //create user
            mAuth!!.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@registerpage) { task ->
                        Toast.makeText(this@registerpage, "Successfully created new account", Toast.LENGTH_SHORT).show()
                        progressBar!!.visibility = View.GONE
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Toast.makeText(this@registerpage, "Authentication failed, please review your details.",
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            startActivity(Intent(this@registerpage, secondact::class.java))
                            finish()
                        }
                    }
        }

    }

    override fun onResume() {
        super.onResume()
        progressBar!!.visibility = View.GONE
    }
}
