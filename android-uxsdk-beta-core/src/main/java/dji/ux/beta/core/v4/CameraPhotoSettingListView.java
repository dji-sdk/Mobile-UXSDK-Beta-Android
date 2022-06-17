package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import dji.common.bus.UXSDKEventBus;
import dji.common.camera.PhotoTimeLapseSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.CameraColor;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.camera.SettingsDefinitions.PhotoAEBCount;
import dji.common.camera.SettingsDefinitions.PhotoAspectRatio;
import dji.common.camera.SettingsDefinitions.PhotoBurstCount;
import dji.common.camera.SettingsDefinitions.PhotoFileFormat;
import dji.common.camera.SettingsDefinitions.PhotoPanoramaMode;
import dji.common.camera.SettingsDefinitions.PhotoTimeIntervalSettings;
import dji.common.camera.SettingsDefinitions.PictureStylePreset;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.common.camera.SettingsDefinitions.WhiteBalancePreset;
import dji.common.camera.WhiteBalance;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

import static dji.ux.beta.core.v4.ListItem.ItemType.PARENT_TYPE;


/**
 * Photo specific settings that are separated into 6 top-level categories
 */
public class CameraPhotoSettingListView extends CameraSettingListView {

    //region Properties
    private ListItem photoModeItem;
    private ListItem imageRatioItem;
    private ListItem imageFormatItem;
    private ListItem whiteBalanceItem;
    private ListItem pictureStyleItem;
    private ListItem colorItem;
    private ListItem photoStorage;

    private DJIKey photoModeKey;
    private DJIKey imageRatioKey;
    private DJIKey imageFormatKey;
    private DJIKey whiteBalanceKey;
    private DJIKey pictureStyleKey;
    private DJIKey colorKey;
    private String[] filterNameArray;
    private TypedArray pictureFormatImgRes;
    private TypedArray pictureSizeImgRes;
    private TypedArray whiteBalanceImgRes;
    private DJIKey intervalParamKey;
    private DJIKey aebParamKey;
    private DJIKey burstCountKey;
    private DJIKey rawBurstCountKey;
    private DJIKey timeLapseParamKey;
    private PhotoTimeIntervalSettings intervalParam;
    private PhotoAEBCount aebParam;
    private PhotoBurstCount burstCount;
    private PhotoTimeLapseSettings timeLapseParam;
    private ShootPhotoMode photoMode;
    private PhotoBurstCount rawBurstCount;
    private DJIKey panoramaModeKey;
    private PhotoPanoramaMode panoramaMode;
    private DJIKey imageRatioRangeKey;
    private DJIKey imageFormatRangeKey;
    private DJIKey colorRangeKey;
    private DJIKey whiteBalanceRangeKey;
    private CameraKey cameraTypeKey;
    private PhotoAspectRatio[] imageRatioRange;
    private PhotoFileFormat[] imageFormatRange;
    private DJIKey isFlatCameraModeSupportedKey;
    private DJIKey flatCameraModeKey;
    private boolean isFlatCameraModeSupported;
    private FlatCameraMode flatCameraMode;

    //endregion

    //region Constructors
    public CameraPhotoSettingListView(Context context) {
        super(context, null, 0);
    }

    public CameraPhotoSettingListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public CameraPhotoSettingListView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    //endregion

    //region List View Init
    @Override
    protected void onInitData() {
        filterNameArray = getResources().getStringArray(R.array.uxsdk_camera_filter_type);
        pictureFormatImgRes = getResources().obtainTypedArray(R.array.uxsdk_camera_picture_format_img_res_array);
        pictureSizeImgRes = getResources().obtainTypedArray(R.array.uxsdk_camera_photo_aspect_ratio_img_array);
        whiteBalanceImgRes = getResources().obtainTypedArray(R.array.uxsdk_camera_white_balance_img_array);

        photoModeItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_photo_mode, PARENT_TYPE));
        imageRatioItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_picture_size, PARENT_TYPE));
        imageFormatItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_photo_format, PARENT_TYPE));
        whiteBalanceItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_white_balance, PARENT_TYPE));
        pictureStyleItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_picture_style, PARENT_TYPE));
        colorItem = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_filter, PARENT_TYPE));
        photoStorage = addItem(new ListItem.ItemProperty(R.string.uxsdk_camera_photo_storage ,PARENT_TYPE ) );
    }

    @Override
    protected void onUpdateDefaultSetting() {
        updatePhotoMode(ShootPhotoMode.SINGLE);
        updateImageRatio(PhotoAspectRatio.RATIO_4_3);
        updateImageFormat(PhotoFileFormat.JPEG);
        updateWhiteBalance(WhiteBalancePreset.AUTO, 0);
        updatePhotoStyle(SettingsDefinitions.PictureStylePresetType.STANDARD.value());
        updatePhotoColor(CameraColor.NONE);
    }

    //endregion

    //region View life cycle

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
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
                                 updateItemState(photoModeItem);
                                 updateItemState(imageRatioItem, imageRatioRange);
                                 updateItemState(imageFormatItem, imageFormatRange);
                             }
                         })
        );
    }

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        removeChildViewIfNeeded();
        if (item.equals(photoModeItem)) {
            childView = new CameraPhotoModeListWidget(getContext());
        } else if (item.equals(imageRatioItem)) {
            childView = new CameraImageRatioListWidget(getContext());
        } else if (item.equals(imageFormatItem)) {
            childView = new CameraImageFormatListWidget(getContext());
        } else if (item.equals(whiteBalanceItem)) {
            childView = new CameraWhiteBalanceListWidget(getContext());
        } else if (item.equals(pictureStyleItem)) {
            childView = new CameraStyleListWidget(getContext());
        } else if (item.equals(colorItem)) {
            childView = new CameraFilterListWidget(getContext());
        } else if (item.equals(photoStorage)) {
            childView = new CameraPhotoStreamListWidget(getContext());
        }
        else {
            childView = null;
        }
        showChildView();
    }
    //endregion

    //region Key life cycle
    @Override
    public void initKey() {
        photoModeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, keyIndex);
        imageRatioKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_ASPECT_RATIO, keyIndex, subKeyIndex);
        imageFormatKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_FILE_FORMAT, keyIndex,subKeyIndex);
        whiteBalanceKey = CameraUtil.createCameraKeys(CameraKey.WHITE_BALANCE, keyIndex,subKeyIndex);
        pictureStyleKey = CameraKey.create(CameraKey.PICTURE_STYLE_PRESET, keyIndex);
        colorKey =  CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR, keyIndex,subKeyIndex);
        intervalParamKey = CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, keyIndex);
        aebParamKey = CameraKey.create(CameraKey.PHOTO_AEB_COUNT, keyIndex);
        burstCountKey = CameraKey.create(CameraKey.PHOTO_BURST_COUNT, keyIndex);
        rawBurstCountKey = CameraKey.create(CameraKey.PHOTO_RAW_BURST_COUNT, keyIndex);
        timeLapseParamKey = CameraKey.create(CameraKey.PHOTO_TIME_LAPSE_SETTINGS, keyIndex); // Get as well
        panoramaModeKey = CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, keyIndex);

        whiteBalanceRangeKey = CameraUtil.createCameraKeys(CameraKey.WHITE_BALANCE_PRESENT_RANGE, keyIndex, subKeyIndex);
        cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);

        imageFormatRangeKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_FILE_FORMAT_RANGE, keyIndex, subKeyIndex);
        imageRatioRangeKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_ASPECT_RATIO_RANGE, keyIndex, subKeyIndex);
        colorRangeKey = CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR_RANGE, keyIndex, subKeyIndex);
        isFlatCameraModeSupportedKey = CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, keyIndex);
        flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, keyIndex);

        addDependentKey(photoModeKey);
        addDependentKey(imageRatioKey);
        addDependentKey(imageFormatKey);
        addDependentKey(whiteBalanceKey);
        addDependentKey(pictureStyleKey);
        addDependentKey(colorKey);
        addDependentKey(intervalParamKey);
        addDependentKey(aebParamKey);
        addDependentKey(burstCountKey);
        addDependentKey(rawBurstCountKey);
        addDependentKey(timeLapseParamKey);
        addDependentKey(panoramaModeKey);
        addDependentKey(imageRatioRangeKey);
        addDependentKey(imageFormatRangeKey);
        addDependentKey(colorRangeKey);
        addDependentKey(whiteBalanceRangeKey);
        addDependentKey(cameraTypeKey);
        addDependentKey(isFlatCameraModeSupportedKey);
        addDependentKey(flatCameraModeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(photoModeKey)) {
            photoMode = (ShootPhotoMode) value;
            updatePhotoMode();
        } else if (key.equals(imageRatioKey)) {
            updateImageRatio((PhotoAspectRatio) value);
        } else if (key.equals(imageFormatKey)) {
            updateImageFormat((PhotoFileFormat) value);
        } else if (key.equals(whiteBalanceKey)) {
            WhiteBalance wb = (WhiteBalance) value;
            updateWhiteBalance(wb.getWhiteBalancePreset(), wb.getColorTemperature());
        } else if (key.equals(pictureStyleKey)) {
            PictureStylePreset style = (PictureStylePreset) value;
            updatePhotoStyle(style.presetType().value());
        } else if (key.equals(colorKey)) {
            updatePhotoColor((CameraColor) value);
        } else if (key.equals(intervalParamKey)) {
            intervalParam = (PhotoTimeIntervalSettings) value;
            if (photoMode == ShootPhotoMode.INTERVAL || flatCameraMode == FlatCameraMode.PHOTO_INTERVAL) {
                updatePhotoMode();
            }
        } else if (key.equals(aebParamKey)) {
            aebParam = (PhotoAEBCount) value;
            if (photoMode == ShootPhotoMode.AEB || flatCameraMode == FlatCameraMode.PHOTO_AEB) {
                updatePhotoMode();
            }
        } else if (key.equals(burstCountKey)) {
            burstCount = (PhotoBurstCount) value;
            if (photoMode == ShootPhotoMode.BURST || flatCameraMode == FlatCameraMode.PHOTO_BURST) {
                updatePhotoMode();
            }
        } else if (key.equals(rawBurstCountKey)) {
            rawBurstCount = (PhotoBurstCount) value;
            if (photoMode == ShootPhotoMode.RAW_BURST) {
                updatePhotoMode();
            }
        } else if (key.equals(timeLapseParamKey)) {
            timeLapseParam = (PhotoTimeLapseSettings) value;
            if (photoMode == ShootPhotoMode.TIME_LAPSE || flatCameraMode == FlatCameraMode.PHOTO_TIME_LAPSE) {
                updatePhotoMode();
            }
        } else if (key.equals(panoramaModeKey)) {
            panoramaMode = (PhotoPanoramaMode) value;
            if (photoMode == ShootPhotoMode.PANORAMA || flatCameraMode == FlatCameraMode.PHOTO_PANORAMA) {
                updatePhotoMode();
            }
        } else if (key.equals(imageRatioRangeKey)) {
            imageRatioRange = (PhotoAspectRatio[])value;
            if (imageRatioRange.length == 1) {
                updateImageRatio(imageRatioRange[0]);
            }
            updateItemState(imageRatioItem, imageRatioRange);
        } else if (key.equals(imageFormatRangeKey)) {
            imageFormatRange = (PhotoFileFormat[])value;
            if (imageFormatRange.length == 1) {
                updateImageFormat(imageFormatRange[0]);
            }
            updateItemState(imageFormatItem, imageFormatRange);
        } else if (key.equals(colorRangeKey)) {
            CameraColor[] colorRange = (CameraColor[])value;
            if (colorRange.length == 1) {
                updatePhotoColor(colorRange[0]);
            }
            updateItemState(colorItem, colorRange);
        } else if (key.equals(cameraTypeKey)) {
            SettingsDefinitions.CameraType cameraType = (SettingsDefinitions.CameraType) value;
            if (!CameraUtil.isSupportCameraStyle(cameraType)) {
                updateItem(pictureStyleItem, State.HIDDEN);
            }
        } else if (key.equals(whiteBalanceRangeKey)) {
            WhiteBalancePreset[] whiteBalancePresets = (WhiteBalancePreset[]) value;
            updateItemState(whiteBalanceItem, whiteBalancePresets);
        } else if (key.equals(isFlatCameraModeSupportedKey)) {
            isFlatCameraModeSupported = (Boolean) value;
            updatePhotoMode();
        } else if (key.equals(flatCameraModeKey)) {
            flatCameraMode = (FlatCameraMode) value;
            updatePhotoMode();
        }
    }

    private void updateImageRatio(PhotoAspectRatio ratio) {

        int value = ratio.value();
        if (value < pictureSizeImgRes.length()) {
            updateItem(imageRatioItem, value, pictureSizeImgRes.getResourceId(value, 0));
        }
    }

    private void updateImageFormat(PhotoFileFormat format) {
        int value = format.value();
        if (value < pictureFormatImgRes.length()) {
            updateItem(imageFormatItem, value, pictureFormatImgRes.getResourceId(value, 0));
        }
    }

    private void updateWhiteBalance(final WhiteBalancePreset wb, final int colorTemp) {
        if (wb != null) {
            int value = wb.value();
            if (value < whiteBalanceImgRes.length()) {
                updateItem(whiteBalanceItem, value, whiteBalanceImgRes.getResourceId(value, 0));
            }
        }
    }

    private void updatePhotoStyle(int style) {
        if (style < CameraResource.pictureStyleImgRes.length) {
            updateItem(pictureStyleItem, style, CameraResource.pictureStyleImgRes[style]);
        }
    }

    private void updatePhotoColor(CameraColor filterObj) {

        int value = filterObj.value();
        if (filterNameArray != null && value < filterNameArray.length) {
            updateItem(colorItem, value, filterNameArray[value], 0);
        }
    }

    private void updatePhotoStorage() {

    }

    private void updatePhotoMode() {
        if (isFlatCameraModeSupported) {
            updatePhotoMode(CameraUtil.toShootPhotoMode(flatCameraMode));
        } else {
            updatePhotoMode(photoMode);
        }
    }

    private void updatePhotoMode(ShootPhotoMode photoMode) {
        if (photoMode == null) return;
        final int photoModeValue = photoMode.value();
        int resId;
        if (ShootPhotoMode.BURST.value() == photoModeValue && burstCount != null) {
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, burstCount.value());
        } else if (ShootPhotoMode.AEB.value() == photoModeValue && aebParam != null) {
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, aebParam.value());
        } else if (ShootPhotoMode.INTERVAL.value() == photoModeValue && intervalParam != null) {
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, intervalParam.getTimeIntervalInSeconds());
        } else if (ShootPhotoMode.RAW_BURST.value() == photoModeValue && rawBurstCount != null){
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, rawBurstCount.value());
        } else if (ShootPhotoMode.PANORAMA.value() == photoModeValue && panoramaMode != null){
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, panoramaMode.value());
        }else {
            resId = CameraResource.getPhotoModeImgResId(photoModeValue, 0);
        }

        updateItem(photoModeItem, photoModeValue, resId);
    }
}

//endregion

