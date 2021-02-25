/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.dji.ux.beta.sample.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ScrollView
import androidx.core.content.res.use
import com.dji.ux.beta.sample.R
import dji.ux.beta.core.extension.getIntAndUse

class SettingsDrawerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    //region Fields
    private var isPanelOpen = true
    private var drawerPosition = DrawerPosition.LEFT
    //endregion

    init {
        attrs?.let { initAttributes(context, it) }
    }

    /**
     * Expands and collapses the panel.
     */
    fun movePanel(btnPanel: View) {
        val translationStart: Float
        val translationEnd: Float
        if (isPanelOpen) {
            translationStart = 0f
            translationEnd = if (drawerPosition == DrawerPosition.RIGHT) {
                width.toFloat()
            } else {
                -width.toFloat()
            }
        } else {
            bringToFront()
            translationStart = if (drawerPosition == DrawerPosition.RIGHT) {
                width.toFloat()
            } else {
                -width.toFloat()
            }
            translationEnd = 0f
        }
        val animate = TranslateAnimation(
                translationStart, translationEnd, 0f, 0f)
        animate.duration = 300
        animate.fillAfter = true
        animate.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                // do nothing
            }

            override fun onAnimationEnd(animation: Animation) {
                btnPanel.bringToFront()
                isPanelOpen = !isPanelOpen
            }

            override fun onAnimationRepeat(animation: Animation) {
                // do nothing
            }
        })
        startAnimation(animate)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SettingsDrawerView).use { typedArray ->
            typedArray.getIntAndUse(R.styleable.SettingsDrawerView_drawerPosition) {
                drawerPosition = DrawerPosition.find(it)
            }
        }
    }

    enum class DrawerPosition(@get:JvmName("value") val value: Int) {
        LEFT(0), RIGHT(1);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(value: Int): DrawerPosition {
                return values.find { it.value == value } ?: LEFT
            }
        }
    }
}