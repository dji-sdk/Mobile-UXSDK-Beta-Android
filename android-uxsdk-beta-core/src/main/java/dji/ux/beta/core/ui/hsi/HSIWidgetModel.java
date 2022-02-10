package dji.ux.beta.core.ui.hsi;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.adsb.AirSenseAirplaneState;
import dji.common.flightcontroller.adsb.AirSenseWarningLevel;
import dji.common.flightcontroller.flightassistant.ObstacleAvoidanceSensorState;
import dji.common.flightcontroller.flightassistant.PerceptionInformation;
import dji.common.gimbal.Attitude;
import dji.common.model.LocationCoordinate2D;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.GimbalKey;
import dji.keysdk.RadarKey;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

public class HSIWidgetModel extends WidgetModel {

    public DataProcessor<LocationCoordinate3D> aircraftLocationDataProcessor = DataProcessor.create(new LocationCoordinate3D(LocationCoordinate2D.UNKNOWN, LocationCoordinate2D.UNKNOWN, -1f));
    public DataProcessor<LocationCoordinate2D> homeLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(LocationCoordinate2D.UNKNOWN, LocationCoordinate2D.UNKNOWN));
    public DataProcessor<AirSenseWarningLevel> airSenseWarningLevelProcessor = DataProcessor.create(AirSenseWarningLevel.UNKNOWN);
    public DataProcessor<AirSenseAirplaneState []> airSenseAirplaneStatesProcessor = DataProcessor.create(new AirSenseAirplaneState[0]);
    public DataProcessor<PerceptionInformation> perceptionTOFDistanceProcessor = DataProcessor.create(new PerceptionInformation.Builder().everyAngleDistance(new int[0]).build());
    public DataProcessor<PerceptionInformation> perceptionFullDistanceProcessor = DataProcessor.create(new PerceptionInformation.Builder().everyAngleDistance(new int[0]).build());
    public DataProcessor<ObstacleAvoidanceSensorState> obstacleAvoidanceSensorStateProcessor = DataProcessor.create(new ObstacleAvoidanceSensorState.Builder().build());
    public DataProcessor<Boolean> omniHorizontalAvoidanceEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<Float> omniHorizontalRadarDistanceProcessor = DataProcessor.create(0F);
    public DataProcessor<Float> horizontalAvoidanceDistanceProcessor = DataProcessor.create(0F);
    public DataProcessor<Boolean> radarConnectionProcessor = DataProcessor.create(false);
    public DataProcessor<Boolean> radarHorizontalObstacleAvoidanceEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<PerceptionInformation> radarObstacleAvoidanceStateProcessor = DataProcessor.create(new PerceptionInformation.Builder().build());
    public DataProcessor<FlightMode> flightModeProcessor = DataProcessor.create(FlightMode.UNKNOWN);
    public DataProcessor<Boolean> multipleFlightModeEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<Float> velocityXProcessor = DataProcessor.create(0F);
    public DataProcessor<Float> velocityYProcessor = DataProcessor.create(0F);
    public DataProcessor<Float> velocityZProcessor = DataProcessor.create(0F);
    public DataProcessor<Double> attitudePitchProcessor = DataProcessor.create(0D);
    public DataProcessor<Double> attitudeRollProcessor= DataProcessor.create(0D);
    public DataProcessor<Double> attitudeYawProcessor = DataProcessor.create(0D);

    public List<DataProcessor<Boolean>> gimbalConnectionProcessorList = new ArrayList<>();
    private DataProcessor<Boolean> gimbalConnection0Processor = DataProcessor.create(false);
    private DataProcessor<Boolean> gimbalConnection1Processor = DataProcessor.create(false);
    private DataProcessor<Boolean> gimbalConnection2Processor = DataProcessor.create(false);

    public List<DataProcessor<Attitude>> gimbalAttitudeInDegreesProcessorList = new ArrayList<>();
    private DataProcessor<Attitude> gimbalAttitudeInDegrees0Processor = DataProcessor.create(new Attitude(Attitude.NO_ROTATION,Attitude.NO_ROTATION,Attitude.NO_ROTATION));
    private DataProcessor<Attitude> gimbalAttitudeInDegrees1Processor = DataProcessor.create(new Attitude(Attitude.NO_ROTATION,Attitude.NO_ROTATION,Attitude.NO_ROTATION));
    private DataProcessor<Attitude> gimbalAttitudeInDegrees2Processor = DataProcessor.create(new Attitude(Attitude.NO_ROTATION,Attitude.NO_ROTATION,Attitude.NO_ROTATION));

    public HSIWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        // HSIMarkerLayer
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION), aircraftLocationDataProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.HOME_LOCATION), homeLocationDataProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL), airSenseWarningLevelProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES), airSenseAirplaneStatesProcessor);

        //HSIPerceptionLayer
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.PERCEPTION_TOF_FULL_DISTANCE), perceptionTOFDistanceProcessor);
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_FULL_DISTANCE), perceptionFullDistanceProcessor);
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE), obstacleAvoidanceSensorStateProcessor);
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_ENABLED), omniHorizontalAvoidanceEnabledProcessor);
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE), omniHorizontalRadarDistanceProcessor);
        bindDataProcessor(FlightControllerKey.createFlightAssistantKey(FlightControllerKey.HORIZONTAL_AVOIDANCE_DISTANCE), horizontalAvoidanceDistanceProcessor);
        bindDataProcessor(RadarKey.create(RadarKey.CONNECTION), radarConnectionProcessor);
        bindDataProcessor(RadarKey.create(RadarKey.RADAR_HORIZONTAL_OBSTACLE_AVOIDANCE_ENABLED), radarHorizontalObstacleAvoidanceEnabledProcessor);
        bindDataProcessor(RadarKey.create(RadarKey.RADAR_OBSTACLE_AVOIDANCE_STATE), radarObstacleAvoidanceStateProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE), flightModeProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.MULTI_MODE_OPEN), multipleFlightModeEnabledProcessor);

        //HSIView
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_X), velocityXProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_Y), velocityYProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.VELOCITY_Z), velocityZProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH), attitudePitchProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL), attitudeRollProcessor);
        bindDataProcessor(FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW), attitudeYawProcessor);

        bindDataProcessor(GimbalKey.create(GimbalKey.CONNECTION, SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex()), gimbalConnection0Processor);
        bindDataProcessor(GimbalKey.create(GimbalKey.CONNECTION, SettingDefinitions.CameraIndex.CAMERA_INDEX_1.getIndex()), gimbalConnection1Processor);
        bindDataProcessor(GimbalKey.create(GimbalKey.CONNECTION, SettingDefinitions.CameraIndex.CAMERA_INDEX_2.getIndex()), gimbalConnection2Processor);
        gimbalConnectionProcessorList.add(gimbalConnection0Processor);
        gimbalConnectionProcessorList.add(gimbalConnection1Processor);
        gimbalConnectionProcessorList.add(gimbalConnection2Processor);

        bindDataProcessor(GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES, SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex()), gimbalAttitudeInDegrees0Processor);
        bindDataProcessor(GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES, SettingDefinitions.CameraIndex.CAMERA_INDEX_1.getIndex()), gimbalAttitudeInDegrees1Processor);
        bindDataProcessor(GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES, SettingDefinitions.CameraIndex.CAMERA_INDEX_2.getIndex()), gimbalAttitudeInDegrees2Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees0Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees1Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees2Processor);
    }

    @Override
    protected void inCleanup() {
        gimbalConnectionProcessorList.clear();
        gimbalAttitudeInDegreesProcessorList.clear();
    }
}
