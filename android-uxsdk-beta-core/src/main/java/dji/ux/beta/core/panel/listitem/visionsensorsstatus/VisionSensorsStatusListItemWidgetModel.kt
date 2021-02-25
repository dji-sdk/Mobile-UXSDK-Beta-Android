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

package dji.ux.beta.core.panel.listitem.visionsensorsstatus

import dji.common.logics.warningstatuslogic.WarningStatusItem.WarningLevel.*
import dji.common.logics.warningstatuslogic.WarningStatusLogic
import dji.common.logics.warningstatuslogic.basicsublogics.VisionWarningStatusLogic
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [VisionSensorsStatusListItemWidget] used to define
 * the underlying logic and communication
 */
class VisionSensorsStatusListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val visionSensorStateProcessor: DataProcessor<VisionSensorListItemState> =
            DataProcessor.create(VisionSensorListItemState.ProductDisconnected)
    private val userAvoidanceEnableProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val visionDiagnosticsProcessor: DataProcessor<VisionWarningStatusLogic.VisionWarningStatusItem> =
            DataProcessor.create(VisionWarningStatusLogic.VisionWarningStatusItem(
                    WarningStatusLogic.getInstance().warningStatusItemVisionStatusNormal))

    /**
     * Get the vision sensors state
     */
    val visionSensorItemState: Flowable<VisionSensorListItemState>
        get() = visionSensorStateProcessor.toFlowable()

    override fun inSetup() {
        val userAvoidanceEnableKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE)
        bindDataProcessor(userAvoidanceEnableKey, userAvoidanceEnableProcessor)
        // TODO When code is ready get diagnostics
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (userAvoidanceEnableProcessor.value) {
                when (visionDiagnosticsProcessor.value.visionWarningStatusItem.warningLevel) {
                    GOOD -> visionSensorStateProcessor.onNext(VisionSensorListItemState.Normal)
                    WARNING -> visionSensorStateProcessor.onNext(
                            VisionSensorListItemState.Warning(visionDiagnosticsProcessor.value.visionWarningStatusItem.message)
                    )
                    ERROR -> visionSensorStateProcessor.onNext(
                            VisionSensorListItemState.Error(visionDiagnosticsProcessor.value.visionWarningStatusItem.message)
                    )
                    OFFLINE -> visionSensorStateProcessor.onNext(VisionSensorListItemState.Disabled)
                    else -> {
                        // Do nothing 
                    }
                }

            } else {
                visionSensorStateProcessor.onNext(VisionSensorListItemState.Disabled)
            }
        } else {
            visionSensorStateProcessor.onNext(VisionSensorListItemState.ProductDisconnected)
        }

    }

    override fun inCleanup() {
        // Do nothing
    }

    /**
     * Class represents states of VisionSensorListItemState
     */
    sealed class VisionSensorListItemState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : VisionSensorListItemState()

        /**
         * When vision sensors are disabled
         */
        object Disabled : VisionSensorListItemState()

        /**
         * When vision sensors are normal
         */
        object Normal : VisionSensorListItemState()

        /**
         * When vision sensors have an active warning
         */
        data class Warning(val message: String) : VisionSensorListItemState()

        /**
         * When vision sensors have an active error
         */
        data class Error(val message: String) : VisionSensorListItemState()

    }

}