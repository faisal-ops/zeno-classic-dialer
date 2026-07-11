package com.zeno.dialer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import com.zeno.dialer.ActiveCallInfo
import com.zeno.dialer.AppPreferences
import com.zeno.dialer.BroadcastActions
import com.zeno.dialer.CallStateHolder
import com.zeno.dialer.InCallActivity
import com.zeno.dialer.R
import java.util.concurrent.ConcurrentHashMap

private const val CHANNEL_ACTIVE     = "zeno_active_call_v2"
private const val CHANNEL_INCOMING   = "zeno_incoming_call"
private const val NOTIFICATION_ID    = 1001

class MyInCallService : InCallService() {

    companion object {
        @Volatile var instance: MyInCallService? = null

        /**
         * Arms a one-time vibration when our *outgoing* call transitions to [Call.STATE_ACTIVE]
         * (i.e. when the other side picks up).
         */
        @Volatile private var outgoingActiveVibrationArmed: Boolean = false
        @Volatile private var outgoingActiveVibrationArmedAtMs: Long = 0L

        fun armOutgoingCallActiveVibration() {
            outgoingActiveVibrationArmed = true
            outgoingActiveVibrationArmedAtMs = SystemClock.elapsedRealtime()
        }

        internal fun takeOutgoingActiveVibrationArmIfFresh(maxAgeMs: Long): Boolean {
            if (!outgoingActiveVibrationArmed) return false
            val age = SystemClock.elapsedRealtime() - outgoingActiveVibrationArmedAtMs
            return if (age in 0..maxAgeMs) {
                outgoingActiveVibrationArmed = false
                true
            } else {
                outgoingActiveVibrationArmed = false
                false
            }
        }
    }

    private lateinit var notifManager: NotificationManager

    /** True while [InCallActivity] is showing the incoming-call UI; suppresses heads-up (redundant with full screen). */
    @Volatile
    private var incomingCallUiForeground = false

    // ── Flip-to-silence ──────────────────────────────────────────────────────
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var flipToSilenceRegistered = false

    private val flipToSilenceListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.size < 3) return
            // Face-down (screen toward a flat surface): z-axis reads close to -g.
            if (event.values[2] < -7f && CallStateHolder.info.value?.state == Call.STATE_RINGING) {
                silenceRingerSafely()
                unregisterFlipToSilenceListener()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /** Registers/unregisters the accelerometer listener based on current ringing state + setting. */
    private fun updateFlipToSilenceListener() {
        val ringing = CallStateHolder.info.value?.state == Call.STATE_RINGING
        val enabled = getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(AppPreferences.KEY_FLIP_TO_SILENCE, false)
        if (ringing && enabled) registerFlipToSilenceListener() else unregisterFlipToSilenceListener()
    }

    private fun registerFlipToSilenceListener() {
        if (flipToSilenceRegistered) return
        val sensor = accelerometer ?: return
        flipToSilenceRegistered = sensorManager?.registerListener(
            flipToSilenceListener, sensor, SensorManager.SENSOR_DELAY_NORMAL
        ) ?: false
    }

    private fun unregisterFlipToSilenceListener() {
        if (!flipToSilenceRegistered) return
        sensorManager?.unregisterListener(flipToSilenceListener)
        flipToSilenceRegistered = false
    }

    // TelecomManager#silenceRinger is annotated as requiring MODIFY_PHONE_STATE, but Telecom
    // grants it to the current default dialer as well (which this InCallService always is while
    // running) — MODIFY_PHONE_STATE itself is a signature permission apps can't hold. Guarded
    // with try/catch since we can't declare the permission to satisfy lint statically.
    @android.annotation.SuppressLint("MissingPermission")
    private fun silenceRingerSafely() {
        try {
            getSystemService(TelecomManager::class.java)?.silenceRinger()
        } catch (_: SecurityException) {
        }
    }

    // ── Caller ID announcement ──────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private val announcedCalls = java.util.Collections.newSetFromMap(ConcurrentHashMap<Call, Boolean>())

    /** Speaks the caller's name/number once per ringing call, per the Caller ID announcement setting. */
    private fun maybeAnnounceCallerId(call: Call) {
        if (call.state != Call.STATE_RINGING) return
        if (!announcedCalls.add(call)) return // already announced this call

        val mode = getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
            .getInt(AppPreferences.KEY_CALLER_ID_ANNOUNCE, 0) // 0=never, 1=always, 2=headset only
        if (mode == 0) return
        if (mode == 2 && !isHeadsetConnected()) return

        val info = CallStateHolder.info.value ?: return
        if (info.call !== call) return
        if (!ttsReady) return

        val text = "${getString(R.string.incoming_call)}, ${info.displayName}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zeno_caller_id_announce")
    }

    private fun isHeadsetConnected(): Boolean {
        val am = getSystemService(AudioManager::class.java) ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
    }

    fun setIncomingCallUiForeground(foreground: Boolean) {
        if (incomingCallUiForeground == foreground) return
        incomingCallUiForeground = foreground
        updateNotification()
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            // Outgoing call "other end picked up": state transition to ACTIVE.
            if (state == Call.STATE_ACTIVE) {
                val shouldVibrate = takeOutgoingActiveVibrationArmIfFresh(maxAgeMs = 90_000L)
                if (shouldVibrate) vibrateAnsweredOnce()
            }
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                // Disarm in case the call never became ACTIVE.
                takeOutgoingActiveVibrationArmIfFresh(maxAgeMs = 0L)
            }

            CallStateHolder.update(call, this@MyInCallService)
            updateNotification()
            updateFlipToSilenceListener()
            maybeAnnounceCallerId(call)
        }
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallStateHolder.update(call, this@MyInCallService)
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        notifManager = getSystemService(NotificationManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tts = TextToSpeech(this) { status -> ttsReady = status == TextToSpeech.SUCCESS }

        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACTIVE,
                getString(R.string.active_call),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.active_call_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        )

        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING,
                getString(R.string.incoming_calls_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.incoming_calls_channel_desc)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
        )
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallStateHolder.update(call, this)
        call.registerCallback(callCallback)
        updateNotification()
        updateFlipToSilenceListener()
        maybeAnnounceCallerId(call)

        startActivity(
            Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterFlipToSilenceListener()
        tts?.shutdown()
        tts = null
        instance = null
    }

    fun applyMute(muted: Boolean)    = setMuted(muted)
    fun applySpeaker(on: Boolean)    = setAudioRoute(
        if (on) android.telecom.CallAudioState.ROUTE_SPEAKER
        else    android.telecom.CallAudioState.ROUTE_EARPIECE
    )
    fun isMuted():     Boolean = callAudioState?.isMuted ?: false
    fun isSpeakerOn(): Boolean = callAudioState?.route == android.telecom.CallAudioState.ROUTE_SPEAKER

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        CallStateHolder.remove(call)
        announcedCalls.remove(call)
        // Stop recording as soon as the specific call being recorded ends — even if another
        // call is still active — so recording never silently bleeds into a different call.
        if (CallRecorder.isRecordingCall(call)) {
            CallRecorder.stop(this)
        }
        updateFlipToSilenceListener()
        if (calls.isEmpty()) {
            CallStateHolder.clear()
            notifManager.cancel(NOTIFICATION_ID)
        } else {
            updateNotification()
        }
    }

    private fun vibrateAnsweredOnce() {
        runCatching {
            val vibrator = getSystemService(Vibrator::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200L)
            }
        }
    }

    private fun updateNotification() {
        val info = CallStateHolder.info.value ?: return
        if (info.state != Call.STATE_RINGING) {
            incomingCallUiForeground = false
        }

        val fullScreenIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (info.state == Call.STATE_RINGING) {
            showIncomingNotification(info, fullScreenIntent)
            return
        }

        val stateText = when (info.state) {
            Call.STATE_ACTIVE     -> getString(R.string.active_call)
            Call.STATE_HOLDING    -> getString(R.string.on_hold)
            Call.STATE_DIALING    -> getString(R.string.calling)
            Call.STATE_CONNECTING -> getString(R.string.connecting)
            else                  -> getString(R.string.tab_calls)
        }

        val hangupIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent(BroadcastActions.HANGUP).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_notification_call)
            .setContentTitle(info.displayName)
            .setContentText(stateText)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(0xFF4CAF50.toInt())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_call),
                    getString(R.string.end_call),
                    hangupIntent
                ).build()
            )
            .build()

        notifManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showIncomingNotification(info: ActiveCallInfo, fullScreenIntent: PendingIntent) {
        val answerIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent(BroadcastActions.ANSWER).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = PendingIntent.getBroadcast(
            this, 3,
            Intent(BroadcastActions.DECLINE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val useHeadsUp = !incomingCallUiForeground
        val channel = if (useHeadsUp) CHANNEL_INCOMING else CHANNEL_ACTIVE
        val b = Notification.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification_call)
            .setContentTitle(info.displayName)
            .setContentText(getString(R.string.incoming_call))
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(0xFF4CAF50.toInt())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_call),
                    getString(R.string.decline),
                    declineIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_call),
                    getString(R.string.answer),
                    answerIntent
                ).build()
            )
        if (useHeadsUp) {
            b.setFullScreenIntent(fullScreenIntent, true)
        }
        notifManager.notify(NOTIFICATION_ID, b.build())
    }
}
