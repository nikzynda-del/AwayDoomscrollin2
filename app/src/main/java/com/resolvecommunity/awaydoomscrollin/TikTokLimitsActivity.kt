package com.resolvecommunity.awaydoomscrollin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Saved TikTok limits. The accessibility service reads these on every swipe. */
object TikTokLimits {
    private const val FILE = "tiktok_limits"
    private const val KEY_SWIPES = "free_swipes"
    private const val KEY_MINUTES = "lockout_minutes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun freeSwipes(context: Context): Int =
        prefs(context).getInt(KEY_SWIPES, 5).coerceIn(1, 15)

    fun lockoutMinutes(context: Context): Int =
        prefs(context).getInt(KEY_MINUTES, 10).coerceIn(1, 60)

    fun setFreeSwipes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SWIPES, value.coerceIn(1, 15)).apply()
    }

    fun setLockoutMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_MINUTES, value.coerceIn(1, 60)).apply()
    }

    fun lockedUntil(context: Context): Long =
    prefs(context).getLong("locked_until", 0L)

fun setLockedUntil(context: Context, value: Long) {
    prefs(context).edit().putLong("locked_until", value).apply()
}

fun lockoutSeconds(context: Context): Int =
    prefs(context).getInt("lockout_seconds", 600).coerceIn(1, 3600)

fun setLockoutSeconds(context: Context, value: Int) {
    prefs(context).edit().putInt("lockout_seconds", value.coerceIn(1, 3600)).apply()
}

fun lockoutMs(context: Context): Long = lockoutSeconds(context) * 1000L

fun limitByTime(context: Context): Boolean =
    prefs(context).getBoolean("limit_by_time", false)

fun setLimitByTime(context: Context, value: Boolean) {
    prefs(context).edit().putBoolean("limit_by_time", value).apply()
}

fun freeSeconds(context: Context): Int =
    prefs(context).getInt("free_seconds", 30).coerceIn(10, 600)

fun setFreeSeconds(context: Context, value: Int) {
    prefs(context).edit().putInt("free_seconds", value.coerceIn(10, 600)).apply()
}
}

/** Plain-views settings screen: two sliders, no XML layout or extra libraries needed. */
class TikTokLimitsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 3, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "TikTok limits"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Changes save automatically."
            setPadding(0, pad / 2, 0, pad / 2)
        })

        fun breakSeconds(p: Int): Int = if (p <= 60) p else (p - 59) * 60
fun breakLabel(sec: Int): String = if (sec < 60) "$sec s" else "${sec / 60} min"

val modeButton = android.widget.Button(this)
val swipesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
val timeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

fun showMode() {
    val byTime = TikTokLimits.limitByTime(this)
    swipesBox.visibility = if (byTime) android.view.View.GONE else android.view.View.VISIBLE
    timeBox.visibility = if (byTime) android.view.View.VISIBLE else android.view.View.GONE
    modeButton.text = if (byTime) "Limit by: TIME (tap to switch)" else "Limit by: SWIPES (tap to switch)"
}
modeButton.setOnClickListener {
    TikTokLimits.setLimitByTime(this, !TikTokLimits.limitByTime(this))
    showMode()
}

root.addView(modeButton)
root.addView(swipesBox)
root.addView(timeBox)
showMode()

addSlider(
    swipesBox, pad, 1, 15, TikTokLimits.freeSwipes(this),
    { "Free swipes before a break: $it" },
    { TikTokLimits.setFreeSwipes(this, it) }
)
addSlider(
    timeBox, pad, 1, 60, TikTokLimits.freeSeconds(this) / 10,
    { "Free time before a break: ${it * 10} s" },
    { TikTokLimits.setFreeSeconds(this, it * 10) }
)

val startSec = TikTokLimits.lockoutSeconds(this)
val startPos = if (startSec <= 60) startSec else (startSec / 60 + 59).coerceAtMost(119)
addSlider(
    root, pad, 1, 119, startPos,
    { "Break length: " + breakLabel(breakSeconds(it)) },
    { TikTokLimits.setLockoutSeconds(this, breakSeconds(it)) }
)

        setContentView(root)
    }

    private fun addSlider(
        root: LinearLayout,
        pad: Int,
        low: Int,
        high: Int,
        start: Int,
        label: (Int) -> String,
        onChange: (Int) -> Unit
    ) {
        val title = TextView(this).apply {
            textSize = 18f
            text = label(start)
            setPadding(0, pad, 0, pad / 2)
        }
        val bar = SeekBar(this).apply {
            max = high - low
            progress = start - low
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + low
                    title.text = label(value)
                    onChange(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        root.addView(title)
        root.addView(bar)
    }
}

internal var tikTokSwipeCount = 0
internal var tikTokLastSwipeAt = 0L
internal var tikTokLockedUntil = 0L
internal val tikTokSwipeDebounceMs = 400L
internal val tikTokCountResetMs = 15 * 60 * 1000L
internal var tikTokSessionStartAt = 0L
internal var tikTokLastToastAt = 0L
