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

package dji.ux.beta.visualcamera.widget.cameraconfig.shutter

import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
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


private const val TAG = "ConfigShutterWidMod"
/**
 * Widget Model for the [CameraConfigShutterWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigShutterWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val exposureSettingsProcessor: DataProcessor<ExposureSettings> =
            DataProcessor.create(ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                    SettingsDefinitions.ShutterSpeed.UNKNOWN,
                    0,
                    SettingsDefinitions.ExposureCompensation.UNKNOWN))
    private val shutterProcessor: DataProcessor<CameraConfigShutterState> =
            DataProcessor.create(CameraConfigShutterState.ProductDisconnected)
    private val cameraConnectedProcessor: DataProcessor<Boolean> =
            DataProcessor.create(false)
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
     * The camera config shutter state.
     */
    val shutterSpeedState: Flowable<CameraConfigShutterState>
        get() = shutterProcessor.toFlowable()

    //endregion
    //region LifeCycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectedProcessor)
        val exposureSettingsKey: DJIKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex.index, lensType.value())
        bindDataProcessor(exposureSettingsKey, exposureSettingsProcessor)
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
            if (cameraConnectedProcessor.value) {
                if (exposureSettingsProcessor.value.shutterSpeed != null
                        && exposureSettingsProcessor.value.shutterSpeed != SettingsDefinitions.ShutterSpeed.UNKNOWN) {
                    shutterProcessor.onNext(CameraConfigShutterState.CurrentShutterValue(exposureSettingsProcessor.value.shutterSpeed))
                } else {
                    shutterProcessor.onNext(CameraConfigShutterState.NotSupported)
                }
            } else {
                shutterProcessor.onNext(CameraConfigShutterState.CameraDisconnected)
            }
        } else {
            shutterProcessor.onNext(CameraConfigShutterState.ProductDisconnected)
        }

    }
    //endregion

    /**
     * Class to represent states of Shutter Speed
     */
    sealed class CameraConfigShutterState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigShutterState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigShutterState()

        /**
         * Shutter value is not supported
         */
        object NotSupported : CameraConfigShutterState()

        /**
         * Current value of camera shutter speed
         */
        data class CurrentShutterValue(val shutterSpeed: SettingsDefinitions.ShutterSpeed) : CameraConfigShutterState()
    }
}