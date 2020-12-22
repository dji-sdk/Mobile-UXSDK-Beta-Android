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

package dji.ux.beta.cameracore.widget.cameracontrols;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.widget.cameracapture.CameraCaptureWidget;
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsMenuIndicatorWidget;
import dji.ux.beta.cameracore.widget.cameracontrols.exposuresettingsindicator.ExposureSettingsIndicatorWidget;
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;

/**
 * Compound widget which combines the state and interaction related to camera.
 * It includes {@link CameraSettingsMenuIndicatorWidget}, {@link ExposureSettingsIndicatorWidget},
 * {@link PhotoVideoSwitchWidget} and {@link CameraCaptureWidget}.
 * <p>
 * The widget gives access to all the child widgets.
 */
public class CameraControlsWidget extends ConstraintLayoutWidget {

    //region Fields
    private CameraSettingsMenuIndicatorWidget cameraSettingsMenuIndicatorWidget;
    private PhotoVideoSwitchWidget photoVideoSwitchWidget;
    private CameraCaptureWidget cameraCaptureWidget;
    private ExposureSettingsIndicatorWidget exposureSettingsIndicatorWidget;
    //endregion

    //region Lifecycle
    public CameraControlsWidget(@NonNull Context context) {
        super(context);
    }

    public CameraControlsWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraControlsWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_camera_controls, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        cameraCaptureWidget = findViewById(R.id.widget_camera_control_camera_capture);
        photoVideoSwitchWidget = findViewById(R.id.widget_camera_control_photo_video_switch);
        exposureSettingsIndicatorWidget = findViewById(R.id.widget_camera_control_camera_exposure_settings);
        cameraSettingsMenuIndicatorWidget = findViewById(R.id.widget_camera_control_camera_settings_menu);
    }

    @Override
    protected void reactToModelChanges() {
        //Do Nothing
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_camera_controls_ratio);
    }
    //endregion

    //region customizations

    /**
     * Get the camera capture widget
     *
     * @return {@link CameraCaptureWidget}
     */
    @NonNull
    public CameraCaptureWidget getCameraCaptureWidget() {
        return cameraCaptureWidget;
    }

    /**
     * Get the camera settings menu widget
     *
     * @return {@link CameraSettingsMenuIndicatorWidget}
     */
    @NonNull
    public CameraSettingsMenuIndicatorWidget getCameraSettingsMenuIndicatorWidget() {
        return cameraSettingsMenuIndicatorWidget;
    }

    /**
     * Get the exposure settings widget
     *
     * @return {@link ExposureSettingsIndicatorWidget}
     */
    @NonNull
    public ExposureSettingsIndicatorWidget getExposureSettingsIndicatorWidget() {
        return exposureSettingsIndicatorWidget;
    }

    /**
     * Get the photo video switch widget
     *
     * @return {@link PhotoVideoSwitchWidget}
     */
    @NonNull
    public PhotoVideoSwitchWidget getPhotoVideoSwitchWidget() {
        return photoVideoSwitchWidget;
    }

    /**
     * Show/Hide {@link CameraSettingsMenuIndicatorWidget}
     *
     * @param isVisible boolean true - visible  false - gone
     */
    public void setCameraSettingsMenuIndicatorWidgetVisibility(boolean isVisible) {
        cameraSettingsMenuIndicatorWidget.setVisibility(isVisible ? VISIBLE : GONE);
    }

    /**
     * Check if {@link CameraSettingsMenuIndicatorWidget} is visible
     *
     * @return boolean true - visible  false - gone
     */
    public boolean isCameraSettingsMenuIndicatorWidgetVisible() {
        return cameraSettingsMenuIndicatorWidget.getVisibility() == VISIBLE;
    }

    /**
     * Show/Hide {@link PhotoVideoSwitchWidget}
     *
     * @param isVisible boolean true - visible false - gone
     */
    public void setPhotoVideoSwitchWidgetVisibility(boolean isVisible) {
        photoVideoSwitchWidget.setVisibility(isVisible ? VISIBLE : GONE);
    }

    /**
     * Check if {@link PhotoVideoSwitchWidget} is Visible
     *
     * @return boolean  true - visible  false - gone
     */
    public boolean isPhotoVideoSwitchWidgetVisible() {
        return photoVideoSwitchWidget.getVisibility() == VISIBLE;
    }

    /**
     * Show/Hide {@link CameraCaptureWidget}
     *
     * @param isVisible boolean  true - visible  false - gone
     */
    public void setCameraCaptureWidgetVisibility(boolean isVisible) {
        cameraCaptureWidget.setVisibility(isVisible ? VISIBLE : GONE);
    }

    /**
     * Check if {@link CameraCaptureWidget} is visible
     *
     * @return boolean  true - visible  false - gone
     */
    public boolean isCameraCaptureWidgetVisible() {
        return cameraCaptureWidget.getVisibility() == VISIBLE;
    }

    /**
     * Show/Hide {@link ExposureSettingsIndicatorWidget}
     *
     * @param isVisible boolean  true - visible  false - gone
     */
    public void setExposureSettingsIndicatorWidgetVisibility(boolean isVisible) {
        exposureSettingsIndicatorWidget.setVisibility(isVisible ? VISIBLE : GONE);
    }

    /**
     * Check if {@link ExposureSettingsIndicatorWidget}
     *
     * @return boolean  true - visible  false - gone
     */
    public boolean isExposureSettingsIndicatorWidgetVisible() {
        return exposureSettingsIndicatorWidget.getVisibility() == VISIBLE;
    }

    //endregion
}
