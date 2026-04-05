package com.zeno.dialer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.Call
import android.telecom.InCallService
import com.zeno.dialer.ActiveCallInfo
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

    private val lastKnownStates = ConcurrentHashMap<Call, Int>()

    fun setIncomingCallUiForeground(foreground: Boolean) {
        if (incomingCallUiForeground == foreground) return
        incomingCallUiForeground = foreground
        updateNotification()
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            lastKnownStates[call] = state

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

        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACTIVE,
                "Active call",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the ongoing call and quick controls"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        )

        notifManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notification for incoming calls"
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

        startActivity(
            Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
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
        lastKnownStates.remove(call)
        if (calls.isEmpty()) {
            CallRecorder.stop(this)
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
            Call.STATE_ACTIVE     -> "Active call"
            Call.STATE_HOLDING    -> "Call on hold"
            Call.STATE_DIALING    -> "Calling…"
            Call.STATE_CONNECTING -> "Connecting…"
            else                  -> "Call"
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
                    "End call",
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
            .setContentText("Incoming call")
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(0xFF4CAF50.toInt())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_call),
                    "Decline",
                    declineIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_call),
                    "Answer",
                    answerIntent
                ).build()
            )
        if (useHeadsUp) {
            b.setFullScreenIntent(fullScreenIntent, true)
        }
        notifManager.notify(NOTIFICATION_ID, b.build())
    }
}
