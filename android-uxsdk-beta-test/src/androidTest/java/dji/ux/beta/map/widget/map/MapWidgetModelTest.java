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

package dji.ux.beta.map.widget.map;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.model.LocationCoordinate2D;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.GimbalKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Test : This class tests the public methods in {@link MapWidgetModel}
 * <p>
 * 1. {@link MapWidgetModelTest#mapWidgetModel_getAircraftLocationTest_updateInOder()}
 * Test the aircraft location updated. Check initial value and updated value
 * <p>
 * 2. {@link MapWidgetModelTest#mapWidgetModel_getHomeLocationTest_latitudeUpdateInOder()}
 * Test the home location update when latitude is updated. Check initial value and updated value
 * <p>
 * 3. {@link MapWidgetModelTest#mapWidgetModel_getHomeLocationTest_longitudeUpdateInOder()}
 * Test the home location update when longitude is updated. Check initial value and updated value
 * <p>
 * 4. {@link MapWidgetModelTest#mapWidgetModel_getAircraftHeading_updateInOrder()}
 * Test the aircraft heading update. Check initial and updated value
 * <p>
 * 5. {@link MapWidgetModelTest#mapWidgetModel_getGimbalHeading_updateInOrder()}
 * Test the gimbal heading update. Check initial and updated value
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MapWidgetModelTest {

    private static final Double INVALID_COORDINATE = 181.0;
    private static final Double UPDATE_COORDINATE = 24.22;
    private static LocationCoordinate3D locationCoordinate3D = new LocationCoordinate3D(37.4238187, -122.1436487, -1);
    private static LocationCoordinate3D default_locationCoordinate3D = new LocationCoordinate3D(INVALID_COORDINATE, INVALID_COORDINATE, -1.0f);
    private static LocationCoordinate2D default_locationCoordinate2D = new LocationCoordinate2D(INVALID_COORDINATE, INVALID_COORDINATE);
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    private TestScheduler testScheduler;
    private CompositeDisposable compositeDisposable;
    private MapWidgetModel widgetModel;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();

        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new MapWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel);


    }


    @Test
    public void mapWidgetModel_getAircraftLocationTest_updateInOder() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION),
                locationCoordinate3D, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<LocationCoordinate3D> testSubscriber =
                widgetModel.getAircraftLocation().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(default_locationCoordinate3D);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(default_locationCoordinate3D, locationCoordinate3D);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void mapWidgetModel_getHomeLocationTest_latitudeUpdateInOder() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                UPDATE_COORDINATE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<LocationCoordinate2D> testSubscriber =
                widgetModel.getHomeLocation().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(default_locationCoordinate2D);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        LocationCoordinate2D locationCoordinate2D = new LocationCoordinate2D(UPDATE_COORDINATE, INVALID_COORDINATE);
        testSubscriber.assertValues(default_locationCoordinate2D, locationCoordinate2D);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void mapWidgetModel_getHomeLocationTest_longitudeUpdateInOder() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                UPDATE_COORDINATE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<LocationCoordinate2D> testSubscriber =
                widgetModel.getHomeLocation().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(default_locationCoordinate2D);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        LocationCoordinate2D locationCoordinate2D = new LocationCoordinate2D(INVALID_COORDINATE, UPDATE_COORDINATE);
        testSubscriber.assertValues(default_locationCoordinate2D, locationCoordinate2D);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void mapWidgetModel_getAircraftHeading_updateInOrder() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING),
                5.0f, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Float> testSubscriber =
                widgetModel.getAircraftHeading().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0.0f);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0.0f, 5.0f);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void mapWidgetModel_getGimbalHeading_updateInOrder() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING));
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE),
                5.0f, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Float> testSubscriber =
                widgetModel.getGimbalHeading().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0.0f);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0.0f, 5.0f);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

}
