package com.example.ucordilleras

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask


class FbUpload : AppCompatActivity() {
    private var mButtonChooseImage: Button? = null
    private var mButtonUpload: Button? = null
    private var mEditTextFileName: EditText? = null
    private var mImageView: ImageView? = null
    private var mProgressBar: ProgressBar? = null
    private var mImageUri: Uri? = null
    private var mStorageRef: StorageReference? = null
    private var mDatabaseRef: DatabaseReference? = null
    private var new_mDatabaseRef: DatabaseReference? = null
    private var demoRef: DatabaseReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var mAuth: FirebaseAuth? = null
    private var name: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fbupload)
        mAuth = FirebaseAuth.getInstance()
        mButtonChooseImage = findViewById(R.id.button_choose_image)
        mButtonUpload = findViewById(R.id.button_upload)
        /*mTextViewShowUploads = findViewById(R.id.text_view_show_uploads);*/
        mEditTextFileName = findViewById(R.id.edit_text_file_name)
        mImageView = findViewById(R.id.image_view)
        mProgressBar = findViewById(R.id.progress_bar)
        name = mAuth!!.currentUser!!.email
        mStorageRef = FirebaseStorage.getInstance().getReference("Diseases/${this.name.toString()}/uploaded_diseases")
        mDatabaseRef = FirebaseDatabase.getInstance().reference
        demoRef = mDatabaseRef!!.child("User")
        mButtonChooseImage!!.setOnClickListener(View.OnClickListener { openFileChooser() })
        mButtonUpload!!.setOnClickListener(View.OnClickListener {
            if (mUploadTask != null && mUploadTask!!.isInProgress) {
                Toast.makeText(this@FbUpload, "Upload in progress", Toast.LENGTH_SHORT).show()
            } else {
                uploadFile()
            }
        })

        /*mTextViewShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });*/
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            mImageUri = data.data
            mImageView.let {
                mImageView?.let { it1 -> Glide.with(this).load(mImageUri).into(it1) }
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun uploadFile() {
        if (mImageUri != null) {
            val fileReference = mStorageRef!!.child(mEditTextFileName!!.text.toString().trim { it <= ' ' }+ "." + getFileExtension(mImageUri!!))
            //Toast.makeText(this,fileReference.toString(),Toast.LENGTH_SHORT).show()
            mUploadTask = fileReference.putFile(mImageUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        val handler = Handler()
                        handler.postDelayed({ mProgressBar!!.progress = 0 }, 500)
                        Toast.makeText(this@FbUpload, "Upload successful", Toast.LENGTH_LONG).show()
                        val upload = Upload(mEditTextFileName!!.text.toString().trim { it <= ' ' },
                                taskSnapshot.metadata!!.reference!!.downloadUrl.toString())

                        demoRef!!.child(name.toString().replace(".", ",")+"_${System.currentTimeMillis().toString()}").setValue(upload)

                    }
                    .addOnFailureListener { e -> Toast.makeText(this@FbUpload, e.message, Toast.LENGTH_SHORT).show() }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        mProgressBar!!.progress = progress.toInt()
                    }
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

}