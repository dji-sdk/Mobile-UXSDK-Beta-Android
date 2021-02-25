/*
 * Copyright (c) 2018-2021 DJI
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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.dji.ux.beta.sample.R
import dji.common.airlink.PhysicalSource
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.widget.fpv.FPVWidget

/**
 * A view that contains two video feeds and a mechanism to switch between them.
 *
 * Any children of this view will be placed below the secondary feed.
 */
class DualVideoFeedView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    //region Fields
    private val fpvWidget: FPVWidget
    private var secondaryFPVWidget: FPVWidget
    private var compositeDisposable: CompositeDisposable? = null
    //endregion

    //region Lifecycle
    init {
        View.inflate(context, R.layout.view_dual_video_feed, this)
        fpvWidget = findViewById(R.id.widget_fpv)
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv)
        secondaryFPVWidget.setOnClickListener { swapVideoSource() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        compositeDisposable = CompositeDisposable()
        compositeDisposable?.add(secondaryFPVWidget.cameraName
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { cameraName: String -> updateSecondaryVideoVisibility(cameraName) })
    }

    override fun onDetachedFromWindow() {
        compositeDisposable?.dispose()
        compositeDisposable = null
        super.onDetachedFromWindow()
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        if (child.id == R.id.widget_fpv
                || child.id == R.id.fpv_gradient_right
                || child.id == R.id.fpv_gradient_bottom
                || child.id == R.id.fpv_gradient_left
                || child.id == R.id.guideline_secondary_fpv
                || child.id == R.id.widget_secondary_fpv) {
            super.addView(child, index, params)
        } else {
            super.addView(child, indexOfChild(secondaryFPVWidget) - 1, params)
        }
    }
    //endregion

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private fun swapVideoSource() {
        if (secondaryFPVWidget.videoSource == SettingDefinitions.VideoSource.SECONDARY) {
            fpvWidget.videoSource = SettingDefinitions.VideoSource.SECONDARY
            secondaryFPVWidget.videoSource = SettingDefinitions.VideoSource.PRIMARY
        } else {
            fpvWidget.videoSource = SettingDefinitions.VideoSource.PRIMARY
            secondaryFPVWidget.videoSource = SettingDefinitions.VideoSource.SECONDARY
        }
    }

    /**
     * Hide the secondary FPV widget when there is no secondary camera.
     *
     * @param cameraName The name of the secondary camera.
     */
    private fun updateSecondaryVideoVisibility(cameraName: String) {
        if (cameraName == PhysicalSource.UNKNOWN.name) {
            secondaryFPVWidget.visibility = GONE
        } else {
            secondaryFPVWidget.visibility = VISIBLE
        }
    }
}