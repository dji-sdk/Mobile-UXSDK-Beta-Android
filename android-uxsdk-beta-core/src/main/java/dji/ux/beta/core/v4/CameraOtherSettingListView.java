package dji.ux.beta.core.v4;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import dji.common.bus.UXSDKEventBus;
import dji.common.camera.CameraUtils;
import dji.common.camera.OriginalPhotoSettings;
import dji.common.camera.QuickPreviewSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.AntiFlickerFrequency;
import dji.common.camera.SettingsDefinitions.FileIndexMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

import static dji.sdk.camera.Camera.DisplayNameMavic2EnterpriseCamera;
import static dji.sdk.camera.Camera.DisplayNameMavic2ProCamera;
import static dji.sdk.camera.Camera.DisplayNameMavic2ZoomCamera;
import static dji.sdk.camera.Camera.DisplayNameMavicAirCamera;
import static dji.sdk.camera.Camera.DisplayNameSparkCamera;

/**
 * Camera settings for misc items
 */
public class CameraOtherSettingListView extends CameraSettingListView {
    //TODO: Uncomment features hidden for 4.9
    //region Properties
    private static final String TAG = "CameraOtherSettingListView";
    private ListItem histogramItem;
    private ListItem portraitModeItem;
    private ListItem dewarpEnabledItem;
    private ListItem colorWaveformEnabledItem;
    private ListItem videoCaptionItem;
    //private ListItem irCutItem;
    private ListItem gridItem;
    //private ListItem centerPointItem;
    private ListItem antiFlickerItem;
    private ListItem fileIndexModeItem;
    private ListItem saveOriginalListItem;
    private ListItem saveOriginalSwitchItem;
    private ListItem storageLocationItem;
    private ListItem resetCameraItem;
    private ListItem formatSdCardItem;
    private ListItem formatInternalStorageItem;
    private ListItem cameraSensorCleaningItem;
    //private ListItem afcEnabledItem;
    //private ListItem autoLockGimbalEnabledItem;
    //private ListItem peakFocusThresholdItem;
    //private ListItem quickPreviewItem;
    //private ListItem overexposureWarningItem;
    private ListItem headLEDsAutoTurnOffEnabledItem;
    private DJIKey headLEDsAutoTurnOffEnabledKey;
    private DJIKey autoLockGimbalEnabledKey;
    private DJIKey histogramKey;
    private DJIKey orientationKey;
    private DJIKey orientationRangeKey;
    private DJIKey videoCaptionKey;
    private DJIKey fileIndexKey;
    private DJIKey antiFlickerKey;
    private DJIKey isRecordingKey;
    private DJIKey irCutKey;
    private boolean isCameraRecording;
    private FileIndexMode cameraFileIndex;
    private AntiFlickerFrequency AntiFlickerFrequency;
    private boolean isHistogramEnabled;
    private boolean isPortraitModeEnabled;
    private SettingsDefinitions.Orientation orientation;
    private SettingsDefinitions.Orientation[] orientationRange;
    private boolean isCameraCaptionEnabled;
    private boolean isIrCutEnabled;
    private SettingsDefinitions.ExposureMode exposureMode;
    //private ListItem fileIndexItem;
    private DJIKey isShootingTimingPhotoKey;
    private boolean isCameraShootingTimingPhoto;
    private TypedArray antiFlickerImageIdArray;
    private String[] fileIndexNameArray;
    private DJIKey antiFlickerRangeKey;
    private CameraKey cameraTypeKey;
    private CameraKey exposureModeKey;
    private DJIKey videoDewarpEnabledKey;
    private DJIKey colorWaveformEnabledKey;
    private DJIKey isColorWaveformSupportedKey;
    private DJIKey isDewarpingSupportedKey;
    private DJIKey isSensorCleaningSupportedKey;
    private DJIKey isThermalCameraKey;
    private DJIKey sdCardInsertedKey;
    private DJIKey focusModeKey;
    private SettingsDefinitions.FocusMode focusMode;
    private DJIKey isAFCSupportedKey;
    private boolean isAFCSupported;
    private boolean isAFCEnabled;
    private boolean isSdCardInserted;
    private boolean isColorWaveformSupported;
    private DJIKey storageLocationKey;
    private SettingsDefinitions.StorageLocation storageLocation;
    private String[] storageLocationNameArray;
    private DJIKey isInternalStorageSupportedKey;
    private boolean isInternalStorageSupported;
    private DJIKey isInternalStorageInsertedKey;
    private boolean isInternalStorageInserted;
    private boolean isHeadLEDAutoTurnOffEnabled;
    private boolean isAutoLockGimbalEnabled;

    private DJIKey panoOriginalPhotoSettingsKey;
    private boolean isSaveOriginalPanoEnabled;

    private DJIKey cameraConnectionKey;
    private DJIKey cameraNameKey;
    private DJIKey fastPlaybackSettingsKey;
    private DJIKey photoQuickViewDurationKey;
    private DJIKey isPhotoQuickViewSupportedKey;
    private QuickPreviewSettings quickPreviewSettings;
    private boolean isCameraConnected;
    private boolean isQuickViewSupported;
    private SettingsDefinitions.CameraType currentCameraType;
    //endregion

    //region Constructors
    public CameraOtherSettingListView(Context context) {
        super(context, null, 0);
    }

    public CameraOtherSettingListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public CameraOtherSettingListView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }
    //endregion

    public static void enterSensorCleaningMode(final int keyIndex) {
        if (KeyManager.getInstance() == null) {
            return;
        }
        KeyManager.getInstance().performAction(CameraKey.create(CameraKey.INIT_SENSOR_CLEANING_MODE), new ActionCallback() {
            @Override
            public void onSuccess() {
                UXSDKEventBus.getInstance().post(
                        new Events.CameraSettingAdvancedPanelControlEvent(false, keyIndex));
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                DJILog.d(TAG, "Failed to enter sensor cleaning mode : " + error.getDescription());
            }
        });
    }

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        antiFlickerImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_anti_flicker_img_array);
        fileIndexNameArray = getResources().getStringArray(R.array.uxsdk_camera_file_index_name_array);
        storageLocationNameArray = getResources().getStringArray(R.array.uxsdk_camera_storage_location_array);
    }

    @Override
    protected void onUpdateDefaultSetting() {
        // do nothing
    }

    @Override
    protected void onInitData() {
        histogramItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_histogram, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        portraitModeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_portrait_mode, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        dewarpEnabledItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_dewarp, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        colorWaveformEnabledItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_color_waveform, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        videoCaptionItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_caption, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        //irCutItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_ir_cut, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        gridItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_grid_name, ListItem.ItemType.PARENT_TYPE));
        //centerPointItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_center_point, ListItem.ItemType.PARENT_TYPE));
        antiFlickerItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_anti_flick_name, ListItem.ItemType.PARENT_TYPE));
        fileIndexModeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_file_index_name, ListItem.ItemType.PARENT_TYPE));
        saveOriginalListItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_save_original, ListItem.ItemType.PARENT_TYPE));
        saveOriginalSwitchItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_save_original_pano, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        storageLocationItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_storage_location, ListItem.ItemType.PARENT_TYPE));
        cameraSensorCleaningItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_dust_reduction_title, ListItem.ItemType.BUTTON_TYPE));
        //quickPreviewItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_long_exposure_auto_preview, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        //overexposureWarningItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_overexposure, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        headLEDsAutoTurnOffEnabledItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_head_led_auto_turn_off, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        //peakFocusThresholdItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_peak_focus_threshold, ListItem.ItemType.PARENT_TYPE));
        //autoLockGimbalEnabledItem = addItem(new ListItem.ItemProperty(R.uxsdk_string.auto_gimbal_lock, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        resetCameraItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_setting_reset, ListItem.ItemType.BUTTON_TYPE));
        formatSdCardItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_format_sd_card, ListItem.ItemType.BUTTON_TYPE));
        formatInternalStorageItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_format_internal_storage, ListItem.ItemType.BUTTON_TYPE));
        //afcEnabledItem = addItem(new ListItem.ItemProperty(R.uxsdk_string.afc_enabled, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        //updateAFCEnabledItem(isAFCEnabled);
        //updateOverexposureWarningEnabledItem(UXSDKSharedPreferences.isOverexposureWarningEnabled(getContext(), keyIndex));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }
        subscription.add(UXSDKEventBus.getInstance()
               .register(Events.OverexposureWarningStatus.class)
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(new Action1<Events.OverexposureWarningStatus>() {
                   @Override
                   public void call(Events.OverexposureWarningStatus status) {
                       if (status.getIndex() == keyIndex) {
                           //updateOverexposureWarningEnabledItem(status.isEnabled());
                       }
                   }
               }));
    }

    private void hideNotSupportedItems() {
        if (KeyManager.getInstance() == null) {
            return;
        }

        if (headLEDsAutoTurnOffEnabledKey != null && headLEDsAutoTurnOffEnabledItem != null) {
            if (KeyManager.getInstance().isKeySupported(headLEDsAutoTurnOffEnabledKey)) {
                updateItem(headLEDsAutoTurnOffEnabledItem, State.VISIBLE);
            } else {
                updateItem(headLEDsAutoTurnOffEnabledItem, State.HIDDEN);
            }
        }

        if (histogramKey != null && histogramItem != null) {
            if (KeyManager.getInstance().isKeySupported(histogramKey)) {
                updateItem(histogramItem, State.VISIBLE);
            } else {
                updateItem(histogramItem, State.HIDDEN);
            }
        }
    }

    //region Key life cycle

    @Override
    public void initKey() {
        histogramKey = CameraKey.create(CameraKey.HISTOGRAM_ENABLED, keyIndex);
        headLEDsAutoTurnOffEnabledKey = CameraKey.create(CameraKey.LED_AUTO_TURN_OFF_ENABLED, keyIndex);
        autoLockGimbalEnabledKey = CameraKey.create(CameraKey.AUTO_LOCK_GIMBAL_ENABLED, keyIndex);
        orientationKey = CameraKey.create(CameraKey.ORIENTATION, keyIndex);
        orientationRangeKey = CameraKey.create(CameraKey.ORIENTATION_RANGE, keyIndex);
        videoCaptionKey = CameraKey.create(CameraKey.VIDEO_CAPTION_ENABLED, keyIndex);
        antiFlickerKey = CameraUtil.createCameraKeys(CameraKey.ANTI_FLICKER_FREQUENCY, keyIndex, subKeyIndex);
        fileIndexKey = CameraKey.create(CameraKey.FILE_INDEX_MODE, keyIndex);
        isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, keyIndex);
        isShootingTimingPhotoKey = CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, keyIndex);
        antiFlickerRangeKey = CameraUtil.createCameraKeys(CameraKey.ANTI_FLICKER_RANGE, keyIndex, subKeyIndex);
        cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);
        exposureModeKey = CameraKey.create(CameraKey.EXPOSURE_MODE, keyIndex);
        irCutKey = CameraKey.create(CameraKey.IRC_ENABLE, keyIndex);
        isDewarpingSupportedKey = CameraKey.create(CameraKey.IS_DEWARPING_SUPPORTED, keyIndex);
        videoDewarpEnabledKey = CameraKey.create(CameraKey.DEWARPING_ENABLED, keyIndex);
        isSensorCleaningSupportedKey =  CameraKey.create(CameraKey.IS_SENSOR_CLEANING_SUPPORTED, keyIndex);
        colorWaveformEnabledKey = CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED, keyIndex);
        isColorWaveformSupportedKey = CameraKey.create(CameraKey.IS_COLOR_WAVEFORM_SUPPORTED, keyIndex);
        isThermalCameraKey = CameraKey.create(CameraKey.IS_THERMAL_CAMERA, keyIndex);
        sdCardInsertedKey = CameraKey.create(CameraKey.SDCARD_IS_INSERTED, keyIndex);
        storageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, keyIndex);
        isInternalStorageSupportedKey = CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, keyIndex);
        isInternalStorageInsertedKey = CameraKey.create(CameraKey.INNERSTORAGE_IS_INSERTED, keyIndex);
        isAFCSupportedKey = CameraKey.create(CameraKey.IS_AFC_SUPPORTED, keyIndex);
        focusModeKey = CameraKey.create(CameraKey.FOCUS_MODE, keyIndex);
        panoOriginalPhotoSettingsKey = CameraKey.create(CameraKey.PANO_ORIGINAL_PHOTO_SETTINGS, keyIndex);
        cameraNameKey = CameraKey.create(CameraKey.DISPLAY_NAME, keyIndex);
        fastPlaybackSettingsKey = CameraKey.create(CameraKey.FAST_PLAYBACK_SETTINGS, keyIndex);
        photoQuickViewDurationKey = CameraKey.create(CameraKey.PHOTO_QUICK_VIEW_DURATION, keyIndex);
        isPhotoQuickViewSupportedKey = CameraKey.create(CameraKey.IS_PHOTO_QUICK_VIEW_SUPPORTED, keyIndex);
        cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, keyIndex);

        addDependentKey(cameraTypeKey);
        addDependentKey(histogramKey);
        addDependentKey(headLEDsAutoTurnOffEnabledKey);
        addDependentKey(autoLockGimbalEnabledKey);
        addDependentKey(orientationKey);
        addDependentKey(orientationRangeKey);
        addDependentKey(videoCaptionKey);
        addDependentKey(antiFlickerKey);
        addDependentKey(fileIndexKey);
        addDependentKey(isRecordingKey);
        addDependentKey(antiFlickerKey);
        addDependentKey(antiFlickerRangeKey);
        addDependentKey(exposureModeKey);
        addDependentKey(irCutKey);
        addDependentKey(videoDewarpEnabledKey);
        addDependentKey(isDewarpingSupportedKey);
        addDependentKey(isSensorCleaningSupportedKey);
        addDependentKey(isColorWaveformSupportedKey);
        addDependentKey(colorWaveformEnabledKey);
        addDependentKey(isThermalCameraKey);
        addDependentKey(sdCardInsertedKey);
        addDependentKey(storageLocationKey);
        addDependentKey(isInternalStorageSupportedKey);
        addDependentKey(isInternalStorageInsertedKey);
        addDependentKey(isAFCSupportedKey);
        addDependentKey(focusModeKey);
        addDependentKey(panoOriginalPhotoSettingsKey);
        addDependentKey(cameraNameKey);
        addDependentKey(fastPlaybackSettingsKey);
        addDependentKey(cameraConnectionKey);
        addDependentKey(isPhotoQuickViewSupportedKey);
    }

    private void updatePortraitModeItem() {
        if (orientationRange != null) {
            //Orientation range will only be greater than one if portrait mode is supported as the default is landscape mode for all aircraft models
            if (orientationRange.length > 1) {
                updateItem(portraitModeItem, State.VISIBLE);
                if (orientation != null) {
                    isPortraitModeEnabled = (orientation == SettingsDefinitions.Orientation.PORTRAIT);
                }
                return;
            }
        }
        updateItem(portraitModeItem, State.HIDDEN);
    }

    private void updateAntiFlickerItem(AntiFlickerFrequency flicker) {
        if (flicker != null && flicker.value() < antiFlickerImageIdArray.length()) {
            updateItem(antiFlickerItem, flicker.value(), antiFlickerImageIdArray.getResourceId(flicker.value(), 0));
        }
    }

    private void updateFileIndexItem(FileIndexMode fileIndex) {
        int value = fileIndex.value();
        if (fileIndexNameArray != null && value < fileIndexNameArray.length) {
            updateItem(fileIndexModeItem, value, fileIndexNameArray[value], 0);
        }
    }

    private void updateFormatSDCardItem() {
        if (isSdCardInserted) {
            updateItem(formatSdCardItem, State.VISIBLE);
        } else {
            updateItem(formatSdCardItem, State.DISABLED);
        }
    }

    private void updateFormatInternalStorageItem() {
        if (isInternalStorageSupported && !isInternalStorageInserted) {
            updateItem(formatInternalStorageItem, State.DISABLED);
        }
    }

    private void updateFormatInternalStorageItemVisibility() {
        if (!isInternalStorageSupported) {
            updateItem(formatInternalStorageItem, State.HIDDEN);
            updateItem(storageLocationItem, State.HIDDEN);
        }
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(histogramKey)) {
            isHistogramEnabled = (boolean) value;
        } else if (key.equals(orientationKey)) {
            orientation = (SettingsDefinitions.Orientation) value;
            updatePortraitModeItem();
        } else if (key.equals(orientationRangeKey)) {
            orientationRange = (SettingsDefinitions.Orientation[]) value;
            updatePortraitModeItem();
        } else if (key.equals(headLEDsAutoTurnOffEnabledKey)) {
            isHeadLEDAutoTurnOffEnabled = (boolean) value;
        } else if (key.equals(videoCaptionKey)) {
            isCameraCaptionEnabled = (boolean) value;
        } else if (key.equals(antiFlickerKey)) {
            AntiFlickerFrequency = (AntiFlickerFrequency) value;
            updateAntiFlickerItem(AntiFlickerFrequency);
        } else if (key.equals(fileIndexKey)) {
            cameraFileIndex = (FileIndexMode) value;
            updateFileIndexItem(cameraFileIndex);
        } else if (key.equals(isRecordingKey)) {
            isCameraRecording = (boolean) value;
        } else if (key.equals(isShootingTimingPhotoKey)) {
            isCameraShootingTimingPhoto = (boolean) value;
        } else if (key.equals(antiFlickerRangeKey)) {
            AntiFlickerFrequency[] range = (AntiFlickerFrequency[]) value;
            if (range.length == 1) {
                updateAntiFlickerItem(range[0]);
                updateItem(antiFlickerItem, State.DISABLED);
            } else {
                updateItem(antiFlickerItem, State.VISIBLE);
            }
            hideGD610NotSupport(currentCameraType);
        } else if (key.equals(exposureModeKey)) {
            exposureMode = (SettingsDefinitions.ExposureMode) value;
        } else if (key.equals(irCutKey)) {
            isIrCutEnabled = (boolean) value;
        } else if (key.equals(cameraTypeKey)) {
            currentCameraType = (SettingsDefinitions.CameraType) value;
            hideGD610NotSupport(currentCameraType);
        } else if (key.equals(videoDewarpEnabledKey)) {
            updateDewarpEnabledUI((boolean) value);
        } else if (key.equals(isDewarpingSupportedKey)) {
            enableDewarpEnabledUI((boolean) value);
        } else if (key.equals(isSensorCleaningSupportedKey)) {
            enableSensorCleaningUI((boolean) value);
        } else if (key.equals(colorWaveformEnabledKey)) {
            updateColorWaveformEnabledUI((boolean) value);
        } else if (key.equals(isColorWaveformSupportedKey)) {
            isColorWaveformSupported = (boolean) value;
            enableColorWaveformUI();
        } else if (key.equals(isThermalCameraKey)) {
            boolean isThermalCamera = (boolean) value;
            if (isThermalCamera) {
                updateItem(histogramItem, State.HIDDEN);
                updateItem(fileIndexModeItem, State.HIDDEN);
                updateItem(videoCaptionItem, State.HIDDEN);
            }
            hideGD610NotSupport(currentCameraType);
        } else if (key.equals(sdCardInsertedKey)) {
            isSdCardInserted = (boolean) value;
            updateFormatSDCardItem();
        } else if (key.equals(storageLocationKey)) {
            storageLocation = (SettingsDefinitions.StorageLocation) value;
            updateStorageLocationItem(storageLocation);
        } else if (key.equals(isInternalStorageSupportedKey)) {
            isInternalStorageSupported = (boolean) value;
            updateFormatInternalStorageItemVisibility();
        } else if (key.equals(isInternalStorageInsertedKey)) {
            isInternalStorageInserted = (boolean) value;
            updateFormatInternalStorageItem();
        } else if (key.equals(cameraNameKey)) {
            updateSaveOriginalItem((String) value);
        } else if (key.equals(panoOriginalPhotoSettingsKey)) {
            isSaveOriginalPanoEnabled = ((OriginalPhotoSettings) value).shouldSaveOriginalPhotos();
        } else if (key.equals(isAFCSupportedKey)) {
            isAFCSupported = (boolean) value;
        } else if (key.equals(focusModeKey)) {
            focusMode = (SettingsDefinitions.FocusMode) value;
            if (focusMode == SettingsDefinitions.FocusMode.AFC) {
                isAFCEnabled = true;
            } else if (focusMode == SettingsDefinitions.FocusMode.AUTO) {
                isAFCEnabled = false;
            }
        } else if (key.equals(autoLockGimbalEnabledKey)) {
            isAutoLockGimbalEnabled = (boolean) value;
        } else if (key.equals(fastPlaybackSettingsKey)) {
            quickPreviewSettings = (QuickPreviewSettings) value;
        } else if (key.equals(cameraConnectionKey)) {
            isCameraConnected = (Boolean) value;
        } else if (key.equals(isPhotoQuickViewSupportedKey)) {
            isQuickViewSupported = (Boolean) value;
        }
    }

    private boolean isSupportIRCut(SettingsDefinitions.CameraType cameraType) {
        return CameraUtils.isGDCamera(cameraType);
    }

    private boolean isSupportFileIndex(SettingsDefinitions.CameraType cameraType) {
        return cameraType != SettingsDefinitions.CameraType.DJICameraTypeGD600;
    }

    private void updateStorageLocationItem(SettingsDefinitions.StorageLocation storageLocation) {
        int value = storageLocation.value();
        if (storageLocationNameArray != null && value < storageLocationNameArray.length) {
            updateItem(storageLocationItem, value, storageLocationNameArray[value], 0);
        }
    }

    private void updateSaveOriginalItem(String cameraName) {
        if (cameraName != null) {
            if (cameraName.equals(DisplayNameMavic2ProCamera)
                    || cameraName.equals(DisplayNameMavic2ZoomCamera)
                    || cameraName.equals(DisplayNameMavic2EnterpriseCamera)) {
                updateItem(saveOriginalSwitchItem, State.HIDDEN);
                updateItem(saveOriginalListItem, State.VISIBLE);
            } else if (cameraName.equals(DisplayNameMavicAirCamera) || cameraName.equals(DisplayNameSparkCamera)) {
                updateItem(saveOriginalSwitchItem, State.VISIBLE);
                updateItem(saveOriginalListItem, State.HIDDEN);
            } else {
                updateItem(saveOriginalSwitchItem, State.HIDDEN);
                updateItem(saveOriginalListItem, State.HIDDEN);
            }
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(histogramKey)) {
            updateHistogramUI();
        } else if (key.equals(orientationKey)) {
            updatePortraitModeUI();
        } else if (key.equals(orientationRangeKey)) {
            updatePortraitModeUI();
        } else if (key.equals(headLEDsAutoTurnOffEnabledKey)) {
            updateHeadLEDsAutoTurnOffUI();
        } else if (key.equals(autoLockGimbalEnabledKey)) {
            //updateAutoGimbalLockEnabledUI();
        } else if (key.equals(videoCaptionKey)) {
            updateVideoCaptionUI();
        } else if (key.equals(antiFlickerKey)) {
            updateAntiFlickerUI();
        } else if (key.equals(irCutKey)) {
            //updateIrCutUI();
        } else if (key.equals(fileIndexKey)) {
            updateFileIndexUI();
        } else if (key.equals(storageLocationKey)) {
            updateStorageLocationUI();
        } else if (key.equals(isAFCSupportedKey)) {
            /*if (isAFCSupported) {
                updateItem(afcEnabledItem, State.VISIBLE);
            } else {
                updateItem(afcEnabledItem, State.HIDDEN);
            }*/
        } else if (key.equals(focusModeKey)) {
            //updateAFCEnabledItem(isAFCEnabled);
        } else if (key.equals(panoOriginalPhotoSettingsKey)) {
            updateSaveOriginalSwitchUI();
        } else if (key.equals(fastPlaybackSettingsKey)) {
            //updateQuickPreview();
        } else if (key.equals(cameraConnectionKey)) {
            if (isCameraConnected) {
                hideNotSupportedItems();
            }
        } else if (key.equals(isPhotoQuickViewSupportedKey)) {
            /*if (isQuickViewSupported) {
                updateItem(quickPreviewItem, State.VISIBLE);
            } else {
                updateItem(quickPreviewItem, State.HIDDEN);
            }*/
        }
        hideGD610NotSupport(currentCameraType);
    }

    /*private void updateAutoGimbalLockEnabledUI() {
        if (autoLockGimbalEnabledItem != null) {
            autoLockGimbalEnabledItem.valueId = isAutoLockGimbalEnabled ? 1 : 0;
            adapter.notifyItemChanged(adapter.findIndexByItem(autoLockGimbalEnabledItem));
        }
    }*/

    private void updateHeadLEDsAutoTurnOffUI() {
        if (headLEDsAutoTurnOffEnabledItem != null) {
            headLEDsAutoTurnOffEnabledItem.valueId = isHeadLEDAutoTurnOffEnabled ? 1 : 0;

            adapter.notifyItemChanged(adapter.findIndexByItem(headLEDsAutoTurnOffEnabledItem));
        }
    }

    /*private void updateAFCEnabledItem(boolean isEnabled) {
        afcEnabledItem.valueId = isEnabled ? 1 : 0;
        adapter.notifyItemChanged(adapter.findIndexByItem(afcEnabledItem));
    }

    private void updateOverexposureWarningEnabledItem(boolean isEnabled) {
        overexposureWarningItem.valueId = isEnabled ? 1 : 0;
        adapter.notifyItemChanged(adapter.findIndexByItem(overexposureWarningItem));
    }*/

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (view instanceof SwitchButton) {
            SwitchButton switchButton = (SwitchButton) view;
            if (item.equals(histogramItem)) {
                handleHistogramSwitchChecked(switchButton.isChecked());
            } else if (item.equals(portraitModeItem)) {
                handlePortraitModeSwitchChecked(switchButton.isChecked());
            } /*else if (item.equals(overexposureWarningItem)) {
                handleOverexposureWarningSwitchChecked(switchButton.isChecked());
            }*/ else if (item.equals(headLEDsAutoTurnOffEnabledItem)) {
                handleHeadLEDsAutoTurnOffSwitchChecked(switchButton.isChecked());
            } /*else if (item.equals(autoLockGimbalEnabledItem)) {
                handleAutoLockGimbalSwitchChecked(switchButton.isChecked());
            }*/ else if (item.equals(videoCaptionItem)) {
                handleVideoCaptionSwitchChecked(switchButton.isChecked());
            } /*else if (item.equals(irCutItem)) {
                handleIrCutSwitchChecked(switchButton.isChecked());
            }*/ else if (item.equals(dewarpEnabledItem)) {
                handleDewarpSwitchChecked(switchButton.isChecked());
            } else if (item.equals(colorWaveformEnabledItem)) {
                handleColorWaveformChecked(switchButton.isChecked());
            } else if (item.equals(saveOriginalSwitchItem)) {
                handleSaveOriginalSwitchChecked(switchButton.isChecked());
            } /*else if (item.equals(afcEnabledItem)) {
                handleAFCSwitchChecked(switchButton.isChecked());
            } else if (item.equals(quickPreviewItem)) {
                handleQuickPreviewChecked(switchButton.isChecked());
            }*/
            return;
        }

        if (item.equals(resetCameraItem)) {
            handleCameraResetting();
        } else if (item.equals(formatSdCardItem)) {
            handleFormatSDCard();
        } else if (item.equals(formatInternalStorageItem)) {
            handleFormatInternalStorage();
        } else if (item.equals(cameraSensorCleaningItem)) {
            enterSensorCleaningMode(keyIndex);
        } else {
            removeChildViewIfNeeded();
            if (item.equals(antiFlickerItem)) {
                if (exposureMode == SettingsDefinitions.ExposureMode.MANUAL) {
                    ViewUtils.showAlertDialog(getContext(),
                            R.string.uxsdk_camera_anti_flick_trigger_title,
                            getContext().getString(R.string.uxsdk_camera_anti_flick_trigger_tip));
                } else {
                    childView = new CameraAntiFlickerListWidget(context);
                }
            } else if (item.equals(fileIndexModeItem)) {
                childView = new CameraFileIndexListWidget(context);
            } else if (item.equals(gridItem)) {
                childView = new CameraGridListWidget(context);
            } /*else if (item.equals(centerPointItem)) {
                childView = new CameraCenterPointListWidget(context);
            }*/ else if (item.equals(storageLocationItem)) {
                childView = new CameraStorageListWidget(context);
            } else if (item.equals(saveOriginalListItem)) {
                childView = new CameraSaveOriginalListWidget(context);
            } /*else if (item.equals(peakFocusThresholdItem)){
                childView = new CameraPeakFocusThresholdListWidget(context);
            }*/
            showChildView();
        }
    }

    /*private void handleQuickPreviewChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(photoQuickViewDurationKey, isChecked ? 2 : 0, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Quick preview enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateQuickPreview();
                    }
                });
            }
        });
    }*/

    /*private void handleAutoLockGimbalSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) return;

        KeyManager.getInstance().setValue(autoLockGimbalEnabledKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Auto Lock Gimbal enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateAutoGimbalLockEnabledUI();
                    }
                });
            }
        });
    }*/

    private void handleHistogramSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) return;

        KeyManager.getInstance().setValue(histogramKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Histogram enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                // ToDo: Restore to previous item.
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateHistogramUI();
                    }
                });
            }
        });
    }

    private void handlePortraitModeSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        SettingsDefinitions.Orientation orientation;
        if (isChecked) {
            orientation = SettingsDefinitions.Orientation.PORTRAIT;
        } else {
            orientation = SettingsDefinitions.Orientation.LANDSCAPE;
        }

        KeyManager.getInstance().setValue(orientationKey, orientation, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Portrait mode set: " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updatePortraitModeUI();
                    }
                });
            }
        });
    }


    private void handleHeadLEDsAutoTurnOffSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) return;

        KeyManager.getInstance().setValue(headLEDsAutoTurnOffEnabledKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Auto LEDs Turn Off enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateHeadLEDsAutoTurnOffUI();
                    }
                });
            }
        });
    }

    private void handleVideoCaptionSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(videoCaptionKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera video caption enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                // ToDo: Restore to previous item.
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateVideoCaptionUI();
                    }
                });
            }
        });
    }

    /*private void handleIrCutSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(irCutKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera ir-cut enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                // ToDo: Restore to previous item.
                updateIrCutUI();
            }
        });
    }*/

    private void handleSaveOriginalSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }
        if (isChecked) {
            showSaveOriginalWarning();
        }
        KeyManager.getInstance()
                .setValue(panoOriginalPhotoSettingsKey,
                        new OriginalPhotoSettings(isChecked, SettingsDefinitions.PhotoFileFormat.JPEG),
                        new SetCallback() {
                            @Override
                            public void onSuccess() {
                                DJILog.d(TAG, "Save unstitched pano photos set: " + isChecked + " successfully");
                            }

                            @Override
                            public void onFailure(@NonNull DJIError error) {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateSaveOriginalSwitchUI();
                                    }
                                });
                            }
                        });
    }

    private void showSaveOriginalWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(true).setTitle(R.string.uxsdk_save_original_warning_title).setMessage(R.string.uxsdk_save_original_warning_message)
                .setPositiveButton(R.string.uxsdk_app_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    private void updateHistogramUI() {
        if (histogramItem != null) {
            histogramItem.valueId = isHistogramEnabled ? 1 : 0;

            adapter.notifyItemChanged(adapter.findIndexByItem(histogramItem));
        }
    }

    private void updatePortraitModeUI() {
        updateItem(portraitModeItem, (isPortraitModeEnabled ? 1 : 0), 0);
    }

    private void updateDewarpEnabledUI(boolean isChecked) {
        updateItem(dewarpEnabledItem, (isChecked ? 1 : 0), 0);
    }

    private void updateColorWaveformEnabledUI(boolean isChecked) {
        updateItem(colorWaveformEnabledItem, (isChecked ? 1 : 0), 0);
    }

    private void enableDewarpEnabledUI(boolean enabled) {
        updateItem(dewarpEnabledItem, enabled ? State.VISIBLE : State.HIDDEN);
    }

    private void enableColorWaveformUI() {
        State state;
        if (isColorWaveformSupported) {
            state = State.VISIBLE;
        } else {
            state = State.HIDDEN;
        }
        updateItem(colorWaveformEnabledItem, state);
    }

    private void updateSaveOriginalSwitchUI() {
        updateItem(saveOriginalSwitchItem, (isSaveOriginalPanoEnabled ? 1 : 0), 0);
    }

    private void enableSensorCleaningUI(boolean enabled) {
        updateItem(cameraSensorCleaningItem, enabled ? State.VISIBLE : State.HIDDEN);
    }

    private void updateVideoCaptionUI() {
        if (videoCaptionItem != null) {
            videoCaptionItem.valueId = (isCameraCaptionEnabled ? 1 : 0);

            adapter.notifyItemChanged(adapter.findIndexByItem(videoCaptionItem));
        }
    }

    private void updateAntiFlickerUI() {
        if (antiFlickerItem != null) {
            antiFlickerItem.valueId = AntiFlickerFrequency.value();

            adapter.notifyItemChanged(adapter.findIndexByItem(antiFlickerItem));
        }
    }

    /*private void updateIrCutUI() {
        if (irCutItem != null) {
            irCutItem.valueId = isIrCutEnabled ? 1 : 0;
            adapter.notifyItemChanged(adapter.findIndexByItem(irCutItem));
        }
    }*/

    private void updateFileIndexUI() {
        if (fileIndexModeItem != null) {
            fileIndexModeItem.valueId = cameraFileIndex.value();

            adapter.notifyItemChanged(adapter.findIndexByItem(fileIndexModeItem));
        }
    }

    private void updateStorageLocationUI() {
        if (storageLocationItem != null) {
            storageLocationItem.valueId = storageLocation.value();

            adapter.notifyItemChanged(adapter.findIndexByItem(storageLocationItem));
        }
    }

    /*private void updateQuickPreview() {
        if (quickPreviewSettings != null) {
            boolean enabled = quickPreviewSettings.isEnable();
            quickPreviewItem.valueId = enabled ? 1 : 0;
            adapter.notifyItemChanged(adapter.findIndexByItem(quickPreviewItem));
        }
    }*/

    private void executeResetCMDToCamera() {
        if (KeyManager.getInstance() == null) return;

        DJIKey resetKey = CameraKey.create(CameraKey.RESTORE_FACTORY_SETTINGS, keyIndex);
        KeyManager.getInstance().performAction(resetKey, new ActionCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera reset setting successfully");
                if (isAttachedToWindow) {
                    ViewUtils.showMessageDialog(getContext(), SlidingDialogV4.TYPE_TIP2, R.string.uxsdk_app_tip, getResources().getString(R.string.uxsdk_camera_setting_reset_success));
                }
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                // ToDo: Restore to previous item.
                if (isAttachedToWindow) {
                    if (error == DJIError.COMMON_TIMEOUT) {
                        ViewUtils.showAlertDialog(getContext(), R.string.uxsdk_camera_setting_reset_timeout_title, "");
                    } else {
                        ViewUtils.showAlertDialog(getContext(), R.string.uxsdk_camera_setting_reset_busy_title, "");
                    }
                }
                DJILog.d(TAG, "Failed to set reset Camera Setting");
            }
        });
    }

    private void executeFormatSDCardOnCamera() {
        if (KeyManager.getInstance() == null) return;

        DJIKey formatSDKey = CameraKey.create(CameraKey.FORMAT_SD_CARD, keyIndex);
        KeyManager.getInstance().performAction(formatSDKey, new ActionCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera reset setting successfully");
                if (isAttachedToWindow) {
                    ViewUtils.showMessageDialog(context, SlidingDialogV4.TYPE_TIP2, R.string.uxsdk_camera_format_sd_card_success, "");
                }
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                if (isAttachedToWindow) {
                    ViewUtils.showAlertDialog(context, R.string.uxsdk_camera_format_sd_card_busy_title, "");
                }
                DJILog.d(TAG, "Failed to set reset Camera Setting");
            }
        });
    }

    private void updateActionToCamera(ListItem item) {
        if (item.equals(resetCameraItem)) {
            executeResetCMDToCamera();
        } else if (item.equals(formatSdCardItem)) {
            executeFormatSDCardOnCamera();
        } else if (item.equals(formatInternalStorageItem)) {
            CameraUtil.formatInternalStorage(getContext());
        }
    }

    private void handleCameraResetting() {
        if (isCameraRecording || isCameraShootingTimingPhoto) {
            ViewUtils.showAlertDialog(getContext(),
                    R.string.uxsdk_camera_setting_reset_busy_title,
                    context.getString(R.string.uxsdk_camera_setting_reset_busy_tip));
        } else {
            showOperateDlg(resetCameraItem,
                    getContext().getString(R.string.uxsdk_camera_setting_reset_camera_setting_confirm));
        }
    }

    private void handleFormatSDCard() {
        showOperateDlg(formatSdCardItem, getContext().getString(R.string.uxsdk_camera_setting_format_sdcard_confirm));
    }

    private void handleFormatInternalStorage() {
        showOperateDlg(formatInternalStorageItem, getContext().getString(R.string.uxsdk_camera_setting_format_internal_storage_confirm));
    }

    private void showOperateDlg(final ListItem item, final String content) {
        SlidingDialogV4.OnEventListener listener = new SlidingDialogV4.OnEventListener() {

            @Override
            public void onRightBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
                updateActionToCamera(item);
            }

            @Override
            public void onLeftBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
            }

            @Override
            public void onCbChecked(final DialogInterface dialog, boolean checked, int arg) {

            }
        };

        ViewUtils.showOperateDlg(getContext(), content, listener);
    }

    private void handleDewarpSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(videoDewarpEnabledKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateDewarpEnabledUI(!isChecked);
                    }
                });
            }
        });
    }

    private void handleColorWaveformChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(colorWaveformEnabledKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateColorWaveformEnabledUI(!isChecked);
                    }
                });
            }
        });
    }

    private void hideGD610NotSupport(SettingsDefinitions.CameraType cameraType){
        if(CameraUtils.isGD610Camera(cameraType)){
            updateItem(videoCaptionItem, State.HIDDEN);
            updateItem(histogramItem, State.HIDDEN);
            updateItem(antiFlickerItem, State.HIDDEN);
            updateItem(dewarpEnabledItem, State.HIDDEN);
            updateItem(colorWaveformEnabledItem, State.HIDDEN);
        }
    }
}
