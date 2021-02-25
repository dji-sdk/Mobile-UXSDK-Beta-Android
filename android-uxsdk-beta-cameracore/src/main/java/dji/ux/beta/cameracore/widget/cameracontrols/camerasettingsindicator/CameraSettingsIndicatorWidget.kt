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

package dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.ExposureMode
import dji.common.camera.SettingsDefinitions.LensType
import dji.log.DJILog
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsIndicatorWidget.ModelState
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsIndicatorWidgetModel.CameraSettingsIndicatorState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.OnStateChangeCallback
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "CamSettingsIndicWidget"

/**
 * Widget indicates the current exposure mode.
 * Tapping on the widget can be linked to open exposure settings
 */
open class CameraSettingsIndicatorWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private var stateChangeResourceId = INVALID_RESOURCE
    private val widgetModel: CameraSettingsIndicatorWidgetModel by lazy {
        CameraSettingsIndicatorWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    private val exposureModeDrawableHashMap: MutableMap<ExposureMode, Drawable?> =
            mutableMapOf(
                    ExposureMode.UNKNOWN to getDrawable(R.drawable.uxsdk_selector_camera_settings_normal),
                    ExposureMode.PROGRAM to getDrawable(R.drawable.uxsdk_selector_camera_settings_program),
                    ExposureMode.MANUAL to getDrawable(R.drawable.uxsdk_selector_camera_settings_manual),
                    ExposureMode.SHUTTER_PRIORITY to getDrawable(R.drawable.uxsdk_selector_camera_settings_shutter),
                    ExposureMode.APERTURE_PRIORITY to getDrawable(R.drawable.uxsdk_selector_camera_settings_aperture)
            )


    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: CameraIndex = widgetModel.cameraIndex
        get() = widgetModel.cameraIndex
        set(value) {
            field = value
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * Type of the lens the widget is reacting to.
     */
    var lensType: LensType = widgetModel.lensType
        get() = widgetModel.lensType
        set(value) {
            field = value
            if (!isInEditMode) {
                widgetModel.lensType = value
            }
        }

    /**
     * Callback for when the widget is tapped.
     * This can be used to link the widget to Camera Settings Panel
     */
    var stateChangeCallback: OnStateChangeCallback<Any>? = null

    /**
     * Color for tint of the drawable when product is connected
     */
    @ColorInt
    var iconTintColor: Int? = INVALID_COLOR

    //endregion
    //region Lifecycle

    init {
        foregroundImageView.imageDrawable = exposureModeDrawableHashMap[ExposureMode.UNKNOWN]
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.cameraSettingsIndicatorState
                .observeOn(SchedulerProvider.ui())
                .subscribe(::updateUI))
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        if (isEnabled) {
            stateChangeCallback?.onStateChange(null)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListener()
    }

    override fun onDetachedFromWindow() {
        destroyListener()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.cameraSettingsIndicatorState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) }, {
                        DJILog.e(TAG, "Update Camera Settings Indicator UI " + it.message)
                    }))
        }
    }

    //endregion
    //region private methods
    private fun initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.rootView != null) {
            val widgetView = this.rootView.findViewById<View>(stateChangeResourceId)
            if (widgetView is OnStateChangeCallback<*>?) {
                stateChangeCallback = widgetView as OnStateChangeCallback<Any>?
            }
        }
    }

    private fun destroyListener() {
        stateChangeCallback = null
    }


    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CameraSettingsIndicatorWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_onStateChange) {
                stateChangeResourceId = it
            }
            typedArray.getDrawableAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_aperturePriorityModeDrawable) {
                setIconByMode(ExposureMode.APERTURE_PRIORITY, it)
            }
            typedArray.getDrawableAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_shutterPriorityModeDrawable) {
                setIconByMode(ExposureMode.SHUTTER_PRIORITY, it)
            }
            typedArray.getDrawableAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_programModeDrawable) {
                setIconByMode(ExposureMode.PROGRAM, it)
            }
            typedArray.getDrawableAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_manualModeDrawable) {
                setIconByMode(ExposureMode.MANUAL, it)
            }
            typedArray.getDrawableAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_unknownModeDrawable) {
                setIconByMode(ExposureMode.UNKNOWN, it)
            }
            typedArray.getColorAndUse(R.styleable.CameraSettingsIndicatorWidget_uxsdk_iconTint) {
                iconTintColor = it
            }
        }

    }

    private fun updateUI(cameraSettingsIndicatorState: CameraSettingsIndicatorState) {
        widgetStateDataProcessor.onNext(ModelState.CameraSettingsIndicatorStateUpdated(cameraSettingsIndicatorState))
        if (cameraSettingsIndicatorState is CameraSettingsIndicatorState.CameraSettingsExposureMode) {
            updateCameraSettingsState(cameraSettingsIndicatorState.exposureMode)
        } else {
            updateDisconnectedState()
        }
    }

    private fun updateDisconnectedState() {
        isEnabled = false
        foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.UNKNOWN])
        foregroundImageView.setColorFilter(getDisconnectedStateIconColor())
    }

    private fun updateCameraSettingsState(exposureMode: ExposureMode) {
        isEnabled = true
        when (exposureMode) {
            ExposureMode.APERTURE_PRIORITY -> foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.APERTURE_PRIORITY])
            ExposureMode.SHUTTER_PRIORITY -> foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.SHUTTER_PRIORITY])
            ExposureMode.MANUAL -> foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.MANUAL])
            ExposureMode.PROGRAM -> foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.PROGRAM])
            else -> foregroundImageView.setImageDrawable(exposureModeDrawableHashMap[ExposureMode.UNKNOWN])
        }
        foregroundImageView.updateColorFilter(iconTintColor)
    }
    //endregion
    //region customizations


    /**
     * Set the icon for exposure mode
     *
     * @param exposureMode instance of [ExposureMode] for which icon should be used
     * @param resourceId   to be used
     */
    fun setIconByMode(exposureMode: ExposureMode, @DrawableRes resourceId: Int) {
        setIconByMode(exposureMode, getDrawable(resourceId))
    }

    /**
     * Set the icon for exposure mode
     *
     * @param exposureMode instance of [ExposureMode] for which icon should be used
     * @param drawable     to be used
     */
    fun setIconByMode(exposureMode: ExposureMode, drawable: Drawable?) {
        exposureModeDrawableHashMap[exposureMode] = drawable
        checkAndUpdateIconColor()
    }


    /**
     * Get the icon used for the exposure mode
     *
     * @param exposureMode instance of [ExposureMode] for which icon is used
     * @return Drawable
     */
    fun getIconByMode(exposureMode: ExposureMode): Drawable? {
        return exposureModeDrawableHashMap[exposureMode]
    }


    //endregion


    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Camera settings state updated
         */
        data class CameraSettingsIndicatorStateUpdated(val cameraSettingsIndicatorState: CameraSettingsIndicatorState) : ModelState()
    }


}