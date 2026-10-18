package com.nipuna.gamedock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    companion object { @Volatile var running = false; private const val CH = "overlay" }

    private lateinit var wm: WindowManager
    private var root: View? = null
    private lateinit var lp: WindowManager.LayoutParams

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        addOverlay()
        running = true
        NativeBridge.setOverlayVisible(true)
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Floating window", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, CH).setContentTitle("GameDock")
            .setContentText("Floating window active").setSmallIcon(android.R.drawable.ic_menu_view).build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun addOverlay() {
        val dp = resources.displayMetrics.density
        var expanded = true
        val body = TextView(this).apply {
            text = "Native v${NativeBridge.version()}"; setTextColor(Color.parseColor("#C9D1D9")); textSize = 13f
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
        }
        val header = TextView(this).apply {
            text = "GameDock  ▾"; setTextColor(Color.WHITE); textSize = 14f
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = 18 * dp; setColor(Color.parseColor("#E6161B22")); setStroke((1 * dp).toInt(), Color.parseColor("#8B5CF6"))
            }
            addView(header); addView(body)
        }
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = (16 * dp).toInt(); y = (120 * dp).toInt() }

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        header.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    lp.x = sx + dx; lp.y = sy + dy
                    runCatching { wm.updateViewLayout(box, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    expanded = !expanded
                    body.visibility = if (expanded) View.VISIBLE else View.GONE
                    header.text = if (expanded) "GameDock  ▾" else "GameDock  ▸"
                    NativeBridge.setOverlayVisible(expanded)
                    body.text = "Native v${NativeBridge.version()} · toggles ${NativeBridge.overlayToggles()}"
                }
            }
            true
        }
        runCatching { wm.addView(box, lp); root = box }.onFailure { stopSelf() }
    }

    override fun onDestroy() {
        root?.let { runCatching { wm.removeView(it) } }; root = null
        running = false
        NativeBridge.setOverlayVisible(false)
        super.onDestroy()
    }
}
