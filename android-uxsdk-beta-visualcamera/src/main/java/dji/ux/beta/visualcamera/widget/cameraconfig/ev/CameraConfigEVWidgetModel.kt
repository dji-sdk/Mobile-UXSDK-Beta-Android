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

package dji.ux.beta.visualcamera.widget.cameraconfig.ev

import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions.*
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "ConfigEVWidMod"

/**
 * Widget Model for the [CameraConfigEVWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigEVWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val exposureSettingsProcessor: DataProcessor<ExposureSettings> = DataProcessor.create(ExposureSettings(Aperture.UNKNOWN,
            ShutterSpeed.UNKNOWN,
            0,
            ExposureCompensation.UNKNOWN))
    private val cameraConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val exposureModeProcessor: DataProcessor<ExposureMode> = DataProcessor.create(ExposureMode.UNKNOWN)
    private val exposureCompensationProcessor: DataProcessor<ExposureCompensation> = DataProcessor.create(ExposureCompensation.UNKNOWN)
    private val exposureSensitivityModeProcessor: DataProcessor<ExposureSensitivityMode> = DataProcessor.create(ExposureSensitivityMode.UNKNOWN)
    private val evStateProcessor: DataProcessor<CameraConfigEVState> = DataProcessor.create(CameraConfigEVState.ProductDisconnected)
    private val lensModule = LensModule()

    /**
     * Index of the camera the widget model is reacting to.
     */
    var cameraIndex: CameraIndex = CameraIndex.find(CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            lensModule.setCameraIndex(this, value)
            restart()
        }

    /**
     * Type of the lens the widget model is reacting to.
     */
    var lensType: LensType = LensType.ZOOM
        set(value) {
            field = value
            restart()
        }
    //endregion
    //region Data

    /**
     * Get the exposure compensation.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    val exposureCompensationState: Flowable<CameraConfigEVState>
        get() = evStateProcessor.toFlowable()

    //endregion
    //region Lifecycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val exposureSettingsKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex.index, lensType.value())
        val exposureModeKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex.index, lensType.value())
        val exposureCompensationKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_COMPENSATION, cameraIndex.index, lensType.value())
        val exposureSensitivityModeKey: DJIKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex.index)
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectedProcessor)
        bindDataProcessor(exposureSettingsKey, exposureSettingsProcessor)
        bindDataProcessor(exposureModeKey, exposureModeProcessor)
        bindDataProcessor(exposureCompensationKey, exposureCompensationProcessor)
        bindDataProcessor(exposureSensitivityModeKey, exposureSensitivityModeProcessor)
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))
    }

    override fun inCleanup() {
        //Nothing to clean
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (cameraConnectedProcessor.value) {
                if (exposureSensitivityModeProcessor.value == ExposureSensitivityMode.EI) {
                    evStateProcessor.onNext(CameraConfigEVState.NotSupported)
                } else {
                    if (exposureModeProcessor.value != ExposureMode.MANUAL
                            && exposureCompensationProcessor.value != ExposureCompensation.FIXED) {
                        evStateProcessor.onNext(CameraConfigEVState.CurrentExposureValue(exposureCompensationProcessor.value))
                    } else {
                        if (exposureSettingsProcessor.value.exposureCompensation != null
                                && exposureSettingsProcessor.value.exposureCompensation == ExposureCompensation.FIXED) {

                            evStateProcessor.onNext(CameraConfigEVState.CurrentExposureValue(ExposureCompensation.N_0_0))
                        } else {
                            evStateProcessor.onNext(CameraConfigEVState.CurrentExposureValue(exposureCompensationProcessor.value))
                        }
                    }
                }
            } else {
                evStateProcessor.onNext(CameraConfigEVState.CameraDisconnected)
            }
        } else {
            evStateProcessor.onNext(CameraConfigEVState.ProductDisconnected)
        }
    }

    //endregion

    /**
     * Class to represent states of Exposure Value
     */
    sealed class CameraConfigEVState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigEVState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigEVState()

        /**
         * Exposure compensation not supported
         */
        object NotSupported : CameraConfigEVState()

        /**
         * Current value of exposure
         */
        data class CurrentExposureValue(val exposureCompensation: ExposureCompensation) : CameraConfigEVState()
    }
}