/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.widget.simulator;

import android.support.annotation.NonNull;

import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.simulator.SimulatorWindData;
import dji.common.model.LocationCoordinate2D;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.SchedulerProviderInterface;
import dji.ux.beta.base.UXSDKError;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.base.UXSDKErrorDescription;
import dji.ux.beta.util.DataProcessor;

/**
 * Simulator Control Widget Model
 * <p>
 * Widget Model for {@link SimulatorControlWidget} used to define the
 * underlying logic and communication
 */
public class SimulatorControlWidgetModel extends WidgetModel {

    //region private fields
    private final DataProcessor<SimulatorState> simulatorStateDataProcessor;
    private final DataProcessor<Integer> satelliteCountDataProcessor;
    private final DataProcessor<SimulatorWindData> simulatorWindDataProcessor;
    private final DataProcessor<Boolean> simulatorActiveDataProcessor;
    private DJIKey simulatorWindDataKey;
    private SchedulerProviderInterface schedulerProvider;

    //endregion

    //region lifecycle
    public SimulatorControlWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore,
                                       @NonNull SchedulerProviderInterface schedulerProvider) {
        super(djiSdkModel, keyedStore);
        this.schedulerProvider = schedulerProvider;
        SimulatorState.Builder simulatorStateBuilder = new SimulatorState.Builder();
        simulatorStateDataProcessor =
            DataProcessor.create(simulatorStateBuilder.location(new LocationCoordinate2D(0.0, 0.0))
                                                      .areMotorsOn(false)
                                                      .isFlying(false)
                                                      .positionX(0f)
                                                      .positionY(0f)
                                                      .positionZ(0f)
                                                      .pitch(0f)
                                                      .yaw(0f)
                                                      .roll(0f)
                                                      .build());
        SimulatorWindData.Builder windBuilder = new SimulatorWindData.Builder();
        simulatorWindDataProcessor = DataProcessor.create(windBuilder.build());
        satelliteCountDataProcessor = DataProcessor.create(0);
        simulatorActiveDataProcessor = DataProcessor.create(false);
    }

    @Override
    protected void inSetup() {
        DJIKey simulatorStateKey = FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE);
        bindDataProcessor(simulatorStateKey, simulatorStateDataProcessor);
        DJIKey satelliteCountKey = FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT);
        bindDataProcessor(satelliteCountKey, satelliteCountDataProcessor);
        simulatorWindDataKey = FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA);
        bindDataProcessor(simulatorWindDataKey, simulatorWindDataProcessor);
        DJIKey simulatorActiveKey = FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE);
        bindDataProcessor(simulatorActiveKey, simulatorActiveDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // No clean up needed
    }

    @Override
    protected void updateStates() {
        // No states to update
    }

    //endregion

    //region actions

    /**
     * Start simulator on the aircraft
     *
     * @param initializationData instance of {@link InitializationData} required to start simulation
     * @return Completable to determine status of the action
     */
    public Completable startSimulator(@NonNull InitializationData initializationData) {
        DJIKey startSimulatorKey = FlightControllerKey.create(FlightControllerKey.START_SIMULATOR);
        return djiSdkModel.performAction(startSimulatorKey, initializationData).subscribeOn(schedulerProvider.io());
    }

    /**
     * Stop simulator on the aircraft
     *
     * @return Completable to determine status of the action
     */
    public Completable stopSimulator() {
        DJIKey stopSimulatorKey = FlightControllerKey.create(FlightControllerKey.STOP_SIMULATOR);
        return djiSdkModel.performAction(stopSimulatorKey).subscribeOn(schedulerProvider.io());
    }

    /**
     * Set values to simulate wind in x, y and z directions
     *
     * @param simulatorWindData {@link SimulatorWindData} instance with values to simulate
     * @return Completable to determine status of the action
     */
    public Completable setSimulatorWindData(@NonNull SimulatorWindData simulatorWindData) {
        if (simulatorActiveDataProcessor.getValue()) {
            return djiSdkModel.setValue(simulatorWindDataKey, simulatorWindData).subscribeOn(schedulerProvider.io());
        } else {
            return Completable.error(new UXSDKError(UXSDKErrorDescription.SIMULATOR_WIND_ERROR));
        }
    }

    //endregion

    //region Data

    /**
     * Get the current state of simulation. Includes
     * pitch, yaw, roll, world coordinates, location coordinates, areMotorsOn, isFlying
     *
     * @return {@link SimulatorState} instance representing the current state of simulator
     */
    public Flowable<SimulatorState> getSimulatorState() {
        return simulatorStateDataProcessor.toFlowable();
    }

    /**
     * Get the current wind simulation values. Includes
     * wind speed in x, y and z directions
     *
     * @return {@link SimulatorWindData} instance representing the current wind simulation state
     */
    public Flowable<SimulatorWindData> getSimulatorWindData() {
        return simulatorWindDataProcessor.toFlowable();
    }

    /**
     * Get the number of satellites being simulated
     *
     * @return Integer flowable value representing the number of satellites
     */
    public Flowable<Integer> getSatelliteCount() {
        return satelliteCountDataProcessor.toFlowable();
    }

    /**
     * Check if the simulator is running
     *
     * @return Boolean flowable value to represent if the simulator is currently running
     */
    public Flowable<Boolean> isSimulatorActive() {
        return simulatorActiveDataProcessor.toFlowable();
    }
    //endregion
}
