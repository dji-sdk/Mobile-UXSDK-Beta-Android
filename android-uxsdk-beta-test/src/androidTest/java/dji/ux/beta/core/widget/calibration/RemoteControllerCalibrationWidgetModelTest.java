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

package dji.ux.beta.core.widget.calibration;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dji.common.Stick;
import dji.common.error.DJIError;
import dji.common.remotecontroller.CalibrationState;
import dji.common.remotecontroller.HardwareState;
import dji.common.remotecontroller.ProfessionalRC;
import dji.keysdk.ProductKey;
import dji.keysdk.RemoteControllerKey;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
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
 * This class tests the public methods in the {@link RemoteControllerCalibrationWidgetModel}
 * 1.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_connectionState_rcOnly()}
 * Test that the connection state is {@link RemoteControllerCalibrationWidgetModel.ConnectionState#RC_ONLY}.
 * 2.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_connectionState_aircraftOnly()}
 * Test that the connection state is {@link RemoteControllerCalibrationWidgetModel.ConnectionState#AIRCRAFT_ONLY}.
 * 3.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_connectionState_aircraftAndRC()}
 * Test that the connection state is {@link RemoteControllerCalibrationWidgetModel.ConnectionState#AIRCRAFT_AND_RC}.
 * 4.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getLeftStick_leftTop()}
 * Test that the left stick position has the correct values when it is moved to the top left.
 * 5.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getRightStick_rightBottom()}
 * Test that the right stick position has the correct values when it is moved to the bottom right.
 * 6.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getLeftDial_left()}
 * Test that the left dial position has the correct values when it is moved to the left.
 * 7.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getLeftDial_rightPro()}
 * Test that the pro RC's left dial position has the correct values when it is moved to the right.
 * 8.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getRightDial_right()}
 * Test that the right dial position has the correct values when it is moved to the right.
 * 9.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getRightDial_leftPro()}
 * Test that the pro RC's right dial position has the correct values when it is moved to the left.
 * 10.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getLeftLever_top()}
 * Test that the left lever position has the correct values when it is moved to the top.
 * 11.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getRightLever_bottom()}
 * Test that the right lever position has the correct values when it is moved to the bottom.
 * 12.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getLeftStickCalibrationStatus_isRecordedInLimitsState()}
 * Test that the left stick calibration state is recorded in the limits state.
 * 13.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_getRightStickCalibrationStatus_isRecordedInLimitsState()}
 * Test that the right stick calibration state is recorded in the limits state.
 * 14.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationState_success()}
 * Test that the calibration state has the correct value when set successfully.
 * 15.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationState_error()}
 * Test that the calibration state has the previous value when setting the value fails.
 * 16.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_nextCalibrationState_errorWhenConnectedToAircraft()}
 * Test that switching to the next calibration state fails when connected to an aircraft.
 * 17.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_nextCalibrationState_errorWhenDisconnected()}
 * Test that switching to the next calibration state fails when no RC is connected.
 * 18.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationType_isMavic()}
 * Test that the calibration type has the correct value when a Mavic controller is connected.
 * 19.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_mavicStep_all()}
 * Test the calibration state cycle for a Mavic controller.
 * 20.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_hasRightDial_cendence()}
 * Test that the Cendence controller has a right dial.
 * 21.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_hasRightDial_mavic2()}
 * Test that the Mavic 2 controller has a right dial.
 * 22.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_hasLevers_cendence()}
 * Test that the Cendence controller has levers.
 * 23.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_default()}
 * Test that a default controller has completed calibration progress after sticks and left dial have reached their maximum values.
 * 24.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_mavic2Incomplete()}
 * Test that a mavic 2 controller has not completed calibration progress after sticks and left dial have reached their maximum values.
 * 25.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_mavic2()}
 * Test that a mavic 2 controller has completed calibration progress after sticks and both dials have reached their maximum values.
 * 26.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_cendenceIncomplete()}
 * Test that a cendence controller has not completed calibration progress after sticks and both dials have reached their maximum values.
 * 27.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_cendence()}
 * Test that a cendence controller has completed calibration progress after sticks, dials, and levers have reached their maximum values.
 * 28.
 * {@link RemoteControllerCalibrationWidgetModelTest#remoteControllerCalibrationWidgetModel_calibrationProgress_mavicPro()}
 * Test that a mavic pro controller has completed calibration progress after sticks and left dial have reached their maximum values.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteControllerCalibrationWidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private RemoteControllerCalibrationWidgetModel widgetModel;
    private TestScheduler testScheduler;
    private BaseProduct testProduct;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = Mockito.spy(new RemoteControllerCalibrationWidgetModel(djiSdkModel, keyedStore));

        testProduct = Mockito.mock(Aircraft.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn(testProduct).when(widgetModel).getProduct();

        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false);

    }

    @Test
    public void remoteControllerCalibrationWidgetModel_connectionState_rcOnly() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the connection state flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.ConnectionState> testSubscriber =
                widgetModel.getConnectionState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED,
                RemoteControllerCalibrationWidgetModel.ConnectionState.RC_ONLY);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_connectionState_aircraftOnly() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(true);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(false);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the connection state flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.ConnectionState> testSubscriber =
                widgetModel.getConnectionState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED,
                RemoteControllerCalibrationWidgetModel.ConnectionState.AIRCRAFT_ONLY);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_connectionState_aircraftAndRC() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(true);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the connection state flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.ConnectionState> testSubscriber =
                widgetModel.getConnectionState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.ConnectionState.DISCONNECTED,
                RemoteControllerCalibrationWidgetModel.ConnectionState.AIRCRAFT_AND_RC);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getLeftStick_leftTop() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        Stick testStick = new Stick(-330, 660);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                testStick, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the left stick flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.StickPosition> testSubscriber =
                widgetModel.getLeftStick().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(stickPosition -> stickPosition.getLeft() == 0
                && stickPosition.getTop() == 0
                && stickPosition.getRight() == 0
                && stickPosition.getBottom() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, stickPosition -> stickPosition.getLeft() == 50
                && stickPosition.getTop() == 100
                && stickPosition.getRight() == 0
                && stickPosition.getBottom() == 0);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getRightStick_rightBottom() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        Stick testStick = new Stick(330, -660);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                testStick, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the right stick flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.StickPosition> testSubscriber =
                widgetModel.getRightStick().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(stickPosition -> stickPosition.getLeft() == 0
                && stickPosition.getTop() == 0
                && stickPosition.getRight() == 0
                && stickPosition.getBottom() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, stickPosition -> stickPosition.getLeft() == 0
                && stickPosition.getTop() == 0
                && stickPosition.getRight() == 50
                && stickPosition.getBottom() == 100);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getLeftDial_left() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        int testDial = -330;
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                testDial, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the left dial flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.DialPosition> testSubscriber =
                widgetModel.getLeftDial().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, dialPosition -> dialPosition.getLeft() == 50
                && dialPosition.getRight() == 0);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getLeftDial_rightPro() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        ProfessionalRC.Event testEvent = new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.LW,
                ProfessionalRC.ProfessionalRCEventType.ROTATE,
                660,
                ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                ProfessionalRC.Event.MAX_VALUE_OF_DIAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC),
                testEvent, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the left dial flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.DialPosition> testSubscriber =
                widgetModel.getLeftDial().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 100);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getRightDial_right() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        HardwareState.RightDial rightDial = new HardwareState.RightDial(true, true, false, 330);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_DIAL),
                rightDial, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the right dial flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.DialPosition> testSubscriber =
                widgetModel.getRightDial().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 50);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getRightDial_leftPro() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        ProfessionalRC.Event testEvent = new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RW,
                ProfessionalRC.ProfessionalRCEventType.ROTATE,
                -660,
                ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                ProfessionalRC.Event.MAX_VALUE_OF_DIAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC),
                testEvent, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the right dial flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.DialPosition> testSubscriber =
                widgetModel.getRightDial().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(dialPosition -> dialPosition.getLeft() == 0
                && dialPosition.getRight() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, dialPosition -> dialPosition.getLeft() == 100
                && dialPosition.getRight() == 0);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getLeftLever_top() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        ProfessionalRC.Event testEvent = new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.LS,
                ProfessionalRC.ProfessionalRCEventType.ROTATE,
                -330,
                ProfessionalRC.Event.MIN_VALUE_OF_LEVER,
                ProfessionalRC.Event.MAX_VALUE_OF_LEVER);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC),
                testEvent, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the left lever flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.LeverPosition> testSubscriber =
                widgetModel.getLeftLever().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(leverPosition -> leverPosition.getTop() == 0
                && leverPosition.getBottom() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, leverPosition -> leverPosition.getTop() == 50
                && leverPosition.getBottom() == 0);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getRightLever_bottom() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        ProfessionalRC.Event testEvent = new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RS,
                ProfessionalRC.ProfessionalRCEventType.ROTATE,
                660,
                ProfessionalRC.Event.MIN_VALUE_OF_LEVER,
                ProfessionalRC.Event.MAX_VALUE_OF_LEVER);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC),
                testEvent, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the right lever flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.LeverPosition> testSubscriber =
                widgetModel.getRightLever().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(leverPosition -> leverPosition.getTop() == 0
                && leverPosition.getBottom() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, leverPosition -> leverPosition.getTop() == 0
                && leverPosition.getBottom() == 100);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getLeftStickCalibrationStatus_isRecordedInLimitsState() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_E_AXIS_STATUS),
                1, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_F_AXIS_STATUS),
                3, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_G_AXIS_STATUS),
                7, 30, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_H_AXIS_STATUS),
                15, 40, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.LIMITS, null);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Switch to next calibration state twice to get to LIMITS state
        widgetModel.nextCalibrationState().test();
        widgetModel.nextCalibrationState().test();

        // Initialize a test subscriber that subscribes to the left stick calibration state flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.StickCalibrationStatus> testSubscriber =
                widgetModel.getLeftStickCalibrationStatus().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 0
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 0
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 1
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 0
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 1
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 3
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(3, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 1
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 3
                && stickCalibrationStatus.getRightSegmentFillStatus() == 7
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(4, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 1
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 3
                && stickCalibrationStatus.getRightSegmentFillStatus() == 7
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 15);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_getRightStickCalibrationStatus_isRecordedInLimitsState() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_A_AXIS_STATUS),
                31, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_B_AXIS_STATUS),
                63, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_C_AXIS_STATUS),
                127, 30, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_D_AXIS_STATUS),
                255, 40, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.LIMITS, null);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Switch to next calibration state twice to get to LIMITS state
        widgetModel.nextCalibrationState().test();
        widgetModel.nextCalibrationState().test();

        // Initialize a test subscriber that subscribes to the right stick calibration state flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.StickCalibrationStatus> testSubscriber =
                widgetModel.getRightStickCalibrationStatus().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 0
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 0
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 31
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 0
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 31
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 63
                && stickCalibrationStatus.getRightSegmentFillStatus() == 0
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(3, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 31
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 63
                && stickCalibrationStatus.getRightSegmentFillStatus() == 127
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 0);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(4, stickCalibrationStatus -> stickCalibrationStatus.getTopSegmentFillStatus() == 31
                && stickCalibrationStatus.getBottomSegmentFillStatus() == 63
                && stickCalibrationStatus.getRightSegmentFillStatus() == 127
                && stickCalibrationStatus.getLeftSegmentFillStatus() == 255);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationState_success() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.LIMITS, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.QUIT, null);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the calibration state flowable from the model
        TestSubscriber<CalibrationState> testSubscriber = widgetModel.getCalibrationState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL);

        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL, CalibrationState.MIDDLE);

        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL, CalibrationState.MIDDLE,
                CalibrationState.LIMITS);

        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL, CalibrationState.MIDDLE,
                CalibrationState.LIMITS, CalibrationState.QUIT, CalibrationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationState_error() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, uxsdkError);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the calibration state flowable from the model
        TestSubscriber<CalibrationState> testSubscriber = widgetModel.getCalibrationState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL);

        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(CalibrationState.NORMAL, CalibrationState.MIDDLE,
                CalibrationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_nextCalibrationState_errorWhenConnectedToAircraft() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(true);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        observer.assertError(throwable -> ((Throwable) throwable).getMessage()
                .equals("Disconnect aircraft before calibrating remote controller"));

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_nextCalibrationState_errorWhenDisconnected() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(false);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        observer.assertError(throwable -> ((Throwable) throwable).getMessage()
                .equals("No remote controller detected"));

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationType_isMavic() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavicPro, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the calibration type flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.CalibrationType> testSubscriber =
                widgetModel.getCalibrationType().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(RemoteControllerCalibrationWidgetModel.CalibrationType.DEFAULT);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.CalibrationType.DEFAULT,
                RemoteControllerCalibrationWidgetModel.CalibrationType.MAVIC);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_mavicStep_all() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavicPro, 5, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.LIMITS, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.QUIT, null);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the mavic step flowable from the model
        TestSubscriber<RemoteControllerCalibrationWidgetModel.MavicStep> testSubscriber =
                widgetModel.getMavicStep().test();
        // Initialize a test subscriber that subscribes to the calibration state flowable from the model
        TestSubscriber<CalibrationState> calibrationStateTestSubscriber =
                widgetModel.getCalibrationState().test();

        // Switch to next calibration state twice to get to LIMITS state
        widgetModel.nextCalibrationState().test();
        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValue(RemoteControllerCalibrationWidgetModel.MavicStep.STICK);
        calibrationStateTestSubscriber.assertValues(CalibrationState.NORMAL,
                CalibrationState.MIDDLE, CalibrationState.LIMITS);

        // Confirm it switches to next mavic step instead of the next calibration state
        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.MavicStep.STICK,
                RemoteControllerCalibrationWidgetModel.MavicStep.DIAL);
        calibrationStateTestSubscriber.assertValues(CalibrationState.NORMAL,
                CalibrationState.MIDDLE, CalibrationState.LIMITS);

        // Confirm the next switch updates the calibration state and resets the mavic step
        widgetModel.nextCalibrationState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(RemoteControllerCalibrationWidgetModel.MavicStep.STICK,
                RemoteControllerCalibrationWidgetModel.MavicStep.DIAL,
                RemoteControllerCalibrationWidgetModel.MavicStep.STICK);
        calibrationStateTestSubscriber.assertValues(CalibrationState.NORMAL,
                CalibrationState.MIDDLE, CalibrationState.LIMITS, CalibrationState.QUIT,
                CalibrationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
        compositeDisposable.add(calibrationStateTestSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_hasRightDial_cendence() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameCendence, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        Assert.assertFalse(widgetModel.hasRightDial());

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        Assert.assertTrue(widgetModel.hasRightDial());

        widgetModel.cleanup();
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_hasRightDial_mavic2() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavic2, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        Assert.assertFalse(widgetModel.hasRightDial());

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        Assert.assertTrue(widgetModel.hasRightDial());

        widgetModel.cleanup();
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_hasLevers_cendence() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameCendence, 10, TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        Assert.assertFalse(widgetModel.hasLevers());

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        Assert.assertTrue(widgetModel.hasLevers());

        widgetModel.cleanup();
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_default() {
        List<Stick> stickData = Arrays.asList(new Stick(-660, 660), new Stick(660, -660));
        List<Integer> leftDialData = Arrays.asList(-660, 660);

        setEmptyValues();
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                3,
                3,
                TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Check progress finished
        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_mavic2Incomplete() {
        List<Stick> stickData = Arrays.asList(new Stick(-660, 660), new Stick(660, -660));
        List<Integer> leftDialData = Arrays.asList(-660, 660);

        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavic2, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                3,
                3,
                TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Check progress finished
        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> !isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_mavic2() {
        List<Stick> stickData = Arrays.asList(new Stick(-660, 660), new Stick(660, -660));
        List<Integer> leftDialData = Arrays.asList(-660, 660);
        List<HardwareState.RightDial> rightDialData = Arrays.asList(
                new HardwareState.RightDial(true, true, false, 660),
                new HardwareState.RightDial(true, true, false, -660));

        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavic2, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_DIAL),
                rightDialData,
                3,
                3,
                TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Check progress finished
        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_cendenceIncomplete() {
        List<Stick> stickData = Arrays.asList(new Stick(-660, 660), new Stick(660, -660));
        List<Integer> leftDialData = Arrays.asList(-660, 660);
        List<HardwareState.RightDial> rightDialData = Arrays.asList(
                new HardwareState.RightDial(true, true, false, 660),
                new HardwareState.RightDial(true, true, false, -660));

        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameCendence, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_DIAL),
                rightDialData,
                3,
                3,
                TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Check progress finished
        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> !isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_cendence() {
        List<Stick> stickData = Arrays.asList(new Stick(-660, 660), new Stick(660, -660));
        List<Integer> leftDialData = Arrays.asList(-660, 660);
        List<ProfessionalRC.Event> buttonData = Arrays.asList(
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RW,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL),
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RW,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        -660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL),
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.LS,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL),
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.LS,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        -660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL),
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RS,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        -660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL),
                new ProfessionalRC.Event(ProfessionalRC.CustomizableButton.RS,
                        ProfessionalRC.ProfessionalRCEventType.ROTATE,
                        660,
                        ProfessionalRC.Event.MIN_VALUE_OF_DIAL,
                        ProfessionalRC.Event.MAX_VALUE_OF_DIAL));

        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameCendence, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE),
                stickData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                3,
                3,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC),
                buttonData,
                3,
                3,
                TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Check progress finished
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerCalibrationWidgetModel_calibrationProgress_mavicPro() {
        List<Integer> leftDialData = Arrays.asList(-660, 660);

        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME),
                RemoteController.DisplayNameMavicPro, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_E_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 4, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_F_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_G_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 7, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_H_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 9, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_A_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 11, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_B_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 13, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_C_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 15, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_D_AXIS_STATUS),
                CalibrationSquareView.MAX_CALIBRATION_STATUS, 17, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL),
                leftDialData,
                20,
                5,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.MIDDLE, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE),
                CalibrationState.LIMITS, null);

        // Mock methods to avoid initializing DJISDKManager
        Mockito.when(testProduct.isConnected()).thenReturn(false);
        Mockito.when(((Aircraft) testProduct).getRemoteController().isConnected()).thenReturn(true);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber = widgetModel.getCalibrationProgress().test();

        // Check default value
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        // Switch to next calibration state twice to get to LIMITS state
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        widgetModel.nextCalibrationState().test();
        widgetModel.nextCalibrationState().test();

        // Stick progress finished
        testScheduler.advanceTimeBy(16, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> isComplete);

        // Switch to next mavic step
        widgetModel.nextCalibrationState().test();

        // Dial progress unfinished
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> !isComplete);

        // Dial progress finished
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1, isComplete -> isComplete);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.RIGHT_DIAL));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_A_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_B_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_C_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_D_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_E_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_F_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_G_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_H_AXIS_STATUS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE));

    }
}
