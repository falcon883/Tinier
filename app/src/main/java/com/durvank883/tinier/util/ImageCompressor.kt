package com.durvank883.tinier.util

import android.content.Context
import id.zelory.compressor.Compressor
import java.io.File

class ImageCompressor {

    suspend fun compressImage(context: Context, imageFile: File) {
        Compressor.compress(
            context = context,
            imageFile = imageFile
        )
    }
}