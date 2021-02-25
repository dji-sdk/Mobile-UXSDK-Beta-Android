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

package dji.ux.beta.core.panel.listitem.imustatus

import dji.common.logics.warningstatuslogic.WarningStatusItem.WarningLevel.*
import dji.common.logics.warningstatuslogic.WarningStatusLogic
import dji.common.logics.warningstatuslogic.basicsublogics.FlightControllerWarningStatusLogic
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [IMUStatusListItemWidget] used to define
 * the underlying logic and communication
 */
class IMUStatusListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val imuStateProcessor: DataProcessor<IMUState> = DataProcessor.create(IMUState.ProductDisconnected)

    private val imuDiagnosticsProcessor: DataProcessor<FlightControllerWarningStatusLogic.IMUWarningStatusItem> =
            DataProcessor.create(FlightControllerWarningStatusLogic.IMUWarningStatusItem(
                    WarningStatusLogic.getInstance().warningStatusItemImuStatusNormal))

    /**
     * Get the IMU state
     *
     * @return Flowable for the data processor that user should subscribe to
     */
    val imuItemState: Flowable<IMUState>
        get() = imuStateProcessor.toFlowable()

    override fun inSetup() {
        // TODO When code is ready get diagnostics
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when (imuDiagnosticsProcessor.value.imuWarningStatusItem.warningLevel) {
                OFFLINE -> imuStateProcessor.onNext(IMUState.Disabled)
                GOOD -> imuStateProcessor.onNext(IMUState.Normal)
                WARNING -> imuStateProcessor.onNext(IMUState.Warning(imuDiagnosticsProcessor
                        .value.imuWarningStatusItem.message))
                ERROR -> imuStateProcessor.onNext(Error(imuDiagnosticsProcessor
                        .value.imuWarningStatusItem.message))
                else -> {
                    // Do nothing
                }
            }
        } else {
            imuStateProcessor.onNext(IMUState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        // No clean up needed
    }

    /**
     * Class represents states of IMUStatusListItemState
     */
    sealed class IMUState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : IMUState()

        /**
         * When IMU is disabled
         */
        object Disabled : IMUState()

        /**
         * When IMU is normal
         */
        object Normal : IMUState()

        /**
         * When IMU has an active warning
         */
        data class Warning(val message: String) : IMUState()

        /**
         * When IMU an active error
         */
        data class Error(val message: String) : IMUState()

    }
}