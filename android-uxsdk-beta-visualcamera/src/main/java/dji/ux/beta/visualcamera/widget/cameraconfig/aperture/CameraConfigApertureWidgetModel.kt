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

package dji.ux.beta.visualcamera.widget.cameraconfig.aperture

import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.Aperture
import dji.common.camera.SettingsDefinitions.LensType
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider.io
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.visualcamera.widget.cameraconfig.aperture.CameraConfigApertureWidgetModel.CameraConfigApertureState.*

private const val TAG = "ConfigApertureWidMod"

/**
 * Widget Model for the [CameraConfigApertureWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigApertureWidgetModel constructor(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val exposureSettingsProcessor: DataProcessor<ExposureSettings> =
            DataProcessor.create(ExposureSettings(Aperture.UNKNOWN,
                    SettingsDefinitions.ShutterSpeed.UNKNOWN,
                    0,
                    SettingsDefinitions.ExposureCompensation.UNKNOWN))
    private val apertureProcessor: DataProcessor<CameraConfigApertureState> =
            DataProcessor.create(ProductDisconnected)
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
     * The camera config aperture state.
     */
    val apertureState: Flowable<CameraConfigApertureState>
        get() = apertureProcessor.toFlowable()

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
                .observeOn(io())
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
                if (exposureSettingsProcessor.value.aperture != null
                        && exposureSettingsProcessor.value.aperture != Aperture.UNKNOWN) {
                    apertureProcessor.onNext(CurrentApertureValue(exposureSettingsProcessor.value.aperture))
                } else {
                    apertureProcessor.onNext(NotSupported)
                }
            } else {
                apertureProcessor.onNext(CameraDisconnected)
            }
        } else {
            apertureProcessor.onNext(ProductDisconnected)
        }

    }
    //endregion

    /**
     * Class to represent states of Aperture
     */
    sealed class CameraConfigApertureState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigApertureState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigApertureState()

        /**
         * Aperture value is not supported
         */
        object NotSupported : CameraConfigApertureState()

        /**
         * Current value of lens aperture
         */
        data class CurrentApertureValue(val aperture: Aperture) : CameraConfigApertureState()
    }
}