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

package dji.ux.beta.visualcamera.widget.cameraconfig.ev

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.extension.show
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.visualcamera.R
import dji.ux.beta.visualcamera.base.widget.BaseCameraConfigWidget
import dji.ux.beta.visualcamera.widget.cameraconfig.ev.CameraConfigEVWidgetModel.CameraConfigEVState

/**
 * Shows the camera's current exposure compensation.
 */
open class CameraConfigEVWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseCameraConfigWidget<CameraConfigEVWidget.ModelState>(
        context,
        attrs,
        defStyleAttr,
        widgetTheme,
        R.style.UXSDKCameraConfigEVWidget
) {
    //region Fields
    private val widgetModel: CameraConfigEVWidgetModel by lazy {
        CameraConfigEVWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

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

    /**
     * The type of the lens for which the widget should react
     */
    var lensType: SettingsDefinitions.LensType
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
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.exposureCompensationState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(cameraConfigEVState: CameraConfigEVState) {
        widgetStateDataProcessor.onNext(ModelState.EVStateUpdated(cameraConfigEVState))
        when (cameraConfigEVState) {
            CameraConfigEVState.ProductDisconnected,
            CameraConfigEVState.CameraDisconnected,
            CameraConfigEVState.NotSupported -> updateDisconnectedState()
            is CameraConfigEVState.CurrentExposureValue -> updateExposureCompensation(cameraConfigEVState)
        }
    }

    private fun updateDisconnectedState() {
        valueString = getString(R.string.uxsdk_string_default_value)
        valueTextColor = disconnectedValueColor
        hide()
    }

    private fun updateExposureCompensation(cameraConfigEVState: CameraConfigEVState.CurrentExposureValue) {
        valueString = CameraUtil.exposureValueDisplayName(cameraConfigEVState.exposureCompensation)
        valueTextColor = normalValueColor
        show()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CameraConfigEVWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.CameraConfigEVWidget_uxsdk_cameraIndex) {
                cameraIndex = SettingDefinitions.CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.CameraConfigEVWidget_uxsdk_lensType) {
                lensType = SettingsDefinitions.LensType.find(it)
            }
        }
    }
    //endregion

    //region Hooks

    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * Camera config EV state updated
         */
        data class EVStateUpdated(val cameraConfigEVState: CameraConfigEVState) : ModelState()
    }

    //endregion

}