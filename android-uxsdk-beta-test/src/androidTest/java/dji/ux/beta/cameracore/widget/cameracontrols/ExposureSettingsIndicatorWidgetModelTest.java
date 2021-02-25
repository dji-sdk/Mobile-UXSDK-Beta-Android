///*
// * Copyright (c) 2018-2020 DJI
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// *
// */
//
//package dji.ux.beta.cameracore.widget.cameracontrols;
//
//import androidx.test.filters.SmallTest;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.concurrent.TimeUnit;
//
//import dji.common.camera.SettingsDefinitions.ExposureMode;
//import dji.keysdk.CameraKey;
//import dji.keysdk.ProductKey;
//import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
//import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
//import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
//import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
//import dji.ux.beta.WidgetTestUtil;
//import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsIndicatorWidgetModel;
//import dji.ux.beta.core.base.DJISDKModel;
//import dji.ux.beta.core.base.SchedulerProvider;
//import dji.ux.beta.core.base.TestSchedulerProvider;
//import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
//
///**
// * Class tests public methods in {@link CameraSettingsIndicatorWidgetModel}
// * 1.{@link ExposureSettingsIndicatorWidgetModelTest#exposureSettingsIndicatorWidgetModel_getExposureMode_updateOrder()}
// * Test the exposure mode update order
// */
//@RunWith(AndroidJUnit4.class)
//@SmallTest
//public class ExposureSettingsIndicatorWidgetModelTest {
//    private CompositeDisposable compositeDisposable;
//    @Mock
//    private DJISDKModel djiSdkModel;
//    private CameraSettingsIndicatorWidgetModel widgetModel;
//    private TestScheduler testScheduler;
//    @Mock
//    private ObservableInMemoryKeyedStore keyedStore;
//
//    @Before
//    public void beforeTest() {
//        MockitoAnnotations.initMocks(this);
//        RxJavaPlugins.reset();
//        compositeDisposable = new CompositeDisposable();
//
//        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
//        testScheduler = testSchedulerProvider.getTestScheduler();
//        SchedulerProvider.setScheduler(testSchedulerProvider);
//        widgetModel = new CameraSettingsIndicatorWidgetModel(djiSdkModel, keyedStore);
//        WidgetTestUtil.initialize(djiSdkModel);
//
//    }
//
//    @Test
//    public void exposureSettingsIndicatorWidgetModel_getExposureMode_updateOrder() {
//        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
//                ProductKey.create(ProductKey.CONNECTION),
//                true, 10, TimeUnit.SECONDS);
//
//        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
//                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
//                ExposureMode.MANUAL, 15, TimeUnit.SECONDS);
//        widgetModel.setup();
//
//        TestSubscriber<ExposureMode> testSubscriber =
//                widgetModel.getExposureMode().test();
//
//        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
//        testSubscriber.assertValue(ExposureMode.UNKNOWN);
//        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
//        testSubscriber.assertValues(ExposureMode.UNKNOWN, ExposureMode.MANUAL);
//
//        widgetModel.cleanup();
//        compositeDisposable.add(testSubscriber);
//    }
//
//    @After
//    public void afterTest() {
//        RxJavaPlugins.reset();
//        compositeDisposable.dispose();
//    }
//}
