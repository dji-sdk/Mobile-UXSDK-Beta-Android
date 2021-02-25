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

import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions

/**
 * Widget Model for the [CameraConfigColorWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigColorWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val cameraConnectionProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val colorDataProcessor: DataProcessor<SettingsDefinitions.CameraColor> =
            DataProcessor.create(SettingsDefinitions.CameraColor.UNKNOWN)
    private val cameraConfigColorStateProcessor: DataProcessor<CameraConfigColorState> =
            DataProcessor.create(CameraConfigColorState.ProductDisconnected)

    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: SettingDefinitions.CameraIndex = SettingDefinitions.CameraIndex.find(SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            restart()
        }

    val cameraColor: Flowable<CameraConfigColorState>
        get() = cameraConfigColorStateProcessor.toFlowable()

    /**
     * Setup method for initialization that must be implemented
     */
    override fun inSetup() {
        val cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionProcessor)
        val cameraColorKey: DJIKey = CameraKey.create(CameraKey.CAMERA_COLOR, cameraIndex.index)
        bindDataProcessor(cameraColorKey, colorDataProcessor)
    }


    /**
     * Method to update states for the required processors in the child classes as required
     */
    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (cameraConnectionProcessor.value) {
                if (colorDataProcessor.value == SettingsDefinitions.CameraColor.NONE
                        || colorDataProcessor.value == SettingsDefinitions.CameraColor.UNKNOWN) {
                    cameraConfigColorStateProcessor.onNext(CameraConfigColorState.NotSupported)
                } else {
                    cameraConfigColorStateProcessor.onNext(CameraConfigColorState.CameraColor(colorDataProcessor.value))
                }
            } else {
                cameraConfigColorStateProcessor.onNext(CameraConfigColorState.CameraDisconnected)
            }
        } else {
            cameraConfigColorStateProcessor.onNext(CameraConfigColorState.ProductDisconnected)
        }
    }

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    override fun inCleanup() {
        // Empty method
    }

    /**
     * Class to represent states of Color
     */
    sealed class CameraConfigColorState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigColorState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigColorState()

        /**
         *  When color is none or unknown
         */
        object NotSupported : CameraConfigColorState()

        /**
         * When camera color value is available
         */
        data class CameraColor(val color: SettingsDefinitions.CameraColor) : CameraConfigColorState()

    }
}