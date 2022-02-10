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

package com.dji.ux.beta.sample.showcase.defaultlayout;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.dji.ux.beta.sample.R;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.airlink.PhysicalSource;
import dji.common.camera.CameraVideoStreamSource;
import dji.common.camera.SettingsDefinitions;
import dji.common.product.Model;
import dji.ux.beta.accessory.widget.rtk.RTKWidget;
import dji.ux.beta.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.ux.beta.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.ux.beta.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidget;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.core.extension.ViewExtensions;
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.CommonUtils;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.core.widget.gpssignal.GPSSignalWidget;
import dji.ux.beta.core.widget.radar.RadarWidget;
import dji.ux.beta.core.widget.simulator.SimulatorIndicatorWidget;
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget;
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget;
import dji.ux.beta.map.widget.map.MapWidget;
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.aperture.CameraConfigApertureWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.ev.CameraConfigEVWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.iso.CameraConfigISOAndEIWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.shutter.CameraConfigShutterWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.ssd.CameraConfigSSDWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.wb.CameraConfigWBWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Displays a sample layout of widgets similar to that of the various DJI apps.
 */
public class DefaultLayoutActivity extends AppCompatActivity {

    //region Fields
    private final static String TAG = "DefaultLayoutActivity";

    @BindView(R.id.widget_radar)
    protected RadarWidget radarWidget;
    @BindView(R.id.widget_primary_fpv)
    protected FPVWidget primaryFpvWidget;
    @BindView(R.id.widget_fpv_interaction)
    protected FPVInteractionWidget fpvInteractionWidget;
    @BindView(R.id.widget_map)
    protected MapWidget mapWidget;
    @BindView(R.id.widget_secondary_fpv)
    protected FPVWidget secondaryFPVWidget;
    @BindView(R.id.root_view)
    protected ConstraintLayout parentView;
    @BindView(R.id.widget_panel_system_status_list)
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;

    @BindView(R.id.widget_rtk)
    protected RTKWidget rtkWidget;
    @BindView(R.id.widget_simulator_control)
    protected SimulatorControlWidget simulatorControlWidget;
    @BindView(R.id.widget_camera_config_iso_and_ei)
    protected CameraConfigISOAndEIWidget cameraConfigISOAndEIWidget;
    @BindView(R.id.widget_lens_control)
    protected LensControlWidget lensControlWidget;

    @BindView(R.id.widget_camera_config_shutter)
    protected CameraConfigShutterWidget cameraConfigShutterWidget;
    @BindView(R.id.widget_camera_config_aperture)
    protected CameraConfigApertureWidget cameraConfigApertureWidget;
    @BindView(R.id.widget_camera_config_ev)
    protected CameraConfigEVWidget cameraConfigEVWidget;
    @BindView(R.id.widget_camera_config_wb)
    protected CameraConfigWBWidget cameraConfigWBWidget;
    @BindView(R.id.widget_camera_config_storage)
    protected CameraConfigStorageWidget cameraConfigStorageWidget;
    @BindView(R.id.widget_camera_config_ssd)
    protected CameraConfigSSDWidget cameraConfigSSDWidget;
    @BindView(R.id.widget_auto_exposure_lock)
    protected AutoExposureLockWidget autoExposureLockWidget;
    @BindView(R.id.widget_focus_mode)
    protected FocusModeWidget focusModeWidget;
    @BindView(R.id.widget_focus_exposure_switch)
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    @BindView(R.id.widget_camera_controls)
    protected CameraControlsWidget cameraControlsWidget;
    @BindView(R.id.panel_camera_controls_exposure_settings)
    protected ExposureSettingsPanel exposureSettingsPanel;

    private boolean isMapMini = true;
    private int widgetHeight;
    private int widgetWidth;
    private int widgetMargin;
    private int deviceWidth;
    private int deviceHeight;
    private CompositeDisposable compositeDisposable;
    private UserAccountLoginWidget userAccountLoginWidget;
    private SettingDefinitions.CameraSide primaryFpvCameraSide;
    private CameraVideoStreamSource primaryCameraVideoStreamSource;
    private final DataProcessor<Boolean> cameraSourceProcessor = DataProcessor.create(false);
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_layout);

        widgetHeight = (int) getResources().getDimension(R.dimen.mini_map_height);
        widgetWidth = (int) getResources().getDimension(R.dimen.mini_map_width);
        widgetMargin = (int) getResources().getDimension(R.dimen.mini_map_margin);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        ButterKnife.bind(this);
        setM200SeriesWarningLevelRanges();
        mapWidget.initAMap(map -> {
            map.setOnMapClickListener(latLng -> onViewClick(mapWidget));
            //map.getUiSettings().setZoomControlsEnabled(false);
        });
        mapWidget.getUserAccountLoginWidget().setVisibility(View.GONE);
        mapWidget.onCreate(savedInstanceState);

        CameraControlsWidget cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        cameraControlsWidget.getExposureSettingsIndicatorWidget().setStateChangeResourceId(R.id.panel_camera_controls_exposure_settings);

        // Setup top bar state callbacks
        TopBarPanelWidget topBarPanel = findViewById(R.id.panel_top_bar);
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setStateChangeCallback(findViewById(R.id.widget_panel_system_status_list));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setStateChangeCallback(findViewById(R.id.widget_simulator_control));
        }

        GPSSignalWidget gpsSignalWidget = topBarPanel.getGPSSignalWidget();
        if (gpsSignalWidget != null) {
            gpsSignalWidget.setStateChangeCallback(findViewById(R.id.widget_rtk));
        }

        userAccountLoginWidget = mapWidget.getUserAccountLoginWidget();
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) userAccountLoginWidget.getLayoutParams();
        params.topMargin = (deviceHeight / 10) + (int) DisplayUtil.dipToPx(this, 10);
        userAccountLoginWidget.setLayoutParams(params);

        primaryFpvWidget.setStateChangeCallback(new FPVWidget.FPVStateChangeCallback() {
            @Override
            public void onCameraNameChange(@Nullable String cameraName) {

            }

            @Override
            public void onCameraSideChange(@Nullable SettingDefinitions.CameraSide cameraSide) {
                primaryFpvCameraSide = cameraSide;
                cameraSourceProcessor.onNext(true);
            }

            @Override
            public void onStreamSourceChange(@Nullable CameraVideoStreamSource streamSource) {
                primaryCameraVideoStreamSource = streamSource;
                cameraSourceProcessor.onNext(true);
            }

            @Override
            public void onFPVSizeChange(@Nullable FPVWidget.FPVSize size) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(secondaryFPVWidget.getCameraName()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateSecondaryVideoVisibility));
        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
        compositeDisposable.add(rtkWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uiState -> {
                    if (uiState instanceof RTKWidget.UIState.VisibilityUpdated) {
                        if (((RTKWidget.UIState.VisibilityUpdated) uiState).isVisible()) {
                            hideOtherPanels(rtkWidget);
                        }
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        compositeDisposable.add(cameraSourceProcessor.toFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .sample(300, TimeUnit.MILLISECONDS)
                .subscribe(result -> {
                    onCameraSourceUpdated(primaryFpvCameraSide, primaryCameraVideoStreamSource);
                })
        );
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        super.onPause();
    }
    //endregion

    //region Utils

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                rtkWidget,
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void onCameraSourceUpdated(SettingDefinitions.CameraSide cameraSide, CameraVideoStreamSource streamSource) {
        SettingDefinitions.CameraIndex cameraIndex = CameraUtil.getCameraIndex(cameraSide);
        SettingsDefinitions.LensType lensType = CameraUtil.getLensIndex(streamSource);
        cameraConfigISOAndEIWidget.updateCameraSource(cameraIndex, lensType);
        fpvInteractionWidget.updateCameraSource(cameraIndex, lensType);
        fpvInteractionWidget.updateGimbalIndex(CommonUtils.getGimbalIndex(cameraSide));
        lensControlWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigShutterWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigEVWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigWBWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigStorageWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigSSDWidget.updateCameraSource(cameraIndex, lensType);
        autoExposureLockWidget.updateCameraSource(cameraIndex, lensType);
        focusModeWidget.updateCameraSource(cameraIndex, lensType);
        focusExposureSwitchWidget.updateCameraSource(cameraIndex, lensType);
        cameraControlsWidget.updateCameraSource(cameraIndex, lensType);
        exposureSettingsPanel.updateCameraSource(cameraIndex, lensType);
    }

    private void setM200SeriesWarningLevelRanges() {
        Model[] m200SeriesModels = {
                Model.MATRICE_200,
                Model.MATRICE_210,
                Model.MATRICE_210_RTK,
                Model.MATRICE_200_V2,
                Model.MATRICE_210_V2,
                Model.MATRICE_210_RTK_V2
        };
        float[] ranges = {70, 30, 20, 12, 6, 3};
        radarWidget.setWarningLevelRanges(m200SeriesModels, ranges);
    }

    /**
     * Handles a click event on the FPV widget
     */
    @OnClick(R.id.widget_primary_fpv)
    public void onFPVClick() {
        onViewClick(primaryFpvWidget);
    }

    /**
     * Handles a click event on the secondary FPV widget
     */
    @OnClick(R.id.widget_secondary_fpv)
    public void onSecondaryFPVClick() {
        swapVideoSource();
    }

    /**
     * Swaps the FPV and Map Widgets.
     *
     * @param view The thumbnail view that was clicked.
     */
    private void onViewClick(View view) {
        if (view == primaryFpvWidget && !isMapMini) {
            //reorder widgets
            parentView.removeView(primaryFpvWidget);
            parentView.addView(primaryFpvWidget, 0);

            //resize widgets
            resizeViews(primaryFpvWidget, mapWidget);
            //enable interaction on FPV
            fpvInteractionWidget.setInteractionEnabled(true);
            //disable user login widget on map
            userAccountLoginWidget.setVisibility(View.GONE);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            //reorder widgets
            parentView.removeView(primaryFpvWidget);
            parentView.addView(primaryFpvWidget, parentView.indexOfChild(mapWidget) + 1);
            //resize widgets
            resizeViews(mapWidget, primaryFpvWidget);
            //disable interaction on FPV
            fpvInteractionWidget.setInteractionEnabled(false);
            //enable user login widget on map
            userAccountLoginWidget.setVisibility(View.VISIBLE);
            isMapMini = false;
        }
    }

    /**
     * Helper method to resize the FPV and Map Widgets.
     *
     * @param viewToEnlarge The view that needs to be enlarged to full screen.
     * @param viewToShrink  The view that needs to be shrunk to a thumbnail.
     */
    private void resizeViews(View viewToEnlarge, View viewToShrink) {
        //enlarge first widget
        ResizeAnimation enlargeAnimation = new ResizeAnimation(viewToEnlarge, widgetWidth, widgetHeight, deviceWidth, deviceHeight, 0);
        viewToEnlarge.startAnimation(enlargeAnimation);

        //shrink second widget
        ResizeAnimation shrinkAnimation = new ResizeAnimation(viewToShrink, deviceWidth, deviceHeight, widgetWidth, widgetHeight, widgetMargin);
        viewToShrink.startAnimation(shrinkAnimation);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == SettingDefinitions.VideoSource.SECONDARY) {
            primaryFpvWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
        } else {
            primaryFpvWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
        }
    }

    /**
     * Hide the secondary FPV widget when there is no secondary camera.
     *
     * @param cameraName The name of the secondary camera.
     */
    private void updateSecondaryVideoVisibility(String cameraName) {
        if (cameraName.equals(PhysicalSource.UNKNOWN.name())) {
            secondaryFPVWidget.setVisibility(View.GONE);
        } else {
            secondaryFPVWidget.setVisibility(View.VISIBLE);
        }
    }
    //endregion

    //region classes

    /**
     * Animation to change the size of a view.
     */
    private static class ResizeAnimation extends Animation {

        private static final int DURATION = 300;

        private View view;
        private int toHeight;
        private int fromHeight;
        private int toWidth;
        private int fromWidth;
        private int margin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            this.toHeight = toHeight;
            this.toWidth = toWidth;
            this.fromHeight = fromHeight;
            this.fromWidth = fromWidth;
            view = v;
            this.margin = margin;
            setDuration(DURATION);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (toHeight - fromHeight) * interpolatedTime + fromHeight;
            float width = (toWidth - fromWidth) * interpolatedTime + fromWidth;
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = margin;
            p.bottomMargin = margin;
            view.requestLayout();
        }
    }
    //endregion
}
