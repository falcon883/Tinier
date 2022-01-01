package com.durvank883.tinier.model

import android.graphics.Bitmap

data class CompressionConfig(
    val quality: Int = 0,
    val maxImageSize: Long = 0,
    val exportFormat: Bitmap.CompressFormat? = Bitmap.CompressFormat.JPEG,
    val trailingName: String = ""
)
