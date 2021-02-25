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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintSet;

import dji.common.remotecontroller.CalibrationState;
import dji.log.DJILog;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.core.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.CalibrationType;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.ConnectionState;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.DialPosition;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.LeverPosition;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.MavicStep;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.StickCalibrationStatus;
import dji.ux.beta.core.widget.calibration.RemoteControllerCalibrationWidgetModel.StickPosition;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Shows the Remote Controller Calibration workflow. Depending on which remote controller is
 * connected, different calibration components will be displayed. The remote controller can only
 * be calibrated when the aircraft is disconnected.
 * <p>
 * The button at the bottom will direct the user through the different {@link CalibrationState}
 * stages and the message above it will give them directions on how to complete each stage.
 */
public class RemoteControllerCalibrationWidget extends ConstraintLayoutWidget {

    //region Constants
    private static final String TAG = "RCCalWidget";
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    //endregion

    //region Fields
    private TextView stickText;
    private ImageView stickImage;
    private CalibrationCrossView leftStickView;
    private CalibrationCrossView rightStickView;

    private TextView dialText;
    private ImageView leftDialImage;
    private ImageView leftDialLeftImage;
    private ImageView leftDialRightImage;
    private ImageView rightDialImage;
    private ImageView rightDialLeftImage;
    private ImageView rightDialRightImage;
    private CalibrationProgressBar leftDialView;
    private CalibrationProgressBar rightDialView;

    private TextView leverText;
    private ImageView leftLeverImage;
    private ImageView leftLeverTopImage;
    private CalibrationProgressBar leftLeverView;
    private ImageView leftLeverBottomImage;
    private ImageView rightLeverImage;
    private ImageView rightLeverTopImage;
    private CalibrationProgressBar rightLeverView;
    private ImageView rightLeverBottomImage;

    private TextView message;
    private Button calibrationButton;

    private CalibrationSquareView mavicLeftStickView;
    private CalibrationSquareView mavicRightStickView;
    private CalibrationDialView mavicDialView;

    private Drawable stickImageDrawable;
    private Drawable proStickImageDrawable;
    private Drawable leftDialImageDrawable;
    private Drawable proLeftDialImageDrawable;

    private RemoteControllerCalibrationWidgetModel widgetModel;
    //endregion

    //region Constructor
    public RemoteControllerCalibrationWidget(@NonNull Context context) {
        super(context);
    }

    public RemoteControllerCalibrationWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteControllerCalibrationWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_remote_controller_calibration, this);
        setBackgroundColor(getResources().getColor(R.color.uxsdk_black));

        stickText = findViewById(R.id.step_1_text);
        stickImage = findViewById(R.id.sticks_image);
        leftStickView = findViewById(R.id.left_stick);
        rightStickView = findViewById(R.id.right_stick);

        dialText = findViewById(R.id.step_2_text);
        leftDialImage = findViewById(R.id.left_dial_image);
        leftDialLeftImage = findViewById(R.id.left_dial_left_image);
        leftDialRightImage = findViewById(R.id.left_dial_right_image);
        rightDialImage = findViewById(R.id.right_dial_image);
        rightDialLeftImage = findViewById(R.id.right_dial_left_image);
        rightDialRightImage = findViewById(R.id.right_dial_right_image);
        leftDialView = findViewById(R.id.left_dial_progress);
        rightDialView = findViewById(R.id.right_dial_progress);

        leverText = findViewById(R.id.step_3_text);
        leftLeverImage = findViewById(R.id.left_lever_image);
        leftLeverTopImage = findViewById(R.id.left_lever_top_image);
        leftLeverView = findViewById(R.id.left_lever_progress);
        leftLeverBottomImage = findViewById(R.id.left_lever_bottom_image);
        rightLeverImage = findViewById(R.id.right_lever_image);
        rightLeverTopImage = findViewById(R.id.right_lever_top_image);
        rightLeverView = findViewById(R.id.right_lever_progress);
        rightLeverBottomImage = findViewById(R.id.right_lever_bottom_image);

        message = findViewById(R.id.message);
        calibrationButton = findViewById(R.id.calibration_button);
        calibrationButton.setOnClickListener(view -> onCalibrationButtonClick());

        mavicLeftStickView = findViewById(R.id.calibration_left_stick);
        mavicRightStickView = findViewById(R.id.calibration_right_stick);
        mavicDialView = findViewById(R.id.calibration_dial);

        stickImageDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_rc_calibration_sticks);
        proStickImageDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_rcpro_calibration_sticks);
        leftDialImageDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_rc_calibration_left_dial);
        proLeftDialImageDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_rcpro_calibration_left_dial);

        if (!isInEditMode()) {
            widgetModel = new RemoteControllerCalibrationWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(
                widgetModel.getLeftStick()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateLeftStick));

        addReaction(
                widgetModel.getRightStick()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateRightStick));

        addReaction(
                widgetModel.getLeftDial()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateLeftDial));

        addReaction(
                widgetModel.getRightDial()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateRightDial));

        addReaction(
                widgetModel.getLeftLever()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateLeftLever));

        addReaction(
                widgetModel.getRightLever()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateRightLever));

        addReaction(
                widgetModel.getConnectionState()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateImages));

        addReaction(
                widgetModel.getCalibrationProgress()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::onMavicStickCalibrationFinished));

        addReaction(
                widgetModel.getLeftStickCalibrationStatus()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateLeftStickCalibrationStatus));

        addReaction(
                widgetModel.getRightStickCalibrationStatus()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateRightStickCalibrationStatus));

        addReaction(reactToUpdateMessage());
        addReaction(reactToUpdateButton());
        addReaction(reactToUpdateSticks());
        addReaction(reactToUpdateLeftDial());
        addReaction(reactToMavicStickCalibrationFinished());
    }
    //endregion

    //region Reaction helpers
    private Disposable reactToUpdateMessage() {
        return Flowable.combineLatest(widgetModel.getCalibrationState(), widgetModel.getMavicStep(), widgetModel.getCalibrationType(), MessageUpdateData::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateMessageState, logErrorConsumer(TAG, "reactToUpdateMessage: "));
    }

    private Disposable reactToUpdateButton() {
        return Flowable.combineLatest(widgetModel.getCalibrationState(), widgetModel.getCalibrationType(), widgetModel.getConnectionState(), widgetModel.getCalibrationProgress(), ButtonUpdateData::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateButtonState, logErrorConsumer(TAG, "reactToUpdateButton: "));
    }

    private Disposable reactToUpdateSticks() {
        return Flowable.combineLatest(widgetModel.getCalibrationType(), widgetModel.getMavicStep(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateStickVisibility(values.first, values.second),
                        logErrorConsumer(TAG, "reactToUpdateSticks: "));
    }

    private Disposable reactToUpdateLeftDial() {
        return Flowable.combineLatest(widgetModel.getCalibrationType(), widgetModel.getMavicStep(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateLeftDialVisibility(values.first, values.second),
                        logErrorConsumer(TAG, "reactToUpdateLeftDial: "));
    }

    private Disposable reactToMavicStickCalibrationFinished() {
        return Flowable.combineLatest(widgetModel.getCalibrationProgress(), widgetModel.getCalibrationState(), widgetModel.getCalibrationType(),
                (hasCalibrationFinished, calibrationState, calibrationType) -> hasCalibrationFinished && calibrationState == CalibrationState.LIMITS && calibrationType == CalibrationType.MAVIC)
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onMavicStickCalibrationFinished, logErrorConsumer(TAG, "reactToMavicStickCalibrationFinished"));
    }
    //endregion

    //region Reactions to model
    private void updateLeftStick(StickPosition position) {
        leftStickView.setValue(position.getLeft(), position.getTop(), position.getRight(), position.getBottom());
        mavicLeftStickView.setCircleCenter(position.getLeft(), position.getTop(), position.getRight(), position.getBottom());
    }

    private void updateRightStick(StickPosition position) {
        rightStickView.setValue(position.getLeft(), position.getTop(), position.getRight(), position.getBottom());
        mavicRightStickView.setCircleCenter(position.getLeft(), position.getTop(), position.getRight(), position.getBottom());
    }

    private void updateLeftDial(DialPosition position) {
        if (position.getLeft() > 0) {
            mavicDialView.setProgress(-position.getLeft());
        } else if (position.getRight() > 0) {
            mavicDialView.setProgress(position.getRight());
        } else {
            mavicDialView.setProgress(0);
        }
        leftDialView.setValue(position.getLeft(), position.getRight());
    }

    private void updateRightDial(DialPosition position) {
        rightDialView.setValue(position.getLeft(), position.getRight());
    }

    private void updateLeftLever(LeverPosition position) {
        leftLeverView.setValue(position.getTop(), position.getBottom());
    }

    private void updateRightLever(LeverPosition position) {
        rightLeverView.setValue(position.getTop(), position.getBottom());
    }

    private void updateImages(ConnectionState connectionState) {
        if (connectionState == ConnectionState.RC_ONLY || connectionState == ConnectionState.AIRCRAFT_AND_RC) {
            if (widgetModel.hasRightDial()) {
                stickImage.setImageDrawable(proStickImageDrawable);
                leftDialImage.setImageDrawable(proLeftDialImageDrawable);
                dialText.setText(R.string.uxsdk_rc_pro_calibration_2);
            } else {
                stickImage.setImageDrawable(stickImageDrawable);
                leftDialImage.setImageDrawable(leftDialImageDrawable);
                dialText.setText(R.string.uxsdk_rc_calibration_2);
            }
            updateRightDialVisibility();
            updateLeversVisibility();
        }
    }

    private void updateMessageState(MessageUpdateData data) {
        switch (data.calibrationState) {
            case MIDDLE:
                message.setVisibility(VISIBLE);
                message.setText(R.string.uxsdk_rc_middle_message);
                break;
            case LIMITS:
                message.setVisibility(VISIBLE);
                if (data.calibrationType == CalibrationType.MAVIC) {
                    if (data.mavicStep == MavicStep.DIAL) {
                        message.setText(R.string.uxsdk_rc_mavic_dial_calibration);
                    } else {
                        message.setText(R.string.uxsdk_rc_mavic_stick_calibration);
                    }
                } else {
                    message.setText(R.string.uxsdk_rc_limits_message);
                }
                break;
            default:
                if (data.calibrationType == CalibrationType.MAVIC) {
                    message.setText(R.string.uxsdk_rc_mavic_calibration);
                    message.setVisibility(VISIBLE);
                } else {
                    message.setVisibility(INVISIBLE);
                }
                break;
        }
    }

    private void updateButtonState(ButtonUpdateData data) {
        switch (data.calibrationState) {
            case MIDDLE:
                calibrationButton.setVisibility(VISIBLE);
                calibrationButton.setText(R.string.uxsdk_rc_start);
                calibrationButton.setEnabled(true);
                break;
            case LIMITS:
                if (data.calibrationType == CalibrationType.MAVIC) {
                    calibrationButton.setVisibility(GONE);
                } else {
                    calibrationButton.setText(R.string.uxsdk_rc_finish);
                    if (data.hasCalibrationFinished) {
                        calibrationButton.setEnabled(true);
                    } else {
                        calibrationButton.setEnabled(false);
                    }
                }
                break;
            default:
                calibrationButton.setVisibility(VISIBLE);
                calibrationButton.setEnabled(true);
                calibrationButton.setText(R.string.uxsdk_rc_calibrate);
                break;
        }

        if (data.connectionState != ConnectionState.RC_ONLY) {
            calibrationButton.setEnabled(false);
        }
    }

    private void onMavicStickCalibrationFinished(boolean hasFinished) {
        if (hasFinished) {
            addDisposable(widgetModel.nextCalibrationState()
                    .subscribe(() -> DJILog.d(TAG, "calibration state set successfully"),
                            logErrorConsumer(TAG, "nextCalibrationState: ")));
        }
    }

    private void updateLeftStickCalibrationStatus(StickCalibrationStatus leftStickCalibrationStatus) {
        mavicLeftStickView.setProgress(leftStickCalibrationStatus.getLeftSegmentFillStatus(),
                leftStickCalibrationStatus.getTopSegmentFillStatus(),
                leftStickCalibrationStatus.getRightSegmentFillStatus(),
                leftStickCalibrationStatus.getBottomSegmentFillStatus());
    }

    private void updateRightStickCalibrationStatus(StickCalibrationStatus rightStickCalibrationStatus) {
        mavicRightStickView.setProgress(rightStickCalibrationStatus.getLeftSegmentFillStatus(),
                rightStickCalibrationStatus.getTopSegmentFillStatus(),
                rightStickCalibrationStatus.getRightSegmentFillStatus(),
                rightStickCalibrationStatus.getBottomSegmentFillStatus());
    }
    //endregion

    //region Reactions to user input
    private void onCalibrationButtonClick() {
        addDisposable(widgetModel.nextCalibrationState().subscribe(() -> {
            //do nothing
        }, logErrorConsumer(TAG, "nextCalibrationState: ")));
    }
    //endregion

    //region Helpers
    private void updateConstraints(View viewAboveMessage) {
        ConstraintSet set = new ConstraintSet();
        set.clone(this);
        set.connect(message.getId(), ConstraintSet.TOP, viewAboveMessage.getId(), ConstraintSet.BOTTOM, 0);
        set.applyTo(this);
    }

    private void updateStickVisibility(CalibrationType calibrationType, MavicStep mavicStep) {
        if (calibrationType == CalibrationType.MAVIC) {
            stickText.setVisibility(GONE);
            stickImage.setVisibility(GONE);
            leftStickView.setVisibility(GONE);
            rightStickView.setVisibility(GONE);

            if (mavicStep == MavicStep.DIAL) {
                mavicLeftStickView.setVisibility(GONE);
                mavicRightStickView.setVisibility(GONE);
            } else {
                mavicLeftStickView.setVisibility(VISIBLE);
                mavicRightStickView.setVisibility(VISIBLE);
                mavicLeftStickView.reset();
                mavicRightStickView.reset();
                updateConstraints(mavicLeftStickView);
            }
            message.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            stickText.setVisibility(VISIBLE);
            stickImage.setVisibility(VISIBLE);
            leftStickView.setVisibility(VISIBLE);
            rightStickView.setVisibility(VISIBLE);
            mavicLeftStickView.setVisibility(GONE);
            mavicRightStickView.setVisibility(GONE);
            message.setGravity(Gravity.START);
        }
    }

    private void updateLeftDialVisibility(CalibrationType calibrationType, MavicStep mavicStep) {
        if (calibrationType == CalibrationType.MAVIC) {
            dialText.setVisibility(GONE);
            leftDialImage.setVisibility(GONE);
            leftDialLeftImage.setVisibility(GONE);
            leftDialView.setVisibility(GONE);
            leftDialRightImage.setVisibility(GONE);

            if (mavicStep == MavicStep.DIAL) {
                mavicDialView.setVisibility(VISIBLE);
                updateConstraints(mavicDialView);
            } else {
                mavicDialView.setVisibility(GONE);
            }
            message.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            dialText.setVisibility(VISIBLE);
            leftDialImage.setVisibility(VISIBLE);
            leftDialLeftImage.setVisibility(VISIBLE);
            leftDialView.setVisibility(VISIBLE);
            leftDialRightImage.setVisibility(VISIBLE);
            mavicDialView.setVisibility(GONE);
            message.setGravity(Gravity.START);
        }
    }

    private void updateRightDialVisibility() {
        int visibility = widgetModel.hasRightDial() ? VISIBLE : GONE;
        rightDialImage.setVisibility(visibility);
        rightDialLeftImage.setVisibility(visibility);
        rightDialView.setVisibility(visibility);
        rightDialRightImage.setVisibility(visibility);
    }

    private void updateLeversVisibility() {
        int visibility = widgetModel.hasLevers() ? VISIBLE : GONE;
        leverText.setVisibility(visibility);
        leftLeverImage.setVisibility(visibility);
        leftLeverTopImage.setVisibility(visibility);
        leftLeverView.setVisibility(visibility);
        leftLeverBottomImage.setVisibility(visibility);
        rightLeverImage.setVisibility(visibility);
        rightLeverTopImage.setVisibility(visibility);
        rightLeverView.setVisibility(visibility);
        rightLeverBottomImage.setVisibility(visibility);
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    /**
     * Set the background of the text views displaying the instructions for each step.
     *
     * @param drawable Drawable resource of the background.
     */
    public void setStepsTextBackground(@Nullable Drawable drawable) {
        stickText.setBackground(drawable);
        dialText.setBackground(drawable);
        leverText.setBackground(drawable);
    }

    /**
     * Set the background of the text views displaying the instructions for each step.
     *
     * @param resourceId Integer ID of the background resource.
     */
    public void setStepsTextBackground(@DrawableRes int resourceId) {
        stickText.setBackgroundResource(resourceId);
        dialText.setBackgroundResource(resourceId);
        leverText.setBackgroundResource(resourceId);
    }

    /**
     * Get the background of the text views displaying the instructions for each step.
     *
     * @return Drawable resource of the background.
     */
    @Nullable
    public Drawable getStepsTextBackground() {
        return stickText.getBackground();
    }

    /**
     * Set the text appearance of the text views displaying the instructions for each step.
     *
     * @param resourceId Style resource for text appearance.
     */
    public void setStepsTextAppearance(@StyleRes int resourceId) {
        stickText.setTextAppearance(getContext(), resourceId);
        dialText.setTextAppearance(getContext(), resourceId);
        leverText.setTextAppearance(getContext(), resourceId);
    }

    /**
     * Set the text color for the text views displaying the instructions for each step.
     *
     * @param color color integer resource.
     */
    public void setStepsTextColor(@ColorInt int color) {
        stickText.setTextColor(color);
        dialText.setTextColor(color);
        leverText.setTextColor(color);
    }

    /**
     * Set the text color state list for the text views displaying the instructions for each step.
     *
     * @param colorStateList ColorStateList resource.
     */
    public void setStepsTextColor(@NonNull ColorStateList colorStateList) {
        stickText.setTextColor(colorStateList);
        dialText.setTextColor(colorStateList);
        leverText.setTextColor(colorStateList);
    }

    /**
     * Get current text color of the text views displaying the instructions for each step.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getStepsTextColor() {
        return stickText.getCurrentTextColor();
    }

    /**
     * Get current text color state list of the text views displaying the instructions for each
     * step.
     *
     * @return ColorStateList resource.
     */
    @Nullable
    public ColorStateList getStepsTextColors() {
        return stickText.getTextColors();
    }

    /**
     * Set the text size of the text views displaying the instructions for each step.
     *
     * @param textSize text size float value.
     */
    public void setStepsTextSize(@Dimension float textSize) {
        stickText.setTextSize(textSize);
        dialText.setTextSize(textSize);
        leverText.setTextSize(textSize);
    }

    /**
     * Get the current text size of the text views displaying the instructions for each step.
     *
     * @return text size of the text views.
     */
    @Dimension
    public float getStepsTextSize() {
        return stickText.getTextSize();
    }

    /**
     * Set the drawable resource for the joystick diagram image.
     *
     * @param drawable Drawable resource for the joystick image.
     */
    public void setStickImageDrawable(@Nullable Drawable drawable) {
        stickImageDrawable = drawable;
    }

    /**
     * Set the resource ID for the joystick diagram image.
     *
     * @param resourceId Integer ID of the joystick image resource.
     */
    public void setStickImageResource(@DrawableRes int resourceId) {
        stickImageDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Get the drawable resource for the joystick diagram image.
     *
     * @return Drawable resource of the joystick image.
     */
    @Nullable
    public Drawable getStickImageDrawable() {
        return stickImageDrawable;
    }

    /**
     * Set the drawable resource for the Cendence joystick diagram image.
     *
     * @param drawable Drawable resource for the Cendence joystick image.
     */
    public void setProStickImageDrawable(@Nullable Drawable drawable) {
        proStickImageDrawable = drawable;
    }

    /**
     * Set the resource ID for the Cendence joystick diagram image.
     *
     * @param resourceId Integer ID of the Cendence joystick image resource.
     */
    public void setProStickImageResource(@DrawableRes int resourceId) {
        proStickImageDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Get the drawable resource for the Cendence joystick diagram image.
     *
     * @return Drawable resource of the Cendence joystick image.
     */
    @Nullable
    public Drawable getProStickImageDrawable() {
        return proStickImageDrawable;
    }

    /**
     * Set the drawable resource for the joystick diagram image's background.
     *
     * @param drawable Drawable resource for the joystick image's background.
     */
    public void setStickBackground(@Nullable Drawable drawable) {
        stickImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the joystick diagram image's background.
     *
     * @param resourceId Integer ID of the joystick image's background resource.
     */
    public void setStickBackground(@DrawableRes int resourceId) {
        stickImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the joystick diagram image's background.
     *
     * @return Drawable resource of the joystick image's background.
     */
    @Nullable
    public Drawable getStickBackground() {
        return stickImage.getBackground();
    }

    /**
     * Set the drawable resource for the left dial diagram image.
     *
     * @param drawable Drawable resource for the left dial image.
     */
    public void setLeftDialImageDrawable(@Nullable Drawable drawable) {
        leftDialImageDrawable = drawable;
    }

    /**
     * Set the resource ID for the left dial diagram image.
     *
     * @param resourceId Integer ID of the left dial image resource.
     */
    public void setLeftDialImageResource(@DrawableRes int resourceId) {
        leftDialImageDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Get the drawable resource for the left dial diagram image.
     *
     * @return Drawable resource of the left dial image.
     */
    @Nullable
    public Drawable getLeftDialImageDrawable() {
        return leftDialImageDrawable;
    }

    /**
     * Set the drawable resource for the Cendence left dial diagram image.
     *
     * @param drawable Drawable resource for the Cendence left dial image.
     */
    public void setProLeftDialImageDrawable(@Nullable Drawable drawable) {
        proLeftDialImageDrawable = drawable;
    }

    /**
     * Set the resource ID for the Cendence left dial diagram image.
     *
     * @param resourceId Integer ID of the Cendence left dial image resource.
     */
    public void setProLeftDialImageResource(@DrawableRes int resourceId) {
        proLeftDialImageDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Get the drawable resource for the Cendence left dial diagram image.
     *
     * @return Drawable resource of the Cendence left dial image.
     */
    @Nullable
    public Drawable getProLeftDialImageDrawable() {
        return proLeftDialImageDrawable;
    }

    /**
     * Set the drawable resource for the left dial diagram image's background.
     *
     * @param drawable Drawable resource for the left dial image's background.
     */
    public void setLeftDialBackground(@Nullable Drawable drawable) {
        leftDialImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the left dial diagram image's background.
     *
     * @param resourceId Integer ID of the left dial image's background resource.
     */
    public void setLeftDialBackground(@DrawableRes int resourceId) {
        leftDialImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the left dial diagram image's background.
     *
     * @return Drawable resource of the left dial image's background.
     */
    @Nullable
    public Drawable getLeftDialBackground() {
        return leftDialImage.getBackground();
    }

    /**
     * Set the drawable resources for the left dial's left and right arrow images.
     *
     * @param leftDrawable  Drawable resource for the left dial's left arrow image.
     * @param rightDrawable Drawable resource for the left dial's right arrow image.
     */
    public void setLeftDialArrowImageDrawables(@Nullable Drawable leftDrawable, @Nullable Drawable rightDrawable) {
        leftDialLeftImage.setImageDrawable(leftDrawable);
        leftDialRightImage.setImageDrawable(rightDrawable);
    }

    /**
     * Set the resource IDs for the left dial's left and right arrow images.
     *
     * @param leftResourceId  Integer ID for the left dial's left arrow image.
     * @param rightResourceId Integer ID for the left dial's right arrow image.
     */
    public void setLeftDialArrowImageResources(@DrawableRes int leftResourceId, @DrawableRes int rightResourceId) {
        leftDialLeftImage.setImageResource(leftResourceId);
        leftDialRightImage.setImageResource(rightResourceId);
    }

    /**
     * Get the drawable resources for the left dial's left and right arrow images. The left arrow
     * image is at index 0 and the right arrow image is at index 1.
     *
     * @return An array of size 2 containing the drawable resources for the left dial's left and
     * right arrow images.
     */
    @NonNull
    public Drawable[] getLeftDialArrowImageDrawables() {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = leftDialLeftImage.getDrawable();
        drawables[1] = leftDialRightImage.getDrawable();
        return drawables;
    }

    /**
     * Set the drawable resource for the arrow images' backgrounds.
     *
     * @param drawable Drawable resource for the images' backgrounds.
     */
    public void setArrowBackground(@Nullable Drawable drawable) {
        leftDialLeftImage.setBackground(drawable);
        leftDialRightImage.setBackground(drawable);
        rightDialLeftImage.setBackground(drawable);
        rightDialRightImage.setBackground(drawable);
        leftLeverTopImage.setBackground(drawable);
        leftLeverBottomImage.setBackground(drawable);
        rightLeverTopImage.setBackground(drawable);
        rightLeverBottomImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the arrow images' backgrounds.
     *
     * @param resourceId Integer ID of the images' backgrounds.
     */
    public void setArrowBackground(@DrawableRes int resourceId) {
        leftDialLeftImage.setBackgroundResource(resourceId);
        leftDialRightImage.setBackgroundResource(resourceId);
        rightDialLeftImage.setBackgroundResource(resourceId);
        rightDialRightImage.setBackgroundResource(resourceId);
        leftLeverTopImage.setBackgroundResource(resourceId);
        leftLeverBottomImage.setBackgroundResource(resourceId);
        rightLeverTopImage.setBackgroundResource(resourceId);
        rightLeverBottomImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the arrow images' backgrounds.
     *
     * @return Drawable resource of the images' backgrounds.
     */
    @Nullable
    public Drawable getArrowBackground() {
        return leftDialImage.getBackground();
    }

    /**
     * Set the drawable resource for the right dial diagram image.
     *
     * @param drawable Drawable resource for the right dial image.
     */
    public void setRightDialImageDrawable(@Nullable Drawable drawable) {
        rightDialImage.setImageDrawable(drawable);
    }

    /**
     * Set the resource ID for the right dial diagram image.
     *
     * @param resourceId Integer ID of the right dial image resource.
     */
    public void setRightDialImageResource(@DrawableRes int resourceId) {
        rightDialImage.setImageResource(resourceId);
    }

    /**
     * Get the drawable resource for the right dial diagram image.
     *
     * @return Drawable resource of the right dial image.
     */
    @Nullable
    public Drawable getRightDialImageDrawable() {
        return rightDialImage.getDrawable();
    }

    /**
     * Set the drawable resource for the right dial diagram image's background.
     *
     * @param drawable Drawable resource for the right dial image's background.
     */
    public void setRightDialBackground(@Nullable Drawable drawable) {
        rightDialImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the right dial diagram image's background.
     *
     * @param resourceId Integer ID of the right dial image's background resource.
     */
    public void setRightDialBackground(@DrawableRes int resourceId) {
        rightDialImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the right dial diagram image's background.
     *
     * @return Drawable resource of the right dial image's background.
     */
    @Nullable
    public Drawable getRightDialBackground() {
        return rightDialImage.getBackground();
    }

    /**
     * Set the drawable resources for the right dial's left and right arrow images.
     *
     * @param leftDrawable  Drawable resource for the right dial's left arrow image.
     * @param rightDrawable Drawable resource for the right dial's right arrow image.
     */
    public void setRightDialArrowImageDrawables(@Nullable Drawable leftDrawable, @Nullable Drawable rightDrawable) {
        rightDialLeftImage.setImageDrawable(leftDrawable);
        rightDialRightImage.setImageDrawable(rightDrawable);
    }

    /**
     * Set the resource IDs for the right dial's left and right arrow images.
     *
     * @param leftResourceId  Integer ID for the right dial's left arrow image.
     * @param rightResourceId Integer ID for the right dial's right arrow image.
     */
    public void setRightDialArrowImageResources(@DrawableRes int leftResourceId, @DrawableRes int rightResourceId) {
        rightDialLeftImage.setImageResource(leftResourceId);
        rightDialRightImage.setImageResource(rightResourceId);
    }

    /**
     * Get the drawable resources for the right dial's left and right arrow images. The left arrow
     * image is at index 0 and the right arrow image is at index 1.
     *
     * @return An array of size 2 containing the drawable resources for the right dial's left and
     * right arrow images.
     */
    @NonNull
    public Drawable[] getRightDialArrowImageDrawables() {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = rightDialLeftImage.getDrawable();
        drawables[1] = rightDialRightImage.getDrawable();
        return drawables;
    }

    /**
     * Set the drawable resource for the left lever diagram image.
     *
     * @param drawable Drawable resource for the left lever image.
     */
    public void setLeftLeverImageDrawable(@Nullable Drawable drawable) {
        leftLeverImage.setImageDrawable(drawable);
    }

    /**
     * Set the resource ID for the left lever diagram image.
     *
     * @param resourceId Integer ID of the left lever image resource.
     */
    public void setLeftLeverImageResource(@DrawableRes int resourceId) {
        leftLeverImage.setImageResource(resourceId);
    }

    /**
     * Get the drawable resource for the left lever diagram image.
     *
     * @return Drawable resource of the left lever image.
     */
    @Nullable
    public Drawable getLeftLeverImageDrawable() {
        return leftLeverImage.getDrawable();
    }

    /**
     * Set the drawable resource for the left lever diagram image's background.
     *
     * @param drawable Drawable resource for the left lever image's background.
     */
    public void setLeftLeverBackground(@Nullable Drawable drawable) {
        leftLeverImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the left lever diagram image's background.
     *
     * @param resourceId Integer ID of the left lever image's background resource.
     */
    public void setLeftLeverBackground(@DrawableRes int resourceId) {
        leftLeverImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the left lever diagram image's background.
     *
     * @return Drawable resource of the left lever image's background.
     */
    @Nullable
    public Drawable getLeftLeverBackground() {
        return leftLeverImage.getBackground();
    }

    /**
     * Set the drawable resources for the left lever's top and bottom arrow images.
     *
     * @param topDrawable    Drawable resource for the left lever's top arrow image.
     * @param bottomDrawable Drawable resource for the left lever's bottom arrow image.
     */
    public void setLeftLeverArrowImageDrawables(@Nullable Drawable topDrawable, @Nullable Drawable bottomDrawable) {
        leftLeverTopImage.setImageDrawable(topDrawable);
        leftLeverBottomImage.setImageDrawable(bottomDrawable);
    }

    /**
     * Set the resource IDs for the left lever's top and bottom arrow images.
     *
     * @param topResourceId    Integer ID for the left lever's top arrow image.
     * @param bottomResourceId Integer ID for the left lever's bottom arrow image.
     */
    public void setLeftLeverArrowImageResources(@DrawableRes int topResourceId, @DrawableRes int bottomResourceId) {
        leftLeverTopImage.setImageResource(topResourceId);
        leftLeverBottomImage.setImageResource(bottomResourceId);
    }

    /**
     * Get the drawable resources for the left lever's top and bottom arrow images. The top arrow
     * image is at index 0 and the bottom arrow image is at index 1.
     *
     * @return An array of size 2 containing the drawable resources for the left lever's top and
     * bottom arrow images.
     */
    @NonNull
    public Drawable[] getLeftLeverArrowImageDrawables() {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = leftLeverTopImage.getDrawable();
        drawables[1] = leftLeverBottomImage.getDrawable();
        return drawables;
    }

    /**
     * Set the drawable resource for the right lever diagram image.
     *
     * @param drawable Drawable resource for the right lever image.
     */
    public void setRightLeverImageDrawable(@Nullable Drawable drawable) {
        rightLeverImage.setImageDrawable(drawable);
    }

    /**
     * Set the resource ID for the right lever diagram image.
     *
     * @param resourceId Integer ID of the right lever image resource.
     */
    public void setRightLeverImageResource(@DrawableRes int resourceId) {
        rightLeverImage.setImageResource(resourceId);
    }

    /**
     * Get the drawable resource for the right lever diagram image.
     *
     * @return Drawable resource of the right lever image.
     */
    @Nullable
    public Drawable getRightLeverImageDrawable() {
        return rightLeverImage.getDrawable();
    }

    /**
     * Set the drawable resource for the right lever diagram image's background.
     *
     * @param drawable Drawable resource for the right lever image's background.
     */
    public void setRightLeverBackground(@Nullable Drawable drawable) {
        rightLeverImage.setBackground(drawable);
    }

    /**
     * Set the resource ID for the right lever diagram image's background.
     *
     * @param resourceId Integer ID of the right lever image's background resource.
     */
    public void setRightLeverBackground(@DrawableRes int resourceId) {
        rightLeverImage.setBackgroundResource(resourceId);
    }

    /**
     * Get the drawable resource for the right lever diagram image's background.
     *
     * @return Drawable resource of the right lever image's background.
     */
    @Nullable
    public Drawable getRightLeverBackground() {
        return rightLeverImage.getBackground();
    }

    /**
     * Set the drawable resources for the right lever's top and bottom arrow images.
     *
     * @param topDrawable    Drawable resource for the right lever's top arrow image.
     * @param bottomDrawable Drawable resource for the right lever's bottom arrow image.
     */
    public void setRightLeverArrowImageDrawables(@Nullable Drawable topDrawable, @Nullable Drawable bottomDrawable) {
        rightLeverTopImage.setImageDrawable(topDrawable);
        rightLeverBottomImage.setImageDrawable(bottomDrawable);
    }

    /**
     * Set the resource IDs for the right lever's top and bottom arrow images.
     *
     * @param topResourceId    Integer ID for the right lever's top arrow image.
     * @param bottomResourceId Integer ID for the right lever's bottom arrow image.
     */
    public void setRightLeverArrowImageResources(@DrawableRes int topResourceId, @DrawableRes int bottomResourceId) {
        rightLeverTopImage.setImageResource(topResourceId);
        rightLeverBottomImage.setImageResource(bottomResourceId);
    }

    /**
     * Get the drawable resources for the right lever's top and bottom arrow images. The top arrow
     * image is at index 0 and the bottom arrow image is at index 1.
     *
     * @return An array of size 2 containing the drawable resources for the right lever's top and
     * bottom arrow images.
     */
    @NonNull
    public Drawable[] getRightLeverArrowImageDrawables() {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = rightLeverTopImage.getDrawable();
        drawables[1] = rightLeverBottomImage.getDrawable();
        return drawables;
    }

    /**
     * Set the background for the message text view.
     *
     * @param drawable Drawable resource for the background.
     */
    public void setMessageTextBackground(@Nullable Drawable drawable) {
        message.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the message text view.
     *
     * @param resourceId Integer ID of the text view's background resource.
     */
    public void setMessageTextBackground(@DrawableRes int resourceId) {
        message.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the message text view.
     *
     * @return Drawable resource of the background.
     */
    @Nullable
    public Drawable getMessageTextBackground() {
        return message.getBackground();
    }

    /**
     * Set text appearance of the message text view.
     *
     * @param textAppearance Style resource for text appearance.
     */
    public void setMessageTextAppearance(@StyleRes int textAppearance) {
        message.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the text color for the message text view.
     *
     * @param color color integer resource.
     */
    public void setMessageTextColor(@ColorInt int color) {
        message.setTextColor(color);
    }

    /**
     * Set text color state list for the message text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setMessageTextColor(@NonNull ColorStateList colorStateList) {
        message.setTextColor(colorStateList);
    }

    /**
     * Get current text color of the message text view.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getMessageTextColor() {
        return message.getCurrentTextColor();
    }

    /**
     * Get current text color state list of the message text view.
     *
     * @return ColorStateList resource.
     */
    @Nullable
    public ColorStateList getMessageTextColors() {
        return message.getTextColors();
    }

    /**
     * Set the text size of the message text view.
     *
     * @param textSize text size float value.
     */
    public void setMessageTextSize(@Dimension float textSize) {
        message.setTextSize(textSize);
    }

    /**
     * Get current text size of the message text view.
     *
     * @return text size of the text view.
     */
    @Dimension
    public float getMessageTextSize() {
        return message.getTextSize();
    }

    /**
     * Set the background for the button.
     *
     * @param drawable Drawable resource for the background.
     */
    public void setButtonBackground(@Nullable Drawable drawable) {
        calibrationButton.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the button.
     *
     * @param resourceId Integer ID of the button's background resource.
     */
    public void setButtonBackground(@DrawableRes int resourceId) {
        calibrationButton.setBackgroundResource(resourceId);
    }

    /**
     * Get the background for the button.
     *
     * @return Drawable resource of the background.
     */
    @Nullable
    public Drawable getButtonBackground() {
        return calibrationButton.getBackground();
    }

    /**
     * Set text appearance of the button.
     *
     * @param textAppearance Style resource for text appearance.
     */
    public void setButtonTextAppearance(@StyleRes int textAppearance) {
        calibrationButton.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the text color for the button.
     *
     * @param color color integer resource
     */
    public void setButtonTextColor(@ColorInt int color) {
        calibrationButton.setTextColor(color);
    }

    /**
     * Set text color state list for the button.
     *
     * @param colorStateList ColorStateList resource.
     */
    public void setButtonTextColor(@NonNull ColorStateList colorStateList) {
        calibrationButton.setTextColor(colorStateList);
    }

    /**
     * Get current text color of the button.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getButtonTextColor() {
        return calibrationButton.getCurrentTextColor();
    }

    /**
     * Get current text color state list of the button.
     *
     * @return ColorStateList resource.
     */
    @Nullable
    public ColorStateList getButtonTextColors() {
        return calibrationButton.getTextColors();
    }

    /**
     * Set the text size of the button.
     *
     * @param textSize text size float value.
     */
    public void setButtonTextSize(@Dimension float textSize) {
        calibrationButton.setTextSize(textSize);
    }

    /**
     * Get current text size of the button.
     *
     * @return text size of the text view
     */
    @Dimension
    public float getButtonTextSize() {
        return calibrationButton.getTextSize();
    }

    /**
     * Set the progress paint color of the calibration views.
     *
     * @param color color integer resource.
     */
    public void setCalibrationProgressColor(@ColorInt int color) {
        leftStickView.setPrimaryColor(color);
        rightStickView.setPrimaryColor(color);
        leftDialView.setPrimaryColor(color);
        rightDialView.setPrimaryColor(color);
        leftLeverView.setPrimaryColor(color);
        rightLeverView.setPrimaryColor(color);
        mavicLeftStickView.setPrimaryColor(color);
        mavicRightStickView.setPrimaryColor(color);
        mavicDialView.setPrimaryColor(color);
    }

    /**
     * Get the progress paint color of the calibration views.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getCalibrationProgressColor() {
        return leftStickView.getPrimaryColor();
    }

    /**
     * Set the border paint color of the calibration views.
     *
     * @param color color integer resource.
     */
    public void setCalibrationBorderColor(@ColorInt int color) {
        leftDialView.setDividerColor(color);
        rightDialView.setDividerColor(color);
        leftLeverView.setDividerColor(color);
        rightLeverView.setDividerColor(color);
        mavicLeftStickView.setRectangleColor(color);
        mavicRightStickView.setRectangleColor(color);
    }

    /**
     * Get the border paint color of the calibration views.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getCalibrationBorderColor() {
        return leftDialView.getDividerColor();
    }

    /**
     * Set the progress background paint color of the calibration views. This color is used to
     * represent the unfilled areas of the progress indicators.
     *
     * @param color color integer resource.
     */
    public void setCalibrationProgressBackgroundColor(@ColorInt int color) {
        leftStickView.setProgressBackgroundColor(color);
        rightStickView.setProgressBackgroundColor(color);
        leftDialView.setProgressBackgroundColor(color);
        rightDialView.setProgressBackgroundColor(color);
        leftLeverView.setProgressBackgroundColor(color);
        rightLeverView.setProgressBackgroundColor(color);
    }

    /**
     * Get the progress background paint color of the calibration views. This color is used to
     * represent the unfilled areas of the progress indicators.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getCalibrationProgressBackgroundColor() {
        return leftStickView.getProgressBackgroundColor();
    }

    /**
     * Set the text color of the calibration views.
     *
     * @param color color integer resource.
     */
    public void setCalibrationTextColor(@ColorInt int color) {
        leftStickView.setTextColor(color);
        rightStickView.setTextColor(color);
        mavicLeftStickView.setTextColor(color);
        mavicRightStickView.setTextColor(color);
    }

    /**
     * Get the text color of the calibration views.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getCalibrationTextColor() {
        return leftStickView.getTextColor();
    }

    /**
     * Set the circle color of the mavic stick calibration views.
     *
     * @param color color integer resource.
     */
    public void setMavicCalibrationCircleColor(@ColorInt int color) {
        mavicLeftStickView.setMovementCircleColor(color);
        mavicRightStickView.setMovementCircleColor(color);
    }

    /**
     * Get the circle color of the mavic stick calibration views.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getMavicCalibrationCircleColor() {
        return mavicLeftStickView.getMovementCircleColor();
    }

    /**
     * Set text size of the stick calibration views.
     *
     * @param textSize the text size.
     */
    public void setCalibrationTextSize(@Dimension float textSize) {
        leftStickView.setTextSize(textSize);
        rightStickView.setTextSize(textSize);
    }

    /**
     * Get text size of the stick calibration views.
     *
     * @return the text size.
     */
    @Dimension
    public float getCalibrationTextSize() {
        return leftStickView.getTextSize();
    }

    /**
     * Set text size of the mavic stick calibration views.
     *
     * @param textSize the text size.
     */
    public void setMavicCalibrationTextSize(@Dimension float textSize) {
        mavicLeftStickView.setTextSize(textSize);
        mavicRightStickView.setTextSize(textSize);
    }

    /**
     * Get text size of the mavic stick calibration views.
     *
     * @return the text size.
     */
    @Dimension
    public float getMavicCalibrationTextSize() {
        return mavicRightStickView.getTextSize();
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RemoteControllerCalibrationWidget);

        Drawable stepsBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stepsTextBackground);
        if (stepsBackground != null) {
            setStepsTextBackground(stepsBackground);
        }

        int stepsTextAppearanceId = typedArray.getResourceId(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stepsTextAppearance, INVALID_RESOURCE);
        if (stepsTextAppearanceId != INVALID_RESOURCE) {
            setStepsTextAppearance(stepsTextAppearanceId);
        }

        @ColorInt int stepsTextColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stepsTextColor, DEFAULT_TEXT_COLOR);
        if (stepsTextColor != DEFAULT_TEXT_COLOR) {
            setStepsTextColor(stepsTextColor);
        }

        float stepsTextSize = typedArray.getDimension(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stepsTextSize, INVALID_RESOURCE);
        if (stepsTextSize != INVALID_RESOURCE) {
            setStepsTextSize(DisplayUtil.pxToSp(context, stepsTextSize));
        }

        Drawable stickImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stickImageDrawable);
        if (stickImage != null) {
            setStickImageDrawable(stickImage);
        }

        Drawable proStickImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_proStickImageDrawable);
        if (proStickImage != null) {
            setProStickImageDrawable(proStickImage);
        }

        Drawable stickBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_stickImageBackgroundDrawable);
        if (stickBackground != null) {
            setStickBackground(stickBackground);
        }

        Drawable leftDialImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftDialImageDrawable);
        if (leftDialImage != null) {
            setLeftDialImageDrawable(leftDialImage);
        }

        Drawable proLeftDialImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_proLeftDialImageDrawable);
        if (proLeftDialImage != null) {
            setProLeftDialImageDrawable(proLeftDialImage);
        }

        Drawable leftDialBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftDialImageBackgroundDrawable);
        if (leftDialBackground != null) {
            setLeftDialBackground(leftDialBackground);
        }

        Drawable leftDialLeftArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftDialLeftArrowImageDrawable);
        Drawable leftDialRightArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftDialRightArrowImageDrawable);
        if (leftDialLeftArrowImage != null && leftDialRightArrowImage != null) {
            setLeftDialArrowImageDrawables(leftDialLeftArrowImage, leftDialRightArrowImage);
        }

        Drawable rightDialImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightDialImageDrawable);
        if (rightDialImage != null) {
            setRightDialImageDrawable(rightDialImage);
        }

        Drawable rightDialBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightDialImageBackgroundDrawable);
        if (rightDialBackground != null) {
            setRightDialBackground(rightDialBackground);
        }

        Drawable rightDialLeftArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightDialLeftArrowImageDrawable);
        Drawable rightDialRightArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightDialRightArrowImageDrawable);
        if (rightDialLeftArrowImage != null && rightDialRightArrowImage != null) {
            setRightDialArrowImageDrawables(rightDialLeftArrowImage, rightDialRightArrowImage);
        }

        Drawable leftLeverImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftLeverImageDrawable);
        if (leftLeverImage != null) {
            setLeftLeverImageDrawable(leftLeverImage);
        }

        Drawable leftLeverBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftLeverImageBackgroundDrawable);
        if (leftLeverBackground != null) {
            setLeftLeverBackground(leftLeverBackground);
        }

        Drawable leftLeverTopArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftLeverTopArrowImageDrawable);
        Drawable leftLeverBottomArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_leftLeverBottomArrowImageDrawable);
        if (leftLeverTopArrowImage != null && leftLeverBottomArrowImage != null) {
            setLeftLeverArrowImageDrawables(leftLeverTopArrowImage, leftLeverBottomArrowImage);
        }

        Drawable rightLeverImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightLeverImageDrawable);
        if (rightLeverImage != null) {
            setRightLeverImageDrawable(rightLeverImage);
        }

        Drawable rightLeverBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightLeverImageBackgroundDrawable);
        if (rightLeverBackground != null) {
            setRightLeverBackground(rightLeverBackground);
        }

        Drawable rightLeverTopArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightLeverTopArrowImageDrawable);
        Drawable rightLeverBottomArrowImage = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_rightLeverBottomArrowImageDrawable);
        if (rightLeverTopArrowImage != null && rightLeverBottomArrowImage != null) {
            setRightLeverArrowImageDrawables(rightLeverTopArrowImage, rightLeverBottomArrowImage);
        }

        Drawable messageBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_messageTextBackground);
        if (messageBackground != null) {
            setMessageTextBackground(messageBackground);
        }

        int messageTextAppearanceId = typedArray.getResourceId(R.styleable.RemoteControllerCalibrationWidget_uxsdk_messageTextAppearance, INVALID_RESOURCE);
        if (messageTextAppearanceId != INVALID_RESOURCE) {
            setMessageTextAppearance(messageTextAppearanceId);
        }

        @ColorInt int messageTextColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_messageTextColor, DEFAULT_TEXT_COLOR);
        if (messageTextColor != DEFAULT_TEXT_COLOR) {
            setMessageTextColor(messageTextColor);
        }

        float messageTextSize = typedArray.getDimension(R.styleable.RemoteControllerCalibrationWidget_uxsdk_messageTextSize, INVALID_RESOURCE);
        if (messageTextSize != INVALID_RESOURCE) {
            setMessageTextSize(DisplayUtil.pxToSp(context, messageTextSize));
        }

        Drawable buttonBackground = typedArray.getDrawable(R.styleable.RemoteControllerCalibrationWidget_uxsdk_buttonBackground);
        if (buttonBackground != null) {
            setButtonBackground(buttonBackground);
        }

        int buttonTextAppearanceId = typedArray.getResourceId(R.styleable.RemoteControllerCalibrationWidget_uxsdk_buttonTextAppearance, INVALID_RESOURCE);
        if (buttonTextAppearanceId != INVALID_RESOURCE) {
            setButtonTextAppearance(buttonTextAppearanceId);
        }

        ColorStateList buttonTextColor = typedArray.getColorStateList(R.styleable.RemoteControllerCalibrationWidget_uxsdk_buttonTextColor);
        if (buttonTextColor != null) {
            setButtonTextColor(buttonTextColor);
        }

        float buttonTextSize = typedArray.getDimension(R.styleable.RemoteControllerCalibrationWidget_uxsdk_buttonTextSize, INVALID_RESOURCE);
        if (buttonTextSize != INVALID_RESOURCE) {
            setButtonTextSize(DisplayUtil.pxToSp(context, buttonTextSize));
        }

        @ColorInt int calibrationProgressColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_calibrationProgressColor, getCalibrationProgressColor());
        if (calibrationProgressColor != getCalibrationProgressColor()) {
            setCalibrationProgressColor(calibrationProgressColor);
        }

        @ColorInt int calibrationBorderColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_calibrationBorderColor, getCalibrationBorderColor());
        if (calibrationBorderColor != getCalibrationBorderColor()) {
            setCalibrationBorderColor(calibrationBorderColor);
        }

        @ColorInt int calibrationProgressBackgroundColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_calibrationProgressBackgroundColor, getCalibrationProgressBackgroundColor());
        if (calibrationProgressBackgroundColor != getCalibrationProgressBackgroundColor()) {
            setCalibrationProgressBackgroundColor(calibrationProgressBackgroundColor);
        }

        @ColorInt int calibrationTextColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_calibrationTextColor, getCalibrationTextColor());
        if (calibrationTextColor != getCalibrationTextColor()) {
            setCalibrationTextColor(calibrationTextColor);
        }

        @ColorInt int mavicCalibrationCircleColor = typedArray.getColor(R.styleable.RemoteControllerCalibrationWidget_uxsdk_mavicCalibrationCircleColor, getMavicCalibrationCircleColor());
        if (mavicCalibrationCircleColor != getMavicCalibrationCircleColor()) {
            setMavicCalibrationCircleColor(mavicCalibrationCircleColor);
        }

        float calibrationTextSize = typedArray.getDimension(R.styleable.RemoteControllerCalibrationWidget_uxsdk_calibrationTextSize, INVALID_RESOURCE);
        if (calibrationTextSize != INVALID_RESOURCE) {
            setCalibrationTextSize(DisplayUtil.pxToSp(context, calibrationTextSize));
        }

        float mavicCalibrationTextSize = typedArray.getDimension(R.styleable.RemoteControllerCalibrationWidget_uxsdk_mavicCalibrationTextSize, INVALID_RESOURCE);
        if (mavicCalibrationTextSize != INVALID_RESOURCE) {
            setMavicCalibrationTextSize(DisplayUtil.pxToSp(context, mavicCalibrationTextSize));
        }

        typedArray.recycle();
    }
    //endregion

    //region Reaction data models
    private static class MessageUpdateData {
        private final CalibrationState calibrationState;
        private final MavicStep mavicStep;
        private final CalibrationType calibrationType;

        private MessageUpdateData(CalibrationState state, MavicStep step, CalibrationType type) {
            calibrationState = state;
            mavicStep = step;
            calibrationType = type;
        }
    }

    private static class ButtonUpdateData {
        private final CalibrationState calibrationState;
        private final CalibrationType calibrationType;
        private final ConnectionState connectionState;
        private final boolean hasCalibrationFinished;

        private ButtonUpdateData(CalibrationState calibrationState, CalibrationType type, ConnectionState connectionState, boolean finished) {
            this.calibrationState = calibrationState;
            calibrationType = type;
            this.connectionState = connectionState;
            hasCalibrationFinished = finished;
        }
    }
    //endregion
}
