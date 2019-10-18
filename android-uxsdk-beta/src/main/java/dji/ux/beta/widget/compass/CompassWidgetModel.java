/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.widget.compass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.remotecontroller.GPSData;
import dji.common.util.MobileGPSLocationUtil;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.GimbalKey;
import dji.keysdk.RemoteControllerKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DataProcessor;
import dji.ux.beta.util.LocationUtil;
import dji.ux.beta.util.MathUtil;

/**
 * Widget Model for the {@link CompassWidget} used to define
 * the underlying logic and communication
 */
public class CompassWidgetModel extends WidgetModel implements SensorEventListener, LocationListener {

    //region Fields
    private static final int SENSOR_SENSITIVE_PARAM = 2;
    private static final int HALF_TURN = 180;
    private static final int QUARTER_TURN = 90;

    private final DataProcessor<Float> attitudePitchProcessor;
    private final DataProcessor<Float> attitudeRollProcessor;
    private final DataProcessor<Float> attitudeYawProcessor;
    private final DataProcessor<Double> homeLocationLatitudeProcessor;
    private final DataProcessor<Double> homeLocationLongitudeProcessor;
    private final DataProcessor<Double> aircraftLocationLatitudeProcessor;
    private final DataProcessor<Double> aircraftLocationLongitudeProcessor;
    private final DataProcessor<GPSData> rcGPSDataProcessor;
    private final DataProcessor<Float> gimbalYawProcessor;
    private final DataProcessor<CenterType> centerTypeProcessor;
    private final DataProcessor<Float> mobileDeviceAzimuthProcessor;
    private final DataProcessor<AircraftAttitude> aircraftAttitudeProcessor;
    private final DataProcessor<AircraftState> aircraftStateProcessor;
    private final DataProcessor<CurrentLocationState> currentLocationStateProcessor;

    private CenterType centerType = CenterType.HOME_GPS;
    private AircraftAttitude latestAircraftAttitude;
    private MobileGPSLocationUtil mobileGPSLocationUtil = null;
    private SensorManager sensorManager;
    private WindowManager windowManager;
    private Sensor rotationVector;
    private double rcOrMobileLatitude;
    private double rcOrMobileLongitude;
    private double aircraftLatitude;
    private double aircraftLongitude;
    private double homeLatitude;
    private double homeLongitude;
    private float latestSensorValue;

    /**
     * values[0]: azimuth, rotation around the Z axis.
     * values[1]: pitch, rotation around the X axis.
     * values[2]: roll, rotation around the Y axis.
     */
    private float[] values = new float[3];
    private float[] rotations = new float[9];
    //endregion

    //region Constructor
    public CompassWidgetModel(@NonNull DJISDKModel djiSdkModel,
                              @NonNull ObservableInMemoryKeyedStore keyedStore,
                              @Nullable SensorManager sensorManager,
                              @Nullable WindowManager windowManager) {
        super(djiSdkModel, keyedStore);
        this.sensorManager = sensorManager;
        this.windowManager = windowManager;
        attitudePitchProcessor = DataProcessor.create(0f);
        attitudeRollProcessor = DataProcessor.create(0f);
        attitudeYawProcessor = DataProcessor.create(0f);
        homeLocationLatitudeProcessor = DataProcessor.create(0.0);
        homeLocationLongitudeProcessor = DataProcessor.create(0.0);
        aircraftLocationLatitudeProcessor = DataProcessor.create(0.0);
        aircraftLocationLongitudeProcessor = DataProcessor.create(0.0);
        rcGPSDataProcessor = DataProcessor.create(new GPSData.Builder().build());
        gimbalYawProcessor = DataProcessor.create(0f);

        centerTypeProcessor = DataProcessor.create(CenterType.HOME_GPS);
        mobileDeviceAzimuthProcessor = DataProcessor.create(0f);
        latestAircraftAttitude = new AircraftAttitude(0f, 0f, 0f);
        aircraftAttitudeProcessor = DataProcessor.create(latestAircraftAttitude);
        aircraftStateProcessor = DataProcessor.create(new AircraftState(0f, 0f));
        currentLocationStateProcessor = DataProcessor.create(new CurrentLocationState(0f, 0f));
    }
    //endregion

    //region Data

    /**
     * Get the center type for the compass widget
     *
     * @return CenterType enum value
     */
    public Flowable<CenterType> getCenterType() {
        return centerTypeProcessor.toFlowable();
    }

    /**
     * Get the mobile device's azimuth
     *
     * @return Float value representing the azimuth
     */
    public Flowable<Float> getMobileDeviceAzimuth() {
        return mobileDeviceAzimuthProcessor.toFlowable();
    }

    /**
     * Get the state of the aircraft
     *
     * @return {@link AircraftState} value for distance and angle of the aircraft
     */
    public Flowable<AircraftState> getAircraftState() {
        return aircraftStateProcessor.toFlowable();
    }

    /**
     * Get the aircraft's attitude (roll, pitch and yaw)
     *
     * @return {@link AircraftAttitude} value for the attitude
     */
    public Flowable<AircraftAttitude> getAircraftAttitude() {
        return aircraftAttitudeProcessor.toFlowable();
    }

    /**
     * Get the state of the current location of the aircraft.
     *
     * @return {@link CurrentLocationState} value for distance and angle of current location
     */
    public Flowable<CurrentLocationState> getCurrentLocationState() {
        return currentLocationStateProcessor.toFlowable();
    }

    /**
     * Get the aircraft gimbal's heading
     *
     * @return Float value representing the gimbal's yaw position
     */
    public Flowable<Float> getGimbalHeading() {
        return gimbalYawProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey attitudePitchKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH);
        DJIKey attitudeRollKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL);
        DJIKey attitudeYawKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW);
        DJIKey homeLatitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE);
        DJIKey homeLongitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE);
        DJIKey aircraftLatitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE);
        DJIKey aircraftLongitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE);
        DJIKey rcGpsDataKey = RemoteControllerKey.create(RemoteControllerKey.GPS_DATA);
        DJIKey gimbalYawKey = GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE);

        // Set AircraftAttitude using roll, pitch and yaw keys
        bindDataProcessor(attitudePitchKey,
                attitudePitchProcessor,
                pitch -> latestAircraftAttitude.setPitch((double) pitch));
        bindDataProcessor(attitudeRollKey,
                attitudeRollProcessor,
                roll -> latestAircraftAttitude.setRoll((double) roll));
        bindDataProcessor(attitudeYawKey, attitudeYawProcessor, yaw -> latestAircraftAttitude.setYaw((double) yaw));

        // Set the home location when changed and update the various calculations
        bindDataProcessor(homeLatitudeKey, homeLocationLatitudeProcessor, latitude -> {
            homeLatitude = (double) latitude;
            updateCalculations();
        });
        bindDataProcessor(homeLongitudeKey, homeLocationLongitudeProcessor, longitude -> {
            homeLongitude = (double) longitude;
            updateCalculations();
        });

        // Update the aircraft's location
        bindDataProcessor(aircraftLatitudeKey,
                aircraftLocationLatitudeProcessor,
                latitude -> updateAircraftLatitude((double) latitude));
        bindDataProcessor(aircraftLongitudeKey,
                aircraftLocationLongitudeProcessor,
                longitude -> updateAircraftLongitude((double) longitude));

        // Update the RC's location
        bindDataProcessor(rcGpsDataKey, rcGPSDataProcessor, gpsData -> updateGPSData((GPSData) gpsData));

        // Update the gimbal heading
        bindDataProcessor(gimbalYawKey, gimbalYawProcessor);

        registerMobileDeviceSensorListener();

        // Start mobile device's location updates if available
        if (mobileGPSLocationUtil != null) {
            mobileGPSLocationUtil.startUpdateLocation();
        }
    }

    @Override
    protected void inCleanup() {
        unregisterMobileDeviceSensorListener();

        // Stop mobile device's location updates if available
        if (mobileGPSLocationUtil != null) {
            mobileGPSLocationUtil.stopUpdateLocation();
        }
    }
    //endregion

    //region Updates
    @Override
    protected void updateStates() {
        aircraftAttitudeProcessor.onNext(latestAircraftAttitude);
    }
    //endregion

    //region Mobile Device Sensor listener
    private void registerMobileDeviceSensorListener() {
        if (sensorManager != null) {
            // Register the mobile device's rotation sensor to start listening for updates
            rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationVector != null) {
                sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    private void unregisterMobileDeviceSensorListener() {
        sensorManager.unregisterListener(this, rotationVector);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Update the mobile device azimuth when updated by the sensor
        float sensorValue = latestSensorValue;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            MathUtil.getRotationMatrixFromVector(rotations, event.values);
            SensorManager.getOrientation(rotations, values);
            sensorValue = (float) Math.toDegrees(values[0]);
        }

        if (Math.abs(sensorValue - latestSensorValue) > SENSOR_SENSITIVE_PARAM) {
            latestSensorValue = sensorValue;
            if (windowManager != null && windowManager.getDefaultDisplay().getRotation() == Surface.ROTATION_270) {
                sensorValue += HALF_TURN;
            }
            float mobileDeviceAzimuth = sensorValue + QUARTER_TURN;
            mobileDeviceAzimuthProcessor.onNext(mobileDeviceAzimuth);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            // Update the center type to be the RC/Mobile device type
            centerType = CenterType.RC_MOBILE_GPS;
            centerTypeProcessor.onNext(centerType);
            // Update location using received location of the mobile device
            rcOrMobileLatitude = location.getLatitude();
            rcOrMobileLongitude = location.getLongitude();

            updateCalculations();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Do nothing
    }
    //endregion

    //region Helpers

    /**
     * Initialize the MobileGPSLocationUtil class that has the `startUpdateLocation()`
     * and `stopUpdateLocation()` functions for the mobile device's location
     *
     * @param mobileGPSLocationUtil Instance of the MobileGPSLocationUtil class
     */
    public void setMobileGPSLocationUtil(@NonNull MobileGPSLocationUtil mobileGPSLocationUtil) {
        this.mobileGPSLocationUtil = mobileGPSLocationUtil;
    }

    private void updateGPSData(@NonNull GPSData data) {
        if (data.isValid()) {
            // Update the center type to be the RC/Mobile device type
            centerType = CenterType.RC_MOBILE_GPS;
            centerTypeProcessor.onNext(centerType);

            // Update location using received location of the RC
            rcOrMobileLatitude = data.getLocation().getLatitude();
            rcOrMobileLongitude = data.getLocation().getLongitude();

            // Stop updating mobile device location once RC location is received
            mobileGPSLocationUtil.stopUpdateLocation();

            updateCalculations();
        }
    }

    private void updateAircraftLatitude(double latitude) {
        if (LocationUtil.checkLatitude(latitude)) {
            aircraftLatitude = latitude;
            calculateAircraftAngleAndDistanceFromCenterLocation();
        }
    }

    private void updateAircraftLongitude(double longitude) {
        if (LocationUtil.checkLongitude(longitude)) {
            aircraftLongitude = longitude;
            calculateAircraftAngleAndDistanceFromCenterLocation();
        }
    }

    private void updateCalculations() {
        calculateAircraftAngleAndDistanceFromCenterLocation();
        calculateAngleAndDistanceBetweenRCAndHome();
    }

    private void calculateAircraftAngleAndDistanceFromCenterLocation() {
        float[] tempCalculatedLocation;
        AircraftState latestAircraftState = new AircraftState(0.0f, 0.0f);
        if (centerType == CenterType.HOME_GPS) {
            if (LocationUtil.checkLatitude(homeLatitude) && LocationUtil.checkLongitude(homeLongitude)) {
                tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(homeLatitude,
                        homeLongitude,
                        aircraftLatitude,
                        aircraftLongitude);
                latestAircraftState.setAngle(tempCalculatedLocation[0]);
                latestAircraftState.setDistance(tempCalculatedLocation[1]);
                aircraftStateProcessor.onNext(latestAircraftState);
            }
        } else if (centerType == CenterType.RC_MOBILE_GPS) {
            if (LocationUtil.checkLatitude(rcOrMobileLatitude) && LocationUtil.checkLongitude(rcOrMobileLongitude)) {
                tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(rcOrMobileLatitude,
                        rcOrMobileLongitude,
                        aircraftLatitude,
                        aircraftLongitude);
                latestAircraftState.setAngle(tempCalculatedLocation[0]);
                latestAircraftState.setDistance(tempCalculatedLocation[1]);
                aircraftStateProcessor.onNext(latestAircraftState);
            }
        }
    }

    private void calculateAngleAndDistanceBetweenRCAndHome() {
        if (centerType != CenterType.HOME_GPS) {
            float[] tempCalculatedLocation = LocationUtil.calculateAngleAndDistance(rcOrMobileLatitude,
                    rcOrMobileLongitude,
                    homeLatitude,
                    homeLongitude);
            CurrentLocationState latestCurrentLocationState = new CurrentLocationState(0.0f, 0.0f);
            latestCurrentLocationState.setAngle(tempCalculatedLocation[0]);
            latestCurrentLocationState.setDistance(tempCalculatedLocation[1]);
            currentLocationStateProcessor.onNext(latestCurrentLocationState);
        }
    }
    //endregion

    /**
     * Class that holds the aircraft's attitude with getters and setters
     * for the roll, pitch and yaw of the aircraft
     */
    public class AircraftAttitude {
        private double roll;
        private double pitch;
        private double yaw;

        public AircraftAttitude(double roll, double pitch, double yaw) {
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
        }

        public double getRoll() {
            return roll;
        }

        public double getPitch() {
            return pitch;
        }

        public double getYaw() {
            return yaw;
        }

        public void setRoll(double roll) {
            this.roll = roll;
        }

        public void setPitch(double pitch) {
            this.pitch = pitch;
        }

        public void setYaw(double yaw) {
            this.yaw = yaw;
        }
    }

    /**
     * Class that holds the angle and distance between the aircraft and the
     * home/RC/Mobile device's location.
     */
    public class AircraftState {
        private float angle;
        private float distance;

        public AircraftState(float angle, float distance) {
            this.angle = angle;
            this.distance = distance;
        }

        public float getAngle() {
            return angle;
        }

        public float getDistance() {
            return distance;
        }

        public void setAngle(float angle) {
            this.angle = angle;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }
    }

    /**
     * Class that holds the angle and distance between current home and RC/Mobile device
     * locations
     */
    public class CurrentLocationState {
        private float angle;
        private float distance;

        public CurrentLocationState(float angle, float distance) {
            this.angle = angle;
            this.distance = distance;
        }

        public float getAngle() {
            return angle;
        }

        public float getDistance() {
            return distance;
        }

        public void setAngle(float angle) {
            this.angle = angle;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }
    }

    /**
     * Enum for the center type used in the calculations
     */
    public enum CenterType {
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
}
