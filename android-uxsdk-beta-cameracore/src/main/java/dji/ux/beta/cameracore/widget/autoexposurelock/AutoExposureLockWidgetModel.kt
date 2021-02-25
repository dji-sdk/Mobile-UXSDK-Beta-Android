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

package dji.ux.beta.cameracore.widget.autoexposurelock

import dji.common.camera.SettingsDefinitions.LensType
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "AutoExpoLockWidMod"

/**
 * Auto Exposure Lock Widget Model
 *
 *
 * Widget Model for the [AutoExposureLockWidget] used to define the
 * underlying logic and communication
 */
class AutoExposureLockWidgetModel(
        djiSdkModel: DJISDKModel,
        uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager) {

    //region Fields
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val autoExposureLockStateProcessor: DataProcessor<AutoExposureLockState> = DataProcessor.create(AutoExposureLockState.ProductDisconnected)
    private val autoExposureLockProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private lateinit var autoExposureLockKey: DJIKey
    private val lensModule = LensModule()

    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: CameraIndex = CameraIndex.find(CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            lensModule.setCameraIndex(this, value)
            restart()
        }

    /**
     * The lens the widget model is reacting to
     */
    var lensType: LensType = LensType.ZOOM
        set(value) {
            field = value
            restart()
        }

    /**
     * AutoExposure lock state
     */
    val autoExposureLockState: Flowable<AutoExposureLockState>
        get() = autoExposureLockStateProcessor.toFlowable()

    //endregion
    //region Lifecycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        autoExposureLockKey = lensModule.createLensKey(CameraKey.AE_LOCK, cameraIndex.index, lensType.value())
        bindDataProcessor(autoExposureLockKey, autoExposureLockProcessor)
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))
    }

    override fun inCleanup() {
        // nothing to clean
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (cameraConnectionDataProcessor.value) {
                if (autoExposureLockProcessor.value) {
                    autoExposureLockStateProcessor.onNext(AutoExposureLockState.Locked)
                } else {
                    autoExposureLockStateProcessor.onNext(AutoExposureLockState.Unlocked)
                }
            } else {
                autoExposureLockStateProcessor.onNext(AutoExposureLockState.CameraDisconnected)
            }
        } else {
            autoExposureLockStateProcessor.onNext(AutoExposureLockState.ProductDisconnected)
        }
    }

    //endregion

    /**
     * Set auto exposure lock the opposite of its current state
     *
     * @return Completable representing success and failure of action
     */
    fun toggleAutoExposureLock(): Completable {
        return djiSdkModel.setValue(autoExposureLockKey, !autoExposureLockProcessor.value)
    }

    /**
     * Class defines states of AutoExposureLock State
     */
    sealed class AutoExposureLockState {
        /**
         * Product is disconnected.
         */
        object ProductDisconnected : AutoExposureLockState()

        /**
         * Camera is disconnected.
         */
        object CameraDisconnected : AutoExposureLockState()

        /**
         *  Auto Exposure unlocked state
         */
        object Unlocked : AutoExposureLockState()

        /**
         *  Auto Exposure locked state
         */
        object Locked : AutoExposureLockState()

    }

}