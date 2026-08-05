package com.zeno.dialer.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Voicemail(
    val id: Long,
    val number: String,
    val name: String?,
    val photoUri: String?,
    val date: Long,
    val durationSeconds: Long?,
    val isRead: Boolean,
    /** VoicemailContract.Voicemails.HAS_CONTENT — some VVM sources fetch audio lazily. */
    val hasContent: Boolean,
    /** The row's own content URI — playable directly via MediaPlayer. */
    val contentUri: Uri,
)
