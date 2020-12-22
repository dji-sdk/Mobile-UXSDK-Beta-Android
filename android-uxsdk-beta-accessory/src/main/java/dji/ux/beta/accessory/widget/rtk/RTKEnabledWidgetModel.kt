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

package dji.ux.beta.accessory.widget.rtk

import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [RTKEnabledWidget] used to define
 * the underlying logic and communication
 */
class RTKEnabledWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isRTKEnabledKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED)
    private val isRTKEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isMotorOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val homePointDataSourceProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val isRTKTakeoffHeightSetProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val canEnableRTKProcessor: DataProcessor<Boolean> = DataProcessor.create(true)
    //endregion

    //region Data
    /**
     * Get whether RTK is enabled.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = isRTKEnabledProcessor.toFlowable()

    /**
     * Get whether RTK can be enabled.
     */
    val canEnableRTK: Flowable<Boolean>
        get() = canEnableRTKProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(isRTKEnabledKey, isRTKEnabledProcessor)
        val isMotorOnKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON)
        bindDataProcessor(isMotorOnKey, isMotorOnProcessor)
        val homePointDataSourceKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HOME_LOCATION_DATA_SOURCE)
        bindDataProcessor(homePointDataSourceKey, homePointDataSourceProcessor)
        val isRTKTakeOffHeightSetKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HAS_SET_TAKE_OFF_ALTITUDE)
        bindDataProcessor(isRTKTakeOffHeightSetKey, isRTKTakeoffHeightSetProcessor)
    }

    override fun inCleanup() {
        // do nothing
    }

    override fun updateStates() {
        canEnableRTKProcessor.onNext(!isMotorOnProcessor.value || (isRTKTakeoffHeightSetProcessor.value
                && HomePointDataSourceType.find(homePointDataSourceProcessor.value) == HomePointDataSourceType.RTK))
    }
    //endregion

    //region User interaction
    fun setRTKEnabled(enabled: Boolean): Completable {
        return djiSdkModel.setValue(isRTKEnabledKey, enabled)
    }
    //endregion
}

/**
 * The data source of the home point
 *
 * @property value Identifier for the item
 */
enum class HomePointDataSourceType(@get:JvmName("value") val value: Int) {

    /**
     * There is no home point data source
     */
    NONE(0),

    /**
     * The home point data source is GPS
     */
    GPS(1),

    /**
     * The home point data source is RTK
     */
    RTK(2),

    /**
     * The home point data source is unknown
     */
    UNKNOWN(255);

    companion object {
        @JvmStatic
        val values = values()

        @JvmStatic
        fun find(value: Int): HomePointDataSourceType {
            return values.find { it.value == value } ?: UNKNOWN
        }
    }
}