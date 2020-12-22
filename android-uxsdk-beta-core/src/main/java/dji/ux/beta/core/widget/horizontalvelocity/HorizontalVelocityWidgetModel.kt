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

package dji.ux.beta.core.widget.horizontalvelocity

import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.toVelocity
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidgetModel.HorizontalVelocityState.CurrentVelocity
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidgetModel.HorizontalVelocityState.ProductDisconnected
import kotlin.math.sqrt


/**
 * Widget Model for the [HorizontalVelocityWidget] used to define
 * the underlying logic and communication
 */
class HorizontalVelocityWidgetModel(djiSdkModel: DJISDKModel,
                                    keyedStore: ObservableInMemoryKeyedStore,
                                    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {


    private val velocityXProcessor: DataProcessor<Float> = DataProcessor.create(0.0f)
    private val velocityYProcessor: DataProcessor<Float> = DataProcessor.create(0.0f)
    private val unitTypeDataProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val horizontalVelocityStateProcessor: DataProcessor<HorizontalVelocityState> = DataProcessor.create(ProductDisconnected)

    /**
     * Get the value of the horizontal velocity state of the aircraft
     */
    val horizontalVelocityState: Flowable<HorizontalVelocityState>
        get() = horizontalVelocityStateProcessor.toFlowable()

    override fun inSetup() {
        val velocityXKey = FlightControllerKey.create(FlightControllerKey.VELOCITY_X)
        bindDataProcessor(velocityXKey, velocityXProcessor)
        val velocityYKey = FlightControllerKey.create(FlightControllerKey.VELOCITY_Y)
        bindDataProcessor(velocityYKey, velocityYProcessor)

        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            horizontalVelocityStateProcessor.onNext(
                    CurrentVelocity(calculateHorizontalVelocity(), unitTypeDataProcessor.value))
        } else {
            horizontalVelocityStateProcessor.onNext(ProductDisconnected)
        }

    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    private fun calculateHorizontalVelocity(): Float {
        val xVelocitySquare = velocityXProcessor.value * velocityXProcessor.value
        val yVelocitySquare = velocityYProcessor.value * velocityYProcessor.value
        return sqrt((xVelocitySquare + yVelocitySquare)).toVelocity(unitTypeDataProcessor.value)
    }


    /**
     * Class to represent states of horizontal velocity
     */
    sealed class HorizontalVelocityState {
        /**
         *  When product is disconnected
         */
        object ProductDisconnected : HorizontalVelocityState()

        /**
         * When aircraft is moving horizontally
         */
        data class CurrentVelocity(val velocity: Float, val unitType: UnitType) : HorizontalVelocityState()

    }
}