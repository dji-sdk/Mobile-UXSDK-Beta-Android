package dji.ux.beta.core.widget.hsi

import dji.common.flightcontroller.LocationCoordinate3D
import dji.common.flightcontroller.flightassistant.PerceptionInformation
import dji.common.model.LocationCoordinate2D
import dji.keysdk.FlightControllerKey
import dji.keysdk.RadarKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/26
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class AttitudeDisplayModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    val perceptionFullDistanceProcessor = DataProcessor.create(PerceptionInformation.Builder().everyAngleDistance(IntArray(0)).build())
    val upAvoidanceDistanceProcessor = DataProcessor.create(0F)
    val downAvoidanceDistanceProcessor = DataProcessor.create(0F)
    val omniUpRadarDistanceProcessor = DataProcessor.create(0F)
    val omniDownRadarDistanceProcessor = DataProcessor.create(0F)
    val omniUpwardsAvoidanceEnabledProcessor = DataProcessor.create(false)
    val velocityZProcessor = DataProcessor.create(0f)
    val altitudeProcessor = DataProcessor.create(0f)
    val returnHomeFlightHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    val limitMaxFlightHeightInMeterProcessor = DataProcessor.create(0F)
    val radarUpwardsObstacleAvoidanceEnabledProcessor = DataProcessor.create(false)
    val radarObstacleAvoidanceStateProcessor = DataProcessor.create(PerceptionInformation.Builder().build())
    val rtkFusionTakeOffAltitudeProcessor = DataProcessor.create(0F)
    val aircraftLocationDataProcessor = DataProcessor.create(LocationCoordinate3D(LocationCoordinate2D.UNKNOWN, LocationCoordinate2D.UNKNOWN, -1f))

    override fun inSetup() {
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_FULL_DISTANCE), perceptionFullDistanceProcessor)
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.UP_AVOIDANCE_DISTANCE), upAvoidanceDistanceProcessor)
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DOWN_AVOIDANCE_DISTANCE), downAvoidanceDistanceProcessor)
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_UP_RADAR_DISTANCE), omniUpRadarDistanceProcessor)
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_DOWN_RADAR_DISTANCE), omniDownRadarDistanceProcessor)
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.UPWARDS_AVOIDANCE_ENABLED), omniUpwardsAvoidanceEnabledProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_Z), velocityZProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ALTITUDE), altitudeProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS), returnHomeFlightHeightProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.LIMIT_MAX_FLIGHT_HEIGHT_IN_METER), limitMaxFlightHeightInMeterProcessor)
        bindDataProcessor(RadarKey.create(RadarKey.RADAR_UPWARDS_OBSTACLE_AVOIDANCE_ENABLED), radarUpwardsObstacleAvoidanceEnabledProcessor)
        bindDataProcessor(RadarKey.create(RadarKey.RADAR_OBSTACLE_AVOIDANCE_STATE), radarObstacleAvoidanceStateProcessor)
        bindDataProcessor(FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_TAKE_OFF_ALTITUDE), rtkFusionTakeOffAltitudeProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION), aircraftLocationDataProcessor)
    }

    override fun inCleanup() {

    }
}