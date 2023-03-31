package com.example.imageclassification

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        managePermissions()
    }

    private fun managePermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        //launcher permissions request dialog
        permissionLauncherMultiple.launch(permissions)
    }

    private val permissionLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        //here we will check if permissions were now (from permission request dialog) or already granted or not

        var allAreGranted = true
        for (isGranted in result.values) {
            Log.d(TAG, "onActivityResult: isGranted: $isGranted")
            allAreGranted = allAreGranted && isGranted
        }

        if (allAreGranted) {
            //All Permissions granted now do the required task here or call the function for that
            multiplePermissionsGranted()
        } else {
            //All or some Permissions were denied so can't do the task that requires that permission
            Log.d(TAG, "onActivityResult: All or some permissions denied...")
            Toast.makeText(this@MainActivity, "All or some permissions denied...", Toast.LENGTH_SHORT).show()
        }
    }
    private fun multiplePermissionsGranted() {
        //Do the required task here, i'll just set the text to the TextView i.e. resultTv. You can do whatever you want
        Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
    }




}