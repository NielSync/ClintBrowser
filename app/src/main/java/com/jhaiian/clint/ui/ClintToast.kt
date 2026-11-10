package com.jhaiian.clint.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.app.ClintApplication
import com.jhaiian.clint.customtheme.isCustomAccent
import com.jhaiian.clint.customtheme.resolveCustomPalette
import com.jhaiian.clint.customtheme.toClintColors
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.ui.theme.resolveClintTheme
import java.lang.ref.WeakReference

/**
 * Swipe-dismissable replacement for [Toast]. System toast windows are drawn
 * by the OS and cannot receive gestures, so messages are shown as an in-app
 * pill attached to the current activity's decor view: it auto-dismisses and
 * can be swiped away horizontally at any time. Falls back to a system toast
 * when no activity is in the foreground (e.g. app backgrounded).
 */
object ClintToast {

    private const val TOAST_TAG = "com.jhaiian.clint.ClintToast"
    private const val SHORT_DURATION_MS = 2000L
    private const val LONG_DURATION_MS = 3500L
    private const val SWIPE_OUT_DURATION_MS = 200L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentToast: WeakReference<ToastView>? = null

    fun show(context: Context, message: CharSequence, longDuration: Boolean = false) {
        val activity = findHostActivity(context)
        if (activity == null) {
            Toast.makeText(context, message, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(activity, message, longDuration) }
            return
        }
        show(activity, message, longDuration)
    }

    fun show(context: Context, @StringRes resId: Int, longDuration: Boolean = false) {
        show(context, context.getText(resId), longDuration)
    }

    private fun show(activity: Activity, message: CharSequence, longDuration: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) {
            Toast.makeText(activity, message, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            return
        }
        currentToast?.get()?.dismiss(animated = false)
        val durationMs = recommendedTimeout(activity, if (longDuration) LONG_DURATION_MS else SHORT_DURATION_MS)
        val view = ToastView(activity, message, durationMs)
        currentToast = WeakReference(view)
        val decor = activity.window.decorView as? ViewGroup ?: return
        view.tag = TOAST_TAG
        decor.addView(view)
    }

    private fun findHostActivity(context: Context): Activity? {
        (context.applicationContext as? ClintApplication)?.currentActivity
            ?.takeIf { !it.isFinishing && !it.isDestroyed }
            ?.let { return it }
        var ctx: Context = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity && !ctx.isFinishing && !ctx.isDestroyed) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun recommendedTimeout(context: Context, timeoutMs: Long): Long {
        // Kept in sync with the snackbar timeout logic in ClintSnackbar.kt.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return timeoutMs
        val manager = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java) ?: return timeoutMs
        return manager.getRecommendedTimeoutMillis(
            timeoutMs.toInt(),
            android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT or
                android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS
        ).toLong()
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private class ToastView(activity: Activity, message: CharSequence, durationMs: Long) : TextView(activity) {

        private val dismissRunnable = Runnable { dismiss(animated = true) }
        private var downX = 0f
        private var dragging = false
        private var dismissed = false

        init {
            val density = resources.displayMetrics.density
            val colors = resolveColors(activity)
            text = message
            setTextColor(colors.popupText.toArgb())
            textSize = 14f
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            background = GradientDrawable().apply {
                setColor(colors.popupBackground.toArgb())
                cornerRadius = 22f * density
                setStroke((1f * density).toInt().coerceAtLeast(1), colors.popupStroke.toArgb())
            }
            elevation = 6f * density
            val padH = (20 * density).toInt()
            val padV = (12 * density).toInt()
            setPadding(padH, padV, padH, padV)

            val insets = WindowInsetsCompat.toWindowInsetsCompat(activity.window.decorView.rootWindowInsets)
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val sideMarginPx = (24 * density).toInt()
            val bottomMarginPx = navInset + (16 * density).toInt()
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                leftMargin = sideMarginPx
                rightMargin = sideMarginPx
                bottomMargin = bottomMarginPx
            }
            maxWidth = resources.displayMetrics.widthPixels - 2 * sideMarginPx

            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        dragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        if (dragging || kotlin.math.abs(dx) > 12f * density) {
                            dragging = true
                            translationX = dx
                            alpha = (1f - (kotlin.math.abs(dx) / (width.coerceAtLeast(1) * 0.9f))).coerceIn(0f, 1f)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = translationX
                        if (dragging && kotlin.math.abs(dx) >= width * 0.45f) {
                            dismiss(animated = true, slideDirection = if (dx > 0) 1 else -1)
                        } else {
                            animate().translationX(0f).alpha(1f).setDuration(150L).start()
                        }
                        dragging = false
                        true
                    }
                    else -> false
                }
            }

            alpha = 0f
            translationY = 24f * density
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator())
                .start()

            mainHandler.postDelayed(dismissRunnable, durationMs)
        }

        fun dismiss(animated: Boolean, slideDirection: Int = 0) {
            if (dismissed) return
            dismissed = true
            mainHandler.removeCallbacks(dismissRunnable)
            if (currentToast?.get() === this) currentToast = null
            if (!animated || !isAttachedToWindow) {
                removeFromParent()
                return
            }
            val animator = animate()
                .alpha(0f)
                .setDuration(SWIPE_OUT_DURATION_MS)
                .withEndAction { removeFromParent() }
            if (slideDirection != 0) {
                animator.translationX(slideDirection * (width + 48f * resources.displayMetrics.density))
            } else {
                animator.translationY(24f * resources.displayMetrics.density)
            }
            animator.start()
        }

        private fun removeFromParent() {
            (parent as? ViewGroup)?.removeView(this)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            mainHandler.removeCallbacks(dismissRunnable)
        }
    }

    private fun resolveColors(activity: Activity): ClintColors {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val isLight = !ThemeMode.isLight(theme)
        val accent = prefs.getString("accent_color", "material_you") ?: "material_you"
        val intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        val palette = resolveCustomPalette(activity, accent, isLight)
        if (palette != null) return palette.toClintColors(isLight)
        return ClintColors(
            resolveClintTheme(
                activity,
                if (isLight) "light" else "dark",
                if (isCustomAccent(accent)) "material_you" else accent,
                intensity
            )
        )
    }
}
