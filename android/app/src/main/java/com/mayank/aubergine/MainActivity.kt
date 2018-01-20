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


class MainActivity : AppCompatActivity() {

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
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
        startActivityForResult(intent, 0)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0) {
            val thumbnail = data.extras!!.get("data") as Bitmap
            val bytes = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
            val destination = File(Environment.getExternalStorageDirectory(), "temp.jpg")
            val fo: FileOutputStream
            try {
                fo = FileOutputStream(destination)
                fo.write(bytes.toByteArray())
                fo.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            uploadFileToServerTask(bytes)
        }
    }

    fun uploadFileToServerTask(bytes: ByteArrayOutputStream) {
        Log.d("hello", "in uploadFiletoServerTask init")
        Fuel.post("http://52.174.181.163:5001/createnote").body("image="+bytes+"&name=mayank").response { request, response, result ->
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

    //send image over to the server, name for the note
    //return the id
    fun uploadNoteToServer(bytes: ByteArrayOutputStream) {
        Log.d("hello", "in uploadFiletoServerTask init")
        Fuel.post("http://52.174.181.163:5001/createnote").body("image="+bytes+"&name=mayank").response { request, response, result ->
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

