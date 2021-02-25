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

package dji.ux.beta.visualcamera.widget.cameraconfig.color

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.extension.show
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.visualcamera.R
import dji.ux.beta.visualcamera.base.widget.BaseCameraConfigWidget
import dji.ux.beta.visualcamera.widget.cameraconfig.color.CameraConfigColorWidgetModel.CameraConfigColorState

/**
 * Shows the current [SettingsDefinitions.CameraColor].
 * Hidden when the camera color is unknown or none.
 */
open class CameraConfigColorWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseCameraConfigWidget<CameraConfigColorWidget.ModelState>(
        context,
        attrs,
        defStyleAttr,
        widgetTheme,
        R.style.UXSDKCameraConfigColorWidget
) {

    //region Fields
    private val widgetModel: CameraConfigColorWidgetModel by lazy {
        CameraConfigColorWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    private val cameraColorNameArray: Array<String> = resources.getStringArray(R.array.uxsdk_camera_color_type)

    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: SettingDefinitions.CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
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
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.cameraColor
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    private fun updateUI(colorState: CameraConfigColorState) {
        widgetStateDataProcessor.onNext(ModelState.ColorStateUpdated(colorState))
        when (colorState) {
            CameraConfigColorState.ProductDisconnected,
            CameraConfigColorState.CameraDisconnected,
            CameraConfigColorState.NotSupported -> updateDisconnectedState()
            is CameraConfigColorState.CameraColor -> updateWidgetUI(colorState)
        }
    }

    private fun updateDisconnectedState() {
        valueString = getString(R.string.uxsdk_string_default_value)
        valueTextColor = disconnectedValueColor
        hide()
    }

    private fun updateWidgetUI(cameraColorState: CameraConfigColorState.CameraColor) {
        valueString = if (cameraColorState.color.value() >= cameraColorNameArray.size) {
            getString(R.string.uxsdk_string_default_value)
        } else {
            cameraColorNameArray[cameraColorState.color.value()]
        }
        valueTextColor = normalValueColor
        show()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CameraConfigColorWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.CameraConfigColorWidget_uxsdk_cameraIndex) {
                cameraIndex = SettingDefinitions.CameraIndex.find(it)
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
         * Color state updated
         */
        data class ColorStateUpdated(val colorState: CameraConfigColorState) : ModelState()
    }
    //endregion
}