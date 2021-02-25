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

package dji.ux.beta.cameracore.widget.fpvinteraction;

import android.graphics.Point;
import android.graphics.PointF;

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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.DJIParamMinMaxCapability;
import dji.keysdk.CameraKey;
import dji.keysdk.GimbalKey;
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
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Test:
 * This class tests the public methods in the {@link FPVInteractionWidgetModel}
 * 1.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_controlMode_isUpdated()}
 * Test that the control mode is updated.
 * 2.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_aeLocked_isUpdated()}
 * Test that the AE locked state is updated.
 * 3.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_setControlMode_success()}
 * Test that the control mode is set successfully.
 * 4.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_setControlMode_throwsError()}
 * Test that setting the control mode throws an error.
 * 5.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateFocusTarget_success()}
 * Test that the focus target is updated successfully.
 * 6.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateFocusTarget_throwsError()}
 * Test that updating the focus target throws an error.
 * 7.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMetering_success()}
 * Test that the metering is updated successfully.
 * 8.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMetering_throwsErrorSettingMeteringMode()}
 * Test that updating the metering throws an error while setting the metering mode.
 * 9.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMetering_throwsErrorSettingSpotMeteringTarget()}
 * Test that updating the metering throws an error while setting the spot meter target.
 * 10.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMeteringFromSpotMeteringMode_success()}
 * Test that the metering is updates successfully when starting in spot metering mode.
 * 11.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMeteringFromSpotMeteringMode_throwsError()}
 * Test that updating the metering throws an error when starting in spot metering mode.
 * 12.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMetering_centerSuccess()}
 * Test that the metering is updated successfully in center control mode.
 * 13.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_updateMetering_centerThrowsError()}
 * Test that updating the metering throws an error when in center control mode.
 * 14.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_canRotateGimbalYaw_true()}
 * Test that the gimbal can rotate in the yaw rotation.
 * 15.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_rotateGimbalBySpeed_success()}
 * Test that the gimbal is rotated successfully.
 * 16.
 * {@link FPVInteractionWidgetModelTest#fpvInteractionWidgetModel_rotateGimbalBySpeed_throwsError()}
 * Test that rotating the gimbal throws an error.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FPVInteractionWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private GlobalPreferencesInterface preferencesManager;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private FPVInteractionWidgetModel widgetModel;
    private TestScheduler testScheduler;

    private PointF fakePointF = new FakePointF(0.5f, 0.5f);
    private Point fakePoint = new FakePoint(6, 4);

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        Mockito.when(preferencesManager.getControlMode()).thenReturn(SettingDefinitions.ControlMode.SPOT_METER);
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = Mockito.spy(new FPVInteractionWidgetModel(djiSdkModel, keyedStore, preferencesManager));
        Mockito.doReturn(fakePointF).when(widgetModel).createPointF(0.5f, 0.5f);
        Mockito.doReturn(fakePoint).when(widgetModel).createPoint(6, 4);

        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void fpvInteractionWidgetModel_controlMode_isUpdated() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the control mode flowable from the model
        TestSubscriber<SettingDefinitions.ControlMode> testSubscriber = widgetModel.getControlMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingDefinitions.ControlMode.SPOT_METER);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingDefinitions.ControlMode.SPOT_METER, SettingDefinitions.ControlMode.MANUAL_FOCUS);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void fpvInteractionWidgetModel_aeLocked_isUpdated() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK, 0),
                true,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ae locked flowable from the model
        TestSubscriber<Boolean> testSubscriber = widgetModel.isAeLocked().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void fpvInteractionWidgetModel_setControlMode_success() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the set control mode method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setControlMode(SettingDefinitions.ControlMode.MANUAL_FOCUS).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_setControlMode_throwsError() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the set control mode method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setControlMode(SettingDefinitions.ControlMode.MANUAL_FOCUS).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateFocusTarget_success() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_TARGET, 0),
                fakePointF, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update focus target method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateFocusTarget(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateFocusTarget_throwsError() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_TARGET, 0),
                fakePointF, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update focus target method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateFocusTarget(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMetering_success() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.SPOT_METERING_TARGET, 0),
                fakePoint, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.SPOT, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMetering_throwsErrorSettingMeteringMode() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                5,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.SPOT_METERING_TARGET, 0),
                fakePoint, null);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.SPOT, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMetering_throwsErrorSettingSpotMeteringTarget() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                5,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.SPOT_METERING_TARGET, 0),
                fakePoint, uxsdkError);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.SPOT, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMeteringFromSpotMeteringMode_success() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.SPOT,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.SPOT_METERING_TARGET, 0),
                fakePoint, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMeteringFromSpotMeteringMode_throwsError() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.SPOT,
                5,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.SPOT_METERING_TARGET, 0),
                fakePoint, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0.5f, 0.5f).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMetering_centerSuccess() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.CENTER_METER,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.CENTER, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0, 0).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_updateMetering_centerThrowsError() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.CENTER_METER,
                5,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.METERING_MODE, 0),
                SettingsDefinitions.MeteringMode.CENTER, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.updateMetering(0, 0).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_canRotateGimbalYaw_true() {
        setEmptyValues();
        Map<CapabilityKey, DJIParamMinMaxCapability> capabilitiesMap = new HashMap<>();
        capabilitiesMap.put(CapabilityKey.ADJUST_YAW, new DJIParamMinMaxCapability(true, 0, 0));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.CAPABILITIES, 0),
                capabilitiesMap,
                10,
                TimeUnit.SECONDS);
        Mockito.doReturn(false).when(widgetModel).isPhantom4Series();

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        Assert.assertFalse(widgetModel.canRotateGimbalYaw());
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        Assert.assertTrue(widgetModel.canRotateGimbalYaw());
        widgetModel.cleanup();

        widgetModel.cleanup();
    }

    @Test
    public void fpvInteractionWidgetModel_rotateGimbalBySpeed_success() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        Rotation r = new Rotation.Builder().mode(RotationMode.SPEED).yaw(Rotation.NO_ROTATION).pitch(-1).build();
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                GimbalKey.create(GimbalKey.ROTATE, 0),
                r, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        TestObserver observer = widgetModel.rotateGimbalBySpeed(0, -1).test();
        testScheduler.triggerActions();
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void fpvInteractionWidgetModel_rotateGimbalBySpeed_throwsError() {
        setEmptyValues();
        // Use util method to set emitted value after given delay for given key
        Rotation r = new Rotation.Builder().mode(RotationMode.SPEED).yaw(Rotation.NO_ROTATION).pitch(-1).build();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                GimbalKey.create(GimbalKey.ROTATE, 0),
                r, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the update metering method
        TestObserver observer = widgetModel.rotateGimbalBySpeed(0, -1).test();
        testScheduler.triggerActions();
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.METERING_MODE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.AE_LOCK, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, GimbalKey.create(GimbalKey.CAPABILITIES, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
    }

    public static class FakePointF extends PointF {
        FakePointF(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class FakePoint extends Point {
        FakePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
