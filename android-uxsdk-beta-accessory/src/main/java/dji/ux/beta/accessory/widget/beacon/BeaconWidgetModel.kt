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

package dji.ux.beta.accessory.widget.beacon

import dji.keysdk.AccessoryAggregationKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.accessory.widget.beacon.BeaconWidgetModel.BeaconState.*
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

private const val TAG = "BeaconWidgetModel"

/**
 * Beacon Widget Model
 *
 * Widget Model for the [BeaconWidget] used to define the
 * underlying logic and communication
 */
class BeaconWidgetModel(
        djiSdkModel: DJISDKModel,
        val keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isBeaconConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isBeaconEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val beaconStateProcessor: DataProcessor<BeaconState> = DataProcessor.create(ProductDisconnected)
    private lateinit var beaconEnabledKey: DJIKey

    /**
     * Beacon state
     */
    val beaconState: Flowable<BeaconState>
        get() = beaconStateProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {
        val beaconConnectedKey: DJIKey = AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.CONNECTION)
        bindDataProcessor(beaconConnectedKey, isBeaconConnectedProcessor)
        beaconEnabledKey = AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED)
        bindDataProcessor(beaconEnabledKey, isBeaconEnabledProcessor)
    }

    override fun inCleanup() {
        // Nothing to clean up
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (isBeaconConnectedProcessor.value) {
                if (isBeaconEnabledProcessor.value) {
                    beaconStateProcessor.onNext(Active)
                } else {
                    beaconStateProcessor.onNext(Inactive)
                }
            } else {
                beaconStateProcessor.onNext(NotSupported)
            }
        } else {
            beaconStateProcessor.onNext(ProductDisconnected)
        }
    }
    //endregion

    //region Actions
    /**
     * Toggle the beacon state between on and off
     *
     * @return Completable representing the success/failure of the action
     */
    fun toggleBeaconState(): Completable {
        return djiSdkModel.setValue(beaconEnabledKey, !isBeaconEnabledProcessor.value)
    }
    //endregion

    /**
     * Class defines the states of the beacon
     */
    sealed class BeaconState {
        /**
         * Product is disconnected
         */
        object ProductDisconnected : BeaconState()

        /**
         * Product does not support the beacon or the beacon is not connected
         */
        object NotSupported : BeaconState()

        /**
         * The beacon is connected to the product and is currently switched OFF
         */
        object Inactive : BeaconState()

        /**
         * The beacon is connected to the product and is currently switched ON
         */
        object Active : BeaconState()
    }

}