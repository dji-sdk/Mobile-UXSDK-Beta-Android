package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.util.SettingDefinitions
import kotlinx.android.synthetic.main.uxsdk_panel_exposure_setting.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureSettingsPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ExposureSettingsPanel.ModelState>(context, attrs, defStyleAttr),
    ICameraIndex {

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_panel_exposure_setting, this)
    }

    override fun reactToModelChanges() {

    }

    override fun getCameraIndex() = exposure_setting_widget.getCameraIndex()

    override fun getLensType() = exposure_setting_widget.getLensType()

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) {
        exposure_setting_widget.updateCameraSource(cameraIndex, lensType)
        iso_and_ei_setting_widget.updateCameraSource(cameraIndex, lensType)
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    sealed class ModelState
}