package com.durvank883.tinier.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.durvank883.tinier.model.ImageRes
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImageCompressor @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private var quality: Int = 80
    private var format: Bitmap.CompressFormat? = Bitmap.CompressFormat.JPEG
    private var size: Long = 0
    private var appendName: String = "_compressed"
    private var appendNameAtStart: Boolean = false
    private var imageRes: ImageRes = ImageRes()

    fun setConfig(
        quality: Int = 80,
        format: Bitmap.CompressFormat? = Bitmap.CompressFormat.JPEG,
        size: Long = 0,
        appendName: String = "_compressed",
        appendNameAtStart: Boolean = false,
        imageRes: ImageRes = ImageRes(),
    ): ImageCompressor {
        this.quality = quality
        this.format = format
        this.size = size
        this.appendName = appendName
        this.appendNameAtStart = appendNameAtStart
        this.imageRes = imageRes

        return this
    }

    fun getPath(
        activity: ComponentActivity,
        listener: PickiTCallbacks,
        imageUri: Uri
    ) = PickiT(
        context,
        listener,
        activity
    ).getPath(imageUri, Build.VERSION.SDK_INT)


    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveImageToStorage(
        imageFile: File,
        filename: String = imageFile.name,
        mimeType: String = "image/${imageFile.extension}",
        directory: String = "${Environment.DIRECTORY_PICTURES}${File.separator}Tinier",
        mediaContentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, directory)
        }

        context.contentResolver.runCatching {
            context.contentResolver.insert(mediaContentUri, values)?.let { uri ->
                openOutputStream(uri)?.let { imageOutStream ->
                    imageOutStream.use { it.write(imageFile.readBytes()) }
                }
            }
        }
    }

    suspend fun compressImage(imagePath: String?) {
        val compressedImagesPath = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).absolutePath + File.separator + "Tinier"
        )

        if (!compressedImagesPath.exists()) {
            compressedImagesPath.mkdirs()
        }

        if (!imagePath.isNullOrBlank()) {
            val imageFile = File(imagePath)

            val extension = when (format) {
                Bitmap.CompressFormat.JPEG -> ".jpg"
                Bitmap.CompressFormat.PNG -> ".png"
                Bitmap.CompressFormat.WEBP -> ".webp"
                else -> ".jpg"
            }

            val nameToAppend = if (appendNameAtStart) {
                "$appendName${imageFile.nameWithoutExtension}$extension"
            } else {
                "${imageFile.nameWithoutExtension}$appendName$extension"
            }

            val compressedFile = Compressor.compress(
                context = context,
                imageFile = imageFile
            ) {
                quality(quality = quality)

                format?.let { format(it) }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    destination(
                        File(
                            compressedImagesPath,
                            nameToAppend
                        )
                    )
                }

                if (size != 0L) {
                    size(maxFileSize = size)
                }

                if (imageRes.imageHeight != 0 || imageRes.imageWidth != 0) {
                    resolution(imageRes.imageWidth, imageRes.imageHeight)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToStorage(
                    imageFile = compressedFile,
                    filename = nameToAppend
                )
            }

            Log.d("TAG", "compressImage: Compressed ${compressedFile.absolutePath}")
        }
    }
}