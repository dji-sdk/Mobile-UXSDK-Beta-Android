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
package dji.ux.beta.cameracore.widget.focusexposureswitch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidgetModel.FocusExposureSwitchState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.util.SettingDefinitions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [FocusExposureSwitchWidgetModel]
 * 1. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_focusExposureSwitchState_cameraDisconnected]
 * Test the update to not supported state when camera is disconnected
 * 2. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_focusExposureSwitchState_notSupported]
 * Test the update to not supported state when focus mode change is not supported
 * 3. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_focusExposureSwitchState_controlModeMetering]
 * Test the update to control mode metering
 * 4. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_focusExposureSwitchState_controlModeAutoFocus]
 * Test the update to control mode auto focus
 * 5. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_switchControlMode_success]
 * Test if the switch of the control mode is successful
 * 6. [FocusExposureSwitchWidgetModelTest.focusExposureSwitchWidgetModel_switchControlMode_error]
 * Test if the switch of the control mode fails
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FocusExposureSwitchWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: FocusExposureSwitchWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.controlMode).thenReturn(SettingDefinitions.ControlMode.SPOT_METER)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = FocusExposureSwitchWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun focusExposureSwitchWidgetModel_focusExposureSwitchState_cameraDisconnected() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                false,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.CENTER_METER,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusExposureSwitchState> = widgetModel.focusExposureSwitchState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusExposureSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.CameraDisconnected)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusExposureSwitchWidgetModel_focusExposureSwitchState_notSupported() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(false)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.CENTER_METER,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusExposureSwitchState> = widgetModel.focusExposureSwitchState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusExposureSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.NotSupported)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.NotSupported,
                FocusExposureSwitchState.NotSupported)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusExposureSwitchWidgetModel_focusExposureSwitchState_controlModeMetering() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.SPOT_METER,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusExposureSwitchState> = widgetModel.focusExposureSwitchState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusExposureSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.ControlModeState(SettingDefinitions.ControlMode.SPOT_METER),
                FocusExposureSwitchState.ControlModeState(SettingDefinitions.ControlMode.SPOT_METER))

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusExposureSwitchWidgetModel_focusExposureSwitchState_controlModeAutoFocus() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusExposureSwitchState> = widgetModel.focusExposureSwitchState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusExposureSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusExposureSwitchState.ProductDisconnected,
                FocusExposureSwitchState.CameraDisconnected,
                FocusExposureSwitchState.ControlModeState(SettingDefinitions.ControlMode.SPOT_METER),
                FocusExposureSwitchState.ControlModeState(SettingDefinitions.ControlMode.AUTO_FOCUS))

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusExposureSwitchWidgetModel_switchControlMode_success() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE, 0),
                SettingsDefinitions.FocusMode.AFC,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.switchControlMode().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun focusExposureSwitchWidgetModel_switchControlMode_error() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE, 0),
                SettingsDefinitions.FocusMode.AFC,
                5,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.switchControlMode().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.FOCUS_MODE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.METERING_MODE, 0))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }
}