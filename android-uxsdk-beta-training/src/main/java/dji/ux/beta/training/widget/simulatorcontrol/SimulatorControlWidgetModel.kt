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
package dji.ux.beta.training.widget.simulatorcontrol

import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.simulator.SimulatorState
import dji.common.flightcontroller.simulator.SimulatorWindData
import dji.common.model.LocationCoordinate2D
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.UXSDKErrorDescription
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Simulator Control Widget Model
 *
 *
 * Widget Model for [SimulatorControlWidget] used to define the
 * underlying logic and communication
 */
class SimulatorControlWidgetModel(djiSdkModel: DJISDKModel,
                                  keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region private fields
    private val simulatorStateBuilder = SimulatorState.Builder()
    private val windBuilder = SimulatorWindData.Builder()
    private val simulatorStateDataProcessor: DataProcessor<SimulatorState> =
            DataProcessor.create(simulatorStateBuilder.location(LocationCoordinate2D(0.0, 0.0))
                    .areMotorsOn(false)
                    .isFlying(false)
                    .positionX(0f)
                    .positionY(0f)
                    .positionZ(0f)
                    .pitch(0f)
                    .yaw(0f)
                    .roll(0f)
                    .build())
    private val satelliteCountDataProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val simulatorWindDataProcessor: DataProcessor<SimulatorWindData> = DataProcessor.create(windBuilder.build())
    private val simulatorActiveDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val simulatorWindDataKey: DJIKey = FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA)

    override fun inSetup() {
        val simulatorStateKey: DJIKey = FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE)
        bindDataProcessor(simulatorStateKey, simulatorStateDataProcessor)
        val satelliteCountKey: DJIKey = FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT)
        bindDataProcessor(satelliteCountKey, satelliteCountDataProcessor)
        bindDataProcessor(simulatorWindDataKey, simulatorWindDataProcessor)
        val simulatorActiveKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE)
        bindDataProcessor(simulatorActiveKey, simulatorActiveDataProcessor)
    }

    override fun inCleanup() { // No clean up needed
    }

    override fun updateStates() { // No states to update
    }
    //endregion

    //region actions
    /**
     * Start simulator on the aircraft
     *
     * @param initializationData instance of [InitializationData] required to start simulation
     * @return Completable to determine status of the action
     */
    fun startSimulator(initializationData: InitializationData): Completable {
        val startSimulatorKey: DJIKey = FlightControllerKey.create(FlightControllerKey.START_SIMULATOR)
        return djiSdkModel.performAction(startSimulatorKey, initializationData)
    }

    /**
     * Stop simulator on the aircraft
     *
     * @return Completable to determine status of the action
     */
    fun stopSimulator(): Completable {
        val stopSimulatorKey: DJIKey = FlightControllerKey.create(FlightControllerKey.STOP_SIMULATOR)
        return djiSdkModel.performAction(stopSimulatorKey)
    }

    /**
     * Set values to simulate wind in x, y and z directions
     * The unit for wind speed is m/s
     *
     * @param simulatorWindData [SimulatorWindData] instance with values to simulate
     * @return Completable to determine status of the action
     */
    fun setSimulatorWindData(simulatorWindData: SimulatorWindData): Completable {
        return if (simulatorActiveDataProcessor.value) {
            djiSdkModel.setValue(simulatorWindDataKey, simulatorWindData)
        } else {
            Completable.error(UXSDKError(UXSDKErrorDescription.SIMULATOR_WIND_ERROR))
        }
    }
    //endregion
    //region Data
    /**
     * Get the current state of simulation. Includes
     * pitch, yaw, roll, world coordinates, location coordinates, areMotorsOn, isFlying
     */
    val simulatorState: Flowable<SimulatorState>
        get() = simulatorStateDataProcessor.toFlowable()

    /**
     * Get the current wind simulation values. Includes
     * wind speed in x, y and z directions
     * The unit for wind speed is m/s
     *
     */
    val simulatorWindData: Flowable<SimulatorWindData>
        get() = simulatorWindDataProcessor.toFlowable()

    /**
     * Get the number of satellites being simulated
     */
    val satelliteCount: Flowable<Int>
        get() = satelliteCountDataProcessor.toFlowable()

    /**
     * Check if the simulator is running
     */
    val isSimulatorActive: Flowable<Boolean>
        get() = simulatorActiveDataProcessor.toFlowable()

    //endregion

}