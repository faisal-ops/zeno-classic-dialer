package com.zeno.dialer.service

import android.content.Context
import android.content.ContentValues
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-oriented call recording using [MediaRecorder].
 *
 * **Audio sources (tried in order):**
 * 1. [MediaRecorder.AudioSource.VOICE_CALL] — uplink + downlink when the app holds ROLE_DIALER (OEM-dependent).
 * 2. [MediaRecorder.AudioSource.VOICE_COMMUNICATION] — VoIP-tuned capture on some devices.
 * 3. [MediaRecorder.AudioSource.MIC] — microphone fallback (typically local side only).
 *
 * **Default save location (scoped storage, no extra storage permissions):**
 * `Android/data/<applicationId>/files/Music/BBClassicDialer/Recordings/`
 *
 * Example (release): `/storage/emulated/0/Android/data/com.zeno.zenoclassicdialer/files/Music/ZenoClassicDialer/Recordings/`
 * Debug builds use the `applicationId` with the `.debug` suffix from Gradle.
 * Use [defaultSavePathSummary] for the exact path on the device.
 */
object CallRecorder {

    private const val TAG = "CallRecorder"

    /** Subfolder under [Context.getExternalFilesDir] [Environment.DIRECTORY_MUSIC]. */
    const val RECORDINGS_SUBDIR = "ZenoClassicDialer/Recordings"

    /** Public-folder target path: `Recordings/ZenoClassicDialer/`. */
    private const val PUBLIC_RECORDINGS_SUBDIR = "ZenoClassicDialer"
    private const val MIME_TYPE_M4A = "audio/mp4"

    private const val MIN_FREE_BYTES = 20L * 1024L * 1024L
    private const val MIN_VALID_FILE_BYTES = 512L

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var recordingStartedAtMs: Long = 0L

    private val lock = Any()

    val isRecording: Boolean get() = synchronized(lock) { recorder != null }

    /** Absolute directory where `.m4a` files are written. */
    fun recordingsDirectory(context: Context): File {
        val base = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.applicationContext.filesDir
        return File(base, RECORDINGS_SUBDIR).also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    /** Full path string for support or a future settings screen. */
    fun defaultSavePathSummary(context: Context): String =
        recordingsDirectory(context).absolutePath

    /**
     * Starts recording. Thread-safe. Returns output file path, or null on failure.
     */
    fun start(context: Context, contactName: String, phoneNumber: String): String? = synchronized(lock) {
        if (recorder != null) return currentFile?.absolutePath

        val appCtx = context.applicationContext

        val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            Log.i(TAG, "AudioManager before record: mode=${it.mode} micMuted=${it.isMicrophoneMute}")
        }
        runCatching {
            val svc = MyInCallService.instance
            Log.i(TAG, "MyInCallService before record: isMuted=${svc?.isMuted()} isSpeakerOn=${svc?.isSpeakerOn()}")
        }
        if (freeBytes(appCtx) < MIN_FREE_BYTES) {
            Log.w(TAG, "Insufficient free space for recording")
            return null
        }

        val dir = recordingsDirectory(appCtx)
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Cannot create recordings dir: ${dir.absolutePath}")
            return null
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = sanitizeFileStem(contactName).take(30)
        val stem = safeName.ifEmpty {
            phoneNumber.filter { it.isDigit() || it == '+' }.ifEmpty { "call" }
        }
        val fileName = "${timestamp}_$stem.m4a"
        val file = File(dir, fileName)

        // VOICE_CALL: both sides via modem, OEM-dependent (may fail/silence on restricted devices).
        // UNPROCESSED: raw mic without AEC — when speaker is on, captures both sides because
        //   AEC is not present to suppress the speaker output.
        // VOICE_COMMUNICATION / MIC: local voice only (AEC suppresses speaker audio).
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
        )

        for (source in sources) {
            val mr = newMediaRecorder(appCtx)
            try {
                mr.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error what=$what extra=$extra")
                    synchronized(lock) {
                        if (recorder === mr) {
                            releaseRecorderInstance(mr)
                            recorder = null
                            currentFile?.delete()
                            currentFile = null
                        } else {
                            releaseRecorderInstance(mr)
                        }
                    }
                }
                mr.setAudioSource(source)
                configureAndPrepare(mr, file)
                mr.start()
                recorder = mr
                currentFile = file
                recordingStartedAtMs = android.os.SystemClock.elapsedRealtime()
                Log.i(TAG, "Recording started: ${file.absolutePath} source=$source")
                return file.absolutePath
            } catch (e: Exception) {
                Log.w(TAG, "AudioSource $source failed: ${e.message}")
                releaseRecorderInstance(mr)
                if (file.exists() && file.length() == 0L) file.delete()
            }
        }

        Log.e(TAG, "All audio sources failed")
        if (file.exists() && file.length() == 0L) file.delete()
        return null
    }

    private fun newMediaRecorder(context: Context): MediaRecorder =
        MediaRecorder(context.applicationContext)

    private fun configureAndPrepare(mr: MediaRecorder, file: File) {
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(128_000)
        mr.setAudioSamplingRate(44_100)
        mr.setAudioChannels(1)
        mr.setOutputFile(file.absolutePath)
        try {
            mr.prepare()
        } catch (e: IOException) {
            if (file.exists() && file.length() == 0L) file.delete()
            throw e
        }
    }

    /**
     * Stops recording and finalizes the file.
     *
     * If [context] is provided, the finished `.m4a` is also copied into the public
     * `Recordings/ZenoClassicDialer/` folder (via [MediaStore]) so it appears in the system
     * “Recordings” UI.
     *
     * Returns the app-scoped absolute path if the file looks valid, else null.
     */
    fun stop(context: Context? = null): String? {
        val fileToFinalize = synchronized(lock) {
            val file = currentFile
            val mr = recorder
            recorder = null
            currentFile = null
            val startAtMs = recordingStartedAtMs
            recordingStartedAtMs = 0L

            var stopOk = true
            if (mr != null) {
                try {
                    mr.stop()
                } catch (e: Exception) {
                    stopOk = false
                    Log.w(TAG, "stop() failed: ${e.message}")
                }
                releaseRecorderInstance(mr)
            }

            if (file == null || !file.exists()) return@synchronized null
            val len = file.length()
            val durMs = if (startAtMs != 0L) (android.os.SystemClock.elapsedRealtime() - startAtMs) else -1L

            if (!stopOk) {
                file.delete()
                Log.w(TAG, "Recording discarded because stop failed (durMs=$durMs, len=$len)")
                return@synchronized null
            }
            if (len < MIN_VALID_FILE_BYTES) {
                file.delete()
                Log.w(TAG, "Discarded short recording (durMs=$durMs, $len bytes)")
                return@synchronized null
            }
            Log.i(TAG, "Recording saved: ${file.absolutePath} (durMs=$durMs, $len bytes)")
            file
        } ?: return null

        if (context != null) {
            copyToPublicRecordings(context, fileToFinalize)
        }

        return fileToFinalize.absolutePath
    }

    private fun copyToPublicRecordings(context: Context, file: File) {
        val resolver = context.applicationContext.contentResolver
        val relativePath = "${Environment.DIRECTORY_RECORDINGS}/$PUBLIC_RECORDINGS_SUBDIR"
        val displayName = file.name

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_M4A)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = try {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore insert failed: ${e.message}")
            null
        } ?: run {
            Log.w(TAG, "MediaStore insert returned null uri")
            return
        }

        try {
            var bytesCopied = 0L
            resolver.openOutputStream(uri, "w").use { out ->
                if (out == null) throw IOException("openOutputStream returned null")
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        bytesCopied += read.toLong()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pending = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, pending, null, null)
            }
            Log.i(TAG, "Copied recording to public: $uri (bytesCopied=$bytesCopied)")
        } catch (e: Exception) {
            Log.e(TAG, "Copy recording to public failed: ${e.message}")
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) { }
        }
    }

    private fun releaseRecorderInstance(mr: MediaRecorder) {
        try {
            mr.release()
        } catch (_: Exception) {
        }
    }

    private fun freeBytes(context: Context): Long {
        return try {
            val dir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.applicationContext.filesDir
            StatFs(dir.path).availableBytes
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    internal fun sanitizeFileStem(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
}
