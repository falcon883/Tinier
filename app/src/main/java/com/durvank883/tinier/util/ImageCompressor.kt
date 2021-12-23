package com.durvank883.tinier.util

import android.content.Context
import android.graphics.Bitmap
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import java.io.File

class ImageCompressor {
    companion object {

        private var quality: Int = 80
        private var format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        private var size: Long = 0
        private var trailingName: String = "_compressed"

        fun setConfig(
            quality: Int = 80,
            format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            size: Long = 0,
            trailingName: String = "_compressed"
        ): Companion {
            this.quality = quality
            this.format = format
            this.size = size
            this.trailingName = trailingName

            return this
        }

        suspend fun compressImage(
            context: Context,
            imageFile: File
        ) {
            Compressor.compress(
                context = context,
                imageFile = imageFile
            ) {
                quality(quality = quality)
                format(format = format)
                size(maxFileSize = size)
            }
        }
    }
}