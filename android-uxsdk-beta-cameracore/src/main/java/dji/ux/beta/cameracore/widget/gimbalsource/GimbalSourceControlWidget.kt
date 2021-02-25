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

package dji.ux.beta.cameracore.widget.gimbalsource

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.annotation.ColorInt
import dji.common.airlink.PhysicalSource
import dji.log.DJILog
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.OnStateChangeCallback
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.extension.showShortToast

private const val TAG = "GimbalSrcControlWidget"

open class GimbalSourceControlWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any?>(
        context,
        attrs,
        defStyleAttr),
        OnStateChangeCallback<Any?> {

    //region Fields
    private val widgetModel by lazy {
        GimbalSourceControlWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    private val gimbalRowFPV: GimbalSourceControlRowView = findViewById(R.id.gimbal_row_fpv)
    private val gimbalRow1: GimbalSourceControlRowView = findViewById(R.id.gimbal_row_1)
    private val gimbalRow2: GimbalSourceControlRowView = findViewById(R.id.gimbal_row_2)
    private val gimbalRow3: GimbalSourceControlRowView = findViewById(R.id.gimbal_row_3)

    private val gimbalSourceCheckedMap = mutableMapOf(gimbalRowFPV to false,
            gimbalRow1 to false,
            gimbalRow2 to false,
            gimbalRow3 to false)
    private val gimbalSourceMap = linkedMapOf(PhysicalSource.LEFT_CAM to gimbalRow1,
            PhysicalSource.RIGHT_CAM to gimbalRow2,
            PhysicalSource.TOP_CAM to gimbalRow3,
            PhysicalSource.FPV_CAM to gimbalRowFPV)

    private val onCheckedChangeListener = { rowView: GimbalSourceControlRowView, isChecked: Boolean ->
        gimbalSourceCheckedMap[rowView] = isChecked
        if (gimbalSourceCheckedMap.filterValues { value -> value }.count() >= 2) {
            gimbalSourceCheckedMap.forEach { (key, value) ->
                if (!value) key.isCheckBoxEnabled = false
            }
        } else {
            gimbalSourceCheckedMap.forEach { (key, _) ->
                key.isCheckBoxEnabled = true
            }
        }
    }

    /**
     * The tint color of the checked checkbox
     */
    @get:ColorInt
    @setparam:ColorInt
    var checkBoxCheckedColor = getColor(dji.ux.beta.core.R.color.uxsdk_blue_highlight)

    /**
     * The tint color of the unchecked checkbox
     */
    @get:ColorInt
    @setparam:ColorInt
    var checkBoxUncheckedColor = getColor(dji.ux.beta.core.R.color.uxsdk_white)
    //endregion

    //region Lifecycle
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_gimbal_source_control, this)
    }

    init {
        setBackgroundResource(R.drawable.uxsdk_background_dialog_rounded_corners)
        initRows()
        findViewById<Button>(R.id.button_negative).setOnClickListener { onNegativeButtonClick() }
        findViewById<Button>(R.id.button_positive).setOnClickListener { onPositiveButtonClick() }
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.portCameraConnectionState
                .observeOn(SchedulerProvider.ui())
                .subscribe { portCameraConnectionState: GimbalSourceControlWidgetModel.CameraConnectionState ->
                    updateRow(gimbalRow1, portCameraConnectionState)
                })
        addReaction(widgetModel.starboardCameraConnectionState
                .observeOn(SchedulerProvider.ui())
                .subscribe { starboardCameraConnectionState: GimbalSourceControlWidgetModel.CameraConnectionState ->
                    updateRow(gimbalRow2, starboardCameraConnectionState)
                })
        addReaction(widgetModel.topCameraConnectionState
                .observeOn(SchedulerProvider.ui())
                .subscribe { topCameraConnectionState: GimbalSourceControlWidgetModel.CameraConnectionState ->
                    updateRow(gimbalRow3, topCameraConnectionState)
                })
        addReaction(widgetModel.gimbalSourceState
                .observeOn(SchedulerProvider.ui())
                .subscribe { gimbalSourceState: GimbalSourceControlWidgetModel.GimbalSourceState ->
                    updateRows(gimbalSourceState)
                })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun onStateChange(state: Any?) {
        toggleVisibility()
    }
    //endregion

    //region private methods
    private fun onNegativeButtonClick() {
        toggleVisibility()
    }

    private fun onPositiveButtonClick() {
        val checkedRows: Set<GimbalSourceControlRowView> = gimbalSourceCheckedMap
                .filterValues { value -> value }
                .keys
        val sources: List<PhysicalSource> = gimbalSourceMap
                .filterValues { value -> checkedRows.contains(value) }
                .map { (key, _) -> key }

        addDisposable(widgetModel.setGimbalSources(sources)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    toggleVisibility()
                }, {
                    DJILog.e(TAG, "Error setting gimbal source: ${it.message}")

                    //TODO show error message in widget
                    showShortToast("Error setting gimbal source")
                }))
    }

    private fun initRows() {
        gimbalSourceCheckedMap.forEach { (key, _) ->
            key.setOnCheckedChangeListener(onCheckedChangeListener)
            key.setCheckBoxColor(checkBoxUncheckedColor, checkBoxCheckedColor)
        }

        gimbalRowFPV.updateUI(PhysicalSource.FPV_CAM, "")
    }

    private fun updateRow(rowView: GimbalSourceControlRowView, cameraConnectionState: GimbalSourceControlWidgetModel.CameraConnectionState) {
        if (cameraConnectionState.connected) {
            rowView.updateUI(cameraConnectionState.physicalSource, cameraConnectionState.displayName)
            rowView.visibility = VISIBLE
        } else {
            rowView.visibility = GONE
        }
    }

    private fun updateRows(gimbalSourceState: GimbalSourceControlWidgetModel.GimbalSourceState) {
        gimbalSourceCheckedMap.forEach { (key, _) ->
            key.isChecked = false
        }

        gimbalSourceMap[gimbalSourceState.primarySource]?.isChecked = true
        gimbalSourceMap[gimbalSourceState.secondarySource]?.isChecked = true
    }

    private fun toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
//        context.obtainStyledAttributes(attrs, R.styleable.GimbalSourceControlWidget).use { typedArray ->
//        }
    }
    //endregion

}