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

package dji.ux.beta.core.panel.listitem.rcstickmode

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.error.DJIError
import dji.common.remotecontroller.AircraftMappingStyle
import dji.keysdk.ProductKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.Single
import dji.thirdparty.io.reactivex.SingleEmitter
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.rcstickmode.RCStickModeListItemWidgetModel.RCStickModeState.*
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
 * This class tests the public methods in [RCStickModeListItemWidgetModel]
 * 1.[RCStickModeListItemWidgetModelTest.rcStickModeListItemWidgetModel_rcStickMode_update]
 * Test if the RC stick mode is updated every time product is connected
 * 2. [RCStickModeListItemWidgetModelTest.rcStickModeListItemWidgetModel_setRCStickMode_success]
 * Test if setting the RC stick mode is successful
 * 3. [RCStickModeListItemWidgetModelTest.rcStickModeListItemWidgetModel_setRCStickMode_fail]
 * Test if setting the RC stick mode fails
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RCStickModeListItemWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: RCStickModeListItemWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = RCStickModeListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun rcStickModeListItemWidgetModel_rcStickMode_update() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE))
        val single1 = Single.create { emitter: SingleEmitter<Any?> ->
            emitter.onSuccess(AircraftMappingStyle.STYLE_1)
        }
        val single2 = Single.create { emitter: SingleEmitter<Any?> ->
            emitter.onSuccess(AircraftMappingStyle.STYLE_2)
        }
        val single3 = Single.create { emitter: SingleEmitter<Any?> ->
            emitter.onSuccess(AircraftMappingStyle.STYLE_3)
        }
        val singleCustom = Single.create { emitter: SingleEmitter<Any?> ->
            emitter.onSuccess(AircraftMappingStyle.STYLE_CUSTOM)
        }
        val singleUnknown = Single.create { emitter: SingleEmitter<Any?> ->
            emitter.onSuccess(AircraftMappingStyle.UNKNOWN)
        }
        val prodConnectionData = listOf(false, true, false, true,
                false, true, false, true, false, true, false)
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                prodConnectionData,
                5,
                5,
                TimeUnit.SECONDS)

        Mockito.`when`(djiSdkModel
                .getValue(RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE)))
                .thenReturn(single1)
                .thenReturn(single2)
                .thenReturn(single3)
                .thenReturn(singleCustom)
                .thenReturn(singleUnknown)
        widgetModel.setup()

        val testSubscriber = widgetModel.rcStickModeState.test()
        testScheduler.advanceTimeBy(60, TimeUnit.SECONDS)
        testSubscriber.assertValues(
                //Initial State
                ProductDisconnected,
                //Product Disconnected
                ProductDisconnected,
                //Product Connected get current value
                Mode1,
                //Product Disconnected
                ProductDisconnected,
                //Product Connected get current value
                Mode2,
                //Product Disconnected
                ProductDisconnected,
                //Product Connected get current value
                Mode3,
                //Product Disconnected
                ProductDisconnected,
                //Product Connected get current value
                Custom,
                //Product Disconnected
                ProductDisconnected,
                //Aircraft Mapping Style Unknown
                ProductDisconnected,
                //Product Disconnected
                ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rcStickModeListItemWidgetModel_setRCStickMode_success() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE))
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE),
                AircraftMappingStyle.STYLE_1,
                null)

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE),
                AircraftMappingStyle.STYLE_2,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setControlStickMode(Mode2).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun rcStickModeListItemWidgetModel_setRCStickMode_fail() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE))
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE),
                AircraftMappingStyle.STYLE_1,
                null)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.AIRCRAFT_MAPPING_STYLE),
                AircraftMappingStyle.STYLE_2,
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setControlStickMode(Mode2).test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }
}