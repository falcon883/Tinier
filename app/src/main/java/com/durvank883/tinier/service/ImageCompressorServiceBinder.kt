package com.durvank883.tinier.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject


@OptIn(ObsoleteCoroutinesApi::class)
class ImageCompressorServiceBinder @Inject constructor() {

    val TAG: String? = this::class.java.canonicalName

    var mService: MutableStateFlow<ImageCompressorService?> = MutableStateFlow(null)
    private var mBound = MutableStateFlow(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as ImageCompressorService.ICBinder
            mService.value = binder.getService()
            Log.d(TAG, "onServiceConnected: IC Service Connected")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mService.value = null
            Log.d(TAG, "onServiceDisconnected: IC Service Disconnected")
        }
    }

    fun cancelJob(context: Context) = mService.value?.cancelJob(context = context)

    fun doBindService(context: Context): Boolean {
        if (context.bindService(
                Intent(context, ImageCompressorService::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
        ) {
            mBound.value = true
        } else {
            Log.e(
                TAG, "Error: The requested service doesn't " +
                        "exist, or this client isn't allowed access to it."
            )
        }

        return mBound.value
    }

    fun doUnbindService(context: Context): Boolean {
        if (mBound.value) {
            // Release information about the service's state.
            context.unbindService(connection)
            mBound.value = false
        }

        return mBound.value
    }
}