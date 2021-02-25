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
package dji.ux.beta.cameracore.widget.focusexposureswitch

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.LensType
import dji.thirdparty.io.reactivex.functions.Action
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget.ModelState
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidgetModel.FocusExposureSwitchState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.communication.GlobalPreferencesManager
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.core.util.SettingDefinitions.ControlMode

private const val TAG = "FocusExpoSwitchWidget"

/**
 * Focus Exposure Switch Widget
 *
 *
 * This widget can be used to switch the [ControlMode] between focus and exposure
 * When in focus mode the [FPVInteractionWidget] will help change the focus point
 * When in exposure mode the [FPVInteractionWidget] will help change exposure/metering
 */
open class FocusExposureSwitchWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {

    private val widgetModel: FocusExposureSwitchWidgetModel by lazy {
        FocusExposureSwitchWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    /**
     * Lens type for the widget
     */
    var lensType: LensType
        get() = widgetModel.lensType
        set(lensType) {
            if (!isInEditMode) {
                widgetModel.lensType = lensType
            }
        }

    /**
     * Camera key index for which this model should subscribe to.
     */
    var cameraIndex: CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * Manual focus icon
     */
    var manualFocusIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_focus_switch_manual)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Auto focus icon
     */
    var autoFocusIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_focus_switch_auto)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Exposure icon
     */
    var meteringIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_metering_switch)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Manual focus icon tint color
     */
    @ColorInt
    var manualFocusIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Auto focus icon tint color
     */
    @ColorInt
    var autoFocusIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Exposure/ Metering icon tint color
     */
    @ColorInt
    var meteringIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    //endregion
    //region Lifecycle
    init {
        background = background ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        attrs?.let { initAttributes(context, it) }
    }


    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.focusExposureSwitchState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        if (isEnabled) {
            addDisposable(widgetModel.switchControlMode()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Action {}, logErrorConsumer(TAG, "switchControlMode: ")))

        }
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


    override fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.focusExposureSwitchState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) }, { logErrorConsumer(TAG, "Update UI ") }))
        }
    }

    //endregion

    //region private methods
    private fun updateUI(focusExposureSwitchState: FocusExposureSwitchState) {
        widgetStateDataProcessor.onNext(ModelState.FocusExposureSwitchUpdated(focusExposureSwitchState))
        when (focusExposureSwitchState) {
            FocusExposureSwitchState.ProductDisconnected,
            FocusExposureSwitchState.CameraDisconnected,
            FocusExposureSwitchState.NotSupported -> updateDisconnectedState()
            is FocusExposureSwitchState.ControlModeState -> updateControlModeUI(focusExposureSwitchState.controlMode)
        }
    }

    private fun updateControlModeUI(controlMode: ControlMode) {
        when (controlMode) {
            ControlMode.AUTO_FOCUS,
            ControlMode.AUTO_FOCUS_CONTINUE -> {
                foregroundImageView.setImageDrawable(autoFocusIcon)
                foregroundImageView.updateColorFilter(autoFocusIconTintColor)
            }
            ControlMode.MANUAL_FOCUS -> {
                foregroundImageView.setImageDrawable(manualFocusIcon)
                foregroundImageView.updateColorFilter(manualFocusIconTintColor)
            }
            else -> {
                foregroundImageView.setImageDrawable(meteringIcon)
                foregroundImageView.updateColorFilter(meteringIconTintColor)
            }
        }
        show()
        isEnabled = true
    }

    private fun updateDisconnectedState() {
        isEnabled = false
        foregroundImageView.setImageDrawable(meteringIcon)
        foregroundImageView.updateColorFilter(getDisconnectedStateIconColor())
        hide()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.FocusExposureSwitchWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
            typedArray.getDrawableAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_meteringDrawable) {
                meteringIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_manualFocusDrawable) {
                manualFocusIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_autoFocusDrawable) {
                autoFocusIcon = it
            }
            typedArray.getColorAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_meteringIconColor) {
                meteringIconTintColor = it
            }
            typedArray.getColorAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_manualFocusIconColor) {
                manualFocusIconTintColor = it
            }
            typedArray.getColorAndUse(R.styleable.FocusExposureSwitchWidget_uxsdk_autoFocusIconColor) {
                autoFocusIconTintColor = it
            }
        }

    }
    //endregion

    //region customization methods
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_default_ratio)
    }


    /**
     * Set manual focus icon
     *
     * @param resourceId to be used
     */
    fun setManualFocusIcon(@DrawableRes resourceId: Int) {
        manualFocusIcon = getDrawable(resourceId)
    }

    /**
     * Set auto focus icon
     *
     * @param resourceId to be used
     */
    fun setAutoFocusIcon(@DrawableRes resourceId: Int) {
        autoFocusIcon = getDrawable(resourceId)
    }


    /**
     * Set metering/exposure mode icon
     *
     * @param resourceId to be used
     */
    fun setMeteringIcon(@DrawableRes resourceId: Int) {
        meteringIcon = getDrawable(resourceId)
    }

    //endregion

    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Focus/Exposure switch updated
         */
        data class FocusExposureSwitchUpdated(val focusExposureSwitchState: FocusExposureSwitchState) : ModelState()
    }

}