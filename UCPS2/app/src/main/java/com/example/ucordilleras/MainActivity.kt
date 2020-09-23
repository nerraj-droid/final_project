package com.example.ucordilleras

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : AppCompatActivity() {

    private var deseaseClassifier = DiseaseClassifier(this)
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var firebasePerformance = FirebasePerformance.getInstance()
    private var mAuth: FirebaseAuth? = null
    private var mStorageRef: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var demoRef: DatabaseReference? = null
    private var name: String? = null
    private val TAG = MainActivity::class.java.simpleName
    val REQUEST_IMAGE = 100
    val CAMERA_REQUEST_CODE = 102
    val RequestPermissionCode = 1
    protected var tflite: Interpreter? = null
    private var inputImageBuffer: TensorImage? = null
    private var imageSizeX = 0
    private var imageSizeY = 0
    private var outputProbabilityBuffer: TensorBuffer? = null
    private var probabilityProcessor: TensorProcessor? = null
    private var bitmap: Bitmap? = null

    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    var ref: DatabaseReference = database.getReference("authentication-3f63c")

    private var labels: List<String>? = null
    var imageView: ImageView? = null
    var imageuri: Uri? = null
    var classitext: TextView? = null
    var bushowDesease: Button? = null
    var bushowmgmt: Button? = null
    var ooptions = arrayOf("Camera", "Gallery")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
        if (mAuth!!.currentUser != null) {
            name = mAuth!!.currentUser?.email
        }
        mAuth = FirebaseAuth.getInstance()
        remoteConfig = FirebaseRemoteConfig.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference("Diseases/${this.name.toString()}/detected_diseases")
        imageView = findViewById<View>(R.id.iv) as ImageView
        classitext = findViewById<View>(R.id.classifytext) as TextView
        bushowDesease = findViewById<View>(R.id.showDetails) as Button
        bushowmgmt = findViewById<View>(R.id.showDesease) as Button

        bushowmgmt?.setVisibility(View.GONE)
        imageView!!.setOnClickListener {
            CropImage.activity(null).setGuidelines(CropImageView.Guidelines.ON).start(this)
        }

        bushowDesease!!.setOnClickListener {
            setupDiseaseClassifier()
            if (bitmap == null) {
                Toast.makeText(this, "No Image", Toast.LENGTH_SHORT).show()
            } else {
                bushowmgmt?.setVisibility(View.VISIBLE)
                val imageTensorIndex = 0
                val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
                imageSizeY = imageShape[1]
                imageSizeX = imageShape[2]
                val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
                val probabilityTensorIndex = 0
                val probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
                val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()
                inputImageBuffer = TensorImage(imageDataType)
                outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
                probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()
                inputImageBuffer = loadImage(bitmap)
                tflite!!.run(inputImageBuffer!!.buffer, outputProbabilityBuffer!!.buffer.rewind())
                showdesease()
            }
        }



        try {
            tflite = Interpreter(loadmodelfile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun loadImage(bitmap: Bitmap?): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer?.load(bitmap)

        // Creates processor for the TensorImage.
        val cropSize = bitmap?.width?.let { Math.min(it, bitmap.height) }
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        val imageProcessor = ImageProcessor.Builder()
                .add(cropSize?.let { ResizeWithCropOrPadOp(it, cropSize) })
                .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(preprocessNormalizeOp)
                .build()
        return imageProcessor.process(inputImageBuffer)
    }


    private fun setupDiseaseClassifier() {
        configureRemoteConfig()
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val modelName = remoteConfig.getString("model_name")
                        val downloadTrace = firebasePerformance.newTrace("download_model")
                        downloadTrace.start()
                        downloadModel("model") //new model for update
                                .addOnSuccessListener {
                                    downloadTrace.stop()
                                }
                    } else {
                        Toast.makeText(this, "Failed to fetch model name.", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun configureRemoteConfig() {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    private fun downloadModel(modelName: String): Task<Void> {
        val remoteModel = FirebaseCustomRemoteModel.Builder(modelName).build()
        val firebaseModelManager = FirebaseModelManager.getInstance()
        return firebaseModelManager
                .isModelDownloaded(remoteModel)
                .continueWithTask { task ->
                    // Create update condition if model is already downloaded, otherwise create download
                    // condition.
                    val conditions = if (task.result != null && task.result == true) {
                        FirebaseModelDownloadConditions.Builder()
                                .requireWifi()
                                .build() // Update condition that requires wifi.
                    } else {
                        FirebaseModelDownloadConditions.Builder().build(); // Download condition.
                    }
                    firebaseModelManager.download(remoteModel, conditions)
                }
                .addOnSuccessListener {
                    firebaseModelManager.getLatestModelFile(remoteModel)
                            .addOnCompleteListener {
                                val model = it.result
                                if (model == null) {
                                    Toast.makeText(this, "Failed to get model file.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Downloaded Remote Model: $modelName", Toast.LENGTH_SHORT).show()
                                    deseaseClassifier.initialize(model)
                                }
                            }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Model download failed for $modelName, please check your connection.", Toast.LENGTH_SHORT).show()
                }
    }

    override fun onDestroy() {
        deseaseClassifier.close()
        super.onDestroy()
    }

    @Throws(IOException::class)
    private fun loadmodelfile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startoffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
    }

    private val preprocessNormalizeOp: TensorOperator
        private get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    private val postprocessNormalizeOp: TensorOperator
        private get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    private fun showdesease() {
        try {
            labels = FileUtil.loadLabels(this, "dict.txt")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val labeledProbability = TensorLabel(labels!!, probabilityProcessor!!.process(outputProbabilityBuffer))
                .mapWithFloatValue
        val maxValueInMap = Collections.max(labeledProbability.values)
        for ((key, value) in labeledProbability) {
            if (value == maxValueInMap) {
                classitext!!.text = key
                    if (key == "leafspot") {
                        bushowmgmt!!.setOnClickListener {
                            val callleafspot = Intent(this, leafspot_disease::class.java)
                            startActivity(callleafspot)
                        }
                    }
                    if (key == "graymold") {
                        bushowmgmt!!.setOnClickListener {
                            val callgraymold = Intent(this, graymold_disease::class.java)
                            startActivity(callgraymold)
                        }
                    }
                    if (key == "leafblight") {
                        bushowmgmt!!.setOnClickListener {
                            val callleafblight = Intent(this, leafblight::class.java)
                            startActivity(callleafblight)
                        }
                    }
                    if (key == "mycosphaerella_fragariae") {
                        bushowmgmt!!.setOnClickListener {
                            val callmycosphaerella_fragariae = Intent(this, leafspot_disease::class.java)
                            startActivity(callmycosphaerella_fragariae)
                        }
                    }
                    if (key == "anthracnose") {
                        bushowmgmt!!.setOnClickListener {
                            val callanthracnose = Intent(this, anthracnose_disease::class.java)
                            startActivity(callanthracnose)
                        }
                    }
                    if (key == "leafscorch") {
                        bushowmgmt!!.setOnClickListener {
                            val callleafscorch = Intent(this, leafscorch_diseases::class.java)
                            startActivity(callleafscorch)
                        }
                    }
                    if (key == "Click below to select a Image") {
                        bushowmgmt!!.setOnClickListener {
                            val repeat = Intent(this, MainActivity::class.java)
                            startActivity(repeat)
                        }
                    }

                }
            }
        uploadFile()
        }

    private fun uploadFile() {

        val fileReference = mStorageRef!!.child(classitext!!.text as String)
        //Toast.makeText(this,fileReference.toString(),Toast.LENGTH_SHORT).show()
        mUploadTask = bitmap?.let { getImageUri(this, it)?.let { fileReference.putFile(it) } }
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null)
        return Uri.parse(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, result.uri)
                val intent = Intent(this, analyze::class.java)
                startActivity(intent)
                imageView.let {
                    imageView?.let { it1 -> Glide.with(this).load(bitmap).into(it1) }
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.error, Toast.LENGTH_LONG).show()
            }
        }
    }
    companion object {
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 1.0f
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 255.0f
    }

}



