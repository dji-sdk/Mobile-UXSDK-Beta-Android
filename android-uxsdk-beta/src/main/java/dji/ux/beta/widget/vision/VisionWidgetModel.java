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

package dji.ux.beta.widget.vision;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import java.util.HashMap;
import java.util.Map;

import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.flightcontroller.VisionDrawHeadingMode;
import dji.common.flightcontroller.VisionDrawStatus;
import dji.common.flightcontroller.VisionSensorPosition;
import dji.common.flightcontroller.VisionSystemWarning;
import dji.common.mission.activetrack.ActiveTrackMode;
import dji.common.mission.tapfly.TapFlyMode;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.sdk.mission.MissionControl;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.util.ProductUtil;
import dji.ux.beta.util.DataProcessor;

/**
 * Widget Model for the {@link VisionWidget} used to define
 * the underlying logic and communication
 */
public class VisionWidgetModel extends WidgetModel {

    //region Fields
    private Map<VisionSensorPosition, VisionSystemStatus> statusMap;
    private final DataProcessor<VisionDetectionState> visionDetectionStateProcessor;
    private final DataProcessor<Boolean> isUserAvoidEnabledProcessor;
    private final DataProcessor<FlightMode> flightModeProcessor;
    private final DataProcessor<ActiveTrackMode> trackingModeProcessor;
    private final DataProcessor<VisionDrawStatus> drawStatusProcessor;
    private final DataProcessor<VisionDrawHeadingMode> drawHeadingModeProcessor;
    private final DataProcessor<Boolean> isFrontRadarOpenProcessor;
    private final DataProcessor<Boolean> isBackRadarOpenProcessor;
    private final DataProcessor<Boolean> isLeftRadarOpenProcessor;
    private final DataProcessor<Boolean> isRightRadarOpenProcessor;
    private final DataProcessor<VisionSystemStatus> visionSystemStatusProcessor;
    private final DataProcessor<AvoidanceSensorStatus> avoidanceSensorStatusProcessor;
    private final MissionControl missionControl;
    //endregion

    //region Constructor
    public VisionWidgetModel(@NonNull DJISDKModel djiSdkModel,
                             @NonNull ObservableInMemoryKeyedStore keyedStore,
                             MissionControl missionControl) {
        super(djiSdkModel, keyedStore);
        statusMap = new HashMap<>();
        visionDetectionStateProcessor = DataProcessor.create(VisionDetectionState.createInstance(false,
                                                                                                 0,
                                                                                                 VisionSystemWarning.INVALID,
                                                                                                 null,
                                                                                                 VisionSensorPosition.UNKNOWN,
                                                                                                 false));
        isUserAvoidEnabledProcessor = DataProcessor.create(false);
        flightModeProcessor = DataProcessor.create(FlightMode.GPS_ATTI);
        trackingModeProcessor = DataProcessor.create(ActiveTrackMode.TRACE);
        drawStatusProcessor = DataProcessor.create(VisionDrawStatus.OTHER);
        drawHeadingModeProcessor = DataProcessor.create(VisionDrawHeadingMode.FORWARD);
        isFrontRadarOpenProcessor = DataProcessor.create(false);
        isBackRadarOpenProcessor = DataProcessor.create(false);
        isLeftRadarOpenProcessor = DataProcessor.create(false);
        isRightRadarOpenProcessor = DataProcessor.create(false);
        avoidanceSensorStatusProcessor = DataProcessor.create(AvoidanceSensorStatus.DISABLED);
        visionSystemStatusProcessor = DataProcessor.create(VisionSystemStatus.DISABLED);
        this.missionControl = missionControl;
    }
    //endregion

    //region Data

    /**
     * Get the status of the vision system
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<VisionSystemStatus> getVisionSystemStatus() {
        return visionSystemStatusProcessor.toFlowable();
    }

    /**
     * Get the status of the omnidirectional vision system
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<AvoidanceSensorStatus> getAvoidanceSensorStatus() {
        return avoidanceSensorStatusProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey visionDetectionStateKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE);
        DJIKey isUserAvoidEnabledKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE);
        DJIKey flightModeKey = FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE);
        DJIKey trackingModeKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE);
        DJIKey drawStatusKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DRAW_STATUS);
        DJIKey drawHeadingModeKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DRAW_HEADING_MODE);
        DJIKey isFrontRadarOpenKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN);
        DJIKey isBackRadarOpenKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN);
        DJIKey isLeftRadarOpenKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_LEFT_RADAR_OPEN);
        DJIKey isRightRadarOpenKey =
            FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_RIGHT_RADAR_OPEN);

        bindDataProcessor(visionDetectionStateKey,
                          visionDetectionStateProcessor,
                          visionDetectionState -> addSingleVisionStatus((VisionDetectionState) visionDetectionState));
        bindDataProcessor(isUserAvoidEnabledKey, isUserAvoidEnabledProcessor);
        bindDataProcessor(flightModeKey, flightModeProcessor);
        bindDataProcessor(trackingModeKey, trackingModeProcessor);
        bindDataProcessor(drawStatusKey, drawStatusProcessor);
        bindDataProcessor(drawHeadingModeKey, drawHeadingModeProcessor);
        bindDataProcessor(isFrontRadarOpenKey, isFrontRadarOpenProcessor);
        bindDataProcessor(isBackRadarOpenKey, isBackRadarOpenProcessor);
        bindDataProcessor(isLeftRadarOpenKey, isLeftRadarOpenProcessor);
        bindDataProcessor(isRightRadarOpenKey, isRightRadarOpenProcessor);
    }

    @Override
    protected void inCleanup() {
        // Nothing to clean
    }

    @Override
    protected void updateStates() {
        if (!isMavic2SeriesProduct()) {
            visionSystemStatusProcessor.onNext(getOverallVisionSystemStatus());
        } else {
            avoidanceSensorStatusProcessor.onNext(getAvoidanceSystemStatus());
        }
    }
    //endregion

    //region Helpers

    /**
     * A wrapper for the {@link ProductUtil#isMavic2SeriesProduct()} method so it can be mocked
     * in unit tests.
     *
     * @return `true` if the connected product is part of the Mavic 2 series. `false` if there is
     * no product connected or if the connected product is not part of the Mavic 2 series.
     */
    @VisibleForTesting
    protected boolean isMavic2SeriesProduct() {
        return ProductUtil.isMavic2SeriesProduct();
    }

    /**
     * A wrapper for the {@link ProductUtil#isMavic2Enterprise()} method so it can be mocked
     * in unit tests.
     *
     * @return `true` if the connected product is a Mavic 2 Enterprise. `false` if there is
     * no product connected or if the connected product is not a Mavic 2 Enterprise.
     */
    @VisibleForTesting
    protected boolean isMavic2Enterprise() {
        return ProductUtil.isMavic2Enterprise();
    }

    private void addSingleVisionStatus(VisionDetectionState state) {
        statusMap.put(state.getPosition(), getSingleVisionSystemStatus(state));
    }

    /**
     * Get the status of all of the vision sensors on the aircraft.
     *
     * @return The overall status of all vision sensors on the aircraft.
     */
    private VisionSystemStatus getOverallVisionSystemStatus() {
        VisionSystemStatus status = VisionSystemStatus.CLOSED;
        for (Map.Entry<VisionSensorPosition, VisionSystemStatus> entry : statusMap.entrySet()) {
            final VisionSystemStatus item = entry.getValue();
            if (item == VisionSystemStatus.NORMAL) {
                status = VisionSystemStatus.NORMAL;
            } else if (item == VisionSystemStatus.CLOSED) {
                status = VisionSystemStatus.CLOSED;
                break;
            } else {
                status = VisionSystemStatus.DISABLED;
                break;
            }
        }
        return status;
    }

    /**
     * Get the status of a single vision sensor on the aircraft based on its vision detection state.
     *
     * @param state The state of single vision sensor on the aircraft.
     * @return The status of the vision sensor.
     */
    private VisionSystemStatus getSingleVisionSystemStatus(VisionDetectionState state) {
        if (isUserAvoidEnabledProcessor.getValue()) {
            if (isVisionSystemEnabled() && !state.isDisabled()) {
                return VisionSystemStatus.NORMAL;
            } else {
                return VisionSystemStatus.DISABLED;
            }
        } else {
            return VisionSystemStatus.CLOSED;
        }
    }

    /**
     * Get the status of the omnidirectional vision system.
     *
     * @return The status of the omnidirectional vision system.
     */
    private AvoidanceSensorStatus getAvoidanceSystemStatus() {
        if (isMavic2Enterprise()) {
            if (isAllAvoidanceDataOpen()) {
                return AvoidanceSensorStatus.ALL;
            } else if (isNoseTailVisionNormal() || isNoseTailDataOpen()) {
                return AvoidanceSensorStatus.FRONT_BACK;
            }
        } else {
            if (getOverallVisionSystemStatus() == VisionSystemStatus.NORMAL && isAllAvoidanceDataOpen()) {
                return AvoidanceSensorStatus.ALL;
            } else if (isNoseTailVisionNormal() && isNoseTailDataOpen()) {
                return AvoidanceSensorStatus.FRONT_BACK;
            }
        }

        if (getOverallVisionSystemStatus() == VisionSystemStatus.CLOSED) {
            return AvoidanceSensorStatus.CLOSED;
        } else {
            return AvoidanceSensorStatus.DISABLED;
        }
    }

    private boolean isAllAvoidanceDataOpen() {
        return isFrontRadarOpenProcessor.getValue()
            && isBackRadarOpenProcessor.getValue()
            && isLeftRadarOpenProcessor.getValue()
            && isRightRadarOpenProcessor.getValue();
    }

    private boolean isNoseTailDataOpen() {
        return isFrontRadarOpenProcessor.getValue() && isBackRadarOpenProcessor.getValue();
    }

    private boolean isNoseTailVisionNormal() {
        return statusMap.get(VisionSensorPosition.NOSE) == VisionSystemStatus.NORMAL
            && statusMap.get(VisionSensorPosition.TAIL) == VisionSystemStatus.NORMAL;
    }

    /**
     * Whether the vision system is enabled. It could be disabled due to the flight mode,
     * tap mode, tracking mode, draw status, or hardware failure.
     *
     * @return `true` if the vision system is enabled, `false` otherwise.
     */
    private boolean isVisionSystemEnabled() {
        TapFlyMode tapMode = TapFlyMode.UNKNOWN;
        if (missionControl != null) {
            tapMode = missionControl.getTapFlyMissionOperator().getTapFlyMode();
        }

        return !isAttiMode(flightModeProcessor.getValue())
            && FlightMode.GPS_SPORT != flightModeProcessor.getValue()
            && FlightMode.AUTO_LANDING != flightModeProcessor.getValue()
            && (ActiveTrackMode.TRACE == trackingModeProcessor.getValue()
            || ActiveTrackMode.QUICK_SHOT == trackingModeProcessor.getValue())
            && TapFlyMode.FREE != tapMode
            && isDrawAssistanceEnabled(drawStatusProcessor.getValue(), drawHeadingModeProcessor.getValue());
    }

    /**
     * Whether draw assistance is enabled.
     *
     * @param status The vision draw status of the aircraft.
     * @param mode The heading mode of the camera.
     * @return `true` if draw assistance is enabled, `false` otherwise.
     */
    private boolean isDrawAssistanceEnabled(final VisionDrawStatus status, final VisionDrawHeadingMode mode) {
        boolean running = (VisionDrawStatus.START_AUTO == status
            || VisionDrawStatus.START_MANUAL == status
            || VisionDrawStatus.PAUSE == status);
        return (!running || VisionDrawHeadingMode.FORWARD == mode);
    }

    /**
     * Whether the given FlightMode is an attitude mode.
     *
     * @param state The aircraft's flight mode
     * @return `true` if the aircraft is in an attitude mode, `false` otherwise.
     */
    private boolean isAttiMode(final FlightMode state) {
        boolean ret = false;
        if (state == FlightMode.ATTI
            || state == FlightMode.ATTI_COURSE_LOCK
            || state == FlightMode.ATTI_HOVER
            || state == FlightMode.ATTI_LIMITED
            || state == FlightMode.ATTI_LANDING) {
            ret = true;
        }
        return ret;
    }
    //endregion

    //region States

    /**
     * The status of the vision system.
     */
    public enum VisionSystemStatus {

        /**
         * Obstacle avoidance is disabled by the user.
         */
        CLOSED,

        /**
         * The vision system is not available. This could be due to the flight mode, tap mode,
         * tracking mode, draw status, or hardware failure.
         */
        DISABLED,

        /**
         * The vision system is functioning normally.
         */
        NORMAL
    }

    /**
     * The status of the omnidirectional obstacle avoidance sensors.
     */
    public enum AvoidanceSensorStatus {

        /**
         * All vision systems are available.
         */
        ALL,

        /**
         * Only forward and backward vision systems are available. Left and right vision
         * systems are only available in ActiveTrack mode and Tripod Mode.
         */
        FRONT_BACK,

        /**
         * The vision system is not available. This could be due to the flight mode, tap mode,
         * tracking mode, draw status, or hardware failure.
         */
        DISABLED,

        /**
         * Obstacle avoidance is disabled by the user.
         */
        CLOSED
    }
    //endregion
}
