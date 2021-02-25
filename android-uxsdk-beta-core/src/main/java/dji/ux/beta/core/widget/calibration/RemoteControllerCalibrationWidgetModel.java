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

package dji.ux.beta.core.widget.calibration;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import dji.common.Stick;
import dji.common.remotecontroller.CalibrationState;
import dji.common.remotecontroller.HardwareState;
import dji.common.remotecontroller.ProfessionalRC;
import dji.keysdk.DJIKey;
import dji.keysdk.ProductKey;
import dji.keysdk.RemoteControllerKey;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

import static dji.ux.beta.core.widget.calibration.CalibrationSquareView.MAX_CALIBRATION_STATUS;

/**
 * Widget Model for the {@link RemoteControllerCalibrationWidget} used to define
 * the underlying logic and communication.
 */
public class RemoteControllerCalibrationWidgetModel extends WidgetModel {

    //region Constants
    private static final int MAX_POSITION = 660;
    private static final int DEFAULT_THRESHOLD = 15;
    private static final int MAVIC_THRESHOLD = 99;
    private static final String TAG = "RCCalWidgetModel";
    //endregion

    //region data input
    private final DataProcessor<Stick> leftStickProcessor;
    private final DataProcessor<Stick> rightStickProcessor;
    private final DataProcessor<Integer> leftDialProcessor;
    private final DataProcessor<HardwareState.RightDial> rightDialProcessor;
    private final DataProcessor<ProfessionalRC.Event> proButtonProcessor;
    private final DataProcessor<Boolean> productConnectionProcessor;
    private final DataProcessor<String> remoteControllerDisplayNameProcessor;
    private final DataProcessor<Integer> aStatusProcessor;
    private final DataProcessor<Integer> bStatusProcessor;
    private final DataProcessor<Integer> cStatusProcessor;
    private final DataProcessor<Integer> dStatusProcessor;
    private final DataProcessor<Integer> eStatusProcessor;
    private final DataProcessor<Integer> fStatusProcessor;
    private final DataProcessor<Integer> gStatusProcessor;
    private final DataProcessor<Integer> hStatusProcessor;
    //endregion

    //region data output
    private final DataProcessor<StickPosition> leftStickPositionProcessor;
    private final DataProcessor<StickPosition> rightStickPositionProcessor;
    private final DataProcessor<DialPosition> leftDialPositionProcessor;
    private final DataProcessor<DialPosition> rightDialPositionProcessor;
    private final DataProcessor<LeverPosition> leftLeverPositionProcessor;
    private final DataProcessor<LeverPosition> rightLeverPositionProcessor;
    private final DataProcessor<ConnectionState> connectionStateProcessor;
    private final DataProcessor<CalibrationState> calibrationStateProcessor;
    private final DataProcessor<Boolean> calibrationProgressProcessor;
    private final DataProcessor<CalibrationType> calibrationTypeProcessor;
    private final DataProcessor<MavicStep> mavicStepProcessor;
    private final DataProcessor<StickCalibrationStatus> leftStickCalibrationStatusProcessor;
    private final DataProcessor<StickCalibrationStatus> rightStickCalibrationStatusProcessor;
    //endregion

    //region keys
    private DJIKey calibrationStateKey;
    //endregion

    //region Fields
    private boolean hasRightDial;
    private boolean hasLevers;
    private CalibrationProgress progress;
    private int threshold = DEFAULT_THRESHOLD;
    //endregion

    //region Constructor
    public RemoteControllerCalibrationWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                                  @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        leftStickProcessor = DataProcessor.create(new Stick(0, 0));
        rightStickProcessor = DataProcessor.create(new Stick(0, 0));
        leftDialProcessor = DataProcessor.create(0);
        rightDialProcessor = DataProcessor.create(new HardwareState.RightDial());
        proButtonProcessor = DataProcessor.create(new ProfessionalRC.Event(ProfessionalRC.ButtonAction.OTHER));
        productConnectionProcessor = DataProcessor.create(false);
        remoteControllerDisplayNameProcessor = DataProcessor.create("");
        aStatusProcessor = DataProcessor.create(0);
        bStatusProcessor = DataProcessor.create(0);
        cStatusProcessor = DataProcessor.create(0);
        dStatusProcessor = DataProcessor.create(0);
        eStatusProcessor = DataProcessor.create(0);
        fStatusProcessor = DataProcessor.create(0);
        gStatusProcessor = DataProcessor.create(0);
        hStatusProcessor = DataProcessor.create(0);

        leftStickPositionProcessor = DataProcessor.create(new StickPosition(new Stick(0, 0)));
        rightStickPositionProcessor = DataProcessor.create(new StickPosition(new Stick(0, 0)));
        leftDialPositionProcessor = DataProcessor.create(new DialPosition(0));
        rightDialPositionProcessor = DataProcessor.create(new DialPosition(0));
        leftLeverPositionProcessor = DataProcessor.create(new LeverPosition(0));
        rightLeverPositionProcessor = DataProcessor.create(new LeverPosition(0));
        connectionStateProcessor = DataProcessor.create(ConnectionState.DISCONNECTED);
        calibrationStateProcessor = DataProcessor.create(CalibrationState.NORMAL);
        calibrationProgressProcessor = DataProcessor.create(false);
        calibrationTypeProcessor = DataProcessor.create(CalibrationType.DEFAULT);
        mavicStepProcessor = DataProcessor.create(MavicStep.STICK);
        leftStickCalibrationStatusProcessor = DataProcessor.create(new StickCalibrationStatus(0, 0, 0, 0));
        rightStickCalibrationStatusProcessor = DataProcessor.create(new StickCalibrationStatus(0, 0, 0, 0));

        progress = new CalibrationProgress();
    }
    //endregion

    //region Data

    /**
     * Get the left joystick position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<StickPosition> getLeftStick() {
        return leftStickPositionProcessor.toFlowable();
    }

    /**
     * Get the right joystick position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<StickPosition> getRightStick() {
        return rightStickPositionProcessor.toFlowable();
    }

    /**
     * Get the left dial position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<DialPosition> getLeftDial() {
        return leftDialPositionProcessor.toFlowable();
    }

    /**
     * Get the right dial position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<DialPosition> getRightDial() {
        return rightDialPositionProcessor.toFlowable();
    }

    /**
     * Get the left lever position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<LeverPosition> getLeftLever() {
        return leftLeverPositionProcessor.toFlowable();
    }

    /**
     * Get the right lever position.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<LeverPosition> getRightLever() {
        return rightLeverPositionProcessor.toFlowable();
    }

    /**
     * Get the connection state of the remote controller and aircraft.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<ConnectionState> getConnectionState() {
        return connectionStateProcessor.toFlowable();
    }

    /**
     * Get the current calibration state.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CalibrationState> getCalibrationState() {
        return calibrationStateProcessor.toFlowable();
    }

    /**
     * Get the current progress of the calibration.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> getCalibrationProgress() {
        return calibrationProgressProcessor.toFlowable();
    }

    /**
     * Get the type of calibration.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CalibrationType> getCalibrationType() {
        return calibrationTypeProcessor.toFlowable();
    }

    /**
     * Get the current step of the Mavic calibration flow.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<MavicStep> getMavicStep() {
        return mavicStepProcessor.toFlowable();
    }

    /**
     * Get the current calibration status of the left Mavic joystick.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<StickCalibrationStatus> getLeftStickCalibrationStatus() {
        return leftStickCalibrationStatusProcessor.toFlowable();
    }

    /**
     * Get the current calibration status of the right Mavic joystick.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<StickCalibrationStatus> getRightStickCalibrationStatus() {
        return rightStickCalibrationStatusProcessor.toFlowable();
    }

    /**
     * Get whether the connected remote controller has a right dial.
     *
     * @return `true` if the connected remote controller has a right dial, `false` otherwise.
     */
    public boolean hasRightDial() {
        return hasRightDial;
    }

    /**
     * Get whether the connected remote controller has levers.
     *
     * @return `true` if the connected remote controller has levers, `false` otherwise.
     */
    public boolean hasLevers() {
        return hasLevers;
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey leftStickKey = RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE);
        DJIKey rightStickKey = RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE);
        DJIKey leftDialKey = RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL);
        DJIKey rightDialKey = RemoteControllerKey.create(RemoteControllerKey.RIGHT_DIAL);
        DJIKey buttonEventKey = RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC);
        DJIKey productConnectionKey = ProductKey.create(ProductKey.CONNECTION);
        DJIKey remoteControllerDisplayNameKey = RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME);
        DJIKey aStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_A_AXIS_STATUS);
        DJIKey bStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_B_AXIS_STATUS);
        DJIKey cStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_C_AXIS_STATUS);
        DJIKey dStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_D_AXIS_STATUS);
        DJIKey eStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_E_AXIS_STATUS);
        DJIKey fStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_F_AXIS_STATUS);
        DJIKey gStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_G_AXIS_STATUS);
        DJIKey hStatusKey = RemoteControllerKey.create(RemoteControllerKey.REMOTE_CONTROLLER_CALIBRATION_H_AXIS_STATUS);
        calibrationStateKey = RemoteControllerKey.create(RemoteControllerKey.CALIBRATION_STATE);

        bindDataProcessor(leftStickKey, leftStickProcessor);
        bindDataProcessor(rightStickKey, rightStickProcessor);
        bindDataProcessor(leftDialKey, leftDialProcessor);
        bindDataProcessor(rightDialKey, rightDialProcessor, this::setDialValue);
        bindDataProcessor(buttonEventKey, proButtonProcessor, this::setDialValue);
        bindDataProcessor(productConnectionKey, productConnectionProcessor, newValue -> onProductConnectionChanged());
        bindDataProcessor(remoteControllerDisplayNameKey, remoteControllerDisplayNameProcessor, displayName -> onDisplayNameUpdated((String) displayName));
        bindDataProcessor(aStatusKey, aStatusProcessor);
        bindDataProcessor(bStatusKey, bStatusProcessor);
        bindDataProcessor(cStatusKey, cStatusProcessor);
        bindDataProcessor(dStatusKey, dStatusProcessor);
        bindDataProcessor(eStatusKey, eStatusProcessor);
        bindDataProcessor(fStatusKey, fStatusProcessor);
        bindDataProcessor(gStatusKey, gStatusProcessor);
        bindDataProcessor(hStatusKey, hStatusProcessor);
    }

    @Override
    protected void inCleanup() {
        // Nothing to clean
    }

    @Override
    protected void updateStates() {
        updateStickCalibrationStatus();
        updateLeftStick();
        updateRightStick();
        updateLeftDial(leftDialProcessor.getValue());
        updateProgress();
    }
    //endregion

    //region Helpers

    /**
     * A wrapper for the {@link DJISDKManager#getProduct()} method so it can be mocked
     * in unit tests.
     *
     * @return The connected product.
     */
    @VisibleForTesting
    protected BaseProduct getProduct() {
        return DJISDKManager.getInstance().getProduct();
    }

    private void updateLeftStick() {
        StickPosition leftStickPosition = new StickPosition(leftStickProcessor.getValue());
        if (calibrationTypeProcessor.getValue() == CalibrationType.DEFAULT) {
            if (leftStickPosition.getLeft() > threshold) progress.leftStickLeftLimitRecorded = true;
            if (leftStickPosition.getRight() > threshold) progress.leftStickRightLimitRecorded = true;
            if (leftStickPosition.getTop() > threshold) progress.leftStickTopLimitRecorded = true;
            if (leftStickPosition.getBottom() > threshold) progress.leftStickBottomLimitRecorded = true;
        } else {
            StickCalibrationStatus leftStickCalibrationStatus = leftStickCalibrationStatusProcessor.getValue();
            if (leftStickCalibrationStatus.getLeftSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.leftStickLeftLimitRecorded = true;
            if (leftStickCalibrationStatus.getRightSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.leftStickRightLimitRecorded = true;
            if (leftStickCalibrationStatus.getTopSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.leftStickTopLimitRecorded = true;
            if (leftStickCalibrationStatus.getBottomSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.leftStickBottomLimitRecorded = true;
        }
        leftStickPositionProcessor.onNext(leftStickPosition);
    }

    private void updateRightStick() {
        StickPosition rightStickPosition = new StickPosition(rightStickProcessor.getValue());
        if (calibrationTypeProcessor.getValue() == CalibrationType.DEFAULT) {
            if (rightStickPosition.getLeft() > threshold) progress.rightStickLeftLimitRecorded = true;
            if (rightStickPosition.getRight() > threshold) progress.rightStickRightLimitRecorded = true;
            if (rightStickPosition.getTop() > threshold) progress.rightStickTopLimitRecorded = true;
            if (rightStickPosition.getBottom() > threshold) progress.rightStickBottomLimitRecorded = true;
        } else {
            StickCalibrationStatus rightStickCalibrationStatus = rightStickCalibrationStatusProcessor.getValue();
            if (rightStickCalibrationStatus.getLeftSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.rightStickLeftLimitRecorded = true;
            if (rightStickCalibrationStatus.getRightSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.rightStickRightLimitRecorded = true;
            if (rightStickCalibrationStatus.getTopSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.rightStickTopLimitRecorded = true;
            if (rightStickCalibrationStatus.getBottomSegmentFillStatus() == MAX_CALIBRATION_STATUS) progress.rightStickBottomLimitRecorded = true;
        }
        rightStickPositionProcessor.onNext(rightStickPosition);
    }

    private void updateLeftDial(int value) {
        if (calibrationTypeProcessor.getValue() == CalibrationType.MAVIC && mavicStepProcessor.getValue() == MavicStep.STICK) {
            return;
        }

        DialPosition leftDialPosition = new DialPosition(value);
        if (leftDialPosition.getLeft() > threshold) progress.leftDialLeftLimitRecorded = true;
        if (leftDialPosition.getRight() > threshold) progress.leftDialRightLimitRecorded = true;
        leftDialPositionProcessor.onNext(leftDialPosition);
    }

    private void updateRightDial(int value) {
        DialPosition rightDialPosition = new DialPosition(value);
        if (rightDialPosition.getLeft() > threshold) progress.rightDialLeftLimitRecorded = true;
        if (rightDialPosition.getRight() > threshold) progress.rightDialRightLimitRecorded = true;
        rightDialPositionProcessor.onNext(rightDialPosition);
    }

    private void updateLeftLever(int value) {
        LeverPosition leftLeverPosition = new LeverPosition(value);
        if (leftLeverPosition.getTop() > threshold) progress.leftLeverTopLimitRecorded = true;
        if (leftLeverPosition.getBottom() > threshold) progress.leftLeverBottomLimitRecorded = true;
        leftLeverPositionProcessor.onNext(leftLeverPosition);
    }

    private void updateRightLever(int value) {
        LeverPosition rightLeverPosition = new LeverPosition(value);
        if (rightLeverPosition.getTop() > threshold) progress.rightLeverTopLimitRecorded = true;
        if (rightLeverPosition.getBottom() > threshold) progress.rightLeverBottomLimitRecorded = true;
        rightLeverPositionProcessor.onNext(rightLeverPosition);
    }

    private void updateProgress() {
        if (progress.isComplete(calibrationTypeProcessor.getValue(), mavicStepProcessor.getValue(),
                hasLevers, hasRightDial)) {
            calibrationProgressProcessor.onNext(true);
        } else {
            calibrationProgressProcessor.onNext(false);
        }
    }

    private void setDialValue(Object newValue) {
        if (newValue instanceof HardwareState.RightDial) {
            int rightDialValue = ((HardwareState.RightDial) newValue).getValue();
            updateRightDial(rightDialValue);
        } else if (newValue instanceof ProfessionalRC.Event) {
            ProfessionalRC.Event event = (ProfessionalRC.Event) newValue;
            switch (event.getCustomizableButton()) {
                case RW:
                    updateRightDial(event.getCurrentValue());
                    break;
                case LW:
                    updateLeftDial(event.getCurrentValue());
                    break;
                case RS:
                    updateRightLever(event.getCurrentValue());
                    break;
                case LS:
                    updateLeftLever(event.getCurrentValue());
                    break;
            }
        }
    }

    private void onDisplayNameUpdated(String displayName) {
        if (RemoteController.DisplayNameCendence.equals(displayName)
                || RemoteController.DisplayNameCendenceSDR.equals(displayName)) {
            hasRightDial = true;
            hasLevers = true;
        } else if (RemoteController.DisplayNameMavic2.equals(displayName)
                || RemoteController.DisplayNameMavic2Enterprise.equals(displayName)
                || RemoteController.DisplayNameDJISmartController.equals(displayName)) {
            hasRightDial = true;
            hasLevers = false;
        } else {
            hasRightDial = false;
            hasLevers = false;
        }
        if (displayName.equals(RemoteController.DisplayNameMavicPro)
                || displayName.equals(RemoteController.DisplayNameSpark)) {
            threshold = MAVIC_THRESHOLD;
            calibrationTypeProcessor.onNext(CalibrationType.MAVIC);
        }
        onProductConnectionChanged();
    }

    private void onProductConnectionChanged() {
        BaseProduct product = getProduct();
        if (product != null) {
            if (product instanceof Aircraft) {
                Aircraft aircraft = (Aircraft) product;
                if (aircraft.isConnected()) {
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        connectionStateProcessor.onNext(ConnectionState.AIRCRAFT_AND_RC);
                    } else {
                        connectionStateProcessor.onNext(ConnectionState.AIRCRAFT_ONLY);
                    }
                } else if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                    connectionStateProcessor.onNext(ConnectionState.RC_ONLY);
                } else {
                    connectionStateProcessor.onNext(ConnectionState.DISCONNECTED);
                }
            } else {
                // Osmo
                connectionStateProcessor.onNext(ConnectionState.OTHER);
            }
        } else {
            //No product connected
            connectionStateProcessor.onNext(ConnectionState.DISCONNECTED);
        }
    }

    private void updateStickCalibrationStatus() {
        if (calibrationStateProcessor.getValue() == CalibrationState.LIMITS) {
            StickCalibrationStatus leftStickCalibrationStatus = new StickCalibrationStatus(
                    eStatusProcessor.getValue(),
                    fStatusProcessor.getValue(),
                    gStatusProcessor.getValue(),
                    hStatusProcessor.getValue());
            leftStickCalibrationStatusProcessor.onNext(leftStickCalibrationStatus);

            StickCalibrationStatus rightStickCalibrationStatus = new StickCalibrationStatus(
                    aStatusProcessor.getValue(),
                    bStatusProcessor.getValue(),
                    cStatusProcessor.getValue(),
                    dStatusProcessor.getValue());
            rightStickCalibrationStatusProcessor.onNext(rightStickCalibrationStatus);
        }
    }
    //endregion

    //region User Interaction

    /**
     * Updates the current {@link MavicStep} or {@link CalibrationState}.
     *
     * @return Completable representing the state of the action. The completable will return an
     * error state if the current connection state is not {@link ConnectionState#RC_ONLY}, or if
     * an error is encountered while switching to the next {@link CalibrationState}.
     */
    @NonNull
    public Completable nextCalibrationState() {
        if (connectionStateProcessor.getValue() != ConnectionState.RC_ONLY) {
            if (connectionStateProcessor.getValue() == ConnectionState.AIRCRAFT_AND_RC) {
                return Completable.error(new Throwable("Disconnect aircraft before calibrating remote controller"));
            } else {
                return Completable.error(new Throwable("No remote controller detected"));
            }
        }

        final CalibrationState currentCalibrationState = calibrationStateProcessor.getValue();
        if (calibrationTypeProcessor.getValue() == CalibrationType.MAVIC &&
                currentCalibrationState == CalibrationState.LIMITS) {
            if (mavicStepProcessor.getValue() == MavicStep.STICK) {
                mavicStepProcessor.onNext(MavicStep.DIAL);
                progress.reset();
                updateProgress();
                return Completable.complete(); // stay in LIMITS state
            } else {
                mavicStepProcessor.onNext(MavicStep.STICK);
            }
        }

        CalibrationState next;
        switch (currentCalibrationState) {
            case MIDDLE:
                next = CalibrationState.LIMITS;
                break;
            case LIMITS:
                progress.reset();
                updateProgress();
                next = CalibrationState.QUIT;
                break;
            default:
                next = CalibrationState.MIDDLE;
                break;
        }
        calibrationStateProcessor.onNext(next);
        return djiSdkModel.setValue(calibrationStateKey, next)
                .doOnComplete(() -> {
                    DJILog.d(TAG, "calibration state set to " + next);
                    if (calibrationStateProcessor.getValue() == CalibrationState.QUIT) {
                        calibrationStateProcessor.onNext(CalibrationState.NORMAL);
                    }
                })
                .doOnError(throwable -> {
                    DJILog.d(TAG, "calibration state setting failed, reverting to " + currentCalibrationState);
                    calibrationStateProcessor.onNext(currentCalibrationState);
                });
    }
    //endregion

    //region States

    /**
     * The connection state of the aircraft and remote controller.
     */
    public enum ConnectionState {
        /**
         * The aircraft is connected to the device without a remote controller.
         */
        AIRCRAFT_ONLY,
        /**
         * The aircraft and its paired remote controller are both connected to the device.
         */
        AIRCRAFT_AND_RC,
        /**
         * The remote controller is connected to the device without an aircraft.
         */
        RC_ONLY,
        /**
         * No remote controller or aircraft is connected to the device.
         */
        DISCONNECTED,
        /**
         * A product other than an aircraft or a remote controller is connected to the device.
         */
        OTHER
    }

    /**
     * The calibration type for the connected remote controller.
     */
    public enum CalibrationType {
        /**
         * The connected remote controller has Mavic calibration.
         */
        MAVIC,
        /**
         * The connected remote controller has default calibration.
         */
        DEFAULT
    }

    /**
     * The step in the Mavic calibration flow.
     */
    public enum MavicStep {
        /**
         * The remote controller is calibrating its joysticks.
         */
        STICK,
        /**
         * The remote controller is calibrating its dial.
         */
        DIAL
    }

    /**
     * The current position of a joystick.
     */
    public static class StickPosition {
        private int left;
        private int top;
        private int right;
        private int bottom;

        private StickPosition(Stick stick) {
            int horizontal = stick.getHorizontalPosition() * 100 / MAX_POSITION;
            int vertical = stick.getVerticalPosition() * 100 / MAX_POSITION;

            left = horizontal < 0 ? -horizontal : 0;
            top = vertical > 0 ? vertical : 0;
            right = horizontal > 0 ? horizontal : 0;
            bottom = vertical < 0 ? -vertical : 0;
        }

        /**
         * Get the percentage of distance the joystick has traveled toward the left maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getLeft() {
            return left;
        }

        /**
         * Get the percentage of distance the joystick has traveled toward the top maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getTop() {
            return top;
        }

        /**
         * Get the percentage of distance the joystick has traveled toward the right maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getRight() {
            return right;
        }

        /**
         * Get the percentage of distance the joystick has traveled toward the bottom maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getBottom() {
            return bottom;
        }
    }

    /**
     * The current position of a dial.
     */
    public static class DialPosition {
        private int left;
        private int right;

        private DialPosition(int value) {
            int ratio = value * 100 / MAX_POSITION;
            left = ratio < 0 ? -ratio : 0;
            right = ratio > 0 ? ratio : 0;
        }

        /**
         * Get the percentage of distance the dial has traveled toward the left maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getLeft() {
            return left;
        }

        /**
         * Get the percentage of distance the dial has traveled toward the right maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getRight() {
            return right;
        }
    }

    /**
     * The current position of a lever.
     */
    public static class LeverPosition {
        private int top;
        private int bottom;

        private LeverPosition(int value) {
            int ratio = value * 100 / MAX_POSITION;
            top = ratio < 0 ? -ratio : 0;
            bottom = ratio > 0 ? ratio : 0;
        }

        /**
         * Get the percentage of distance the lever has traveled toward the top maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getTop() {
            return top;
        }

        /**
         * Get the percentage of distance the lever has traveled toward the bottom maximum.
         *
         * @return A percentage value from 0 to 100.
         */
        @IntRange(from = 0, to = 100)
        public int getBottom() {
            return bottom;
        }
    }

    private static class CalibrationProgress {
        private boolean leftStickLeftLimitRecorded;
        private boolean leftStickRightLimitRecorded;
        private boolean leftStickBottomLimitRecorded;
        private boolean leftStickTopLimitRecorded;
        private boolean rightStickLeftLimitRecorded;
        private boolean rightStickRightLimitRecorded;
        private boolean rightStickBottomLimitRecorded;
        private boolean rightStickTopLimitRecorded;
        private boolean leftDialLeftLimitRecorded;
        private boolean leftDialRightLimitRecorded;
        private boolean rightDialLeftLimitRecorded;
        private boolean rightDialRightLimitRecorded;
        private boolean leftLeverTopLimitRecorded;
        private boolean leftLeverBottomLimitRecorded;
        private boolean rightLeverTopLimitRecorded;
        private boolean rightLeverBottomLimitRecorded;

        private void reset() {
            leftStickLeftLimitRecorded = false;
            leftStickRightLimitRecorded = false;
            leftStickBottomLimitRecorded = false;
            leftStickTopLimitRecorded = false;
            rightStickLeftLimitRecorded = false;
            rightStickRightLimitRecorded = false;
            rightStickBottomLimitRecorded = false;
            rightStickTopLimitRecorded = false;
            leftDialLeftLimitRecorded = false;
            leftDialRightLimitRecorded = false;
            rightDialLeftLimitRecorded = false;
            rightDialRightLimitRecorded = false;
            leftLeverTopLimitRecorded = false;
            leftLeverBottomLimitRecorded = false;
            rightLeverTopLimitRecorded = false;
            rightLeverBottomLimitRecorded = false;
        }

        private boolean isComplete(CalibrationType calibrationType, MavicStep mavicStep,
                                   boolean hasLevers, boolean hasRightDial) {
            boolean sticksComplete = leftStickLeftLimitRecorded &&
                    leftStickRightLimitRecorded &&
                    leftStickBottomLimitRecorded &&
                    leftStickTopLimitRecorded &&
                    rightStickLeftLimitRecorded &&
                    rightStickRightLimitRecorded &&
                    rightStickBottomLimitRecorded &&
                    rightStickTopLimitRecorded;
            boolean leftDialComplete = leftDialLeftLimitRecorded && leftDialRightLimitRecorded;
            boolean rightDialComplete = rightDialLeftLimitRecorded && rightDialRightLimitRecorded;
            boolean dialsComplete = leftLeverTopLimitRecorded &&
                    leftLeverBottomLimitRecorded &&
                    rightLeverTopLimitRecorded &&
                    rightLeverBottomLimitRecorded;

            if (calibrationType == CalibrationType.MAVIC) {
                if (mavicStep == MavicStep.STICK) {
                    return sticksComplete;
                } else {
                    return leftDialComplete;
                }
            } else {
                if (hasLevers) {
                    return sticksComplete && leftDialComplete && rightDialComplete && dialsComplete;
                } else if (hasRightDial) {
                    return sticksComplete && leftDialComplete && rightDialComplete;
                } else {
                    return sticksComplete && leftDialComplete;
                }
            }
        }
    }

    /**
     * Each of the lines of this square are separated into several segments.
     * The number of the segments could be get by method getNumberOfSegments(). Normally
     * the number would be set as 15.
     * <p>
     * The segment is kind of reflecting the progress of the remote controller calibration.
     * Once all of the segment of the line are filled up, that means that direction has
     * been already calibrated. The data structure for representing the filled up
     * segment are as follow:
     * <p>
     * For example, getTopSegmentFillStatus() might return 000010000000001 which would mean that
     * the 1st and 11th segment of the top side of the square are filled up.
     */
    public static class StickCalibrationStatus {
        private int topSegmentFillStatus;
        private int bottomSegmentFillStatus;
        private int rightSegmentFillStatus;
        private int leftSegmentFillStatus;

        private StickCalibrationStatus(int top, int bottom, int right, int left) {
            topSegmentFillStatus = top;
            bottomSegmentFillStatus = bottom;
            rightSegmentFillStatus = right;
            leftSegmentFillStatus = left;
        }

        /**
         * The top segment's fill status.
         *
         * @return An integer whose binary digits represent the state of the top segment.
         */
        @IntRange(from = 0, to = MAX_CALIBRATION_STATUS)
        public int getTopSegmentFillStatus() {
            return topSegmentFillStatus;
        }

        /**
         * The bottom segment's fill status.
         *
         * @return An integer whose binary digits represent the state of the bottom segment.
         */
        @IntRange(from = 0, to = MAX_CALIBRATION_STATUS)
        public int getBottomSegmentFillStatus() {
            return bottomSegmentFillStatus;
        }

        /**
         * The right segment's fill status.
         *
         * @return An integer whose binary digits represent the state of the right segment.
         */
        @IntRange(from = 0, to = MAX_CALIBRATION_STATUS)
        public int getRightSegmentFillStatus() {
            return rightSegmentFillStatus;
        }

        /**
         * The left segment's fill status.
         *
         * @return An integer whose binary digits represent the state of the left segment.
         */
        @IntRange(from = 0, to = MAX_CALIBRATION_STATUS)
        public int getLeftSegmentFillStatus() {
            return leftSegmentFillStatus;
        }
    }
    //endregion
}
