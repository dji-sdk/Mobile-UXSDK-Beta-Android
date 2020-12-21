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

package dji.ux.beta.core.panel.listitem.sdcardstatus

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
 * Widget Model for the [SDCardStatusListItemWidget] used to define
 * the underlying logic and communication
 */
class SDCardStatusListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {


    private val sdCardStateProcessor: DataProcessor<SDCardState> = DataProcessor.create(SDCardState.ProductDisconnected)
    private val sdCardRemainingCapacityProcessor = DataProcessor.create(0)
    private val sdCardOperationStateProcessor = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN)

    /**
     * Index of sdCard
     */
    var cameraIndex: Int = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index
        set(value) {
            field = value
            restart()
        }

    /**
     * Get the sd card state
     */
    val sdCardState: Flowable<SDCardState> = sdCardStateProcessor.toFlowable()

    override fun inSetup() {
        val sdCardRemainingCapacityKey = CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB, cameraIndex)
        val sdCardOperationStateKey = CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex)

        bindDataProcessor(sdCardRemainingCapacityKey, sdCardRemainingCapacityProcessor)
        bindDataProcessor(sdCardOperationStateKey, sdCardOperationStateProcessor)

    }

    override fun inCleanup() {
        //No clean up necessary
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            sdCardStateProcessor.onNext(SDCardState.CurrentSDCardState(sdCardOperationStateProcessor.value,
                    sdCardRemainingCapacityProcessor.value))
        } else {
            sdCardStateProcessor.onNext(SDCardState.ProductDisconnected)
        }

    }

    /**
     * Format SDCard
     */
    fun formatSDCard(): Completable {
        val sdCardFormatKey = CameraKey.create(CameraKey.FORMAT_SD_CARD, cameraIndex)
        return djiSdkModel.performAction(sdCardFormatKey)

    }

    /**
     * Class represents states of SDCard List Item
     */
    sealed class SDCardState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : SDCardState()

        /**
         * When product is connected
         * @property sdCardOperationState - Current operation State of SDCard
         * @property remainingSpace - Remaining space in MB
         */
        data class CurrentSDCardState(val sdCardOperationState: SettingsDefinitions.SDCardOperationState,
                                      val remainingSpace: Int) : SDCardState()

    }


}