package com.hanyi.gamecontroller.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemInterruptionRepository @Inject constructor(
    private val context: Context
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun observeInterruption(): Flow<Unit> = callbackFlow {

        val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        trySend(Unit)
                    }
                }
            }
        } else null

        try {
            // Only register if we have the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
            }
        } catch (e: SecurityException) {
            Log.e("SystemInterruption", "Permission READ_PHONE_STATE not granted yet.")
            // Do not close the flow, just wait. Or close with error if you prefer.
        }

        // Audio Focus listener (Alarms) usually doesn't require "Dangerous" permissions
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                trySend(Unit)
            }
        }

        // Registering Call Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
            telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
        }

        // Registering Audio Focus Listener (How Alarms are detected)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        }

        awaitClose {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
                    telephonyManager.unregisterTelephonyCallback(telephonyCallback)
                }
            } catch (e: Exception) { }
        }
    }
}