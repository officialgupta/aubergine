package com.mayank.aubergine

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.support.annotation.NonNull
import android.os.AsyncTask.execute
import android.os.Environment.getExternalStorageDirectory
import android.graphics.Bitmap
import android.os.Environment
import android.view.View
import android.widget.Button
import android.content.ContentValues.TAG
import android.os.AsyncTask
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import android.widget.EditText
import android.R.attr.button
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Base64
import kotlin.experimental.and
import android.graphics.BitmapFactory
import android.support.v4.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val CAP_CODE = 0
    private val SCAN_CODE = 1

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE")
        requestPermissions(permissions, 0)

        val cap: Button = findViewById(R.id.capture)
        cap.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Code here executes on main thread after user presses button
                captureTime()
            }
        })

        val scan: Button = findViewById(R.id.scan)
        scan.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Code here executes on main thread after user presses button
                scanTime()
            }
        })

//        val mEdit = findViewById(R.id.noteName) as EditText
//
//        cap.setOnClickListener(
//                View.OnClickListener { Log.v("EditText", mEdit.getText().toString()) })
    }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 0) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Log.e("Permissions", "Some permission wasn't granted")
                        return
                    }
                }
                Log.d("Permissions", "Permissions OK")
            }
        }

    var mCurrentPhotoPath: String = ""

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        Log.d("Storage file", storageDir.toString())
        Log.d("Storage file", image.toString())

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }


    fun captureTime(){
//        Log.d("captureTime", "niceTime")
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            Log.d("captureTime", "1")
//
//            // Create the File where the photo should go
//            var photoFile: File? = null
//            try {
//                photoFile = createImageFile();
//            } catch (ex: IOException) {
//                Log.e("Error", ex.toString())
//            }
//
//            Log.d("captureTime", "2")
//        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val imageFile = File(getExternalFilesDir(null), "image.jpg")
        if (imageFile != null) {
            val photoURI = FileProvider.getUriForFile(this,
                    "com.mayank.aubergine.fileprovider",
                    imageFile)
            if (photoURI != null) {
                Log.d("photoURI", "photoURI not null")
            } else {
                Log.d("photoURI", "photoURI null")
            }
            Log.d("captureTime", "3")

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, CAP_CODE)
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI)
        }

        startActivityForResult(intent, CAP_CODE)
    }

    fun scanTime() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val imageFileUri = File(getExternalFilesDir(null), "image.jpg").toURI()
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri)

        startActivityForResult(intent, SCAN_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        Log.d("onActivityResult", "1")

        if(resultCode != RESULT_CANCELED && data != null && resultCode == RESULT_OK && resultCode != null) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == CAP_CODE) {
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/image.jpeg")
                val size = file.length().toInt()
                val bytes = ByteArray(size)
                try {
                    val buf = BufferedInputStream(FileInputStream(file))
                    buf.read(bytes, 0, bytes.size)
                    buf.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                uploadFileToServerTask(bytes, "createnote")
            } else if (requestCode == SCAN_CODE) {
                val file = File(getExternalFilesDir(null).toString() + "/image.jpeg")
                val size = file.length().toInt()
                val bytes = ByteArray(size)
                try {
                    val buf = BufferedInputStream(FileInputStream(file))
                    buf.read(bytes, 0, bytes.size)
                    buf.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                uploadFileToServerTask(bytes, "findnote")
            }
        }
    }

    fun uploadFileToServerTask(bytes: ByteArray, endpoint: String) {
        Log.d("Size of image in B", bytes.size.toString())

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // 8 is URL safe
        val b64str = Base64.encodeToString(bytes, 8)
        Log.d("Base64Conv", b64str)

        Fuel.post("http://52.174.181.163:5001/$endpoint").body("image=$b64str&name=$timeStamp").response { request, response, result ->
            //do something with response
            when (result) {
                is Result.Failure -> {
                    print("error m9")
                    Log.d("hello", "FAILURE")
                    Log.d("hello", result.toString())

                }
                is Result.Success -> {
                    println(response)
                    Log.d("hello", response.httpResponseMessage)
                    val (bytes, error) = result
                    if (bytes != null) {
                        println(bytes)
                    }
                }
            }
        }
    }
}

