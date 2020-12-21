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
package dji.ux.beta.core.widget.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.remotecontroller.GPSData
import dji.common.remotecontroller.GPSData.GPSLocation
import dji.keysdk.FlightControllerKey
import dji.keysdk.GimbalKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.MobileGPSLocationUtil
import dji.ux.beta.core.widget.compass.CompassWidgetModel.*
import io.mockk.every
import io.mockk.mockkClass
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [CompassWidgetModel] class.
 *
 * 1. - 2.
 * [CompassWidgetModelTest.compassWidgetModel_centerType_isUpdatedByRCKeyValueUpdate]
 * [CompassWidgetModelTest.compassWidgetModel_centerType_isUpdatedByDeviceLocationUpdate]
 * Test the initial value returned by the center type flowable is as expected and it is updated with the
 * test value from both the RC GPS data and mobile device data as expected.
 *
 * 3. - 4.
 * [CompassWidgetModelTest.compassWidgetModel_mobileDeviceAzimuthAtRotation0_isUpdatedBySensorUpdate]
 * [CompassWidgetModelTest.compassWidgetModel_mobileDeviceAzimuthAtRotation270_isUpdatedBySensorUpdate]
 * Test the initial value returned by the mobile device azimuth flowable is as expected and it is updated correctly
 * by the sensor updates for the given mobile device rotation values.
 *
 * 5. - 6.
 * [CompassWidgetModelTest.compassWidgetModel_aircraftStateWithCenterHome_isUpdated]
 * [CompassWidgetModelTest.compassWidgetModel_aircraftStateWithCenterRCOrMobile_isUpdated]
 * Test the initial value returned by the aircraft state flowable is as expected and it is updated correctly with
 * the test value when the center type is home as well as when the center type is RC or mobile device type.
 *
 * 7. - 8.
 * [CompassWidgetModelTest.compassWidgetModel_currentLocationState_isUpdatedByRCLocationUpdate]
 * [CompassWidgetModelTest.compassWidgetModel_currentLocationState_isUpdatedByDeviceLocationUpdate]
 * Test the initial value returned by the current location state flowable is as expected and it is updated correctly
 * with the test value with an RC location update as well as a mobile device location update.
 *
 * 9.
 * [CompassWidgetModelTest.compassWidgetModel_aircraftAttitude_isUpdated]
 * Test that the initial value of the aircraft attitude flowable is as expected and it is updated with the latest
 * value whenever a roll, pitch or yaw value is received.
 *
 * 10.
 * [CompassWidgetModelTest.compassWidgetModel_gimbalHeading_isUpdated]
 * Test that the initial value of the gimbal heading flowable is as expected and it is updated correctly when the
 * new test value is received.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CompassWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var widgetModel: CompassWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var windowManager: WindowManager

    @Mock
    private lateinit var mobileGPSLocationUtil: MobileGPSLocationUtil

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        djiSdkModel = Mockito.mock(DJISDKModel::class.java)
        val keyedStore = Mockito.mock(ObservableInMemoryKeyedStore::class.java)
        val sensorManager = Mockito.mock(SensorManager::class.java)
        windowManager = Mockito.mock(WindowManager::class.java)
        widgetModel = CompassWidgetModel(djiSdkModel, keyedStore, sensorManager, windowManager)
        widgetModel.mobileGPSLocationUtil = mobileGPSLocationUtil
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun compassWidgetModel_centerType_isUpdatedByRCKeyValueUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA),
                GPSData.Builder().location(GPSLocation(-122.134860, 37.421401))
                        .isValid(true)
                        .build(),
                10,
                TimeUnit.SECONDS)

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.centerType == CenterType.HOME_GPS }
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.centerType == CenterType.RC_MOBILE_GPS }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_centerType_isUpdatedByDeviceLocationUpdate() {
        setEmptyValuesToAllKeys()
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.centerType == CenterType.HOME_GPS }
        val testLocation = Location("MOCK_GPS_PROVIDER")
        testLocation.latitude = 37.421401
        testLocation.longitude = -122.134860
        widgetModel.onLocationChanged(testLocation)
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.centerType == CenterType.RC_MOBILE_GPS }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_mobileDeviceAzimuthAtRotation0_isUpdatedBySensorUpdate() {
        val testMobileDeviceAzimuthValue = 112.003555f
        setEmptyValuesToAllKeys()
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.phoneAzimuth == 0.0f }
        val testRotationVector = floatArrayOf(0.014476884f, 0.008707724f, -0.19067287f)
        val testSensorEvent = mockSensorEventAndWindowManager(testRotationVector, Surface.ROTATION_0)
        widgetModel.onSensorChanged(testSensorEvent)
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.phoneAzimuth == testMobileDeviceAzimuthValue }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_mobileDeviceAzimuthAtRotation270_isUpdatedBySensorUpdate() {
        val testMobileDeviceAzimuthValue = 292.003555f
        setEmptyValuesToAllKeys()
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.phoneAzimuth == 0.0f }
        val testRotationVector = floatArrayOf(0.014476884f, 0.008707724f, -0.19067287f)
        val testSensorEvent = mockSensorEventAndWindowManager(testRotationVector, Surface.ROTATION_270)
        widgetModel.onSensorChanged(testSensorEvent)
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.phoneAzimuth == testMobileDeviceAzimuthValue }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_aircraftStateWithCenterHome_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE),
                37.421791,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE),
                -122.137648,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                37.421401,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -122.134860,
                10,
                TimeUnit.SECONDS)

        // Use util method to set empty values to all other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        val defaultAircraftState = AircraftState(0.0f, 0.0f)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Aircraft State Default Angle",
                    it.aircraftState.angle,
                    defaultAircraftState.angle,
                    DELTA)
            Assert.assertEquals("Aircraft State Default Distance",
                    it.aircraftState.distance,
                    defaultAircraftState.distance,
                    DELTA)
            (it.aircraftState.angle == defaultAircraftState.angle
                    && it.aircraftState.distance == defaultAircraftState.distance
                    && it.centerType == CenterType.HOME_GPS)
        }
        val testAircraftState = AircraftState(170.05202f, 250.55476f)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Aircraft State Test Angle",
                    it.aircraftState.angle,
                    testAircraftState.angle,
                    DELTA)
            Assert.assertEquals("Aircraft State Test Distance",
                    it.aircraftState.distance,
                    testAircraftState.distance,
                    DELTA)
            (it.aircraftState.angle == testAircraftState.angle
                    && it.aircraftState.distance == testAircraftState.distance
                    && it.centerType == CenterType.HOME_GPS)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_aircraftStateWithCenterRCOrMobile_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE),
                37.421791,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE),
                -122.137648,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA),
                GPSData.Builder().location(GPSLocation(-122.134860, 37.421401))
                        .isValid(true)
                        .build(),
                10,
                TimeUnit.SECONDS)

        // Use util method to set empty values to all other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        val defaultAircraftState = AircraftState(0.0f, 0.0f)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Aircraft State Default Angle",
                    it.aircraftState.angle,
                    defaultAircraftState.angle,
                    DELTA)
            Assert.assertEquals("Aircraft State Default Distance",
                    it.aircraftState.distance,
                    defaultAircraftState.distance,
                    DELTA)
            (it.aircraftState.angle == defaultAircraftState.angle
                    && it.aircraftState.distance == defaultAircraftState.distance
                    && it.centerType == CenterType.HOME_GPS)
        }
        val testAircraftState = AircraftState(170.05202f, 250.55476f)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Aircraft State Test Angle",
                    it.aircraftState.angle,
                    testAircraftState.angle,
                    DELTA)
            Assert.assertEquals("Aircraft State Test Distance",
                    it.aircraftState.distance,
                    testAircraftState.distance,
                    DELTA)
            (it.aircraftState.angle == testAircraftState.angle
                    && it.aircraftState.distance == testAircraftState.distance
                    && it.centerType == CenterType.RC_MOBILE_GPS)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_currentLocationState_isUpdatedByRCLocationUpdate() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA),
                GPSData.Builder().location(GPSLocation(-122.137648, 37.421791))
                        .isValid(true)
                        .build(),
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                37.421401,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -122.134860,
                12,
                TimeUnit.SECONDS)

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        val defaultCurrentLocationState = CurrentLocationState(0.0f, 0.0f)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Current Location State Default Angle",
                    it.currentLocationState.angle,
                    defaultCurrentLocationState.angle,
                    DELTA)
            Assert.assertEquals("Current Location State Default Distance",
                    it.currentLocationState.distance,
                    defaultCurrentLocationState.distance,
                    DELTA)
            (it.currentLocationState.angle == defaultCurrentLocationState.angle
                    && it.currentLocationState.distance == defaultCurrentLocationState.distance
                    && it.centerType == CenterType.HOME_GPS)
        }
        val testCurrentLocationState = CurrentLocationState(350.052f, 250.55476f)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Current Location State Test Angle",
                    it.currentLocationState.angle,
                    testCurrentLocationState.angle,
                    DELTA)
            Assert.assertEquals("Current Location State Test Distance",
                    it.currentLocationState.distance,
                    testCurrentLocationState.distance,
                    DELTA)
            (it.currentLocationState.angle == testCurrentLocationState.angle
                    && it.currentLocationState.distance == testCurrentLocationState.distance
                    && it.centerType == CenterType.RC_MOBILE_GPS)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_currentLocationState_isUpdatedByDeviceLocationUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                37.421401,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -122.134860,
                10,
                TimeUnit.SECONDS)

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        val defaultCurrentLocationState = CurrentLocationState(0.0f, 0.0f)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Current Location State Default Angle",
                    it.currentLocationState.angle,
                    defaultCurrentLocationState.angle,
                    DELTA)
            Assert.assertEquals("Current Location State Default Distance",
                    it.currentLocationState.distance,
                    defaultCurrentLocationState.distance,
                    DELTA)
            (it.currentLocationState.angle == defaultCurrentLocationState.angle
                    && it.currentLocationState.distance == defaultCurrentLocationState.distance
                    && it.centerType == CenterType.HOME_GPS)
        }
        val testCurrentLocationState = CurrentLocationState(350.052f, 250.55476f)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        val testLocation = Location("MOCK_GPS_PROVIDER")
        testLocation.latitude = 37.421791
        testLocation.longitude = -122.137648
        widgetModel.onLocationChanged(testLocation)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals("Current Location State Test Angle",
                    it.currentLocationState.angle,
                    testCurrentLocationState.angle,
                    DELTA)
            Assert.assertEquals("Current Location State Test Distance",
                    it.currentLocationState.distance,
                    testCurrentLocationState.distance,
                    DELTA)
            (it.currentLocationState.angle == testCurrentLocationState.angle
                    && it.currentLocationState.distance == testCurrentLocationState.distance
                    && it.centerType == CenterType.RC_MOBILE_GPS)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_aircraftAttitude_isUpdated() {
        val testRollValue = 26.456
        val testPitchValue = 10.537
        val testYawValue = 168.982
        val aircraftRoll = "Aircraft Attitude Roll"
        val aircraftPitch = "Aircraft Attitude Pitch"
        val aircraftYaw = "Aircraft Attitude Yaw"
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL),
                testRollValue,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH),
                testPitchValue,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW),
                testYawValue,
                15,
                TimeUnit.SECONDS)

        // Use util method to set empty values to all other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        val defaultAttitudeValue = AircraftAttitude(0.0, 0.0, 0.0)
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals(aircraftRoll, it.aircraftAttitude.roll, defaultAttitudeValue.roll, DELTA.toDouble())
            Assert.assertEquals(aircraftPitch, it.aircraftAttitude.pitch, defaultAttitudeValue.pitch, DELTA.toDouble())
            Assert.assertEquals(aircraftYaw, it.aircraftAttitude.yaw, defaultAttitudeValue.yaw, DELTA.toDouble())
            it.aircraftAttitude.roll == defaultAttitudeValue.roll && it.aircraftAttitude.pitch == defaultAttitudeValue.pitch && it.aircraftAttitude.yaw == defaultAttitudeValue.yaw
        }
        val testAttitudeValue = AircraftAttitude(testRollValue, testPitchValue, testYawValue)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals(aircraftRoll, it.aircraftAttitude.roll, testAttitudeValue.roll, DELTA.toDouble())
            Assert.assertEquals(aircraftPitch, it.aircraftAttitude.pitch, defaultAttitudeValue.pitch, DELTA.toDouble())
            Assert.assertEquals(aircraftYaw, it.aircraftAttitude.yaw, defaultAttitudeValue.yaw, DELTA.toDouble())
            it.aircraftAttitude.roll == testAttitudeValue.roll && it.aircraftAttitude.pitch == defaultAttitudeValue.pitch && it.aircraftAttitude.yaw == defaultAttitudeValue.yaw
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals(aircraftRoll, it.aircraftAttitude.roll, testAttitudeValue.roll, DELTA.toDouble())
            Assert.assertEquals(aircraftPitch, it.aircraftAttitude.pitch, testAttitudeValue.pitch, DELTA.toDouble())
            Assert.assertEquals(aircraftYaw, it.aircraftAttitude.yaw, defaultAttitudeValue.yaw, DELTA.toDouble())
            it.aircraftAttitude.roll == testAttitudeValue.roll && it.aircraftAttitude.pitch == testAttitudeValue.pitch && it.aircraftAttitude.yaw == defaultAttitudeValue.yaw
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            Assert.assertEquals(aircraftRoll, it.aircraftAttitude.roll, testAttitudeValue.roll, DELTA.toDouble())
            Assert.assertEquals(aircraftPitch, it.aircraftAttitude.pitch, testAttitudeValue.pitch, DELTA.toDouble())
            Assert.assertEquals(aircraftYaw, it.aircraftAttitude.yaw, testAttitudeValue.yaw, DELTA.toDouble())
            it.aircraftAttitude.roll == testAttitudeValue.roll && it.aircraftAttitude.pitch == testAttitudeValue.pitch && it.aircraftAttitude.yaw == testAttitudeValue.yaw
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun compassWidgetModel_gimbalHeading_isUpdated() {
        val testGimbalYawValue = 148.234f
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE),
                testGimbalYawValue,
                10,
                TimeUnit.SECONDS)
        // Use util method to set empty values to all other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriber = widgetModel.compassWidgetState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.gimbalHeading == 0.0f }
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it.gimbalHeading == testGimbalYawValue }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    //region Helper functions
    private fun setEmptyValuesToAllKeys() {
        // Use util method to set empty values to all keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.GPS_DATA))
    }

    private fun mockSensorEventAndWindowManager(testRotationVector: FloatArray, rotation: Int): SensorEvent {
        val contextMock = mockkClass(Context::class)
        every { contextMock.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        val display = mockkClass(Display::class)
        Mockito.`when`(windowManager.defaultDisplay).thenReturn(display)
        every { display.rotation } returns rotation
        val testSensorEvent = mockkClass(SensorEvent::class)
        try {
            // Get the 'sensor' field and set required value to it
            val sensorField = SensorEvent::class.java.getField("sensor")
            sensorField.isAccessible = true
            val sensor = mockkClass(Sensor::class)
            widgetModel.onAccuracyChanged(sensor, 3)
            every { sensor.type } returns Sensor.TYPE_ROTATION_VECTOR
            sensorField[testSensorEvent] = sensor
            // Get the 'values' field and and set values to be returned to it
            val valuesField = SensorEvent::class.java.getField("values")
            valuesField.isAccessible = true
            valuesField[testSensorEvent] = testRotationVector
        } catch (e: NoSuchFieldException) {
            Assert.fail(e.message)
        } catch (e: IllegalAccessException) {
            Assert.fail(e.message)
        }
        return testSensorEvent
    } //endregion

    companion object {
        private const val DELTA = 0.0001f
    }
}