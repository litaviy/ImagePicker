package com.rammstein.imagepickernewone

import android.content.ContentResolver
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import java.io.*

/**
 * Created by klitaviy on 5/21/18-12:22 PM.
 */
open class BitmapUtils {

    fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri, target: File, targetWidth: Int): Bitmap? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var bitmap: Bitmap? = null
        try {

            inputStream = contentResolver.openInputStream(uri)
            outputStream = FileOutputStream(target)

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)

            options.inSampleSize = calculateInSampleSize(options, options.outWidth, options.outHeight)
            options.inJustDecodeBounds = false
            options.inTempStorage = ByteArray(16 * 1024)

            inputStream = contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } finally {
            outputStream?.flush()
            outputStream?.close()
            inputStream?.close()
        }

        return if (bitmap != null) {
            optimizeBitmap(bitmap, target.absolutePath, targetWidth)
        } else {
            null
        }
    }

    fun getBitmapFromFile(contentResolver: ContentResolver, filePath: String, targetWidth: Int): Bitmap {
        val imageFile = File(filePath)
        val sourceBitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(imageFile))

        return optimizeBitmap(sourceBitmap, filePath, targetWidth)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }

        return inSampleSize
    }

    private fun optimizeBitmap(sourceBitmap: Bitmap, filePath: String, targetWidth: Int): Bitmap {
        val matrix = Matrix()

        val exif = ExifInterface(filePath)
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Check Rotation
        if (rotation != ExifInterface.ORIENTATION_UNDEFINED) {
            val rotationInDegrees = exifToDegrees(rotation)
            matrix.postRotate(rotationInDegrees.toFloat())
        }

        // Check Size
        if (sourceBitmap.width > targetWidth) {
            val aspectRatio = sourceBitmap.width / sourceBitmap.height.toFloat()
            val height = Math.round(targetWidth / aspectRatio)

            val scaledImage = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)

            val originalWidth = sourceBitmap.width.toFloat()
            val originalHeight = sourceBitmap.height.toFloat()

            val canvas = Canvas(scaledImage)

            val scale = targetWidth / originalWidth

            val xTranslation = 0.0f
            val yTranslation = (height - originalHeight * scale) / 2.0f

            matrix.postTranslate(xTranslation, yTranslation)
            matrix.preScale(scale, scale)

            val paint = Paint()
            paint.isFilterBitmap = true

            canvas.drawBitmap(sourceBitmap, matrix, paint)
        }

        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, false)
    }

    private fun exifToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}