package com.example.floatvol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FloatService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        if (bubble == null) addBubble()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val ch = "floatvol"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(ch, "Floating volume", NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, ch)
            .setContentTitle("Floating volume is on")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .build()
    }

    private fun addBubble() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val size = (48 * resources.displayMetrics.density).toInt()

        val v = View(this)
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#66000000"))
            setStroke(3, Color.parseColor("#88FFFFFF"))
        }
        v.alpha = 0.7f

        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 0
        lp.y = 400

        fun change(direction: Int) {
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        var longPressed = false

        val lowerRunnable = object : Runnable {
            override fun run() {
                longPressed = true
                change(AudioManager.ADJUST_LOWER)
                handler.postDelayed(this, 150)
            }
        }

        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    longPressed = false
                    handler.postDelayed(lowerRunnable, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        moved = true
                        handler.removeCallbacks(lowerRunnable)
                    }
                    if (moved) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(lowerRunnable)
                    if (!moved && !longPressed) change(AudioManager.ADJUST_RAISE)
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(lowerRunnable)
            }
            true
        }

        wm.addView(v, lp)
        bubble = v
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        bubble?.let { wm.removeView(it) }
        super.onDestroy()
    }
}
