package com.example.foodmemory.feature.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/** Returns a memory-sized bitmap with EXIF orientation applied to the actual pixels. */
fun decodeUprightBitmap(path: String, maxDimension: Int = 2560): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) sampleSize *= 2

    val source = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize }) ?: return null
    val exif = runCatching { ExifInterface(path) }.getOrNull()
    val rotation = exif?.rotationDegrees ?: 0
    val flipped = exif?.isFlipped ?: false
    if (rotation == 0 && !flipped) return source

    val transform = Matrix().apply {
        postRotate(rotation.toFloat())
        if (flipped) postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, transform, true).also {
        if (it !== source) source.recycle()
    }
}

/** Physically applies EXIF rotation before a newly captured JPEG is cached or shared. */
fun normalizeJpegOrientation(file: File, maxDimension: Int = 2560, quality: Int = 92) {
    val exif = ExifInterface(file.absolutePath)
    val rotation = exif.rotationDegrees
    val flipped = exif.isFlipped
    if (rotation == 0 && !flipped) return

    val upright = decodeUprightBitmap(file.absolutePath, maxDimension)
        ?: throw IllegalStateException("Captured photo could not be decoded")
    val temporary = File(file.parentFile, "${file.nameWithoutExtension}-upright.jpg")
    try {
        FileOutputStream(temporary).use { output ->
            check(upright.compress(Bitmap.CompressFormat.JPEG, quality, output))
        }
        ExifInterface(temporary.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
        temporary.inputStream().use { input -> FileOutputStream(file, false).use { output -> input.copyTo(output) } }
    } finally {
        upright.recycle()
        temporary.delete()
    }
}
