package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import dji.common.camera.SettingsDefinitions
import dji.keysdk.DJIKey
import dji.keysdk.KeyManager
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidgetModel
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.RxUtil
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureModeSettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ExposureModeSettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener {

    private var modePLayout: FrameLayout? = null
    private var modeSLayout: FrameLayout? = null
    private var modeALayout: FrameLayout? = null
    private var modeMLayout: FrameLayout? = null

    private val widgetModel by lazy {
        ExposureModeSettingModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {

    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.currentExposureModeRange.observeOn(SchedulerProvider.ui()).subscribe {
            updateExposureModeRange(it)
        })
        addReaction(widgetModel.currentExposureMode.observeOn(SchedulerProvider.ui()).subscribe {
            updateExposureMode(it)
        })
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {

        val previousExposureMode: SettingsDefinitions.ExposureMode? = widgetModel.exposureModeProcessor.value
        var exposureMode: SettingsDefinitions.ExposureMode = SettingsDefinitions.ExposureMode.UNKNOWN

        when (v?.id) {
//            R.id.layout_camera_mode_p -> exposureMode = SettingsDefinitions.ExposureMode.PROGRAM
//            R.id.layout_camera_mode_a -> exposureMode = SettingsDefinitions.ExposureMode.APERTURE_PRIORITY
//            R.id.layout_camera_mode_s -> exposureMode = SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY
//            R.id.layout_camera_mode_m -> exposureMode = SettingsDefinitions.ExposureMode.MANUAL
        }

        if (exposureMode == previousExposureMode) {
            return
        }

        updateExposureMode(exposureMode)

        addDisposable(
            widgetModel.setExposureMode(exposureMode)
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action { }, RxUtil.errorConsumer({
                    restoreToCurrentExposureMode()
                }, this.toString(), "setExposureMode: "))
        )
    }

    private fun updateExposureModeRange(range: Array<SettingsDefinitions.ExposureMode>) {
        modeALayout?.isEnabled = rangeContains(range, SettingsDefinitions.ExposureMode.APERTURE_PRIORITY)
        modeSLayout?.isEnabled = rangeContains(range, SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY)
        modeMLayout?.isEnabled = rangeContains(range, SettingsDefinitions.ExposureMode.MANUAL)
        modePLayout?.isEnabled = rangeContains(range, SettingsDefinitions.ExposureMode.PROGRAM)
    }

    private fun updateExposureMode(mode: SettingsDefinitions.ExposureMode) {
        modePLayout?.isSelected = false
        modeMLayout?.isSelected = false
        modeSLayout?.isSelected = false
        modeALayout?.isSelected = false

        when (mode) {
            SettingsDefinitions.ExposureMode.PROGRAM -> modePLayout?.isSelected = true
            SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY -> modeSLayout?.isSelected = true
            SettingsDefinitions.ExposureMode.APERTURE_PRIORITY -> modeALayout?.isSelected = true
            SettingsDefinitions.ExposureMode.MANUAL -> modeMLayout?.isSelected = true
            else -> {
            }
        }
    }

    private fun restoreToCurrentExposureMode() {
        val evValue = widgetModel.exposureModeProcessor.value
        if (evValue != null) {
            updateExposureMode(evValue)
        }
    }

    private fun rangeContains(range: Array<SettingsDefinitions.ExposureMode>?, value: SettingsDefinitions.ExposureMode): Boolean {
        if (range == null) {
            return false
        }
        for (item in range) {
            if (item == value) {
                return true
            }
        }
        return false
    }

    sealed class ModelState {

    }
}