package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.SettingDefinitions

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index
    private var lensType = SettingsDefinitions.LensType.ZOOM

    override fun getCameraIndex(): SettingDefinitions.CameraIndex {
        return SettingDefinitions.CameraIndex.find(cameraIndex)
    }

    override fun setCameraIndex(cameraIndex: SettingDefinitions.CameraIndex) {
        this.cameraIndex = cameraIndex.index
        restart()
    }

    override fun getLensType(): SettingsDefinitions.LensType {
        return lensType
    }

    override fun setLensType(lensType: SettingsDefinitions.LensType) {
        this.lensType = lensType
        restart()
    }

    override fun inSetup() {
    }

    override fun inCleanup() {
    }

    override fun updateStates() {
    }
}