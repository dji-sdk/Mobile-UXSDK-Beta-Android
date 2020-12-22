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

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import dji.common.remotecontroller.GPSData
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.keysdk.GimbalKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.*
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex
import kotlin.math.abs

private const val SENSOR_SENSITIVE_PARAM = 2
private const val HALF_TURN = 180
private const val QUARTER_TURN = 90

/**
 * Widget Model for the [CompassWidget] used to define
 * the underlying logic and communication
 */
class CompassWidgetModel(djiSdkModel: DJISDKModel,
                         keyedStore: ObservableInMemoryKeyedStore,
                         private val sensorManager: SensorManager?,
                         private val windowManager: WindowManager?
) : WidgetModel(djiSdkModel, keyedStore), SensorEventListener, LocationListener {

    //region Fields
    private val attitudePitchProcessor: DataProcessor<Float> = DataProcessor.create(0f)
    private val attitudeRollProcessor: DataProcessor<Float> = DataProcessor.create(0f)
    private val attitudeYawProcessor: DataProcessor<Float> = DataProcessor.create(0f)
    private val homeLocationLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val homeLocationLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val aircraftLocationLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val aircraftLocationLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val rcGPSDataProcessor: DataProcessor<GPSData> = DataProcessor.create(GPSData.Builder().build())
    private val gimbalYawProcessor: DataProcessor<Float> = DataProcessor.create(0f)
    private val centerTypeProcessor: DataProcessor<CenterType> = DataProcessor.create(CenterType.HOME_GPS)
    private val mobileDeviceAzimuthProcessor: DataProcessor<Float> = DataProcessor.create(0f)

    private val aircraftAttitudeProcessor: DataProcessor<AircraftAttitude> = DataProcessor.create(AircraftAttitude(0.0, 0.0, 0.0))
    private val aircraftStateProcessor: DataProcessor<AircraftState> = DataProcessor.create(AircraftState(0f, 0f))
    private val currentLocationStateProcessor: DataProcessor<CurrentLocationState> = DataProcessor.create(CurrentLocationState(0f, 0f))

    private val compassWidgetStateProcessor: DataProcessor<CompassWidgetState> = DataProcessor.create(
            CompassWidgetState(0f,
                    AircraftAttitude(0.0, 0.0, 0.0),
                    AircraftState(0f, 0f),
                    CurrentLocationState(0f, 0f),
                    0f,
                    CenterType.HOME_GPS))

    private var rotationVector: Sensor? = null
    private var rcOrMobileLatitude = 0.0
    private var rcOrMobileLongitude = 0.0
    private var aircraftLatitude = 0.0
    private var aircraftLongitude = 0.0
    private var homeLatitude = 0.0
    private var homeLongitude = 0.0
    private var latestSensorValue = 0f
    private var gimbalIndex = GimbalIndex.PORT.index

    /**
     * values[0]: azimuth, rotation around the Z axis.
     * values[1]: pitch, rotation around the X axis.
     * values[2]: roll, rotation around the Y axis.
     */
    private val values = FloatArray(3)
    private val rotations = FloatArray(9)

    /**
     * The MobileGPSLocationUtil class that has the `startUpdateLocation()`
     * and `stopUpdateLocation()` functions for the mobile device's location
     */
    var mobileGPSLocationUtil: MobileGPSLocationUtil? = null

    /**
     * The state of the compass widget
     */
    val compassWidgetState: Flowable<CompassWidgetState>
        get() = compassWidgetStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val attitudePitchKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH)
        val attitudeRollKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL)
        val attitudeYawKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW)
        val homeLatitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE)
        val homeLongitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE)
        val aircraftLatitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE)
        val aircraftLongitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE)
        val rcGpsDataKey: DJIKey = RemoteControllerKey.create(RemoteControllerKey.GPS_DATA)
        val gimbalYawKey: DJIKey = GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE, gimbalIndex)

        // Set AircraftAttitude using roll, pitch and yaw keys
        bindDataProcessor(attitudePitchKey, attitudePitchProcessor) { pitch: Any ->
            aircraftAttitudeProcessor.onNext(AircraftAttitude(aircraftAttitudeProcessor.value.roll,
                    pitch as Double,
                    aircraftAttitudeProcessor.value.yaw))
        }
        bindDataProcessor(attitudeRollKey, attitudeRollProcessor) { roll: Any ->
            aircraftAttitudeProcessor.onNext(AircraftAttitude(roll as Double,
                    aircraftAttitudeProcessor.value.pitch,
                    aircraftAttitudeProcessor.value.yaw))
        }
        bindDataProcessor(attitudeYawKey, attitudeYawProcessor) { yaw: Any ->
            aircraftAttitudeProcessor.onNext(AircraftAttitude(aircraftAttitudeProcessor.value.roll,
                    aircraftAttitudeProcessor.value.pitch,
                    yaw as Double))
        }

        // Set the home location when changed and update the various calculations
        bindDataProcessor(homeLatitudeKey, homeLocationLatitudeProcessor) { latitude: Any ->
            homeLatitude = latitude as Double
            updateCalculations()
        }
        bindDataProcessor(homeLongitudeKey, homeLocationLongitudeProcessor) { longitude: Any ->
            homeLongitude = longitude as Double
            updateCalculations()
        }

        // Update the aircraft's location
        bindDataProcessor(aircraftLatitudeKey, aircraftLocationLatitudeProcessor) { latitude: Any ->
            updateAircraftLatitude(latitude as Double)
        }
        bindDataProcessor(aircraftLongitudeKey, aircraftLocationLongitudeProcessor) { longitude: Any ->
            updateAircraftLongitude(longitude as Double)
        }

        // Update the RC's location
        bindDataProcessor(rcGpsDataKey, rcGPSDataProcessor) { gpsData: Any ->
            updateGPSData(gpsData as GPSData)
        }

        // Update the gimbal heading
        bindDataProcessor(gimbalYawKey, gimbalYawProcessor)
        registerMobileDeviceSensorListener()

        // Start mobile device's location updates if available
        mobileGPSLocationUtil?.startUpdateLocation()
    }

    override fun inCleanup() {
        unregisterMobileDeviceSensorListener()

        // Stop mobile device's location updates if available
        mobileGPSLocationUtil?.stopUpdateLocation()
    }
    //endregion

    //region Updates
    override fun updateStates() {
        compassWidgetStateProcessor.onNext(CompassWidgetState(
                mobileDeviceAzimuthProcessor.value,
                aircraftAttitudeProcessor.value,
                aircraftStateProcessor.value,
                currentLocationStateProcessor.value,
                gimbalYawProcessor.value,
                centerTypeProcessor.value))
    }
    //endregion

    //region Mobile Device Sensor listener
    private fun registerMobileDeviceSensorListener() {
        if (sensorManager != null) {
            // Register the mobile device's rotation sensor to start listening for updates
            // DJI devices cannot get rotation from TYPE_ROTATION_VECTOR, so use TYPE_ORIENTATION
            rotationVector = if (DJIDeviceUtil.isDJIDevice()) {
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            } else {
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            }
            if (rotationVector != null) {
                sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    private fun unregisterMobileDeviceSensorListener() {
        sensorManager?.unregisterListener(this, rotationVector)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Update the mobile device azimuth when updated by the sensor
        var sensorValue = latestSensorValue
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            sensorValue = event.values[0]
        } else if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            MathUtil.getRotationMatrixFromVector(rotations, event.values)
            SensorManager.getOrientation(rotations, values)
            sensorValue = Math.toDegrees(values[0].toDouble()).toFloat()
        }
        if (abs(sensorValue - latestSensorValue) > SENSOR_SENSITIVE_PARAM) {
            latestSensorValue = sensorValue
            val rotation: Int = getDisplayRotation()
            if (rotation == Surface.ROTATION_270) {
                sensorValue += HALF_TURN.toFloat()
            }
            if (DJIDeviceUtil.isSmartController()) {
                sensorValue += QUARTER_TURN.toFloat()
            }
            val mobileDeviceAzimuth = sensorValue + QUARTER_TURN
            mobileDeviceAzimuthProcessor.onNext(mobileDeviceAzimuth)
        }
        updateStates()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            // Update the center type to be the RC/Mobile device type
            centerTypeProcessor.onNext(CenterType.RC_MOBILE_GPS)
            // Update location using received location of the mobile device
            rcOrMobileLatitude = location.latitude
            rcOrMobileLongitude = location.longitude
            updateCalculations()
            updateStates()
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // Do nothing
    }

    override fun onProviderEnabled(provider: String) {
        // Do nothing
    }

    override fun onProviderDisabled(provider: String) {
        // Do nothing
    }
    //endregion

    //region Helpers
    private fun updateGPSData(data: GPSData) {
        if (data.isValid) {
            // Update the center type to be the RC/Mobile device type
            centerTypeProcessor.onNext(CenterType.RC_MOBILE_GPS)

            // Update location using received location of the RC
            rcOrMobileLatitude = data.location.latitude
            rcOrMobileLongitude = data.location.longitude

            // Stop updating mobile device location once RC location is received
            mobileGPSLocationUtil?.stopUpdateLocation()
            updateCalculations()
        }
    }

    private fun updateAircraftLatitude(latitude: Double) {
        if (LocationUtil.checkLatitude(latitude)) {
            aircraftLatitude = latitude
            calculateAircraftAngleAndDistanceFromCenterLocation()
        }
    }

    private fun updateAircraftLongitude(longitude: Double) {
        if (LocationUtil.checkLongitude(longitude)) {
            aircraftLongitude = longitude
            calculateAircraftAngleAndDistanceFromCenterLocation()
        }
    }

    private fun updateCalculations() {
        calculateAircraftAngleAndDistanceFromCenterLocation()
        calculateAngleAndDistanceBetweenRCAndHome()
    }

    private fun calculateAircraftAngleAndDistanceFromCenterLocation() {
        val tempCalculatedLocation: FloatArray
        val latestAircraftState = AircraftState(0.0f, 0.0f)
        if (centerTypeProcessor.value == CenterType.HOME_GPS) {
            if (LocationUtil.checkLatitude(homeLatitude) && LocationUtil.checkLongitude(homeLongitude)) {
                tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(homeLatitude,
                        homeLongitude,
                        aircraftLatitude,
                        aircraftLongitude)
                latestAircraftState.angle = tempCalculatedLocation[0]
                latestAircraftState.distance = tempCalculatedLocation[1]
                aircraftStateProcessor.onNext(latestAircraftState)
            }
        } else if (centerTypeProcessor.value == CenterType.RC_MOBILE_GPS) {
            if (LocationUtil.checkLatitude(rcOrMobileLatitude) && LocationUtil.checkLongitude(rcOrMobileLongitude)) {
                tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(rcOrMobileLatitude,
                        rcOrMobileLongitude,
                        aircraftLatitude,
                        aircraftLongitude)
                latestAircraftState.angle = tempCalculatedLocation[0]
                latestAircraftState.distance = tempCalculatedLocation[1]
                aircraftStateProcessor.onNext(latestAircraftState)
            }
        }
    }

    private fun calculateAngleAndDistanceBetweenRCAndHome() {
        if (centerTypeProcessor.value != CenterType.HOME_GPS) {
            val tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(rcOrMobileLatitude,
                    rcOrMobileLongitude,
                    homeLatitude,
                    homeLongitude)
            val latestCurrentLocationState = CurrentLocationState(0.0f, 0.0f)
            latestCurrentLocationState.angle = tempCalculatedLocation[0]
            latestCurrentLocationState.distance = tempCalculatedLocation[1]
            currentLocationStateProcessor.onNext(latestCurrentLocationState)
        }
    }

    private fun getDisplayRotation(): Int {
        var rotation = 0
        if (windowManager != null) {
            rotation = windowManager.defaultDisplay.rotation
        }
        if (DJIDeviceUtil.isDJIDevice()) {
            rotation = (rotation + 1) % 4 // DJI device default offset is 90, which is modified to support the turn screen function.
        }
        return rotation
    }
    //endregion

    //region Customization
    /**
     * Get the gimbal index for which the model is reacting.
     *
     * @return current gimbal index.
     */
    fun getGimbalIndex(): GimbalIndex? {
        return GimbalIndex.find(gimbalIndex)
    }

    /**
     * Set gimbal index to which the model should react.
     *
     * @param gimbalIndex index of the gimbal.
     */
    fun setGimbalIndex(gimbalIndex: GimbalIndex?) {
        if (gimbalIndex != null) {
            this.gimbalIndex = gimbalIndex.index
        }
        restart()
    }
    //endregion

    //region Classes
    /**
     * Enum for the center type used in the calculations
     */
    enum class CenterType {
        /**
         * The center is determined by RC location data or mobile device
         * location data
         */
        RC_MOBILE_GPS,

        /**
         * The center is determined by the home location's data
         */
        HOME_GPS
    }

    /**
     * Class that holds the aircraft's attitude with getters and setters
     * for the [roll], [pitch] and [yaw] of the aircraft
     */
    data class AircraftAttitude(var roll: Double, var pitch: Double, var yaw: Double)

    /**
     * Class that holds the [angle] and [distance] between the aircraft and the
     * home/RC/Mobile device's location.
     */
    data class AircraftState(var angle: Float, var distance: Float)

    /**
     * Class that holds the [angle] and [distance] between current home and RC/Mobile device
     * locations
     */
    data class CurrentLocationState(var angle: Float, var distance: Float)

    data class CompassWidgetState(
            var phoneAzimuth: Float,
            var aircraftAttitude: AircraftAttitude,
            var aircraftState: AircraftState,
            var currentLocationState: CurrentLocationState,
            var gimbalHeading: Float,
            var centerType: CenterType
    )
    //endregion
}