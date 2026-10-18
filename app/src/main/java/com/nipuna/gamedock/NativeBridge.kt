package com.nipuna.gamedock

import android.util.Log

/** Safe wrapper: the app keeps working even if the native lib fails to load. */
object NativeBridge {
    private const val TAG = "NativeBridge"
    @Volatile var available = false
        private set

    init {
        available = try { System.loadLibrary("gamedock"); true }
        catch (t: Throwable) { Log.e(TAG, "native load failed", t); false }
    }

    fun init(): Boolean = available && runCatching { nativeInit() }.getOrDefault(false)
    fun version(): String = if (available) runCatching { nativeVersion() }.getOrDefault("n/a") else "n/a"
    fun setOverlayVisible(v: Boolean) { if (available) runCatching { nativeSetOverlayVisible(v) } }
    fun overlayToggles(): Int = if (available) runCatching { nativeToggleCount() }.getOrDefault(0) else 0
    fun shutdown() { if (available) runCatching { nativeShutdown() } }

    private external fun nativeInit(): Boolean
    private external fun nativeVersion(): String
    private external fun nativeSetOverlayVisible(visible: Boolean)
    private external fun nativeToggleCount(): Int
    private external fun nativeShutdown()
}
