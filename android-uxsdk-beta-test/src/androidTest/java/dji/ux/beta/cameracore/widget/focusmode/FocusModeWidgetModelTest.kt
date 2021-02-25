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

package dji.ux.beta.cameracore.widget.focusmode

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
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidgetModel.FocusModeState
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
 * This class tests the public methods in [FocusModeWidgetModel]
 * 1. [FocusModeWidgetModelTest.focusModeWidgetModel_focusModeState_cameraDisconnected]
 * Test if camera is disconnected.
 * 2. [FocusModeWidgetModelTest.focusModeWidgetModel_focusModeState_notSupported]
 * Test focus mode state not supported.
 * 3. [FocusModeWidgetModelTest.focusModeWidgetModel_focusModeState_manualFocus]}
 * Test focus mode state is manual focus with AFC enabled and disabled
 * 4. [FocusModeWidgetModelTest.focusModeWidgetModel_focusModeState_autoFocus]}
 * Test focus mode state is auto focus with AFC enabled and disabled
 * 4. [FocusModeWidgetModelTest.focusModeWidgetModel_toggleFocusMode_success]
 * Test if the focus mode is changed successfully
 * 5. [FocusModeWidgetModelTest.focusModeWidgetModel_toggleFocusMode_error]}
 * Test if changing the focus mode fails
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FocusModeWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: FocusModeWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.controlMode).thenReturn(SettingDefinitions.ControlMode.SPOT_METER)
        Mockito.`when`(preferencesManager.afcEnabled).thenReturn(false)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = FocusModeWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun focusModeWidgetModel_focusModeState_cameraDisconnected() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
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
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.AUTO,
                18,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusModeState> = widgetModel.focusModeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusModeState.ProductDisconnected)
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.AutoFocus(isAFCEnabled = false))

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusModeWidgetModel_focusModeState_notSupported() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(false)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
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
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.AUTO,
                18,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusModeState> = widgetModel.focusModeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusModeState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.NotSupported)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusModeWidgetModel_focusModeState_manualFocus() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
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
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.MANUAL,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED, 0),
                true,
                40,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                true,
                40,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusModeState> = widgetModel.focusModeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusModeState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported)
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.ManualFocus(false))
        testScheduler.advanceTimeBy(45, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.ManualFocus(false),
                FocusModeState.ManualFocus(false),
                FocusModeState.ManualFocus(true))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusModeWidgetModel_focusModeState_autoFocus() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
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
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.AUTO,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED, 0),
                true,
                40,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                true,
                40,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber: TestSubscriber<FocusModeState> = widgetModel.focusModeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(FocusModeState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported)
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.AutoFocus(false))
        testScheduler.advanceTimeBy(45, TimeUnit.SECONDS)
        testSubscriber.assertValues(FocusModeState.ProductDisconnected,
                FocusModeState.CameraDisconnected,
                FocusModeState.NotSupported,
                FocusModeState.AutoFocus(false),
                FocusModeState.AutoFocus(false),
                FocusModeState.AutoFocus(true))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun focusModeWidgetModel_toggleFocusMode_success() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.AUTO,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 2, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                false,
                2,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, key,
                SettingsDefinitions.FocusMode.MANUAL, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleFocusMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun focusModeWidgetModel_toggleFocusMode_error() {
        setEmptyValues()
        val key = CameraKey.create(CameraKey.FOCUS_MODE, 0)
        Mockito.`when`(djiSdkModel.isKeySupported(key)).thenReturn(true)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel, key, SettingsDefinitions.FocusMode.AUTO, null)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED), false, null)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                key,
                SettingsDefinitions.FocusMode.AUTO,
                1,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 2, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                false,
                2,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS,
                4,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, key,
                SettingsDefinitions.FocusMode.MANUAL, uxsdkError)
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleFocusMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_AFC_SUPPORTED, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.FOCUS_MODE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.createLensKey(CameraKey.FOCUS_MODE, 0, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.createLensKey(CameraKey.IS_AFC_SUPPORTED, 0, 0))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED))

    }
}