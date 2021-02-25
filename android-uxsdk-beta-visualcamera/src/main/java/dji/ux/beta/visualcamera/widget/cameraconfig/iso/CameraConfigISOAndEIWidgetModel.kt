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
package dji.ux.beta.visualcamera.widget.cameraconfig.iso

import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.ExposureSensitivityMode
import dji.common.camera.SettingsDefinitions.LensType
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

/**
 * The value to display when the ISO is locked.
 */
private const val LOCKED_ISO_VALUE = "500"
private const val TAG = "ConfigISOEIWidMod"

/**
 * Widget Model for the [CameraConfigISOAndEIWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigISOAndEIWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isoEIStateProcessor: DataProcessor<CameraConfigISOEIState> = DataProcessor.create(CameraConfigISOEIState.ProductDisconnected)
    private val exposureSettingsProcessor: DataProcessor<ExposureSettings> = DataProcessor.create(ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
            SettingsDefinitions.ShutterSpeed.UNKNOWN,
            0,
            SettingsDefinitions.ExposureCompensation.UNKNOWN))
    private val isoProcessor: DataProcessor<SettingsDefinitions.ISO> = DataProcessor.create(SettingsDefinitions.ISO.UNKNOWN)
    private val exposureSensitivityModeProcessor: DataProcessor<ExposureSensitivityMode> = DataProcessor.create(ExposureSensitivityMode.UNKNOWN)
    private val eiValueProcessor: DataProcessor<Int> = DataProcessor.create(0)
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
     * ISO/EI state
     */
    val isoEIState: Flowable<CameraConfigISOEIState>
        get() = isoEIStateProcessor.toFlowable()


    //endregion
    //region LifeCycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        val exposureSettingsKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex.index, lensType.value())
        val isoKey: DJIKey = lensModule.createLensKey(CameraKey.ISO, cameraIndex.index, lensType.value())
        val exposureSensitivityModeKey: DJIKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex.index)
        val eiValueKey: DJIKey = CameraKey.create(CameraKey.EI_VALUE, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        bindDataProcessor(exposureSettingsKey, exposureSettingsProcessor)
        bindDataProcessor(isoKey, isoProcessor)
        bindDataProcessor(exposureSensitivityModeKey, exposureSensitivityModeProcessor)
        bindDataProcessor(eiValueKey, eiValueProcessor)
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))
    }

    override fun inCleanup() {
        //Nothing to cleanup
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if(cameraConnectionDataProcessor.value) {
                if (exposureSensitivityModeProcessor.value == ExposureSensitivityMode.EI) {
                    isoEIStateProcessor.onNext(CameraConfigISOEIState.EI(eiValueProcessor.value.toString()))
                } else {
                    when {
                        isoProcessor.value == SettingsDefinitions.ISO.FIXED && exposureSettingsProcessor.value.iso == 0 -> {
                            isoEIStateProcessor.onNext(CameraConfigISOEIState.FIXED(LOCKED_ISO_VALUE))
                        }
                        isoProcessor.value == SettingsDefinitions.ISO.AUTO -> {
                            isoEIStateProcessor.onNext(CameraConfigISOEIState.AUTO(exposureSettingsProcessor.value.iso.toString()))
                        }
                        else -> {
                            isoEIStateProcessor.onNext(CameraConfigISOEIState.ISO(exposureSettingsProcessor.value.iso.toString()))
                        }
                    }
                }
            } else {
                isoEIStateProcessor.onNext(CameraConfigISOEIState.CameraDisconnected)
            }
        } else {
            isoEIStateProcessor.onNext(CameraConfigISOEIState.ProductDisconnected)
        }
    }

    //endregion

    /**
     * Class to represent states of ISO/EI
     */
    sealed class CameraConfigISOEIState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigISOEIState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigISOEIState()

        /**
         * Exposure sensitivity mode is EI
         */
        data class EI(val eiValue: String) : CameraConfigISOEIState()

        /**
         * Exposure sensitivity mode is AUTO ISO
         */
        data class AUTO(val isoValue: String) : CameraConfigISOEIState()

        /**
         * Exposure sensitivity mode is FIXED ISO
         */
        data class FIXED(val isoValue: String) : CameraConfigISOEIState()

        /**
         * Exposure sensitivity mode is ISO
         */
        data class ISO(val isoValue: String) : CameraConfigISOEIState()
    }
}