/*
 * Copyright (c) 2018-2020 DJI
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

package dji.ux.beta.core.panel.listitem.emmcstatus

import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions

/**
 * Widget Model for the [EMMCStatusListItemWidget] used to define
 * the underlying logic and communication
 */
class EMMCStatusListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {


    private val eMMCStateProcessor: DataProcessor<EMMCState> = DataProcessor.create(EMMCState.ProductDisconnected)
    private val eMMCRemainingCapacityProcessor = DataProcessor.create(0)
    private val eMMCSupportedProcessor = DataProcessor.create(false)
    private val eMMCOperationStateProcessor = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN)

    /**
     * Index of eMMC
     */
    var cameraIndex: Int = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index
        set(value) {
            field = value
            restart()
        }

    /**
     * Get the eMMC state
     */
    val eMMCState: Flowable<EMMCState> = eMMCStateProcessor.toFlowable()

    override fun inSetup() {
        val eMMCRemainingCapacityKey = CameraKey.create(CameraKey.INNERSTORAGE_REMAINING_SPACE_IN_MB, cameraIndex)
        val eMMCOperationStateKey = CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex)
        val eMMCSupportedKey = CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, cameraIndex)
        bindDataProcessor(eMMCSupportedKey, eMMCSupportedProcessor)
        bindDataProcessor(eMMCRemainingCapacityKey, eMMCRemainingCapacityProcessor)
        bindDataProcessor(eMMCOperationStateKey, eMMCOperationStateProcessor)

    }

    override fun inCleanup() {
        //No clean up necessary
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (eMMCSupportedProcessor.value) {
                eMMCStateProcessor.onNext(EMMCState.CurrentEMMCState(eMMCOperationStateProcessor.value,
                        eMMCRemainingCapacityProcessor.value))
            } else {
                eMMCStateProcessor.onNext(EMMCState.NotSupported)
            }
        } else {
            eMMCStateProcessor.onNext(EMMCState.ProductDisconnected)
        }

    }

    /**
     * Format eMMC
     */
    fun formatEMMC(): Completable {
        val eMMCFormatKey = CameraKey.create(CameraKey.FORMAT_INTERNAL_STORAGE, cameraIndex)
        return djiSdkModel.performAction(eMMCFormatKey,
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE)
    }

    /**
     * Class represents states of eMMC Item
     */
    sealed class EMMCState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : EMMCState()

        /**
         * When product does not support eMMC
         */
        object NotSupported : EMMCState()

        /**
         * When product is connected
         * @property eMMCOperationState - Current operation State of eMMC
         * @property remainingSpace - Remaining space in MB
         */
        data class CurrentEMMCState(val eMMCOperationState: SettingsDefinitions.SDCardOperationState,
                                    val remainingSpace: Int) : EMMCState()

    }


}