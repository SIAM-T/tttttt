package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageHelper {

    fun saveUriToInternalStorage(context: Context, uri: Uri, prefix: String = "img"): String? {
        return try {
            val imagesDir = File(context.filesDir, "store_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "proof"): String? {
        return try {
            val imagesDir = File(context.filesDir, "store_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFileIfExist(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatCurrency(amount: Double): String {
        return String.format(Locale.US, "৳%,.2f", amount)
    }
}
