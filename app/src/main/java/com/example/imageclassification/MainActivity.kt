package com.example.imageclassification

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.imageclassification.databinding.ActivityMainBinding
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val SELECT_IMAGES = 1
    val CAMERA_REQUEST = 2
    val MY_CAMERA_PERMISSION_CODE = 100
    var selectedImagesPaths // Paths of the image(s) selected by the user.
            : ArrayList<Uri?>? = null
    var imagesSelected = false // Whether the user selected at least an image or not.
    private val pickImage = 100
    private var imageUri: Uri? = null
    private val serverURL="http://192.168.43.186:5000"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.CAMERA),
            2
        )
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
        binding.uploadBtn.setOnClickListener {
            selectImage()
        }
        binding.predictBtn.setOnClickListener {
            connectServer()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Granted.", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return
            }
            2 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "Camera Permission Granted.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    fun connectServer() {
        if (imagesSelected == false) { // This means no image is selected and thus nothing to upload.
            binding.resultTextView.text = "No Image Selected to Upload. \nSelect Image and Try Again."
            return
        }
        binding.resultTextView.text = "Sending the Files. Please Wait ..."
        val postUrl = "$serverURL/predict/"
        val multipartBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        for (i in selectedImagesPaths!!.indices) {
            var byteArray: ByteArray? = null
            try {
                val inputStream = contentResolver.openInputStream(
                    selectedImagesPaths!![i]!!
                )
                val byteBuffer = ByteArrayOutputStream()
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var len = 0
                while (inputStream!!.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
                byteArray = byteBuffer.toByteArray()
            } catch (e: Exception) {
                binding.resultTextView.text = "Please Make Sure the Selected File is an Image."
                return
            }
            multipartBodyBuilder.addFormDataPart(
                "image$i", "input_img.bmp", RequestBody.create(
                    MediaType.parse("image/*"), byteArray
                )
            )
        }
        val postBodyImage: RequestBody = multipartBodyBuilder.build()

//        RequestBody postBodyImage = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
//                .build();
        postRequest(postUrl, postBodyImage)
    }

    fun postRequest(postUrl: String?, postBody: RequestBody?) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(postUrl)
            .post(postBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                call.cancel()
                Log.d("FAIL", e.message!!)
                runOnUiThread {
                    binding.resultTextView.text = "Failed to Connect to Server. Please Try Again."
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                runOnUiThread {
                    try {
                        val res = response.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (res[1].trim { it <= ' ' } == "code=200") binding.resultTextView.text = "Server's Response\n" + response.body().string()
                        else binding.resultTextView.text = "Oops! Something went wrong. \nPlease try again"
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }


                }

            }
        })

    }

    fun captureImage() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_CAMERA_PERMISSION_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == SELECT_IMAGES && resultCode == RESULT_OK && data != null) {
                selectedImagesPaths = ArrayList()
                val imgName = binding.imgName
                val imgView = binding.imageView
                if (data.data != null) {
                    val uri = data.data
                    Log.d("ImageDetails", "URI : $uri")
                    selectedImagesPaths!!.add(uri)
                    imagesSelected = true
                    imgName.text = getFileName(selectedImagesPaths!![0])
                    imgView.setImageURI(selectedImagesPaths!![0])
                }
            } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
                selectedImagesPaths = ArrayList()
                val imgName = binding.imgName
                val imgView = binding.imageView
                if (data.extras!!["data"] != null) {
                    val photo = data.extras!!["data"] as Bitmap?
                    val uri = getImageUri(applicationContext, photo)
                    Log.d("ImageDetails", "URI : $uri")
                    selectedImagesPaths!!.add(uri)
                    imagesSelected = true
                    imgName.text = getFileName(selectedImagesPaths!![0])
                    imgView.setImageURI(selectedImagesPaths!![0])
                }
            } else {
                Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun getImageUri(inContext: Context, inImage: Bitmap?): Uri {
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri?): String? {
        var result: String? = null
        if (uri!!.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
//        binding.uploadBtn.setOnClickListener {
//            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
//            startActivityForResult(gallery, pickImage)
//        }
//        managePermissions()
//    }
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK && requestCode == pickImage) {
//            imageUri = data?.data
//            binding.imageView.setImageURI(imageUri)
//        }
//    }
//
//    private fun managePermissions() {
//        val permissions = arrayOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//        permissionLauncherMultiple.launch(permissions)
//    }
//
//    private val permissionLauncherMultiple = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { result ->
//        var allAreGranted = true
//        for (isGranted in result.values) {
//            Log.d(TAG, "onActivityResult: isGranted: $isGranted")
//            allAreGranted = allAreGranted && isGranted
//        }
//
//        if (allAreGranted) {
//            multiplePermissionsGranted()
//        } else {
//            Log.d(TAG, "onActivityResult: All or some permissions denied...")
//            Toast.makeText(this@MainActivity, "All or some permissions denied...", Toast.LENGTH_SHORT).show()
//        }
//    }
//    private fun multiplePermissionsGranted() {
//        Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
//    }
//
//
//

}