package com.nipuna.gamedock

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    companion object { const val TARGET_PACKAGE = "com.dts.freefireth" }

    private lateinit var status: TextView
    private lateinit var btnOverlayPerm: Button
    private lateinit var btnToggle: Button
    private lateinit var btnLaunch: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeBridge.init()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun isInstalled(pkg: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33)
            packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        else @Suppress("DEPRECATION") packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) { false }

    private fun hasOverlay() = Settings.canDrawOverlays(this)

    private fun requestOverlay() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun toggleOverlay() {
        if (!hasOverlay()) { requestOverlay(); return }
        val i = Intent(this, OverlayService::class.java)
        if (OverlayService.running) stopService(i) else ContextCompat.startForegroundService(this, i)
        btnToggle.postDelayed({ refresh() }, 300)
    }

    private fun launchGame() {
        val intent = packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)
        if (intent == null) { Toast.makeText(this, "Game not installed", Toast.LENGTH_SHORT).show(); return }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun refresh() {
        val installed = isInstalled(TARGET_PACKAGE)
        val overlay = hasOverlay()
        status.text = buildString {
            append(if (installed) "●  Game installed\n" else "○  Game not installed\n")
            append(if (overlay) "●  Overlay permission granted\n" else "○  Overlay permission needed\n")
            append(if (OverlayService.running) "●  Floating window ON\n" else "○  Floating window OFF\n")
            append("Native core v${NativeBridge.version()}")
        }
        btnLaunch.isEnabled = installed
        btnOverlayPerm.isEnabled = !overlay
        btnToggle.text = if (OverlayService.running) "Hide floating window" else "Show floating window"
    }

    private fun buildUi() {
        val dp = resources.displayMetrics.density
        fun btn(t: String, c: Int, onClick: () -> Unit) = Button(this).apply {
            text = t; setTextColor(Color.WHITE); isAllCaps = false; textSize = 15f
            background = GradientDrawable().apply { cornerRadius = 16 * dp; setColor(c) }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, (52 * dp).toInt()).apply { topMargin = (12 * dp).toInt() }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0E1116")); setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), 0)
        }
        root.addView(TextView(this).apply { text = "GameDock"; textSize = 30f; setTextColor(Color.WHITE) })
        status = TextView(this).apply {
            setTextColor(Color.parseColor("#9AA4B2")); textSize = 14f; setLineSpacing(0f, 1.3f)
            setPadding(0, (16 * dp).toInt(), 0, (8 * dp).toInt())
        }
        root.addView(status)
        btnOverlayPerm = btn("Grant overlay permission", Color.parseColor("#3B82F6")) { requestOverlay() }
        btnToggle = btn("Show floating window", Color.parseColor("#8B5CF6")) { toggleOverlay() }
        btnLaunch = btn("Launch game", Color.parseColor("#10B981")) { launchGame() }
        root.addView(btnOverlayPerm); root.addView(btnToggle); root.addView(btnLaunch)
        setContentView(root)
    }
}
