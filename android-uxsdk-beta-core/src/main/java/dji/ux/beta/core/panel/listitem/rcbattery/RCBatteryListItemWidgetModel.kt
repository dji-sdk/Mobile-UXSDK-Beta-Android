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

package dji.ux.beta.core.panel.listitem.rcbattery

import dji.common.remotecontroller.BatteryState
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.rcbattery.RCBatteryListItemWidgetModel.RCBatteryState.RCDisconnected
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [RCBatteryListItemWidget] used to define
 * the underlying logic and communication
 */

class RCBatteryListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields

    private val rcBatteryLevelProcessor: DataProcessor<BatteryState> = DataProcessor.create(BatteryState(0, 0))
    private val rcBatteryStateProcessor: DataProcessor<RCBatteryState> = DataProcessor.create(RCDisconnected)
    private val rcBatteryLowProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rcConnectionProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    //endregion

    //region Data
    /**
     * Get the RC battery state
     */
    val rcBatteryState: Flowable<RCBatteryState>
        get() = rcBatteryStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val rcConnectionKey = RemoteControllerKey.create(RemoteControllerKey.CONNECTION)
        bindDataProcessor(rcConnectionKey, rcConnectionProcessor)
        val rcBatteryLevelKey = RemoteControllerKey.create(RemoteControllerKey.BATTERY_STATE)
        bindDataProcessor(rcBatteryLevelKey, rcBatteryLevelProcessor)
        val rcBatteryLowKey = RemoteControllerKey.create(RemoteControllerKey.IS_CHARGE_REMAINING_LOW)
        bindDataProcessor(rcBatteryLowKey, rcBatteryLowProcessor)
    }

    override fun inCleanup() {
        // No Clean up
    }

    override fun updateStates() {
        val rcBatteryLevelPercent = rcBatteryLevelProcessor.value.remainingChargeInPercent
        if (rcConnectionProcessor.value && rcBatteryLowProcessor.value) {
            rcBatteryStateProcessor.onNext(RCBatteryState.Low(rcBatteryLevelPercent))
        } else if (rcConnectionProcessor.value) {
            rcBatteryStateProcessor.onNext(RCBatteryState.Normal(rcBatteryLevelPercent))
        } else {
            rcBatteryStateProcessor.onNext(RCDisconnected)
        }
    }

    /**
     * Class to represent states of RCBatteryListItem
     */
    sealed class RCBatteryState {
        /**
         * When remote controller is disconnected
         */
        object RCDisconnected : RCBatteryState()

        /**
         * When product is connected and rc battery is normal
         */
        data class Normal(val remainingChargePercent: Int) : RCBatteryState()

        /**
         * When product is connected and rc battery is critically low
         */
        data class Low(val remainingChargePercent: Int) : RCBatteryState()
    }
    //endregion
}
