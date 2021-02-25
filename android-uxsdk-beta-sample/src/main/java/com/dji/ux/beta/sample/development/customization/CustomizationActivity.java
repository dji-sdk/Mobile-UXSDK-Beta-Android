/*
 * Copyright (c) 2018-2021 DJI
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

package com.dji.ux.beta.sample.development.customization;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.ux.beta.sample.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import dji.common.airlink.PhysicalSource;
import dji.common.camera.CameraVideoStreamSource;
import dji.common.camera.SettingsDefinitions;
import dji.common.product.Model;
import dji.keysdk.AirLinkKey;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.ux.beta.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.ux.beta.cameracore.widget.cameraaperturesetting.CameraApertureSettingWidget;
import dji.ux.beta.cameracore.widget.cameracapture.CameraCaptureWidget;
import dji.ux.beta.cameracore.widget.cameracapture.recordvideo.RecordVideoWidget;
import dji.ux.beta.cameracore.widget.cameraevsetting.CameraEVSettingWidget;
import dji.ux.beta.cameracore.widget.cameraexposuremodesetting.CameraExposureModeSettingWidget;
import dji.ux.beta.cameracore.widget.cameraisoandeisetting.CameraISOAndEISettingWidget;
import dji.ux.beta.cameracore.widget.camerashuttersetting.CameraShutterSettingWidget;
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidget;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.cameracore.widget.gimbalsource.GimbalSourceControlWidget;
import dji.ux.beta.cameracore.widget.manualfocus.ManualFocusWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.communication.MessagingKeys;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.model.VoiceNotificationType;
import dji.ux.beta.core.model.WarningMessage;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.ProductUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.compass.CompassWidget;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.core.widget.radar.RadarWidget;
import dji.ux.beta.core.widget.simulator.SimulatorIndicatorWidget;
import dji.ux.beta.media.widget.accesslocker.AccessLockerControlWidget;
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.aperture.CameraConfigApertureWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.ev.CameraConfigEVWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.iso.CameraConfigISOAndEIWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.shutter.CameraConfigShutterWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.ssd.CameraConfigSSDWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidget;
import dji.ux.beta.visualcamera.widget.cameraconfig.wb.CameraConfigWBWidget;
import dji.ux.beta.visualcamera.widget.colorwaveform.ColorWaveformWidget;
import dji.ux.beta.visualcamera.widget.histogram.HistogramWidget;
import dji.ux.beta.visualcamera.widget.manualzoom.ManualZoomWidget;

public class CustomizationActivity extends AppCompatActivity {

    //region Fields
    private final static String TAG = "CustomizationActivity";

    @BindView(R.id.button_calibration)
    protected Button calibrationButton;
    @BindView(R.id.widget_gimbal_source_control)
    protected GimbalSourceControlWidget gimbalSourceControlWidget;
    @BindView(R.id.widget_control_access)
    protected AccessLockerControlWidget accessLockerControlWidget;
    @BindView(R.id.switch_histogram)
    protected Switch histogramSwitch;
    @BindView(R.id.switch_color_waveform)
    protected Switch colorWaveformSwitch;
    @BindView(R.id.widget_histogram)
    protected HistogramWidget histogramWidget;
    @BindView(R.id.widget_color_waveform)
    protected ColorWaveformWidget colorWaveformWidget;
    @BindView(R.id.widget_radar)
    protected RadarWidget radarWidget;
    @BindView(R.id.widget_fpv)
    protected FPVWidget fpvWidget;
    @BindView(R.id.widget_fpv_interaction)
    protected FPVInteractionWidget fpvInteractionWidget;
    @BindView(R.id.widget_compass)
    protected CompassWidget compassWidget;
    @BindView(R.id.widget_auto_exposure_lock)
    protected AutoExposureLockWidget autoExposureLockWidget;
    @BindView(R.id.widget_focus_exposure_switch)
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    @BindView(R.id.widget_focus_mode)
    protected FocusModeWidget focusModeWidget;
    @BindView(R.id.widget_camera_config_aperture)
    protected CameraConfigApertureWidget cameraConfigApertureWidget;
    @BindView(R.id.widget_camera_config_ev)
    protected CameraConfigEVWidget cameraConfigEVWidget;
    @BindView(R.id.widget_camera_config_iso_and_ei)
    protected CameraConfigISOAndEIWidget cameraConfigISOAndEIWidget;
    @BindView(R.id.widget_camera_config_shutter)
    protected CameraConfigShutterWidget cameraConfigShutterWidget;
    @BindView(R.id.widget_camera_config_ssd)
    protected CameraConfigSSDWidget cameraConfigSSDWidget;
    @BindView(R.id.widget_camera_config_storage)
    protected CameraConfigStorageWidget cameraConfigStorageWidget;
    @BindView(R.id.widget_camera_config_wb)
    protected CameraConfigWBWidget cameraConfigWBWidget;
    @BindView(R.id.widget_camera_aperture)
    protected CameraApertureSettingWidget cameraApertureSettingWidget;
    @BindView(R.id.widget_camera_ev_setting)
    protected CameraEVSettingWidget cameraEVSettingWidget;
    @BindView(R.id.widget_camera_exposure_mode_setting)
    protected CameraExposureModeSettingWidget cameraExposureModeSettingWidget;
    @BindView(R.id.widget_camera_iso_setting)
    protected CameraISOAndEISettingWidget cameraISOAndEISettingWidget;
    @BindView(R.id.widget_camera_shutter)
    protected CameraShutterSettingWidget cameraShutterSettingWidget;
    @BindView(R.id.widget_manual_focus)
    protected ManualFocusWidget manualFocusWidget;
    @BindView(R.id.widget_manual_zoom)
    protected ManualZoomWidget manualZoomWidget;
    @BindView(R.id.widget_camera_capture)
    protected CameraCaptureWidget cameraCaptureWidget;
    private RecordVideoWidget recordVideoWidget;

    @BindView(R.id.textview_fpv_stream_source)
    protected TextView fpvStreamSourceTextView;
    @BindView(R.id.spinner_fpv_stream_source)
    protected Spinner fpvStreamSourceSpinner;
    @BindView(R.id.textview_assign_source)
    protected TextView assignSourceTextView;
    @BindView(R.id.spinner_assign_source)
    protected Spinner assignSourceSpinner;
    @BindView(R.id.textview_fpv_orientation)
    protected View fpvOrientationTextView;
    @BindView(R.id.spinner_fpv_orientation)
    protected Spinner fpvOrientationSpinner;
    @BindView(R.id.view_scroll)
    protected ScrollView widgetListScrollView;
    @BindView(R.id.list_item_scroll_view)
    protected ScrollView listItemScrollView;

    private CompositeDisposable compositeDisposable;
    private boolean isOriginalSize = true;
    private List<CameraCombination> cameraCombinations = new ArrayList<>();

    private DJIKey lbBandwidthKey;
    private DJIKey leftCameraBandwidthKey;
    private DJIKey mainCameraBandwidthKey;
    private DJIKey assignPrimarySourceKey;

    private SettingDefinitions.CameraIndex cameraIndex;
    private SettingDefinitions.GimbalIndex gimbalIndex;
    private SettingsDefinitions.LensType lensType;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customization);
        ButterKnife.bind(this);
        calibrationButton.setOnClickListener((View view) -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(CustomizationActivity.this);
            dialog.setView(View.inflate(CustomizationActivity.this, R.layout.dialog_remote_controller_calibration, null));
            dialog.setCancelable(true);
            dialog.show();
        });
        setM200SeriesWarningLevelRanges();
        fpvWidget.setCodecManagerCallback(codecManager -> colorWaveformWidget.setCodecManager(codecManager)); //TODO inter widget communication

        initializeStateChangedListeners();
        updateOrientationVisibility();

        lbBandwidthKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT);
        leftCameraBandwidthKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LEFT_CAMERA);
        mainCameraBandwidthKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_PRIMARY_VIDEO);
        assignPrimarySourceKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.ASSIGN_SOURCE_TO_PRIMARY_CHANNEL);

        recordVideoWidget = cameraCaptureWidget.getRecordVideoWidget();
    }

    private void initializeStateChangedListeners() {
        SimulatorControlWidget simulatorControlWidget = findViewById(R.id.widget_simulator);
        SimulatorIndicatorWidget simulatorIndicatorWidget = findViewById(R.id.widget_simulator_indicator);
        simulatorIndicatorWidget.setStateChangeCallback(simulatorControlWidget);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(histogramWidget.getHistogramEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(enabled -> histogramSwitch.setChecked(enabled)));
        compositeDisposable.add(colorWaveformWidget.getColorWaveformEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(enabled -> colorWaveformSwitch.setChecked(enabled)));
        compositeDisposable.add(ObservableInMemoryKeyedStore.getInstance().addObserver(UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE))
                .filter(broadcastValues -> broadcastValues.getCurrentValue().getData() != null)
                .map(broadcastValues -> broadcastValues.getCurrentValue().getData())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> displayWarningMessage((WarningMessage) o), throwable -> {
                }));
        compositeDisposable.add(ObservableInMemoryKeyedStore.getInstance().addObserver(UXKeys.create(MessagingKeys.SEND_VOICE_NOTIFICATION))
                .filter(broadcastValues -> broadcastValues.getCurrentValue().getData() != null)
                .map(broadcastValues -> broadcastValues.getCurrentValue().getData())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> playVoiceNotification((VoiceNotificationType) o), throwable -> {
                }));
        compositeDisposable.add(fpvWidget.getWidgetStateUpdate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(modelState -> {
                    if (modelState instanceof FPVWidget.ModelState.CameraSideUpdated) {
                        FPVWidget.ModelState.CameraSideUpdated cameraSideUpdated = (FPVWidget.ModelState.CameraSideUpdated) modelState;
                        SettingDefinitions.GimbalIndex gimbalIndex;
                        SettingDefinitions.CameraIndex cameraIndex;
                        if (cameraSideUpdated.getCameraSide() == SettingDefinitions.CameraSide.PORT) {
                            gimbalIndex = SettingDefinitions.GimbalIndex.PORT;
                            cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0;
                        } else if (cameraSideUpdated.getCameraSide() == SettingDefinitions.CameraSide.STARBOARD) {
                            gimbalIndex = SettingDefinitions.GimbalIndex.STARBOARD;
                            cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_2;
                        } else {
                            gimbalIndex = SettingDefinitions.GimbalIndex.TOP;
                            cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_4;
                        }

                        if (this.gimbalIndex != gimbalIndex) {
                            setGimbalIndex(gimbalIndex);
                        }
                        if (this.cameraIndex != cameraIndex) {
                            setCameraIndex(cameraIndex);
                        }
                    }
                }));
        compositeDisposable.add(DJISDKModel.getInstance().addListener(CameraKey.create(CameraKey.CONNECTION), this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> initAssignSourceSpinner(), throwable -> {
                }));

    }

    @Override
    public void onStop() {
        compositeDisposable.dispose();
        super.onStop();
    }
    //endregion

    //region Utils
    private void displayWarningMessage(WarningMessage warningMessage) {
        if (warningMessage.getAction() == WarningMessage.Action.INSERT) {
            int duration = warningMessage.getType() == WarningMessage.Type.PINNED || warningMessage.getType() == WarningMessage.Type.PINNED_NOT_CLOSE ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
            Toast.makeText(this, warningMessage.getReason(), duration).show();
        }
    }

    private void playVoiceNotification(VoiceNotificationType voiceNotificationType) {
        AudioUtil.playSoundInBackground(this, voiceNotificationType.value());
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

    private void setGimbalIndex(SettingDefinitions.GimbalIndex gimbalIndex) {
        this.gimbalIndex = gimbalIndex;
        compassWidget.setGimbalIndex(gimbalIndex);
    }

    private void setCameraIndex(SettingDefinitions.CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex;
        initFPVStreamSourceSpinner(cameraIndex);
        autoExposureLockWidget.setCameraIndex(cameraIndex);
        focusExposureSwitchWidget.setCameraIndex(cameraIndex);
        focusModeWidget.setCameraIndex(cameraIndex);
        cameraConfigApertureWidget.setCameraIndex(cameraIndex);
        cameraConfigEVWidget.setCameraIndex(cameraIndex);
        cameraConfigISOAndEIWidget.setCameraIndex(cameraIndex);
        cameraConfigShutterWidget.setCameraIndex(cameraIndex);
        cameraConfigSSDWidget.setCameraIndex(cameraIndex);
        cameraConfigStorageWidget.setCameraIndex(cameraIndex);
        cameraConfigWBWidget.setCameraIndex(cameraIndex);
        histogramWidget.setCameraIndex(cameraIndex);
        cameraApertureSettingWidget.setCameraIndex(cameraIndex);
        cameraEVSettingWidget.setCameraIndex(cameraIndex);
        cameraExposureModeSettingWidget.setCameraIndex(cameraIndex);
        cameraISOAndEISettingWidget.setCameraIndex(cameraIndex);
        cameraShutterSettingWidget.setCameraIndex(cameraIndex);
        manualFocusWidget.setCameraIndex(cameraIndex);
        manualZoomWidget.setCameraIndex(cameraIndex);
        recordVideoWidget.setCameraIndex(cameraIndex);
    }

    private void setLensType(SettingsDefinitions.LensType lensType) {
        this.lensType = lensType;
        autoExposureLockWidget.setLensType(lensType);
        focusExposureSwitchWidget.setLensType(lensType);
        focusModeWidget.setLensType(lensType);
        cameraConfigApertureWidget.setLensType(lensType);
        cameraConfigEVWidget.setLensType(lensType);
        cameraConfigISOAndEIWidget.setLensType(lensType);
        cameraConfigShutterWidget.setLensType(lensType);
        cameraConfigStorageWidget.setLensType(lensType);
        cameraConfigWBWidget.setLensType(lensType);
        cameraApertureSettingWidget.setLensType(lensType);
        cameraEVSettingWidget.setLensType(lensType);
        cameraExposureModeSettingWidget.setLensType(lensType);
        cameraISOAndEISettingWidget.setLensType(lensType);
        cameraShutterSettingWidget.setLensType(lensType);
        manualFocusWidget.setLensType(lensType);
        manualZoomWidget.setLensType(lensType);
        recordVideoWidget.setLensType(lensType);
    }

    private void initFPVStreamSourceSpinner(SettingDefinitions.CameraIndex cameraIndex) {
        DJIKey cameraKey = CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex.getIndex());
        String displayName = (String) KeyManager.getInstance().getValue(cameraKey);
        String[] list;
        if (Camera.DisplayNameZenmuseH20.equals(displayName)) {
            list = getResources().getStringArray(R.array.fpv_source_h20_array);
            fpvStreamSourceSpinner.setVisibility(View.VISIBLE);
            fpvStreamSourceTextView.setVisibility(View.VISIBLE);
        } else if (Camera.DisplayNameZenmuseH20T.equals(displayName)) {
            list = getResources().getStringArray(R.array.fpv_source_h20t_array);
            fpvStreamSourceSpinner.setVisibility(View.VISIBLE);
            fpvStreamSourceTextView.setVisibility(View.VISIBLE);
        } else {
            list = null;
            fpvStreamSourceSpinner.setVisibility(View.GONE);
            fpvStreamSourceTextView.setVisibility(View.GONE);
        }

        if (list != null) {
            ArrayAdapter<String> fpvSourceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, list);
            fpvSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fpvStreamSourceSpinner.setAdapter(fpvSourceAdapter);
        }
    }

    private void initAssignSourceSpinner() {
        initCameraCombinations();

        if (cameraCombinations.size() < 2) {
            assignSourceSpinner.setVisibility(View.GONE);
            assignSourceTextView.setVisibility(View.GONE);
        } else {
            assignSourceSpinner.setVisibility(View.VISIBLE);
            assignSourceTextView.setVisibility(View.VISIBLE);

            List<String> strings = new ArrayList<>();
            for (CameraCombination cameraCombination : cameraCombinations) {
                strings.add(getString(cameraCombination.displayName));
            }

            ArrayAdapter<String> assignSourceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, strings);
            assignSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            assignSourceSpinner.setAdapter(assignSourceAdapter);
        }
    }

    private void initCameraCombinations() {
        cameraCombinations.clear();

        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product instanceof Aircraft) {
            Camera portCamera = product.getCameraWithComponentIndex(0);
            Camera starboardCamera = product.getCameraWithComponentIndex(1);
            Camera topCamera = product.getCameraWithComponentIndex(4);

            if (portCamera != null && portCamera.isConnected()) {
                cameraCombinations.add(CameraCombination.PORT_FPV);
                if (starboardCamera != null && starboardCamera.isConnected()) {
                    cameraCombinations.add(CameraCombination.PORT_STARBOARD);
                }
                if (topCamera != null && topCamera.isConnected()) {
                    cameraCombinations.add(CameraCombination.PORT_TOP);
                }
            }
            if (starboardCamera != null && starboardCamera.isConnected()) {
                cameraCombinations.add(CameraCombination.STARBOARD_FPV);
                if (topCamera != null && topCamera.isConnected()) {
                    cameraCombinations.add(CameraCombination.STARBOARD_TOP);
                }
            }
            if (topCamera != null && topCamera.isConnected()) {
                cameraCombinations.add(CameraCombination.TOP_FPV);
            }
        }
    }

    public void updateOrientationVisibility() {
        if (!ProductUtil.isProductAvailable() ||
                !Model.MAVIC_PRO.equals(DJISDKManager.getInstance().getProduct().getModel())) {
            fpvOrientationTextView.setVisibility(View.GONE);
            fpvOrientationSpinner.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.widget_access_locker_indicator)
    public void onAccessLockerIndicatorClick() {
        if (accessLockerControlWidget.getVisibility() == View.VISIBLE) {
            accessLockerControlWidget.setVisibility(View.GONE);
        } else {
            accessLockerControlWidget.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.button_gimbal_source)
    public void onGimbalSourceClick() {
        gimbalSourceControlWidget.setVisibility(View.VISIBLE);
    }

    @OnCheckedChanged(R.id.switch_histogram)
    public void onHistogramSwitchClick(boolean checked) {
        histogramWidget.setHistogramEnabled(checked);
    }

    @OnCheckedChanged(R.id.switch_color_waveform)
    public void onColorWaveformClick(boolean checked) {
        colorWaveformWidget.setColorWaveformEnabled(checked);
    }

    @OnClick(R.id.button_toggle_widget_view)
    public void onShowWidgetListClick() {
        if (widgetListScrollView.getVisibility() == View.VISIBLE) {
            widgetListScrollView.setVisibility(View.GONE);
        } else {
            widgetListScrollView.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.button_toggle_list_view)
    public void onShowListClick() {
        if (listItemScrollView.getVisibility() == View.VISIBLE) {
            listItemScrollView.setVisibility(View.GONE);
        } else {
            listItemScrollView.setVisibility(View.VISIBLE);
        }
    }

    @OnItemSelected(R.id.spinner_fpv_source)
    public void onFPVSourceSelected(int position) {
        fpvWidget.setVideoSource(SettingDefinitions.VideoSource.find(position));
    }

    @OnItemSelected(R.id.spinner_fpv_stream_source)
    public void onFPVStreamSourceSelected(int position) {
        CameraVideoStreamSource streamSource = CameraVideoStreamSource.find(position + 1);
        fpvWidget.setCameraVideoStreamSource(streamSource);

        SettingsDefinitions.LensType lensType;
        switch (streamSource) {
            case INFRARED_THERMAL:
                lensType = SettingsDefinitions.LensType.INFRARED_THERMAL;
                break;
            case WIDE:
                lensType = SettingsDefinitions.LensType.WIDE;
                break;
            default:
                lensType = SettingsDefinitions.LensType.ZOOM;
                break;
        }
        if (this.lensType != lensType) {
            setLensType(lensType);
        }
    }

    @OnItemSelected(R.id.spinner_assign_source)
    public void onAssignSourceSelected(int position) {
        switch (cameraCombinations.get(position)) {
            case PORT_FPV:
                portAndFpv();
                break;
            case STARBOARD_FPV:
                starboardAndFpv();
                break;
            case PORT_STARBOARD:
                portAndStarboard();
                break;
            case TOP_FPV:
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.TOP_CAM, PhysicalSource.FPV_CAM);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 1.0f, null);
                break;
            case PORT_TOP:
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.LEFT_CAM, PhysicalSource.TOP_CAM);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null);
                break;
            case STARBOARD_TOP:
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.RIGHT_CAM, PhysicalSource.TOP_CAM);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null);
                break;
        }
    }

    private void portAndFpv() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM);
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 1.0f, null);
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null);
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 1.0f, null);
        }
    }

    private void starboardAndFpv() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.RIGHT_CAM, PhysicalSource.FPV_CAM);
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.0f, null);
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null);
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 0.0f, null);
        }
    }

    private void portAndStarboard() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.LEFT_CAM, PhysicalSource.RIGHT_CAM);
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null);
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 1.0f, null);
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 0.5f, null);
        }
    }

    private boolean isM200V2OrM300() {
        if (ProductUtil.isProductAvailable()) {
            Model model = DJISDKManager.getInstance().getProduct().getModel();

            return ProductUtil.isM200V2OrM300(model);
        } else {
            return false;
        }
    }

    @OnItemSelected(R.id.spinner_fpv_orientation)
    public void onFPVOrientationSelected(int position) {
        DJIKey orientationKey = CameraKey.create(CameraKey.ORIENTATION, fpvInteractionWidget.getCameraIndex().getIndex());
        SettingsDefinitions.Orientation orientation = SettingsDefinitions.Orientation.find(position);
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(orientationKey, orientation, null);
        }
    }

    @OnClick(R.id.button_fpv_resize)
    public void onResizeFPVClick() {
        ViewGroup.LayoutParams params = fpvWidget.getLayoutParams();
        if (!isOriginalSize) {
            params.height = 2 * fpvWidget.getHeight();
            params.width = 2 * fpvWidget.getWidth();
        } else {
            params.height = fpvWidget.getHeight() / 2;
            params.width = fpvWidget.getWidth() / 2;
        }
        isOriginalSize = !isOriginalSize;
        fpvWidget.setLayoutParams(params);
    }
    //endregion

    //region classes
    private enum CameraCombination {
        PORT_FPV(R.string.port_fpv),
        STARBOARD_FPV(R.string.starboard_fpv),
        PORT_STARBOARD(R.string.port_starboard),
        TOP_FPV(R.string.top_fpv),
        PORT_TOP(R.string.port_top),
        STARBOARD_TOP(R.string.starboard_top);

        @StringRes
        private int displayName;

        CameraCombination(@StringRes int displayName) {
            this.displayName = displayName;
        }
    }
    //endregion
}
