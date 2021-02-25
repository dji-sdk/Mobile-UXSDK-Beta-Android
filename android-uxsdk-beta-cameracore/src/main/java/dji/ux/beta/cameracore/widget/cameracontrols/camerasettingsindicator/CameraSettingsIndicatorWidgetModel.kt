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

package dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator

import dji.common.camera.SettingsDefinitions.ExposureMode
import dji.common.camera.SettingsDefinitions.LensType
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsIndicatorWidgetModel.CameraSettingsIndicatorState.CameraSettingsExposureMode
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsIndicatorWidgetModel.CameraSettingsIndicatorState.ProductDisconnected
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "SettingsIndicatorWidMod"

/**
 * Exposure Settings Indicator Widget Model
 *
 *
 * Widget Model for the [CameraSettingsIndicatorWidget] used to define the
 * underlying logic and communication
 */
class CameraSettingsIndicatorWidgetModel(
        djiSdkModel: DJISDKModel,
        uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager) {

    //region Fields
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val exposureModeDataProcessor: DataProcessor<ExposureMode> = DataProcessor.create(ExposureMode.UNKNOWN)
    private val cameraSettingsStateDataProcessor: DataProcessor<CameraSettingsIndicatorState> = DataProcessor.create(ProductDisconnected)
    private val lensModule = LensModule()

    /**
     * Lens for which the widget model should react
     */
    var lensType: LensType = LensType.ZOOM
        set(value) {
            field = value
            restart()
        }

    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: CameraIndex = CameraIndex.CAMERA_INDEX_0
        set(value) {
            field = value
            restart()
        }

    /**
     * Camera settings indicator state
     */
    val cameraSettingsIndicatorState: Flowable<CameraSettingsIndicatorState>
        get() = cameraSettingsStateDataProcessor.toFlowable()
    //endregion

    //region Lifecycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        val exposureModeKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex.index, lensType.value())
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor)
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))
    }

    override fun inCleanup() {
        // No Clean up needed
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (cameraConnectionDataProcessor.value) {
                cameraSettingsStateDataProcessor.onNext(CameraSettingsExposureMode(exposureModeDataProcessor.value))
            } else {
                cameraSettingsStateDataProcessor.onNext(CameraSettingsIndicatorState.CameraDisconnected)
            }
        } else {
            cameraSettingsStateDataProcessor.onNext(ProductDisconnected)
        }
    }
    //endregion

    /**
     * State for the camera settings indicator
     */
    sealed class CameraSettingsIndicatorState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : CameraSettingsIndicatorState()

        /**
         * When camera is disconnected
         */
        object CameraDisconnected : CameraSettingsIndicatorState()

        /**
         * When product is connected and exposure mode is updated
         */
        data class CameraSettingsExposureMode(val exposureMode: ExposureMode) : CameraSettingsIndicatorState()
    }

}