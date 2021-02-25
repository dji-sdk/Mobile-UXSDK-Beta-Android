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

package dji.ux.beta.visualcamera.widget.cameraconfig.wb

import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.LensType
import dji.common.camera.WhiteBalance
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
import dji.ux.beta.visualcamera.widget.cameraconfig.wb.CameraConfigWBWidgetModel.CameraConfigWBState.*

private const val TAG = "ConfigWBWidMod"

/**
 * Widget Model for the [CameraConfigWBWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigWBWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val wbProcessor: DataProcessor<WhiteBalance> =
            DataProcessor.create(WhiteBalance(SettingsDefinitions.WhiteBalancePreset.UNKNOWN))

    private val wbStateProcessor: DataProcessor<CameraConfigWBState> =
            DataProcessor.create(ProductDisconnected)
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
     * The camera config white balance state.
     */
    val whiteBalanceState: Flowable<CameraConfigWBState>
        get() = wbStateProcessor.toFlowable()

    //endregion
    //region LifeCycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        val whiteBalanceKey: DJIKey = lensModule.createLensKey(CameraKey.WHITE_BALANCE, cameraIndex.index, lensType.value())
        bindDataProcessor(whiteBalanceKey, wbProcessor)
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
            if (cameraConnectionDataProcessor.value) {
                wbStateProcessor.onNext(CurrentWBValue(wbProcessor.value))
            } else {
                wbStateProcessor.onNext(CameraDisconnected)
            }
        } else {
            wbStateProcessor.onNext(ProductDisconnected)
        }

    }
    //endregion

    /**
     * Class to represent states of White Balance
     */
    sealed class CameraConfigWBState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigWBState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigWBState()

        /**
         * Current value of camera white balance
         */
        data class CurrentWBValue(val whiteBalance: WhiteBalance) : CameraConfigWBState()
    }
}