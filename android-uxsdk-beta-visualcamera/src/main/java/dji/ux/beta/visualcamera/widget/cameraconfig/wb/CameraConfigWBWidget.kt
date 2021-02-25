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

package dji.ux.beta.visualcamera.widget.cameraconfig.wb

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.LensType
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.visualcamera.R
import dji.ux.beta.visualcamera.base.widget.BaseCameraConfigWidget
import dji.ux.beta.visualcamera.widget.cameraconfig.wb.CameraConfigWBWidget.ModelState.ProductConnected

private const val EMPTY_STRING = ""
private const val COLOR_TEMP_MULTIPLIER = 100

/**
 * Shows the camera's current white balance.
 */
open class CameraConfigWBWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseCameraConfigWidget<CameraConfigWBWidget.ModelState>(
        context,
        attrs,
        defStyleAttr,
        widgetTheme,
        R.style.UXSDKCameraConfigWBWidget
) {

    //region Fields
    private val widgetModel: CameraConfigWBWidgetModel by lazy {
        CameraConfigWBWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    private var wbNameArray: Array<String> = resources.getStringArray(R.array.uxsdk_camera_white_balance_name_array)

    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * The type of the lens for which the widget should react
     */
    var lensType: LensType
        get() = widgetModel.lensType
        set(lensType) {
            if (!isInEditMode) {
                widgetModel.lensType = lensType
            }
        }

    //endregion

    //region LifeCycle
    init {
        attrs?.let { initAttributes(context, it) }
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

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.whiteBalanceState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(whiteBalanceState: CameraConfigWBWidgetModel.CameraConfigWBState) {
        widgetStateDataProcessor.onNext(ModelState.WhiteBalanceStateUpdated(whiteBalanceState))
        if (whiteBalanceState is CameraConfigWBWidgetModel.CameraConfigWBState.CurrentWBValue
                && whiteBalanceState.whiteBalance.whiteBalancePreset.value() < wbNameArray.size) {
            labelString = resources.getString(R.string.uxsdk_white_balance_title,
                    wbNameArray[whiteBalanceState.whiteBalance.whiteBalancePreset.value()])
            valueString = resources.getString(R.string.uxsdk_white_balance_temp,
                    whiteBalanceState.whiteBalance.colorTemperature * COLOR_TEMP_MULTIPLIER)
            valueTextColor = normalValueColor
        } else {
            labelString = resources.getString(R.string.uxsdk_white_balance_title, EMPTY_STRING)
            valueString = getString(R.string.uxsdk_string_default_value)
            valueTextColor = disconnectedValueColor
        }
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CameraConfigWBWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.CameraConfigWBWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.CameraConfigWBWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
        }
    }
    //endregion

    //region Hooks
    /**
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * White balance state updated
         */
        data class WhiteBalanceStateUpdated(val whiteBalanceState: CameraConfigWBWidgetModel.CameraConfigWBState) : ModelState()
    }

    //endregion
}