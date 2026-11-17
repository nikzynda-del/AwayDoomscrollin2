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

        addSlider(
            root, pad, 1, 15, TikTokLimits.freeSwipes(this),
            { "Free swipes before a break: $it" },
            { TikTokLimits.setFreeSwipes(this, it) }
        )
        addSlider(
            root, pad, 1, 60, TikTokLimits.lockoutMinutes(this),
            { "Break length: $it min" },
            { TikTokLimits.setLockoutMinutes(this, it) }
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
