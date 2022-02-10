package dji.ux.beta.core.base

import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/9
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
interface IGimbalIndex {

    /**
     * Get the gimbal index for which the model is reacting.
     *
     * @return current gimbal index.
     */
    fun getGimbalIndex(): GimbalIndex

    /**
     * Set gimbal index to which the model should react.
     *
     * @param gimbalIndex index of the gimbal.
     */
    fun updateGimbalIndex(gimbalIndex: GimbalIndex)
}