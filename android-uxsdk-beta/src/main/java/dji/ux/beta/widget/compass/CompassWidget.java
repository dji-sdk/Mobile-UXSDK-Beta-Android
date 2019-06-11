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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import dji.common.util.MobileGPSLocationUtil;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.thirdparty.io.reactivex.functions.Function3;
import dji.thirdparty.io.reactivex.functions.Function4;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;

/**
 * This widget aggregates the attitude and location data of the aircraft
 * into one widget. This includes -
 * - Position of the aircraft relative to the pilot
 * - Distance of the aircraft from the pilot
 * - Heading of the aircraft relative to the pilot
 * - True north relative to the pilot and the aircraft
 * - The aircraft's last recorded home location
 * - Attitude of the aircraft
 * - Yaw of the gimbal
 */
public class CompassWidget extends ConstraintLayoutWidget {

    //region Constants
    private static final String TAG = "CompassWidget";
    private final static int MAX_DISTANCE = 400;
    private final static int MAX_SCALE_DISTANCE = 2000;
    private final static float MIN_SCALE = 0.6f;
    private final static int MAX_PROGRESS = 100;
    private final static int MIN_PROGRESS = 0;
    private static final int FULL_TURN = 360;
    private static final int HALF_TURN = 180;
    private static final int QUARTER_TURN = 90;

    //endregion

    //region Fields
    private CompassWidgetModel widgetModel;
    private float halfNorthIconWidth;
    private float halfAttitudeBallWidth;
    private float paddingWidth;
    private float paddingHeight;
    //endregion

    //region Views
    private ImageView homeImageView;
    private ImageView rcImageView;
    private ImageView aircraftImageView;
    private ImageView gimbalYawImageView;
    private ImageView innerCirclesImageView;
    private ImageView northImageView;
    private ImageView compassBackgroundImageView;
    private ProgressBar aircraftAttitudeProgressBar;
    private VisualCompassView visualCompassView;
    private GimbalYawView gimbalYawView;
    //endregion

    public CompassWidget(Context context) {
        super(context);
    }

    public CompassWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompassWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_compass, this);

        compassBackgroundImageView = findViewById(R.id.imageview_compass_background);
        homeImageView = findViewById(R.id.imageview_compass_home);
        rcImageView = findViewById(R.id.imageview_compass_rc);
        northImageView = findViewById(R.id.imageview_north);
        innerCirclesImageView = findViewById(R.id.imageview_inner_circles);
        aircraftImageView = findViewById(R.id.imageview_compass_aircraft);
        gimbalYawImageView = findViewById(R.id.imageview_gimbal_heading);
        aircraftAttitudeProgressBar = findViewById(R.id.progressbar_compass_attitude);
        visualCompassView = findViewById(R.id.visual_compass_view);
        gimbalYawView = findViewById(R.id.gimbal_yaw_view);

        if (!isInEditMode()) {
            widgetModel = new CompassWidgetModel(DJISDKModel.getInstance(),
                                                 ObservableInMemoryKeyedStore.getInstance(),
                                                 (SensorManager) context.getSystemService(Context.SENSOR_SERVICE),
                                                 (WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
            widgetModel.setMobileGPSLocationUtil(new MobileGPSLocationUtil(context, widgetModel));
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInEditMode()) {
            synchronized (this) {
                gimbalYawImageView.setPivotX(gimbalYawImageView.getMeasuredWidth() / 2f);
                gimbalYawImageView.setPivotY(gimbalYawImageView.getMeasuredHeight());
            }
            halfNorthIconWidth = (float) northImageView.getWidth() / 2;
            halfAttitudeBallWidth = (float) compassBackgroundImageView.getWidth() / 2;
            paddingWidth = (float) getWidth() - compassBackgroundImageView.getWidth();
            paddingHeight = (float) getHeight() - compassBackgroundImageView.getHeight();
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getAircraftAttitude()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateAircraftAttitudeUI));
        addReaction(widgetModel.getMobileDeviceAzimuth()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateNorthHeadingUI));
        addReaction(reactToUpdateAircraftHeading());
        addReaction(reactToUpdateAircraftLocation());
        addReaction(reactToUpdateGimbalHeading());
        addReaction(reactToUpdateSecondGPSLocation());
    }
    //endregion

    //region reaction Helpers
    private Disposable reactToUpdateAircraftHeading() {
        // Use the mobile device azimuth and the aircraft attitude to update the aircraft's heading
        return Flowable.combineLatest(widgetModel.getMobileDeviceAzimuth(),
                                      widgetModel.getAircraftAttitude(),
                                      Pair::new)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(values -> updateAircraftHeadingUI(values.first, values.second),
                                  logErrorConsumer(TAG, "reactToUpdateAircraftHeading: "));
    }

    private Disposable reactToUpdateAircraftLocation() {
        // Use the mobile device azimuth, and the aircraft and current location states to update the aircraft location UI
        return Flowable.combineLatest(widgetModel.getMobileDeviceAzimuth(),
                                      widgetModel.getAircraftState(),
                                      widgetModel.getCurrentLocationState(),
                                      (Function3<Float, CompassWidgetModel.AircraftState, CompassWidgetModel.CurrentLocationState, Pair>) (phoneAzimuth, aircraftState, state) -> {
                                          if (aircraftState != null && state != null) {
                                              ViewCoordinates viewCoordinates =
                                                  getAircraftLocationCoordinates(phoneAzimuth, aircraftState, state);
                                              return Pair.create(Pair.create(getMaxDistance(aircraftState, state),
                                                                             calculateScale(aircraftState.getDistance())),
                                                                 viewCoordinates);
                                          }
                                          return null;
                                      }).observeOn(AndroidSchedulers.mainThread()).subscribe(values -> {
            if (values != null) {
                Pair pair = (Pair) values.first;
                updateAircraftLocationUI((float) pair.first, (float) pair.second, (ViewCoordinates) values.second);
            }
        }, logErrorConsumer(TAG, "reactToUpdateAircraftLocation: "));
    }

    private Disposable reactToUpdateGimbalHeading() {
        // Use the mobile device azimuth, the aircraft attitude and the gimbal heading to update the gimbal heading UI
        return Flowable.combineLatest(widgetModel.getMobileDeviceAzimuth(),
                                      widgetModel.getAircraftAttitude(),
                                      widgetModel.getGimbalHeading(),
                                      (phoneAzimuth, aircraftAttitude, gimbalHeading) -> {
                                          if (aircraftAttitude != null) {
                                              return Pair.create(gimbalHeading,
                                                                 (float) aircraftAttitude.getYaw() - phoneAzimuth);
                                          }
                                          return null;
                                      }).observeOn(AndroidSchedulers.mainThread()).subscribe(values -> {
            if (values != null) {
                updateGimbalHeadingUI(values.first, values.second);
            }
        }, logErrorConsumer(TAG, "reactToUpdateGimbalHeading: "));
    }

    private Disposable reactToUpdateSecondGPSLocation() {
        // Use the center type, mobile device azimuth, and the aircraft and current location states
        // to update the home or RC/Mobile device location UI
        return Flowable.combineLatest(widgetModel.getCenterType(),
                                      widgetModel.getMobileDeviceAzimuth(),
                                      widgetModel.getCurrentLocationState(),
                                      widgetModel.getAircraftState(),
                                      (Function4<CompassWidgetModel.CenterType, Float, CompassWidgetModel.CurrentLocationState, CompassWidgetModel.AircraftState, Pair>) (centerType, phoneAzimuth, state, aircraftState) -> {
                                          if (aircraftState != null && state != null) {
                                              ViewCoordinates viewCoordinates =
                                                  getSecondGPSLocationCoordinates(phoneAzimuth, state, aircraftState);
                                              return Pair.create(centerType, viewCoordinates);
                                          }
                                          return null;
                                      }).observeOn(AndroidSchedulers.mainThread()).subscribe(values -> {
            if (values != null) {
                updateSecondGPSLocationUI((CompassWidgetModel.CenterType) values.first,
                                          (ViewCoordinates) values.second);
            }
        }, logErrorConsumer(TAG, "reactToUpdateSecondGPSLocation: "));
    }
    //endregion

    //region Calculations
    private ViewCoordinates getSecondGPSLocationCoordinates(float phoneAzimuth,
                                                            @NonNull CompassWidgetModel.CurrentLocationState state,
                                                            @NonNull CompassWidgetModel.AircraftState aircraftState) {
        final double radians = Math.toRadians(state.getAngle() + phoneAzimuth);
        final float maxDistance = getMaxDistance(aircraftState, state);
        float rcHomeDistance = state.getDistance();
        float x, y;
        if (rcHomeDistance == maxDistance) {
            x = (float) Math.cos(radians);
            y = (float) Math.sin(radians);
        } else {
            x = (float) (rcHomeDistance * Math.cos(radians) / maxDistance);
            y = (float) (rcHomeDistance * Math.sin(radians) / maxDistance);
        }
        return new ViewCoordinates(x, y);
    }

    private float getMaxDistance(@NonNull CompassWidgetModel.AircraftState aircraftState,
                                 @NonNull CompassWidgetModel.CurrentLocationState state) {
        float maxDistance = aircraftState.getDistance();
        if (maxDistance < state.getDistance()) {
            maxDistance = state.getDistance();
        }
        if (maxDistance < MAX_DISTANCE) {
            maxDistance = MAX_DISTANCE;
        }
        return maxDistance;
    }

    private ViewCoordinates getAircraftLocationCoordinates(float phoneAzimuth,
                                                           @NonNull CompassWidgetModel.AircraftState aircraftState,
                                                           @NonNull CompassWidgetModel.CurrentLocationState state) {
        float maxDistance = getMaxDistance(aircraftState, state);
        final double radians = Math.toRadians(aircraftState.getAngle() + phoneAzimuth);
        float aircraftDistance = aircraftState.getDistance();
        float x, y;
        if (aircraftDistance >= maxDistance) {
            x = (float) Math.cos(radians);
            y = (float) Math.sin(radians);
        } else {
            x = (float) (aircraftDistance * Math.cos(radians) / maxDistance);
            y = (float) (aircraftDistance * Math.sin(radians) / maxDistance);
        }
        return new ViewCoordinates(x, y);
    }

    private float calculateScale(final float distance) {
        float scale = 1.0f;
        if (distance >= MAX_SCALE_DISTANCE) {
            scale = MIN_SCALE;
        } else if (distance > MAX_DISTANCE) {
            scale = 1 - MIN_SCALE + ((MAX_SCALE_DISTANCE - distance) / (MAX_SCALE_DISTANCE - MAX_DISTANCE) * MIN_SCALE);
        }
        return scale;
    }
    //endregion

    //region update UI
    private void updateNorthHeadingUI(float phoneAzimuth) {
        // update north image
        double northRadian = Math.toRadians((FULL_TURN - phoneAzimuth) % FULL_TURN);
        final float moveX =
            (float) (halfAttitudeBallWidth + paddingWidth / 2 + halfAttitudeBallWidth * Math.sin(northRadian));
        final float moveY =
            (float) (halfAttitudeBallWidth + paddingHeight / 2 - halfAttitudeBallWidth * Math.cos(northRadian));
        northImageView.setX(moveX - halfNorthIconWidth);
        northImageView.setY(moveY - halfNorthIconWidth);
    }

    private void updateAircraftAttitudeUI(@NonNull CompassWidgetModel.AircraftAttitude aircraftAttitude) {
        //Update aircraft roll
        if (aircraftAttitudeProgressBar != null) {
            aircraftAttitudeProgressBar.setRotation((float) aircraftAttitude.getRoll());
        }
        //Update aircraft pitch
        float tempPitch = (float) -aircraftAttitude.getPitch() + QUARTER_TURN;
        int progress = (int) ((tempPitch * 100) / HALF_TURN);
        if (progress < MIN_PROGRESS) {
            progress = MIN_PROGRESS;
        } else if (progress > MAX_PROGRESS) {
            progress = MAX_PROGRESS;
        }
        if (aircraftAttitudeProgressBar != null) {
            if (aircraftAttitudeProgressBar.getProgress() != progress) {
                aircraftAttitudeProgressBar.setProgress(progress);
            }
        }
    }

    private void updateAircraftHeadingUI(float phoneAzimuth,
                                         @NonNull CompassWidgetModel.AircraftAttitude aircraftAttitude) {
        if (aircraftImageView != null) {
            aircraftImageView.setRotation((float) aircraftAttitude.getYaw() - phoneAzimuth);
        }
    }

    private void updateAircraftLocationUI(float maxDistance, float scale, @NonNull ViewCoordinates viewCoordinates) {
        final float wRadius = (getMeasuredWidth() - paddingWidth - aircraftImageView.getWidth()) / 2.0f;
        final float hRadius = (getMeasuredHeight() - paddingHeight - aircraftImageView.getHeight()) / 2.0f;

        //update the size and heading of the aircraft
        aircraftImageView.setX((paddingWidth / 2.0f) + wRadius + viewCoordinates.getX() * wRadius);
        aircraftImageView.setY((paddingHeight / 2.0f) + hRadius - viewCoordinates.getY() * hRadius);
        aircraftImageView.setScaleX(scale);
        aircraftImageView.setScaleY(scale);

        // Update the size and heading of the gimbal
        gimbalYawImageView.setX(aircraftImageView.getX() + aircraftImageView.getWidth() / 2f
                                    - gimbalYawImageView.getWidth() / 2f);
        gimbalYawImageView.setY(aircraftImageView.getY() + aircraftImageView.getHeight() / 2f
                                    - gimbalYawImageView.getHeight());
        gimbalYawImageView.setScaleX(scale);
        gimbalYawImageView.setScaleY(scale);

        //update the compass view
        if (visualCompassView != null) {
            visualCompassView.setVisibility(VISIBLE);
            innerCirclesImageView.setVisibility(GONE);
            visualCompassView.setDistance(maxDistance);
        }
    }

    private void updateGimbalHeadingUI(float gimbalHeading, float rotationOffset) {
        gimbalYawView.setYaw(gimbalHeading);
        if (gimbalYawImageView != null) {
            gimbalYawImageView.setRotation(gimbalHeading + rotationOffset);
        }
    }

    private void updateSecondGPSLocationUI(@NonNull CompassWidgetModel.CenterType type,
                                           @NonNull ViewCoordinates viewCoordinates) {
        // Calculate the second GPS image's parameters using the center point's parameters
        ImageView centerGPSImage, secondGPSImage;
        if (type == CompassWidgetModel.CenterType.HOME_GPS) {
            centerGPSImage = homeImageView;
            secondGPSImage = rcImageView;
        } else {
            centerGPSImage = rcImageView;
            secondGPSImage = homeImageView;
        }

        centerGPSImage.setVisibility(VISIBLE);
        final ConstraintLayout.LayoutParams centerParam =
            (ConstraintLayout.LayoutParams) centerGPSImage.getLayoutParams();
        centerParam.leftMargin = 0;
        centerParam.topMargin = 0;
        centerGPSImage.setLayoutParams(centerParam);

        //Updating second GPS location and show the second GPS image if both exist
        if (secondGPSImage == null) return;
        if (type != CompassWidgetModel.CenterType.HOME_GPS) {
            secondGPSImage.setVisibility(VISIBLE);
            final float wRadius = (getMeasuredWidth() - paddingWidth - secondGPSImage.getWidth()) / 2.0f;
            final float hRadius = (getMeasuredHeight() - paddingHeight - secondGPSImage.getHeight()) / 2.0f;
            secondGPSImage.setX((paddingWidth / 2.0f) + wRadius + viewCoordinates.getX() * wRadius);
            secondGPSImage.setY((paddingHeight / 2.0f) + hRadius - viewCoordinates.getY() * hRadius);
        } else {
            secondGPSImage.setVisibility(GONE);
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_compass_ratio);
    }

    /**
     * Set the resource ID for the home icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setHomeIcon(@DrawableRes int resourceId) {
        homeImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the home icon
     *
     * @param icon Drawable resource for the image
     */
    public void setHomeIcon(@Nullable Drawable icon) {
        homeImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the home icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getHomeIcon() {
        return homeImageView.getDrawable();
    }

    /**
     * Set the resource ID for the home icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setHomeIconBackground(@DrawableRes int resourceId) {
        homeImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the home icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setHomeIconBackground(@Nullable Drawable background) {
        homeImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the home icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getHomeIconBackground() {
        return homeImageView.getBackground();
    }

    /**
     * Set the resource ID for the RC location icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setRCLocationIcon(@DrawableRes int resourceId) {
        rcImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the RC location icon
     *
     * @param icon Drawable resource for the image
     */
    public void setRCLocationIcon(@Nullable Drawable icon) {
        rcImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the RC location icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getRCLocationIcon() {
        return rcImageView.getDrawable();
    }

    /**
     * Set the resource ID for the RC location icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setRCLocationIconBackground(@DrawableRes int resourceId) {
        rcImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the RC location icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setRCLocationIconBackground(@Nullable Drawable background) {
        rcImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the RC location icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getRCLocationIconBackground() {
        return rcImageView.getBackground();
    }

    /**
     * Set the resource ID for the aircraft icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setAircraftIcon(@DrawableRes int resourceId) {
        aircraftImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the aircraft icon
     *
     * @param icon Drawable resource for the image
     */
    public void setAircraftIcon(@Nullable Drawable icon) {
        aircraftImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the aircraft icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getAircraftIcon() {
        return aircraftImageView.getDrawable();
    }

    /**
     * Set the resource ID for the aircraft icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setAircraftIconBackground(@DrawableRes int resourceId) {
        aircraftImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the aircraft icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setAircraftIconBackground(@Nullable Drawable background) {
        aircraftImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the aircraft icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getAircraftIconBackground() {
        return aircraftImageView.getBackground();
    }

    /**
     * Set the resource ID for the gimbal yaw icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setGimbalYawIcon(@DrawableRes int resourceId) {
        gimbalYawImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the gimbal yaw icon
     *
     * @param icon Drawable resource for the image
     */
    public void setGimbalYawIcon(@Nullable Drawable icon) {
        gimbalYawImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the gimbal yaw icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getGimbalYawIcon() {
        return gimbalYawImageView.getDrawable();
    }

    /**
     * Set the resource ID for the gimbal yaw icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setGimbalYawIconBackground(@DrawableRes int resourceId) {
        gimbalYawImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the gimbal yaw icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setGimbalYawIconBackground(@Nullable Drawable background) {
        gimbalYawImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the gimbal yaw icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getGimbalYawIconBackground() {
        return gimbalYawImageView.getBackground();
    }

    /**
     * Set the resource ID for the north icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setNorthIcon(@DrawableRes int resourceId) {
        northImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the north icon
     *
     * @param icon Drawable resource for the image
     */
    public void setNorthIcon(@Nullable Drawable icon) {
        northImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the north icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getNorthIcon() {
        return northImageView.getDrawable();
    }

    /**
     * Set the resource ID for the north icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setNorthIconBackground(@DrawableRes int resourceId) {
        northImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the north icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setNorthIconBackground(@Nullable Drawable background) {
        northImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the north icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getNorthIconBackground() {
        return northImageView.getBackground();
    }

    /**
     * Set the resource ID for the inner circles icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setInnerCirclesIcon(@DrawableRes int resourceId) {
        innerCirclesImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the inner circles icon
     *
     * @param icon Drawable resource for the image
     */
    public void setInnerCirclesIcon(@Nullable Drawable icon) {
        innerCirclesImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the inner circles icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getInnerCirclesIcon() {
        return innerCirclesImageView.getDrawable();
    }

    /**
     * Set the resource ID for the inner circles icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setInnerCirclesIconBackground(@DrawableRes int resourceId) {
        innerCirclesImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the inner circles icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setInnerCirclesIconBackground(@Nullable Drawable background) {
        innerCirclesImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the inner circles icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getInnerCirclesIconBackground() {
        return innerCirclesImageView.getBackground();
    }

    /**
     * Set the resource ID for the compass background icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setCompassBackgroundIcon(@DrawableRes int resourceId) {
        compassBackgroundImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the compass background icon
     *
     * @param icon Drawable resource for the image
     */
    public void setCompassBackgroundIcon(@Nullable Drawable icon) {
        compassBackgroundImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the compass background icon
     *
     * @return Drawable resource for the icon
     */
    public Drawable getCompassBackgroundIcon() {
        return compassBackgroundImageView.getDrawable();
    }

    /**
     * Set the resource ID for the compass background icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setCompassBackgroundIconBackground(@DrawableRes int resourceId) {
        compassBackgroundImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the compass background icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setCompassBackgroundIconBackground(@Nullable Drawable background) {
        compassBackgroundImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the compass background icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getCompassBackgroundIconBackground() {
        return compassBackgroundImageView.getBackground();
    }

    /**
     * Set the drawable resource for the aircraft attitude icon
     *
     * @param icon Drawable resource for the progress bar
     */
    public void setAircraftAttitudeIcon(@Nullable Drawable icon) {
        aircraftAttitudeProgressBar.setProgressDrawable(icon);
    }

    /**
     * Get the drawable resource for the aircraft attitude icon
     *
     * @return Drawable resource for the progress bar
     */
    public Drawable getAircraftAttitudeIcon() {
        return aircraftAttitudeProgressBar.getProgressDrawable();
    }

    /**
     * Set the resource ID for the aircraft attitude icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setAircraftAttitudeIconBackground(@DrawableRes int resourceId) {
        aircraftAttitudeProgressBar.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the aircraft attitude icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setAircraftAttitudeIconBackground(@Nullable Drawable background) {
        aircraftAttitudeProgressBar.setBackground(background);
    }

    /**
     * Get the background drawable resource for the aircraft attitude icon
     *
     * @return Drawable for the icon's background
     */
    public Drawable getAircraftAttitudeIconBackground() {
        return aircraftAttitudeProgressBar.getBackground();
    }

    /**
     * Set the stroke width for the lines in the visual compass view
     *
     * @param strokeWidth Float value of stroke width in px
     */
    public void setVisualCompassViewStrokeWidth(
        @FloatRange(from = 1.0, to = VisualCompassView.MAX_LINE_WIDTH) float strokeWidth) {
        visualCompassView.setStrokeWidth(strokeWidth);
    }

    /**
     * Set the color for the lines in the visual compass view
     *
     * @param color Color integer resource
     */
    public void setVisualCompassViewLineColor(@ColorInt int color) {
        visualCompassView.setLineColor(color);
    }

    /**
     * Set the interval between the lines in the visual compass view
     *
     * @param interval Integer value of the interval
     */
    public void setVisualCompassViewLineInterval(@IntRange(from = 1) int interval) {
        visualCompassView.setLineInterval(interval);
    }

    /**
     * Set the number of lines to be drawn in the visual compass view
     *
     * @param numberOfLines Number of lines as an integer value
     */
    public void setVisualCompassViewNumberOfLines(@IntRange(from = 3) int numberOfLines) {
        visualCompassView.setNumberOfLines(numberOfLines);
    }

    /**
     * Set the stroke width for the lines in the gimbal yaw view
     *
     * @param strokeWidth Float value of stroke width in px
     */
    public void setGimbalYawViewStrokeWidth(
        @FloatRange(from = 1.0, to = GimbalYawView.MAX_LINE_WIDTH) float strokeWidth) {
        gimbalYawView.setStrokeWidth(strokeWidth);
    }

    /**
     * Set the yaw color in the gimbal yaw view
     *
     * @param color Color integer resource
     */
    public void setGimbalYawViewYawColor(@ColorInt int color) {
        gimbalYawView.setYawColor(color);
    }

    /**
     * Set the invalid color in the gimbal yaw view
     *
     * @param color Color integer resource
     */
    public void setGimbalYawViewInvalidColor(@ColorInt int color) {
        gimbalYawView.setInvalidColor(color);
    }

    /**
     * Set the blink color in the gimbal yaw view
     *
     * @param color Color integer resource
     */
    public void setGimbalYawViewBlinkColor(@ColorInt int color) {
        gimbalYawView.setBlinkColor(color);
    }
    //endregion

    //region Customization Helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CompassWidget);

        Drawable homeIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_homeIcon);
        if (homeIcon != null) {
            setHomeIcon(homeIcon);
        }

        Drawable rcLocationIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_rcLocationIcon);
        if (rcLocationIcon != null) {
            setRCLocationIcon(rcLocationIcon);
        }

        Drawable aircraftIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_aircraftIcon);
        if (aircraftIcon != null) {
            setAircraftIcon(aircraftIcon);
        }

        Drawable gimbalYawIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_gimbalYawIcon);
        if (gimbalYawIcon != null) {
            setGimbalYawIcon(gimbalYawIcon);
        }

        Drawable northIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_northIcon);
        if (northIcon != null) {
            setNorthIcon(northIcon);
        }

        Drawable innerCirclesIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_innerCirclesIcon);
        if (innerCirclesIcon != null) {
            setInnerCirclesIcon(innerCirclesIcon);
        }

        Drawable compassBackgroundIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_compassBackgroundIcon);
        if (compassBackgroundIcon != null) {
            setCompassBackgroundIcon(compassBackgroundIcon);
        }

        Drawable aircraftAttitudeIcon = typedArray.getDrawable(R.styleable.CompassWidget_uxsdk_aircraftAttitudeIcon);
        if (aircraftAttitudeIcon != null) {
            setAircraftAttitudeIcon(aircraftAttitudeIcon);
        }

        float visualCompassViewStrokeWidth =
            typedArray.getDimension(R.styleable.CompassWidget_uxsdk_visualCompassViewStrokeWidth, INVALID_RESOURCE);
        if (visualCompassViewStrokeWidth != INVALID_RESOURCE) {
            setVisualCompassViewStrokeWidth(visualCompassViewStrokeWidth);
        }

        int visualCompassViewLineColor =
            typedArray.getColor(R.styleable.CompassWidget_uxsdk_visualCompassViewLineColor, INVALID_COLOR);
        if (visualCompassViewLineColor != INVALID_COLOR) {
            setVisualCompassViewLineColor(visualCompassViewLineColor);
        }

        int visualCompassViewLineInterval =
            typedArray.getInteger(R.styleable.CompassWidget_uxsdk_visualCompassViewLineInterval, INVALID_RESOURCE);
        if (visualCompassViewLineInterval != INVALID_RESOURCE) {
            setVisualCompassViewLineInterval(visualCompassViewLineInterval);
        }

        int visualCompassViewNumberOfLines =
            typedArray.getInteger(R.styleable.CompassWidget_uxsdk_visualCompassViewNumberOfLines, INVALID_RESOURCE);
        if (visualCompassViewNumberOfLines != INVALID_RESOURCE) {
            setVisualCompassViewNumberOfLines(visualCompassViewNumberOfLines);
        }

        float gimbalYawViewStrokeWidth =
            typedArray.getDimension(R.styleable.CompassWidget_uxsdk_gimbalYawViewStrokeWidth, INVALID_RESOURCE);
        if (gimbalYawViewStrokeWidth != INVALID_RESOURCE) {
            setGimbalYawViewStrokeWidth(gimbalYawViewStrokeWidth);
        }

        int gimbalYawViewYawColor =
            typedArray.getColor(R.styleable.CompassWidget_uxsdk_gimbalYawViewYawColor, INVALID_COLOR);
        if (gimbalYawViewYawColor != INVALID_COLOR) {
            setGimbalYawViewYawColor(gimbalYawViewYawColor);
        }

        int gimbalYawViewInvalidColor =
            typedArray.getColor(R.styleable.CompassWidget_uxsdk_gimbalYawViewInvalidColor, INVALID_COLOR);
        if (gimbalYawViewInvalidColor != INVALID_COLOR) {
            setGimbalYawViewInvalidColor(gimbalYawViewInvalidColor);
        }

        int gimbalYawViewBlinkColor =
            typedArray.getColor(R.styleable.CompassWidget_uxsdk_gimbalYawViewBlinkColor, INVALID_COLOR);
        if (gimbalYawViewBlinkColor != INVALID_COLOR) {
            setGimbalYawViewBlinkColor(gimbalYawViewBlinkColor);
        }

        typedArray.recycle();
    }
    //endregion

    /**
     * Wrapper that holds the x and y values of the view coordinates
     */
    private class ViewCoordinates {
        private float x, y;
        ViewCoordinates(float x, float y) {
            this.x = x;
            this.y = y;
        }
        private float getX() {
            return x;
        }
        private float getY() {
            return y;
        }
    }
}
