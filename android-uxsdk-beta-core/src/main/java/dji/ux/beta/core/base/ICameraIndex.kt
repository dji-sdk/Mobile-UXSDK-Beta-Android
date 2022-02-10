package dji.ux.beta.core.base

import dji.common.camera.SettingsDefinitions.LensType
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/20
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
interface ICameraIndex {

    /**
     * Get the camera index for which the model is reacting.
     *
     * @return int representing [CameraIndex].
     */
    fun getCameraIndex(): CameraIndex

    /**
     * Get the current type of the lens the widget model is reacting to
     *
     * @return current lens type.
     */
    fun getLensType(): LensType

    /**
     * Update camera/lens index to which the model should react.
     *
     * @param cameraIndex index of the camera.
     * @param lensType index of the lens.
     */
    fun updateCameraSource(cameraIndex: CameraIndex, lensType: LensType)
}