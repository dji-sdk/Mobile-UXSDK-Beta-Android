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

package dji.ux.beta.visualcamera.widget.cameraconfig.shutter

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.LensType
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.visualcamera.R
import dji.ux.beta.visualcamera.base.widget.BaseCameraConfigWidget
import dji.ux.beta.visualcamera.widget.cameraconfig.shutter.CameraConfigShutterWidgetModel.CameraConfigShutterState

/**
 * Shows the camera's current shutter speed.
 */
open class CameraConfigShutterWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseCameraConfigWidget<CameraConfigShutterWidget.ModelState>(
        context,
        attrs,
        defStyleAttr,
        widgetTheme,
        R.style.UXSDKCameraConfigShutterWidget
) {
    //region Fields
    private val widgetModel: CameraConfigShutterWidgetModel by lazy {
        CameraConfigShutterWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

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
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.shutterSpeedState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }
    //endregion

    //region Reactions to model
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CameraConfigShutterWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.CameraConfigShutterWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.CameraConfigShutterWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
        }
    }
    
    private fun updateUI(cameraConfigShutterSpeedState: CameraConfigShutterState) {
        widgetStateDataProcessor.onNext(ModelState.ShutterStateUpdated(cameraConfigShutterSpeedState))
        when (cameraConfigShutterSpeedState) {
            CameraConfigShutterState.ProductDisconnected,
            CameraConfigShutterState.CameraDisconnected,
            CameraConfigShutterState.NotSupported -> updateDisconnectedState()
            is CameraConfigShutterState.CurrentShutterValue -> updateShutterValue(cameraConfigShutterSpeedState)
        }
    }

    private fun updateDisconnectedState() {
        valueString = getString(R.string.uxsdk_string_default_value)
        valueTextColor = disconnectedValueColor
    }

    private fun updateShutterValue(shutterSpeedState: CameraConfigShutterState.CurrentShutterValue) {
        valueString = CameraUtil.shutterSpeedDisplayName(shutterSpeedState.shutterSpeed)
        valueTextColor = normalValueColor
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
         * Shutter state updated
         */
        data class ShutterStateUpdated(val shutterState: CameraConfigShutterState) : ModelState()
    }

    //endregion

}