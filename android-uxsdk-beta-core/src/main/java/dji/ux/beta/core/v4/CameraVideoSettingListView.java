package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import dji.common.bus.UXSDKEventBus;
import dji.common.camera.CameraSSDVideoLicense;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.CameraColor;
import dji.common.camera.SettingsDefinitions.EIColor;
import dji.common.camera.SettingsDefinitions.ExposureSensitivityMode;
import dji.common.camera.SettingsDefinitions.PictureStylePreset;
import dji.common.camera.SettingsDefinitions.SSDColor;
import dji.common.camera.SettingsDefinitions.VideoFileCompressionStandard;
import dji.common.camera.SettingsDefinitions.VideoFileFormat;
import dji.common.camera.SettingsDefinitions.VideoFrameRate;
import dji.common.camera.SettingsDefinitions.VideoResolution;
import dji.common.camera.SettingsDefinitions.VideoStandard;
import dji.common.camera.SettingsDefinitions.WhiteBalancePreset;
import dji.common.camera.WhiteBalance;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

import static dji.ux.beta.core.v4.ListItem.ItemType.PARENT_TYPE;

/**
 * Video specific settings that are separated into 6 top-level categories
 */
public class CameraVideoSettingListView extends CameraSettingListView {

    //region Properties
    private ListItem videoSizeItem;
    private ListItem videoFormatItem;
    private ListItem videoTypeItem;
    private ListItem whiteBalanceItem;
    private ListItem pictureStyleItem;
    private ListItem colorItem;
    private ListItem ssdEnabledItem;
    private ListItem ssdVideoLicenseItem;
    private ListItem ssdVideoSizeItem;
    private ListItem eiEnabledItem;
    private ListItem ssdColorItem;
    private ListItem videoCompressionFormatItem;
    private ListItem videoStorage;

    private DJIKey videoSizeAndRateKey;
    private DJIKey videoFormatKey;
    private DJIKey videoStandardKey;
    private DJIKey whiteBalanceKey;
    private DJIKey whiteBalanceRangeKey;
    private DJIKey colorKey;
    private String[] filterNameArray;
    private String[] ssdColorNameArray;
    private String[] licenseNameArray;
    private String[] videoCompressionStandardNameArray;
    private TypedArray whiteBalanceImgRes;
    private DJIKey pictureStyleKey;
    private DJIKey videoSizeAndRateRangeKey;
    private DJIKey videoFormatRangeKey;
    private DJIKey colorRangeKey;
    private DJIKey videoStandardRangeKey;
    private DJIKey ssdEnabledKey;
    private DJIKey isEISupportedKey;
    private DJIKey exposureIndexEnabledKey;
    private DJIKey isSSDSupportedKey;
    private DJIKey ssdVideoSizeAndRateKey;
    private DJIKey ssdVideoSizeAndRateRangeKey;
    private DJIKey ssdVideoLicenseKey;
    private DJIKey ssdColorKey;
    private DJIKey ssdColorRangeKey;
    private DJIKey cameraTypeKey;
    private CameraKey eiColorKey;
    private EIColor eiColor;
    private boolean isSSDSupported;
    private boolean isSSDEnabled;
    private boolean isEIEnabled;
    private CameraColor cameraColor;
    private String[] eiColorArray;
    private SSDColor ssdColor;
    private CameraColor[] colorRange;
    private VideoFileCompressionStandard[] videoFileCompressionStandardRangeArray;
    private DJIKey videoCompressionFormatRangeKey;
    private DJIKey videoCompressionFormatKey;
    private ResolutionAndFrameRate[] videoSizeRange;
    private WhiteBalancePreset[] whiteBalancePresets;
    private VideoFileFormat[] videoFormatRange;
    private VideoStandard[] videoStandardRange;
    private ResolutionAndFrameRate[] ssdVideoSizeRange;
    private SSDColor[] ssdColorRange;

    //endregion

    //region Constructors
    public CameraVideoSettingListView(Context context) {
        super(context, null, 0);
    }

    public CameraVideoSettingListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public CameraVideoSettingListView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected void onInitData() {
        filterNameArray = getResources().getStringArray(R.array.uxsdk_camera_filter_type);
        eiColorArray = getResources().getStringArray(R.array.uxsdk_camera_ei_color_type);
        ssdColorNameArray = getResources().getStringArray(R.array.uxsdk_camera_ssd_color_array);
        licenseNameArray = getResources().getStringArray(R.array.uxsdk_camera_ssd_license_type);
        whiteBalanceImgRes = getResources().obtainTypedArray(R.array.uxsdk_camera_white_balance_img_array);
        videoCompressionStandardNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_compression_standard_name_array);
        videoSizeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_resolution, PARENT_TYPE));
        videoFormatItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_format, PARENT_TYPE));
        videoTypeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_standard_name, PARENT_TYPE));
        whiteBalanceItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_white_balance, PARENT_TYPE));
        pictureStyleItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_picture_style, PARENT_TYPE));
        colorItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_filter, PARENT_TYPE));
        ssdEnabledItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_ssd, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        ssdVideoLicenseItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_ssd_video_license, PARENT_TYPE));
        ssdVideoSizeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_ssd_video_size, PARENT_TYPE));
        eiEnabledItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_ei, ListItem.ItemType.SWITCH_BUTTON_TYPE));
        ssdColorItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_ssd_looks, PARENT_TYPE));
        videoCompressionFormatItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_video_compression_standard_label, PARENT_TYPE));
        videoStorage = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_stream_video_storage ,PARENT_TYPE ) );
    }

    //endregion

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        subscription.add(
                UXSDKEventBus.getInstance()
                        .register(Events.CameraBusyEvent.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Events.CameraBusyEvent>() {
                            @Override
                            public void call(Events.CameraBusyEvent event) {
                                isCameraBusy = event.isBusy();
                                updateVideoCompressionFormatRange();
                                updateItemState(videoSizeItem, videoSizeRange);
                                updateItemState(videoFormatItem, videoFormatRange);
                                updateItemState(videoTypeItem, videoStandardRange);
                                if (isSSDSupported) {
                                    updateItemState(ssdVideoSizeItem, ssdVideoSizeRange);
                                } else {
                                    updateItem(ssdVideoSizeItem, State.HIDDEN);
                                }
                            }
                        })
        );
    }

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (view instanceof SwitchButton) {
            SwitchButton switchButton = (SwitchButton) view;
            switchButton.setChecked(switchButton.isChecked());
            if (item.equals(ssdEnabledItem)) {
                handleSSDEnabledSwitchChecked(switchButton.isChecked());
            } else if (item.equals(eiEnabledItem)) {
                handleEIModeSwitchChecked(switchButton.isChecked());
            }
            return;
        }

        removeChildViewIfNeeded();
        if (item.equals(videoSizeItem)) {
            childView = new CameraVideoSizeListWidget(getContext());
        } else if (item.equals(videoFormatItem)) {
            childView = new CameraVideoFormatListWidget(getContext());
        } else if (item.equals(videoTypeItem)) {
            childView = new CameraVideoStandardListWidget(getContext());
        } else if (item.equals(whiteBalanceItem)) {
            childView = new CameraWhiteBalanceListWidget(getContext());
        } else if (item.equals(pictureStyleItem)) {
            childView = new CameraStyleListWidget(getContext());
        } else if (item.equals(colorItem)) {
            childView = new CameraFilterListWidget(getContext());
        } else if (item.equals(ssdVideoLicenseItem)) {
            childView = new CameraSSDVideoLicenseListWidget(getContext());
        } else if (item.equals(ssdVideoSizeItem)) {
            childView = new CameraSSDVideoSizeListWidget(getContext());
        } else if (item.equals(ssdColorItem)) {
            childView = new CameraSSDColorListWidget(getContext());
        } else if (item.equals(videoCompressionFormatItem)) {
            childView = new CameraVideoCompressionStandardListWidget(getContext());
        } else if (item.equals(videoStorage)) {
            childView = new CameraVideoStreamListWidget(getContext());
        } else {
            childView = null;
        }
        showChildView();
    }

    @Override
    protected void onUpdateDefaultSetting() {
        updateVideoSize(VideoResolution.RESOLUTION_4096x2160, VideoFrameRate.FRAME_RATE_30_FPS);
        updateVideoFormat(VideoFileFormat.MOV);
        updateWhiteBalance(WhiteBalancePreset.AUTO);
        updatePhotoStyle(0);
        updatePhotoColor(CameraColor.NONE);
        updateSSDEnabledUI(false);
    }

    //endregion

    //region Key life cycle
    @Override
    public void initKey() {
        videoSizeAndRateKey = CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, keyIndex);
        videoFormatKey = CameraUtil.createCameraKeys(CameraKey.VIDEO_FILE_FORMAT, keyIndex, subKeyIndex);
        videoStandardKey = CameraKey.create(CameraKey.VIDEO_STANDARD, keyIndex);
        whiteBalanceKey = CameraKey.create(CameraKey.WHITE_BALANCE, keyIndex);
        whiteBalanceRangeKey = CameraUtil.createCameraKeys(CameraKey.WHITE_BALANCE_PRESENT_RANGE, keyIndex, subKeyIndex);
        pictureStyleKey = CameraKey.create(CameraKey.PICTURE_STYLE_PRESET, keyIndex);
        colorKey = CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR, keyIndex, subKeyIndex);

        ssdEnabledKey = CameraKey.create(CameraKey.SSD_VIDEO_RECORDING_ENABLED, keyIndex);
        isSSDSupportedKey = CameraKey.create(CameraKey.IS_SSD_SUPPORTED, keyIndex);

        videoSizeAndRateRangeKey = CameraKey.create(CameraKey.VIDEO_RESOLUTION_FRAME_RATE_RANGE, keyIndex);
        videoFormatRangeKey = CameraUtil.createCameraKeys(CameraKey.VIDEO_FILE_FORMAT_RANGE, keyIndex, subKeyIndex);
        colorRangeKey = CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR_RANGE, keyIndex, subKeyIndex);
        videoStandardRangeKey = CameraUtil.createCameraKeys(CameraKey.VIDEO_STANDARD_RANGE, keyIndex, subKeyIndex);

        ssdVideoLicenseKey = CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, keyIndex);
        ssdVideoSizeAndRateKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE, keyIndex);
        ssdVideoSizeAndRateRangeKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_FRAME_RATE_RANGE, keyIndex);

        isEISupportedKey = CameraKey.create(CameraKey.IS_EI_MODE_SUPPORTED, keyIndex);
        exposureIndexEnabledKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, keyIndex);

        ssdColorKey = CameraKey.create(CameraKey.SSD_COLOR, keyIndex);
        ssdColorRangeKey = CameraKey.create(CameraKey.SSD_COLOR_RANGE, keyIndex);

        eiColorKey = CameraKey.create(CameraKey.EI_COLOR, keyIndex);
        videoCompressionFormatKey = CameraKey.create(CameraKey.VIDEO_FILE_COMPRESSION_STANDARD);
        videoCompressionFormatRangeKey = CameraKey.create(CameraKey.VIDEO_COMPRESSION_STANDARD_RANGE);

        cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);

        addDependentKey(videoSizeAndRateKey);
        addDependentKey(videoFormatKey);
        addDependentKey(videoStandardKey);
        addDependentKey(whiteBalanceKey);
        addDependentKey(whiteBalanceRangeKey);
        addDependentKey(pictureStyleKey);
        addDependentKey(colorKey);

        addDependentKey(videoSizeAndRateRangeKey);
        addDependentKey(videoFormatRangeKey);
        addDependentKey(colorRangeKey);
        addDependentKey(videoStandardRangeKey);

        addDependentKey(ssdEnabledKey);
        addDependentKey(isSSDSupportedKey);
        addDependentKey(ssdVideoLicenseKey);
        addDependentKey(ssdVideoSizeAndRateKey);
        addDependentKey(ssdVideoSizeAndRateRangeKey);

        addDependentKey(isEISupportedKey);
        addDependentKey(exposureIndexEnabledKey);

        addDependentKey(ssdColorKey);
        addDependentKey(ssdColorRangeKey);

        addDependentKey(eiColorKey);

        addDependentKey(videoCompressionFormatKey);
        addDependentKey(videoCompressionFormatRangeKey);

        addDependentKey(cameraTypeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (value != null && key != null) {
            if (key.equals(videoCompressionFormatRangeKey)) {
                videoFileCompressionStandardRangeArray = (VideoFileCompressionStandard[]) value;
                updateVideoCompressionFormatRange();
            } else if (key.equals(videoCompressionFormatKey)) {
                updateVideoCompressionFormatStandard((VideoFileCompressionStandard) value);
            } else if (key.equals(videoSizeAndRateKey)) {
                ResolutionAndFrameRate resolutionAndFrameRate = (ResolutionAndFrameRate) value;
                updateVideoSize(resolutionAndFrameRate.getResolution(), resolutionAndFrameRate.getFrameRate());
            } else if (key.equals(videoFormatKey)) {
                updateVideoFormat((VideoFileFormat) value);
            } else if (key.equals(videoStandardKey)) {
                updateVideoStandard((VideoStandard) value);
            } else if (key.equals(whiteBalanceKey)) {
                WhiteBalance whiteBalance = (WhiteBalance) value;
                updateWhiteBalance(whiteBalance.getWhiteBalancePreset());
            } else if (key.equals(whiteBalanceRangeKey)) {
                whiteBalancePresets = (WhiteBalancePreset[]) value;
                updateItemState(whiteBalanceItem, whiteBalancePresets);
            } else if (key.equals(pictureStyleKey)) {
                PictureStylePreset style = (PictureStylePreset) value;
                updatePhotoStyle(style.presetType().value());
            } else if (key.equals(colorKey)) {
                //DJILog.d("LWF", "transformValue colorkey value " + value);
                cameraColor = (CameraColor) value;
                updateColor(isSSDEnabled, isEIEnabled);
            } else if (key.equals(videoSizeAndRateRangeKey)) {
                videoSizeRange = (ResolutionAndFrameRate[]) value;
                updateItemState(videoSizeItem, videoSizeRange);
            } else if (key.equals(videoFormatRangeKey)) {
                videoFormatRange = (VideoFileFormat[]) value;
                if (videoFormatRange.length == 1) {
                    updateVideoFormat(videoFormatRange[0]);
                }
                updateItemState(videoFormatItem, videoFormatRange);
            } else if (key.equals(colorRangeKey)) {
                colorRange = (CameraColor[]) value;
                //DJILog.d("LWF", "transformValue colorRange value " + colorRange);
                if (colorRange.length == 1) {
                    cameraColor = colorRange[0];
                }
                updateItemState(colorItem, colorRange);
                updateColor(isSSDEnabled, isEIEnabled);
            } else if (key.equals(videoStandardRangeKey)) {
                videoStandardRange = (VideoStandard[]) value;
                if (videoStandardRange.length == 1) {
                    updateVideoStandard(videoStandardRange[0]);
                }
                updateItemState(videoTypeItem, videoStandardRange);
            } else if (key.equals(isSSDSupportedKey)) {
                isSSDSupported = (boolean) value;
                updateSSDSupportedItems();
                updateSSDSupportedUI(isSSDSupported);
            } else if (key.equals(ssdEnabledKey)) {
                isSSDEnabled = (boolean) value;
                updateColorItemTitle(isSSDEnabled);
                updateSSDEnabledUI(isSSDEnabled);
                updateColor(isSSDEnabled, isEIEnabled);
                updateSSDLooks(isSSDEnabled);
                updateSSDSupportedItems();
            } else if (key.equals(ssdVideoLicenseKey)) {
                CameraSSDVideoLicense license = (CameraSSDVideoLicense) value;
                updateSSDLicense(license);
            } else if (key.equals(ssdVideoSizeAndRateKey)) {
                ResolutionAndFrameRate resolutionAndFrameRate = (ResolutionAndFrameRate) value;
                updateVideoSizeForItem(ssdVideoSizeItem, resolutionAndFrameRate.getResolution(), resolutionAndFrameRate.getFrameRate());
            } else if (key.equals(ssdVideoSizeAndRateRangeKey)) {
                ssdVideoSizeRange = (ResolutionAndFrameRate[]) value;
                if (ssdVideoSizeRange.length == 1) {
                    updateVideoSize(ssdVideoSizeRange[0].getResolution(), ssdVideoSizeRange[0].getFrameRate());
                }
                updateItemState(ssdVideoSizeItem, ssdVideoSizeRange);
            } else if (key.equals(isEISupportedKey)) {
                updateItem(eiEnabledItem, (boolean) value ? State.VISIBLE : State.DISABLED);
            } else if (key.equals(exposureIndexEnabledKey)) {
                isEIEnabled = (value == ExposureSensitivityMode.EI);
                updateEIModeUI(isEIEnabled);
                updateColor(isSSDEnabled, isEIEnabled);
            } else if (key.equals(ssdColorKey)) {
                ssdColor = (SSDColor) value;
                //DJILog.d("LWF", "Videosetting list ssd color value is " + ssdColor);
                updateSSDColor();
            } else if (key.equals(ssdColorRangeKey)) {
                ssdColorRange = (SSDColor[]) value;
                //DJILog.d("LWF", "Videosetting ssdColor range is " + ssdColorRange + " o is " + ssdColorRange[0]);
                if (ssdColorRange.length == 1) {
                    ssdColor = ssdColorRange[0];
                }
                updateItemState(ssdColorItem, ssdColorRange);
                updateSSDColor();
                updateSSDSupportedItems();
            } else if (key.equals(eiColorKey)) {
                eiColor = (EIColor) value;
                updateColor(isSSDEnabled, isEIEnabled);
            } else if (key.equals(cameraTypeKey)) {
                SettingsDefinitions.CameraType cameraType = (SettingsDefinitions.CameraType) value;
                if (!CameraUtil.isSupportCameraStyle(cameraType)) {
                    updateItem(pictureStyleItem, State.HIDDEN);
                }
            }
        }
    }
    //endregion

    private void updateSSDLooks(boolean flag) {
        updateItem(ssdColorItem, flag ? State.VISIBLE : State.DISABLED);
    }

    private void updateVideoSize(VideoResolution ratio, VideoFrameRate frameRate) {
        updateVideoSizeForItem(videoSizeItem, ratio, frameRate);
    }

    private void updateVideoSizeForItem(ListItem item, VideoResolution resolution, VideoFrameRate frameRate) {
        int resolutionValue = resolution.value();
        int rate = frameRate.value();
        if (resolution == VideoResolution.NO_SSD_VIDEO) {
            rate = 0;
        }
        int resId = 0;
        if (resolutionValue < CameraResource.videoFpsImgResIds.length
                && rate < CameraResource.videoFpsImgResIds[resolutionValue].length) {
            resId = CameraResource.videoFpsImgResIds[resolutionValue][rate];
        }
        updateItem(item, resolutionValue, resId);
    }

    private void updateVideoFormat(VideoFileFormat format) {
        final int videoFormat = format.value();
        int resId = 0;
        if (videoFormat < CameraResource.videoFormatImgRes.length) {
            resId = CameraResource.videoFormatImgRes[videoFormat];
        }
        updateItem(videoFormatItem, videoFormat, resId);
    }

    private void updateVideoStandard(VideoStandard standard) {
        final int videoStandard = standard.value();
        int resId = 0;
        if (videoStandard < CameraResource.videoTypeImgRes.length) {
            resId = CameraResource.videoTypeImgRes[videoStandard];
        }
        updateItem(videoTypeItem, videoStandard, resId);
    }

    private void updateWhiteBalance(WhiteBalancePreset wb) {
        if (wb != null) {
            final int value = wb.value();
            updateItem(whiteBalanceItem, value, whiteBalanceImgRes.getResourceId(value, 0));
        }
    }

    private void updatePhotoStyle(int style) {
        updateItem(pictureStyleItem, style, CameraResource.pictureStyleImgRes[style]);
    }

    private void updateColor(boolean isSSDEnabled, boolean isEIEnabled) {
        if (isSSDEnabled && isEIEnabled) {
            updateEIColor(eiColor);
        } else {
            updatePhotoColor(cameraColor);
        }
    }

    private void updateEIColor(EIColor eiColor) {
        EIColor[] eiColorRange = EIColor.getValues();
        for (int i = 0; i < eiColorRange.length; i++) {
            if (eiColorRange[i] == eiColor && eiColorArray != null && i < eiColorArray.length) {
                updateItem(colorItem, eiColor.value(), eiColorArray[i], 0);
                break;
            }
        }
    }

    private boolean isColorValid(CameraColor[] range, CameraColor color) {
        if (range != null) {

            for (int i = 0; i < range.length; i++) {
                if (range[i] == color) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateVideoCompressionFormatRange() {
        if (videoFileCompressionStandardRangeArray == null || videoFileCompressionStandardRangeArray.length <= 1) {
            updateItem(videoCompressionFormatItem, State.HIDDEN);
        } else if (isCameraBusy) {
            updateItem(videoCompressionFormatItem, State.DISABLED);
        } else {
            updateItem(videoCompressionFormatItem, State.VISIBLE);
        }
    }

    private void updateVideoCompressionFormatStandard(VideoFileCompressionStandard videoFileCompressionStandard) {
        if (isVideoCompressionFormatValid(videoFileCompressionStandardRangeArray, videoFileCompressionStandard)) {
            int value = videoFileCompressionStandard.value();
            updateItem(videoCompressionFormatItem, value, videoCompressionStandardNameArray[value], 0);
        }
    }

    private boolean isVideoCompressionFormatValid(VideoFileCompressionStandard[] range, VideoFileCompressionStandard videoFileCompressionStandard) {
        if (range != null) {
            for (int i = 0; i < range.length; i++) {
                if (range[i] == videoFileCompressionStandard) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updatePhotoColor(CameraColor filterObj) {
        if (filterObj != null && colorRange != null && colorRange.length > 0) {
            int value = filterObj.value();
            if (isColorValid(colorRange, filterObj)) {
                if (filterNameArray != null && value < filterNameArray.length) {
                    updateItem(colorItem, value, filterNameArray[value], 0);
                }
            } else {
                // Fixed the DJIGo UI bug that just set the Color to "Normal" from "White&Black" only on UI
                final CameraColor newFilter = colorRange[0];
                KeyManager.getInstance().setValue(colorKey, newFilter, new SetCallback() {
                    @Override
                    public void onSuccess() {
                        DJILog.d(TAG, "Camera Color " + newFilter.name() + " set successfully");
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        DJILog.d(TAG, "Failed to set Camera Color");
                    }
                });
            }
        }
    }

    private void updateSSDColor() {
        if (ssdColor != null) {
            int value = ssdColor.value();
            Integer nameIndex = CameraUtil.getSSDColorIndex(ssdColor);
            if (nameIndex != null && ssdColorNameArray != null && nameIndex < ssdColorNameArray.length) {
                updateItem(ssdColorItem, value, ssdColorNameArray[nameIndex], 0);
            }
        }
    }

    private void updateColorItemTitle(boolean isSSDEnabled) {
        int titleId = R.string.uxsdk_camera_filter;
        if (isSSDEnabled) {
            titleId = R.string.uxsdk_camera_looks;
        }
        updateItemTitle(colorItem, getResources().getString(titleId));
    }

    private void updateSSDSupportedUI(boolean isSSDSupported) {
        State state = isSSDSupported ? State.VISIBLE : State.HIDDEN;
        updateItem(ssdEnabledItem, state);
    }

    private void updateSSDEnabledUI(boolean isSSDEnabled) {
        int switchPosition = isSSDEnabled ? 1 : 0;
        updateItem(ssdEnabledItem, switchPosition, 0);
    }

    // only set hidden when we know we have the right value for isSSDSupported
    private void updateSSDSupportedItems() {
        State state = State.VISIBLE;
        if (!isSSDSupported) {
            state = State.HIDDEN;
        } else if (!isSSDEnabled) {
            state = State.DISABLED;
        }
        updateItem(ssdVideoLicenseItem, state);
        updateItem(ssdVideoSizeItem, state);
        updateItem(ssdColorItem, state);
    }

    private void updateSSDLicense(CameraSSDVideoLicense license) {
        int value = license.value();

        if (licenseNameArray != null && licenseNameArray.length > value) {
            updateItem(ssdVideoLicenseItem, value, licenseNameArray[value], 0);
        }
    }

    private void updateEIModeUI(boolean isChecked) {
        updateItem(eiEnabledItem, (isChecked ? 1 : 0), 0);
    }

    private void handleSSDEnabledSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }
        KeyManager.getInstance().setValue(ssdEnabledKey, isChecked, new SetCallback() {
            @Override
            public void onSuccess() {
                //DJILog.d("LWF", "SSD enabled " + isChecked + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                //DJILog.d("LWF", "SSD enabled failed " + error.getDescription());
                post(new Runnable() {
                    @Override
                    public void run() {
                        isSSDEnabled = !isChecked;
                        updateSSDEnabledUI(isSSDEnabled);
                    }
                });
            }
        });
    }

    private void handleEIModeSwitchChecked(final boolean isChecked) {
        if (KeyManager.getInstance() == null) {
            return;
        }
        ExposureSensitivityMode mode = ExposureSensitivityMode.ISO;
        if (isChecked) {
            mode = ExposureSensitivityMode.EI;
        }
        KeyManager.getInstance().setValue(exposureIndexEnabledKey, mode, new SetCallback() {
            @Override
            public void onSuccess() {
                //DJILog.d("LWF", "EI Mode set successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                //DJILog.d("LWF", "EI Mode set failed " + error.getDescription());
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateEIModeUI(!isChecked);
                    }
                });
            }
        });
    }
}
