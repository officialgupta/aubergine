package com.mayank.aubergine

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.view.View
import android.widget.Button
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import java.io.*
import android.annotation.SuppressLint
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

        val cap: Button = findViewById(R.id.capture);
        cap.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Code here executes on main thread after user presses button
                captureTime()
            }
        })

        val scan: Button = findViewById(R.id.scan);
        scan.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Code here executes on main thread after user presses button
                scanTime()
            }
        })

        val ann_txt : EditText = findViewById(R.id.annotation)
        val noteid : EditText = findViewById(R.id.noteid)
        val ann: Button = findViewById(R.id.addannbutton)
        ann.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (ann_txt.text.equals("") || noteid.text.equals("")) {
                    return
                }
                addAnnotation(ann_txt.text.toString(), noteid.text.toString())
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

    fun addAnnotation(annotation: String, noteid: String) {
        Fuel.get("http://52.174.181.163:5001/addannotation/$noteid/$annotation").response { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    print("error m9")
                    Log.d("addAnnotation", "FAILURE")
                    Log.d("hello", result.toString())

                }
                is Result.Success -> {
                    println(response)
                    Log.d("addAnnotation", response.httpResponseMessage)
                    val (bytes, error) = result
                    if (bytes != null) {
                        println(bytes)
                    }
                }
            }
        }
    }

    fun captureTime(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        startActivityForResult(intent, CAP_CODE)
    }

    fun scanTime() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        startActivityForResult(intent, SCAN_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        Log.d("onActivityResult", "1")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAP_CODE) {
            val thumbnail = data.extras!!.get("data") as Bitmap
            val bytes = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

            uploadFileToServerTask(bytes, "createnote")
        } else if (requestCode == SCAN_CODE) {
            val thumbnail = data.extras!!.get("data") as Bitmap
            val bytes = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

            uploadFileToServerTask(bytes, "findnote")
        }
    }

    fun uploadFileToServerTask(bytes: ByteArrayOutputStream, endpoint: String) {
        Log.d("Size of image in B", bytes.toByteArray().size.toString())

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // 8 is URL safe
        val b64str = Base64.encodeToString(bytes.toByteArray(), 8)
        Log.d("Base64Conv", b64str)

        val noteName: EditText = findViewById(R.id.noteName);

        Fuel.post("http://52.174.181.163:5001/$endpoint").body("image=$b64str&name=${noteName.text}").response { request, response, result ->
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
                    if (endpoint == "findnote") {
                        response.httpResponseMessage.
                    }
                }
            }
        }

    }

}

