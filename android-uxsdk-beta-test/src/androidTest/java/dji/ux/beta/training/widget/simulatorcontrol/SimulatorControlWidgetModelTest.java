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

package dji.ux.beta.training.widget.simulatorcontrol;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.simulator.SimulatorWindData;
import dji.common.model.LocationCoordinate2D;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.observers.TestObserver;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Test:
 * This class tests the public methods in {@link SimulatorControlWidgetModel}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SimulatorControlWidgetModelTest {

    private static final double TEST_LATITUDE = 37.4256293;
    private static final double TEST_LONGITUDE = -122.20539;
    private static SimulatorState defaultSimulatorState = new SimulatorState.Builder()
            .location(new LocationCoordinate2D(0.0, 0.0))
            .areMotorsOn(false)
            .isFlying(false)
            .positionX(0f)
            .positionY(0f)
            .positionZ(0f)
            .pitch(0f)
            .yaw(0f)
            .roll(0f)
            .build();
    private static SimulatorState expectedSimulatorState = new SimulatorState.Builder()
            .areMotorsOn(true)
            .isFlying(true)
            .location(new LocationCoordinate2D(TEST_LATITUDE, TEST_LONGITUDE))
            .positionX(1f)
            .positionY(2f)
            .positionZ(3f)
            .pitch(10f)
            .yaw(11f)
            .roll(12f)
            .build();

    private static SimulatorWindData defaultSimulatorWindData = new SimulatorWindData.Builder()
            .windSpeedX(0)
            .windSpeedY(0)
            .windSpeedZ(0)
            .build();

    private static SimulatorWindData expectedSimulatorWindData = new SimulatorWindData.Builder()
            .windSpeedX(1)
            .windSpeedY(1)
            .windSpeedZ(1)
            .build();

    private static CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private SimulatorControlWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new SimulatorControlWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel);

    }

    /**
     * Test initial value emitted by the flowable is as expected and is updated
     * by the key to active state
     */
    @Test
    public void simulatorControlWidgetModel_isActive() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isSimulatorActive().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    /**
     * Test initial value emitted by the flowable is as expected and is updated
     * by the key to In active state
     */
    @Test
    public void simulatorControlWidgetModel_isInActive() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                false, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isSimulatorActive().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    /**
     * Test the get Satellite count flowable
     */
    @Test
    public void simulatorControlWidget_satelliteCount() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT),
                12, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getSatelliteCount().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 12);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    /**
     * Test the get simulator state flowable
     */
    @Test
    public void simulatorControlWidget_simulatorState() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE),
                expectedSimulatorState, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<SimulatorState> testSubscriber =
                widgetModel.getSimulatorState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultSimulatorState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultSimulatorState, expectedSimulatorState);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    /**
     * Test the get simulator wind state flowable
     */
    @Test
    public void simulatorControlWidget_simulatorWindState() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA),
                expectedSimulatorWindData, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<SimulatorWindData> testSubscriber =
                widgetModel.getSimulatorWindData().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultSimulatorWindData);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultSimulatorWindData, expectedSimulatorWindData);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    /**
     * Test start simulator success case
     */
    @Test
    public void simulatorControlWidget_startSimulator_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        InitializationData initializationData = InitializationData.createInstance(
                new LocationCoordinate2D(TEST_LATITUDE, TEST_LONGITUDE), 20, 12);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_SIMULATOR),
                initializationData,
                null);
        widgetModel.setup();
        TestObserver observer = widgetModel.startSimulator(initializationData).test();
        testScheduler.triggerActions();
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    /**
     * Test start simulator error case
     */
    @Test
    public void simulatorControlWidget_startSimulator_error() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        InitializationData initializationData = InitializationData.createInstance(
                new LocationCoordinate2D(TEST_LATITUDE, TEST_LONGITUDE), 20, 12);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_SIMULATOR),
                initializationData,
                uxsdkError);
        widgetModel.setup();
        TestObserver observer = widgetModel.startSimulator(initializationData).test();
        testScheduler.triggerActions();
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    /**
     * Test stop simulator success case
     */
    @Test
    public void simulatorControlWidget_stopimulator_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.STOP_SIMULATOR),
                null);
        widgetModel.setup();
        TestObserver observer = widgetModel.stopSimulator().test();
        testScheduler.triggerActions();
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    /**
     * Test stop simulator error case
     */
    @Test
    public void simulatorControlWidget_stopSimulator_error() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.STOP_SIMULATOR),
                uxsdkError);
        widgetModel.setup();
        TestObserver observer = widgetModel.stopSimulator().test();
        testScheduler.triggerActions();
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }


    /**
     * Test set wind simulation data success case
     */
    @Test
    public void simulatorControlWidget_setWindData_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA),
                expectedSimulatorWindData, null);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setSimulatorWindData(expectedSimulatorWindData).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    /**
     * Test set wind simulation data error case
     */
    @Test
    public void simulatorControlWidget_setWindData_error() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_STATE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                true, 5, TimeUnit.SECONDS);
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA));
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SIMULATOR_WIND_DATA),
                expectedSimulatorWindData, uxsdkError);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setSimulatorWindData(expectedSimulatorWindData).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }


    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
