package com.durvank883.tinier.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Photo(val uri: Uri, val isSelected: Boolean = false) : Parcelable
