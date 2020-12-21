package dji.ux.beta.core.widget.distancehome

import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.toDistance
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.LocationUtil.*
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidgetModel.DistanceHomeState.CurrentDistanceToHome

/**
 * Widget Model for the [DistanceHomeWidget] used to define
 * the underlying logic and communication
 */
class DistanceHomeWidgetModel(djiSdkModel: DJISDKModel,
                              keyedStore: ObservableInMemoryKeyedStore,
                              private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val aircraftLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val aircraftLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val homeLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val homeLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val unitTypeDataProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val distanceHomeStateProcessor: DataProcessor<DistanceHomeState> = DataProcessor.create(DistanceHomeState.ProductDisconnected)

    /**
     * Value of the distance to home state of the aircraft
     */
    val distanceHomeState: Flowable<DistanceHomeState>
        get() = distanceHomeStateProcessor.toFlowable()

    override fun inSetup() {
        val aircraftLatitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE)
        val aircraftLongitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE)
        val homeLatitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE)
        val homeLongitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE)

        bindDataProcessor(aircraftLatitudeKey, aircraftLatitudeProcessor)
        bindDataProcessor(aircraftLongitudeKey, aircraftLongitudeProcessor)
        bindDataProcessor(homeLatitudeKey, homeLatitudeProcessor)
        bindDataProcessor(homeLongitudeKey, homeLongitudeProcessor)

        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (checkLatitude(aircraftLatitudeProcessor.value)
                    && checkLongitude(aircraftLongitudeProcessor.value)
                    && checkLatitude(homeLatitudeProcessor.value)
                    && checkLongitude(homeLongitudeProcessor.value)) {
                distanceHomeStateProcessor.onNext(CurrentDistanceToHome(
                        distanceBetween(homeLatitudeProcessor.value,
                                homeLongitudeProcessor.value,
                                aircraftLatitudeProcessor.value,
                                aircraftLongitudeProcessor.value).toDistance(unitTypeDataProcessor.value),
                        unitTypeDataProcessor.value))
            } else {
                distanceHomeStateProcessor.onNext(DistanceHomeState.LocationUnavailable)
            }
        } else {
            distanceHomeStateProcessor.onNext(DistanceHomeState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states distance of aircraft from the home point
     */
    sealed class DistanceHomeState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : DistanceHomeState()

        /**
         * Product is connected but gps location fix is unavailable
         */
        object LocationUnavailable : DistanceHomeState()

        /**
         * Reflecting the distance to the home point
         */
        data class CurrentDistanceToHome(val distance: Float, val unitType: UnitConversionUtil.UnitType) : DistanceHomeState()

    }
}