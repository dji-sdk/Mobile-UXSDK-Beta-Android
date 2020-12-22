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

package dji.ux.beta.flight.widget.returnhome

import dji.common.flightcontroller.flyzone.FlyZoneReturnToHomeState
import dji.common.remotecontroller.RCMode
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProviderInterface
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.LocationUtil
import dji.ux.beta.core.util.UnitConversionUtil

/**
 * Widget Model for the [ReturnHomeWidget] used to define
 * the underlying logic and communication
 */
class ReturnHomeWidgetModel(djiSdkModel: DJISDKModel,
                            keyedStore: ObservableInMemoryKeyedStore,
                            private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isGoingHomeDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isAutoLandingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val areMotorsOnDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val returnHomeDataProcessor: DataProcessor<ReturnHomeState> = DataProcessor.create(ReturnHomeState.DISCONNECTED)
    private val isCancelReturnToHomeDisabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rcModeDataProcessor: DataProcessor<RCMode> = DataProcessor.create(RCMode.UNKNOWN)
    private val flyZoneReturnToHomeStateProcessor: DataProcessor<FlyZoneReturnToHomeState> = DataProcessor.create(FlyZoneReturnToHomeState.UNKNOWN)
    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    //endregion

    //region Data
    /**
     * Get the return home state
     */
    val returnHomeState: Flowable<ReturnHomeState>
        get() = returnHomeDataProcessor.toFlowable().distinctUntilChanged()

    /**
     * Get the distance from the aircraft to the home point
     */
    val distanceToHome: ReturnHomeDistance
        get() {
            val goHomeHeightKey: DJIKey = FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS)
            val homeLatitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE)
            val homeLongitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE)
            val currentHeightKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ALTITUDE)
            val aircraftLatKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE)
            val aircraftLongKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE)
            var currentHeight = 0f
            var goHomeHeight = 0f
            var homeLatitude = Double.NaN
            var homeLongitude = Double.NaN
            var aircraftLocationLat = 0.0
            var aircraftLocationLong = 0.0
            val unitType = unitTypeProcessor.value

            djiSdkModel.getCacheValue(currentHeightKey)?.let {
                currentHeight = it as Float
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    currentHeight = UnitConversionUtil.convertMetersToFeet(currentHeight)
                }
            }
            djiSdkModel.getCacheValue(goHomeHeightKey)?.let {
                goHomeHeight = (it as Int).toFloat()
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    goHomeHeight = UnitConversionUtil.convertMetersToFeet(goHomeHeight)
                }
            }
            djiSdkModel.getCacheValue(homeLatitudeKey)?.let {
                homeLatitude = it as Double
            }
            djiSdkModel.getCacheValue(homeLongitudeKey)?.let {
                homeLongitude = it as Double
            }
            djiSdkModel.getCacheValue(aircraftLatKey)?.let {
                aircraftLocationLat = it as Double
            }
            djiSdkModel.getCacheValue(aircraftLongKey)?.let {
                aircraftLocationLong = it as Double
            }
            var distanceToHome = 0f
            if (!homeLatitude.isNaN() && !homeLongitude.isNaN()) {
                distanceToHome = distanceBetween(homeLatitude, homeLongitude, aircraftLocationLat, aircraftLocationLong)
            }
            return ReturnHomeDistance(distanceToHome, currentHeight, goHomeHeight, unitType)
        }

    /**
     * Get whether returning to home at the current altitude is enabled
     */
    val isRTHAtCurrentAltitudeEnabled: Boolean
        get() {
            val rthAtCurrentHeightKey = FlightControllerKey.create(FlightControllerKey.CONFIG_RTH_IN_CURRENT_ALTITUDE)
            var isRTHAtCurrentAltitudeEnabled = true
            djiSdkModel.getCacheValue(rthAtCurrentHeightKey)?.let {
                isRTHAtCurrentAltitudeEnabled = it as Boolean
            }
            return isRTHAtCurrentAltitudeEnabled
        }

    /**
     * Get the latest [FlyZoneReturnToHomeState]
     */
    val flyZoneReturnToHomeState: FlyZoneReturnToHomeState
        get() = flyZoneReturnToHomeStateProcessor.value
    //endregion

    //region Constructor
    init {
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.unitType)
        }
    }
    //endregion

    //region Actions
    /**
     * Performs return to home action
     */
    fun performReturnHomeAction(): Completable {
        val goHome: DJIKey = FlightControllerKey.create(FlightControllerKey.START_GO_HOME)
        return djiSdkModel.performAction(goHome)
    }

    /**
     * Performs cancel return to home action
     */
    fun performCancelReturnHomeAction(): Completable {
        val cancelGoHomeAction: DJIKey = FlightControllerKey.create(FlightControllerKey.CANCEL_GO_HOME)
        return djiSdkModel.performAction(cancelGoHomeAction)
    }

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val isFlyingKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_FLYING)
        bindDataProcessor(isFlyingKey, isFlyingDataProcessor)
        val isAutoLandingKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_LANDING)
        bindDataProcessor(isAutoLandingKey, isAutoLandingDataProcessor)
        val areMotorsOnKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON)
        bindDataProcessor(areMotorsOnKey, areMotorsOnDataProcessor)
        val isGoingHomeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME)
        bindDataProcessor(isGoingHomeKey, isGoingHomeDataProcessor)
        val isCancelReturnToHomeDisabledKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_CANCEL_RETURN_TO_HOME_DISABLED)
        bindDataProcessor(isCancelReturnToHomeDisabledKey, isCancelReturnToHomeDisabledProcessor)
        val rcModeKey: DJIKey = RemoteControllerKey.create(RemoteControllerKey.MODE)
        bindDataProcessor(rcModeKey, rcModeDataProcessor)
        val flyZoneReturnToHomeState: DJIKey = FlightControllerKey.create(FlightControllerKey.RETURN_TO_HOME_STATE)
        bindDataProcessor(flyZoneReturnToHomeState, flyZoneReturnToHomeStateProcessor)
        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitKey, unitTypeProcessor)
        preferencesManager?.setUpListener()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        if (!productConnectionProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.DISCONNECTED)
        } else if (!isFlyingDataProcessor.value
                || !areMotorsOnDataProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.RETURN_HOME_DISABLED)
        } else if (isAutoLandingDataProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.AUTO_LANDING)
        } else if (isGoingHomeDataProcessor.value && !isAutoLandingDataProcessor.value) {
            if (isCancelReturnHomeDisabled()) {
                returnHomeDataProcessor.onNext(ReturnHomeState.FORCED_RETURNING_TO_HOME)
            } else {
                returnHomeDataProcessor.onNext(ReturnHomeState.RETURNING_TO_HOME)
            }
        } else {
            returnHomeDataProcessor.onNext(ReturnHomeState.READY_TO_RETURN_HOME)
        }
    }

    private fun isCancelReturnHomeDisabled(): Boolean {
        return isCancelReturnToHomeDisabledProcessor.value ||
                rcModeDataProcessor.value == RCMode.SLAVE
    }
    //endregion

    //region Helpers for unit testing
    private fun distanceBetween(latitude1: Double,
                                longitude1: Double,
                                latitude2: Double,
                                longitude2: Double): Float {
        return LocationUtil.distanceBetween(latitude1, longitude1, latitude2, longitude2)
    }
    //endregion

    //region Classes
    /**
     * The state of the aircraft
     */
    enum class ReturnHomeState {
        /**
         * The aircraft is ready to return to home
         */
        READY_TO_RETURN_HOME,

        /**
         * The aircraft cannot return to home
         */
        RETURN_HOME_DISABLED,

        /**
         * The aircraft has started returning to home
         */
        RETURNING_TO_HOME,

        /**
         * The aircraft has started returning to home and it cannot be canceled
         */
        FORCED_RETURNING_TO_HOME,

        /**
         * The aircraft has started auto landing
         */
        AUTO_LANDING,

        /**
         * The aircraft is disconnected
         */
        DISCONNECTED
    }

    /**
     * The measurements describing the return to home behavior
     *
     * @property distanceToHome The distance to home in meters
     * @property currentHeight The current height of the aircraft in [unitType]
     * @property goToHomeHeight The height at which the aircraft will return to home in [unitType]
     * @property unitType The unit type of [currentHeight] and [goToHomeHeight]
     */
    data class ReturnHomeDistance(val distanceToHome: Float,
                                  val currentHeight: Float,
                                  val goToHomeHeight: Float,
                                  val unitType: UnitConversionUtil.UnitType)

    //endregion
}