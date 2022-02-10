package dji.ux.beta.core.widget.hsi

import dji.common.flightcontroller.FlightWindWarning
import dji.common.flightcontroller.WindDirection
import dji.keysdk.FlightControllerKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class SpeedDisplayModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    val velocityXProcessor = DataProcessor.create(0f)
    val velocityYProcessor = DataProcessor.create(0f)
    val velocityZProcessor = DataProcessor.create(0f)
    val attitudePitchProcessor = DataProcessor.create(0.0)
    val attitudeRollProcessor = DataProcessor.create(0.0)
    val attitudeYawProcessor = DataProcessor.create(0.0)
    val windSpeedProcessor = DataProcessor.create(0)
    val windDirectionProcessor = DataProcessor.create(WindDirection.WINDLESS)
    val windWarningProcessor = DataProcessor.create(FlightWindWarning.UNKNOWN)

    override fun inSetup() {
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_X), velocityXProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_Y), velocityYProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_Z), velocityZProcessor)

        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH), attitudePitchProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL), attitudeRollProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW), attitudeYawProcessor)

        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.WIND_SPEED), windSpeedProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.WIND_DIRECTION), windDirectionProcessor)
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.FLIGHT_WIND_WARNING), windWarningProcessor)
    }

    override fun inCleanup() {

    }
}