package com.rammstein.imagepickernewone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val RES_1080 = 1080

    private val FILE_PROVIDER = ".fileprovider"

    private val CAMERA_IMAGE_CAPTURE = 3
    private val CAMERA_IMAGE_FILE_PREFIX = "JPEG"
    private val CAMERA_IMAGE_FILE_SUFFIX = ".jpg"
    private val CAMERA_IMAGE_FILE_DATE_FORMAT = "yyyMMdd_HHmmss"

    private val STORAGE_IMAGE_GET = 4
    private val STORAGE_IMAGE_MIME_TYPE = "image/*"


    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fabCamera.setOnClickListener { view -> dispatchTakePictureFromCameraIntent() }
        fabGallery.setOnClickListener { view -> dispatchTakePictureFromStorageIntent() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_IMAGE_CAPTURE -> {
                    val image = BitmapUtils().getBitmapFromFile(
                            contentResolver, currentPhotoPath ?: "", RES_1080
                    )
                    mainImage.setImageBitmap(image)
                }
                STORAGE_IMAGE_GET -> {
                    data?.let {
                        val imageFile = createImageFile(
                                imageFileName = getImageFileName(),
                                imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
                        )
                        val image = BitmapUtils().getBitmapFromUri(contentResolver, it.data, imageFile, RES_1080)
                        image?.let { mainImage.setImageBitmap(image) }
                    }
                }
            }
        }
    }

    private fun dispatchTakePictureFromStorageIntent() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = STORAGE_IMAGE_MIME_TYPE
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, STORAGE_IMAGE_GET)
        }
    }

    private fun dispatchTakePictureFromCameraIntent() {
        val takePictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {

            val imageFile = createImageFile(
                    imageFileName = getImageFileName(),
                    imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
            )
            val photoUri = FileProvider.getUriForFile(this, packageName.plus(FILE_PROVIDER), imageFile)
            currentPhotoPath = imageFile.absolutePath

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, CAMERA_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(imageFileName: String, imageFilePath: String): File {
        val storageDir: File = File(imageFilePath)
        return File.createTempFile(imageFileName, CAMERA_IMAGE_FILE_SUFFIX, storageDir)
    }

    private fun getImageFileName(): String = CAMERA_IMAGE_FILE_PREFIX.plus(SimpleDateFormat(CAMERA_IMAGE_FILE_DATE_FORMAT).format(Date())).plus("_")
}
