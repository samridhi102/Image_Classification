package com.example.imageclassification

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.imageclassification.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val pickImage = 100
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        binding.uploadBtn.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        managePermissions()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            binding.imageView.setImageURI(imageUri)
        }
    }

    private fun managePermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        permissionLauncherMultiple.launch(permissions)
    }

    private val permissionLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        var allAreGranted = true
        for (isGranted in result.values) {
            Log.d(TAG, "onActivityResult: isGranted: $isGranted")
            allAreGranted = allAreGranted && isGranted
        }

        if (allAreGranted) {
            multiplePermissionsGranted()
        } else {
            Log.d(TAG, "onActivityResult: All or some permissions denied...")
            Toast.makeText(this@MainActivity, "All or some permissions denied...", Toast.LENGTH_SHORT).show()
        }
    }
    private fun multiplePermissionsGranted() {
        Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
    }




}