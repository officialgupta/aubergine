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
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
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
            if (requestCode == 0) { // request code?
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, 0)
                }
            }
        }


    fun captureTime(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 0)
    }

    fun scanTime() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val imageFileUri = File(Environment.getExternalStorageDirectory(), "image.jpg")
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri)

        startActivityForResult(intent, 0)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0) {
            val bitmap = data.extras!!.get("data") as Bitmap
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

            uploadFileToServerTask(bytes)
        }
    }

    fun uploadFileToServerTask(bytes: ByteArrayOutputStream) {
        Log.d("Size of image in B", bytes.toByteArray().size.toString())

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // 8 is URL safe
        val b64str = Base64.encodeToString(bytes.toByteArray(), 8)
        Log.d("Base64Conv", b64str)

        Fuel.post("http://52.174.181.163:5001/createnote").body("image=$b64str&name=$timeStamp").response { request, response, result ->
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

