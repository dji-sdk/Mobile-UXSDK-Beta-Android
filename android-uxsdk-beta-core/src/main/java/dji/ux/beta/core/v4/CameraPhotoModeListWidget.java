package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.camera.PhotoTimeLapseSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.camera.SettingsDefinitions.PhotoAEBCount;
import dji.common.camera.SettingsDefinitions.PhotoBurstCount;
import dji.common.camera.SettingsDefinitions.PhotoPanoramaMode;
import dji.common.camera.SettingsDefinitions.PhotoTimeIntervalSettings;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.Subscriber;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.thirdparty.rx.functions.Func1;
import dji.thirdparty.rx.schedulers.Schedulers;
import dji.ux.beta.core.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.util.CameraUtil;

import static dji.common.camera.SettingsDefinitions.PhotoTimeLapseFileFormat.JPEG_AND_VIDEO;
import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_CHILD_IMAGE;
import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_CHILD_TEXT;

public class CameraPhotoModeListWidget extends ExpandableListViewWidget {
    //region Properties
    private static final String TAG = "CameraPhotoModeListWidget";
    private ShootPhotoMode shootPhotoMode;
    private FlatCameraMode flatCameraMode;
    private int[] shootPhotoRange;
    private int[] flatCameraRange;
    private int[][] photoModeChildRange;
    private int[][] flatCameraChildRange;
    private DJIKey shootPhotoModeRangeKey;
    private DJIKey flatCameraModeRangeKey;
    private DJIKey isShootingIntervalPhotoKey;
    private DJIKey shootPhotoModeKey;
    private DJIKey flatCameraModeKey;
    private boolean isShootingIntervalPhoto;
    private DJIKey shootPhotoModeChildRangeKey;
    private DJIKey flatCameraModeChildRangeKey;
    private DJIKey intervalParamKey;
    private DJIKey aebParamKey;
    private DJIKey burstCountKey;
    private DJIKey rawBurstCountKey;
    private DJIKey panoramaModeKey;
    private PhotoTimeIntervalSettings intervalParam;
    private DJIKey timeLapseParamKey;
    private DJIKey isFlatCameraModeSupportedKey;
    private DJIKey isFlyingKey;
    private PhotoAEBCount aebParam;
    private PhotoBurstCount burstCount;
    private PhotoTimeLapseSettings timeLapseParam;
    private PhotoBurstCount rawBurstCount;
    private PhotoPanoramaMode panoramaMode;
    private boolean isFlatCameraModeSupported;
    private boolean isFlying;

    //endregion

    //region Default Constructors
    public CameraPhotoModeListWidget(Context context) {
        super(context);
    }

    public CameraPhotoModeListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPhotoModeListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        initAdapter(null, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }
        disableItemsWhenCameraBusy();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_photo_name);
        }
    }

    protected List<ExpandableGroupListItem> generateData(int[] pmRange, int[][] pmChildRange) {
        final ArrayList<ExpandableGroupListItem> data = new ArrayList<>();

        int[] imgResIds;
        int[] shootPhotoNameResId;
        int[] range;
        int[][] childRange;

        if (pmRange == null) {
            range = isFlatCameraModeSupported ? CameraResource.getFlatModeDefaultCmdId() : CameraResource.getPhotoModeDefaultCmdId();
        } else {
            range = pmRange;
        }

        if (pmChildRange == null) {
            childRange = isFlatCameraModeSupported ? CameraResource.getFlatModeChildDefaultValue() : CameraResource.getPhotoModeChildDefaultValue();
        } else {
            childRange = pmChildRange;
        }

        if (isFlatCameraModeSupported) {
            shootPhotoNameResId = CameraResource.getFlatModeStrId();
            imgResIds = CameraResource.getFlatModeImageResId();
        } else {
            shootPhotoNameResId = CameraResource.getPhotoModeStrId();
            imgResIds = CameraResource.getPhotoModeImageResId();
        }

        if (null != range && range.length > 0) {
            for (int aShootPhotoRange : range) {
                ExpandableGroupListItem group = new ExpandableGroupListItem();

                group.valueId = aShootPhotoRange;
                if (group.valueId < shootPhotoNameResId.length) {
                    group.groupStr = getContext().getString(shootPhotoNameResId[group.valueId]);
                    if (group.groupStr.equals(getResources().getString(R.string.uxsdk_camera_photomode_panorama))) {
                        group.childType = TYPE_CHILD_IMAGE;
                    } else {
                        group.childType = TYPE_CHILD_TEXT;
                    }
                    group.imgResId = imgResIds[group.valueId];

                    if (group.valueId < childRange.length) {
                        final int[] childIds = childRange[group.valueId];
                        if (null != childIds) {
                            if (childIds.length > 1) {
                                initChildrenViewForGroup(group, childIds);
                            } else {
                                group.childStr = getChildString(group, childIds[0]);
                            }
                        }
                    }
                    data.add(group);
                }
            }
        }

        return data;
    }

    private void initChildrenViewForGroup(ExpandableGroupListItem group, int[] childIds) {
        for (int j = 0, size = childIds.length; j < size; j++) {
            final ExpandableChildListItem child = new ExpandableChildListItem();
            child.childStr = getChildString(group, childIds[j]);
            child.groupValueId = group.valueId;
            child.valueId = childIds[j];
            if (group.childType == TYPE_CHILD_IMAGE) {
                child.childImageResource = getChildImages(group, childIds[j]);
            }

            group.childs.add(child);
        }
    }

    private String getChildString(ExpandableGroupListItem group, int childId) {
        String childStr = String.valueOf(childId);
        if (isFlatCameraModeSupported) {
            if (group.valueId == FlatCameraMode.PHOTO_INTERVAL.value()) {
                childStr = String.valueOf(childId) + "s";
            } else if (group.valueId == FlatCameraMode.PHOTO_PANORAMA.value()) {
                if (PhotoPanoramaMode.PANORAMA_MODE_3X1.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_3x1);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_3X3.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_3x3);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_1X3.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_1x3);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_180.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_180);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_SPHERE.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_sphere);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_SUPER_RESOLUTION.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_super_resolution);
                }
            }
        } else {
            if (group.valueId == ShootPhotoMode.INTERVAL.value()) {
                childStr = String.valueOf(childId) + "s";
            } else if ((group.valueId == ShootPhotoMode.RAW_BURST.value() || group.valueId == ShootPhotoMode.BURST.value())
                    && childId == PhotoBurstCount.CONTINUOUS.value()) {
                childStr = getResources().getString(R.string.uxsdk_camera_photomode_raw_burst_infinity);
            } else if (group.valueId == ShootPhotoMode.PANORAMA.value()) {
                if (PhotoPanoramaMode.PANORAMA_MODE_3X1.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_3x1);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_3X3.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_3x3);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_1X3.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_1x3);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_180.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_180);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_SPHERE.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_sphere);
                } else if (PhotoPanoramaMode.PANORAMA_MODE_SUPER_RESOLUTION.value() == childId) {
                    childStr = getResources().getString(R.string.uxsdk_camera_photomode_panorama_super_resolution);
                }
            }
        }

        return childStr;
    }

    private int getChildImages(ExpandableGroupListItem group, int childId) {
        int childImg = R.drawable.uxsdk_advanced_more_photomode_pano3x1;
        if ((isFlatCameraModeSupported && group.valueId == FlatCameraMode.PHOTO_PANORAMA.value())
                || (!isFlatCameraModeSupported && group.valueId == ShootPhotoMode.PANORAMA.value())) {
            if (PhotoPanoramaMode.PANORAMA_MODE_3X1.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_3x1;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_3X3.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_3x3;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_1X3.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_180;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_180.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_180;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SPHERE.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_sphere;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SUPER_RESOLUTION.value() == childId) {
                childImg = R.drawable.uxsdk_selector_pano_super_res;
            }
        }

        return childImg;
    }


    private void initAdapter(int[] range, int[][] childRange) {
        groupValueId = Integer.MAX_VALUE;
        childValueId = Integer.MAX_VALUE;
        adapter.setDataList(generateData(range, childRange));
        if (sdGroupLy != null && sdGroupLy.isShown()) {
            sdGroupLy.setVisibility(View.GONE);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        shootPhotoModeRangeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE_RANGE, keyIndex);
        flatCameraModeRangeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE_RANGE, keyIndex);
        shootPhotoModeChildRangeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE_CHILD_RANGE, keyIndex);
        flatCameraModeChildRangeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE_CHILD_RANGE, keyIndex);
        isShootingIntervalPhotoKey = CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, keyIndex);
        shootPhotoModeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, keyIndex);
        flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, keyIndex);
        intervalParamKey = CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, keyIndex);
        aebParamKey = CameraKey.create(CameraKey.PHOTO_AEB_COUNT, keyIndex);
        burstCountKey = CameraKey.create(CameraKey.PHOTO_BURST_COUNT, keyIndex); // This key is Get, not push key
        timeLapseParamKey = CameraKey.create(CameraKey.PHOTO_TIME_LAPSE_SETTINGS, keyIndex); // Get as well
        rawBurstCountKey = CameraKey.create(CameraKey.PHOTO_RAW_BURST_COUNT, keyIndex); // Get as well
        panoramaModeKey = CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, keyIndex); // Get as well
        isFlatCameraModeSupportedKey = CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, keyIndex);
        isFlyingKey = FlightControllerKey.create(FlightControllerKey.IS_FLYING);

        addDependentKey(shootPhotoModeRangeKey);
        addDependentKey(flatCameraModeRangeKey);
        addDependentKey(isShootingIntervalPhotoKey);
        addDependentKey(shootPhotoModeKey);
        addDependentKey(flatCameraModeKey);
        addDependentKey(shootPhotoModeChildRangeKey);
        addDependentKey(flatCameraModeChildRangeKey);
        addDependentKey(intervalParamKey);
        addDependentKey(aebParamKey);
        addDependentKey(burstCountKey);
        addDependentKey(timeLapseParamKey);
        addDependentKey(rawBurstCountKey);
        addDependentKey(panoramaModeKey);
        addDependentKey(isFlatCameraModeSupportedKey);
        addDependentKey(isFlyingKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(shootPhotoModeKey)) {
            shootPhotoMode = (ShootPhotoMode) value;
            if (shootPhotoMode == ShootPhotoMode.BURST) {
                getBurstCount();
            } else if (shootPhotoMode == ShootPhotoMode.TIME_LAPSE) {
                getTimeLapseParam();
            }
        } else if (key.equals(flatCameraModeKey)) {
            flatCameraMode = (FlatCameraMode) value;
            if (flatCameraMode == FlatCameraMode.PHOTO_BURST) {
                getBurstCount();
            } else if (flatCameraMode == FlatCameraMode.PHOTO_TIME_LAPSE) {
                getTimeLapseParam();
            }
        } else if (key.equals(shootPhotoModeRangeKey)) {
            Object[] array = (ShootPhotoMode[]) value;
            shootPhotoRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                shootPhotoRange[i] = ((ShootPhotoMode) array[i]).value();
            }
        } else if (key.equals(flatCameraModeRangeKey)) {
            Object[] array = (FlatCameraMode[]) value;

            List<Integer> availableRangeList = new ArrayList<>();
            for (Object o : array) {
                FlatCameraMode topMode = (FlatCameraMode) o;
                if (CameraUtil.isPictureMode(topMode)) {
                    availableRangeList.add(topMode.value());
                }
            }
            flatCameraRange = new int[availableRangeList.size()];
            for (int i = 0; i < availableRangeList.size(); i++) {
                flatCameraRange[i] = availableRangeList.get(i);
            }
        } else if (key.equals(isShootingIntervalPhotoKey)) {
            isShootingIntervalPhoto = (boolean) value;
        } else if (key.equals(shootPhotoModeChildRangeKey)) {
            photoModeChildRange = (int[][]) value;
        } else if (key.equals(flatCameraModeChildRangeKey)) {
            flatCameraChildRange = (int[][]) value;
        } else if (key.equals(intervalParamKey)) {
            intervalParam = (PhotoTimeIntervalSettings) value;
        } else if (key.equals(aebParamKey)) {
            aebParam = (PhotoAEBCount) value;
        } else if (key.equals(burstCountKey)) {
            burstCount = (PhotoBurstCount) value;
        } else if (key.equals(timeLapseParamKey)) {
            timeLapseParam = (PhotoTimeLapseSettings) value;
        } else if (key.equals(rawBurstCountKey)) {
            rawBurstCount = (PhotoBurstCount) value;
        } else if (key.equals(panoramaModeKey)) {
            panoramaMode = (PhotoPanoramaMode) value;
            DJILog.d(TAG, "panoramaMode " + panoramaMode);
        } else if (key.equals(isFlatCameraModeSupportedKey)) {
            isFlatCameraModeSupported = (Boolean) value;
        } else if (key.equals(isFlyingKey)) {
            isFlying = (Boolean) value;
        }
    }

    private void getBurstCount() {
        DJISDKModelV4.getInstance().getValueOfKey(burstCountKey, this);
    }

    private void getTimeLapseParam() {
        DJISDKModelV4.getInstance().getValueOfKey(timeLapseParamKey, this);
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(shootPhotoModeRangeKey)
                || key.equals(flatCameraModeRangeKey)
                || key.equals(shootPhotoModeChildRangeKey)
                || key.equals(flatCameraModeChildRangeKey)
                || key.equals(isFlatCameraModeSupportedKey)) {
            initAdapter(getRange(), getChildRange());
        }
        updateItemSelection();
    }

    private int[] getRange() {
        if (isFlatCameraModeSupported) {
            return flatCameraRange;
        } else {
            return shootPhotoRange;
        }
    }

    private int[][] getChildRange() {
        if (isFlatCameraModeSupported) {
            return flatCameraChildRange;
        } else {
            return photoModeChildRange;
        }
    }

    private void updateItemSelection() {
        if (isFlatCameraModeSupported) {
            updateFlatCameraModeItemSelection();
        } else {
            updateShootPhotoModeItemSelection();
        }
    }

    private void updateFlatCameraModeItemSelection() {
        if (flatCameraMode != null) {
            ExpandableGroupListItem toBeSelectedItem = adapter.getGroupItemByValueId(flatCameraMode.value());
            if (toBeSelectedItem != null) {
                toBeSelectedItem.childValueId =
                        getPhotoTypeValueByMode(CameraUtil.toShootPhotoMode(flatCameraMode), ExpandableGroupListItem.INVALID_VALUE);
                if (toBeSelectedItem.childValueId != ExpandableGroupListItem.INVALID_VALUE) {
                    toBeSelectedItem.childStr = getChildString(toBeSelectedItem, toBeSelectedItem.childValueId);
                }
                adapter.setSelectedItem(toBeSelectedItem);
            }
        }
    }
    private void updateShootPhotoModeItemSelection() {
        if (shootPhotoMode != null) {
            ExpandableGroupListItem toBeSelectedItem = adapter.getGroupItemByValueId(shootPhotoMode.value());
            if (toBeSelectedItem != null) {
                toBeSelectedItem.childValueId =
                        getPhotoTypeValueByMode(shootPhotoMode, ExpandableGroupListItem.INVALID_VALUE);
                if (toBeSelectedItem.childValueId != ExpandableGroupListItem.INVALID_VALUE) {
                    toBeSelectedItem.childStr = getChildString(toBeSelectedItem, toBeSelectedItem.childValueId);
                }
                adapter.setSelectedItem(toBeSelectedItem);
            }
        }
    }

    //endregion

    private void updateShootPhotoModeToCamera(final ShootPhotoMode type, int childID) {
        if (KeyManager.getInstance() == null) return;

        Object value = null;
        DJIKey key = null;
        if (type == ShootPhotoMode.BURST) {
            key = burstCountKey;
            value = PhotoBurstCount.find(childID);
        } else if (type == ShootPhotoMode.AEB) {
            key = aebParamKey;
            value = PhotoAEBCount.find(childID);
        } else if (type == ShootPhotoMode.INTERVAL) {
            key = intervalParamKey;
            int count;
            // count is valid in range [2, 255]
            if (intervalParam == null || intervalParam.getCaptureCount() < 2) {
                count = 255;
            } else {
                count = intervalParam.getCaptureCount();
            }
            value = new PhotoTimeIntervalSettings(count, childID);
        } else if (type == ShootPhotoMode.TIME_LAPSE) {
            key = timeLapseParamKey;
            int duration = 0;
            SettingsDefinitions.PhotoTimeLapseFileFormat format = JPEG_AND_VIDEO;
            if (timeLapseParam != null) {
                format = timeLapseParam.getFileFormat();
                duration = timeLapseParam.getDuration();
            }

            value = new PhotoTimeLapseSettings(childID, duration, format);
        } else if (type == ShootPhotoMode.RAW_BURST) {
            key = rawBurstCountKey;
            value = PhotoBurstCount.find(childID);
        } else if (type == ShootPhotoMode.PANORAMA) {
            key = panoramaModeKey;
            value = PhotoPanoramaMode.find(childID);
        }

        if (key != null) {
            setValueByKey(key, value).retry(3).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    updateShootPhotoMode(type);
                }
            });
        } else {
            updateShootPhotoMode(type);
        }
    }

    private void updateFlatModeToCamera(final FlatCameraMode type, int childID) {
        if (KeyManager.getInstance() == null) return;

        Object value = null;
        DJIKey key = null;
        if (type == FlatCameraMode.PHOTO_BURST) {
            key = burstCountKey;
            value = PhotoBurstCount.find(childID);
        } else if (type == FlatCameraMode.PHOTO_AEB) {
            key = aebParamKey;
            value = PhotoAEBCount.find(childID);
        } else if (type == FlatCameraMode.PHOTO_INTERVAL) {
            key = intervalParamKey;
            int count;
            // count is valid in range [2, 255]
            if (intervalParam == null || intervalParam.getCaptureCount() < 2) {
                count = 255;
            } else {
                count = intervalParam.getCaptureCount();
            }
            value = new PhotoTimeIntervalSettings(count, childID);
        } else if (type == FlatCameraMode.PHOTO_TIME_LAPSE) {
            key = timeLapseParamKey;
            int duration = 0;
            SettingsDefinitions.PhotoTimeLapseFileFormat format = JPEG_AND_VIDEO;
            if (timeLapseParam != null) {
                format = timeLapseParam.getFileFormat();
                duration = timeLapseParam.getDuration();
            }

            value = new PhotoTimeLapseSettings(childID, duration, format);
        } else if (type == FlatCameraMode.PHOTO_PANORAMA) {
            key = panoramaModeKey;
            value = PhotoPanoramaMode.find(childID);
        }

        final Object finalValue = value;
        final DJIKey finalKey = key;
        updateFlatCameraMode(type, new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                setValueByKey(finalKey, finalValue).retry(3).observeOn(AndroidSchedulers.mainThread()).subscribe();
            }
        });
    }

    private void updateShootPhotoMode(final ShootPhotoMode mode) {
        if (KeyManager.getInstance() == null) return;

        DJIKey photoModeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, keyIndex);
        setValueByKey(photoModeKey, mode).retry(3).observeOn(AndroidSchedulers.mainThread()).subscribe();
    }

    private void updateFlatCameraMode(final FlatCameraMode mode, Action1<Boolean> actionOnSubscribe) {
        if (KeyManager.getInstance() == null) return;

        DJIKey flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, keyIndex);
        setValueByKey(flatCameraModeKey, mode).retry(3).observeOn(AndroidSchedulers.mainThread()).subscribe(actionOnSubscribe);
    }

    protected Observable<Boolean> setValueByKey(final DJIKey key, final Object value) {
        if (KeyManager.getInstance() == null) {
            return Observable.error(new NullPointerException("KeyManager is null."));
        }

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                KeyManager.getInstance().setValue(key, value, new SetCallback() {
                    @Override
                    public void onSuccess() {
                        DJILog.e(TAG, key.toString() + " successed");
                        subscriber.onStart();
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        DJILog.e(TAG, key.toString() + " failed " + error);
                        subscriber.onError(new Throwable(error.getDescription()));
                    }
                });
            }
        }).subscribeOn(Schedulers.computation()).onErrorReturn(new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable throwable) {
                DJILog.e(TAG, key.toString() + " " + throwable.getMessage());
                return false;
            }
        });
    }

    private int getPhotoTypeValueByMode(ShootPhotoMode type, int defValue) {
        int value = defValue;
        if (type == ShootPhotoMode.BURST && burstCount != null) {
            if (burstCount != PhotoBurstCount.UNKNOWN) {
                value = burstCount.value();
            }
        } else if (type == ShootPhotoMode.AEB && aebParam != null) {
            value = aebParam.value();
        } else if (type == ShootPhotoMode.INTERVAL && intervalParam != null) {
            if (intervalParam.getTimeIntervalInSeconds() > 0) {
                value = intervalParam.getTimeIntervalInSeconds();
            }
        } else if (type == ShootPhotoMode.TIME_LAPSE && timeLapseParam != null) {
            value = timeLapseParam.getInterval();
        } else if (type == ShootPhotoMode.RAW_BURST && rawBurstCount != null) {
            value = rawBurstCount.value();
        } else if (type == ShootPhotoMode.PANORAMA
            && panoramaMode != null
            && panoramaMode != PhotoPanoramaMode.UNKNOWN) {
            value = panoramaMode.value();
        }

        return value;
    }

    @Override
    protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final ExpandableGroupListItem group = (ExpandableGroupListItem) adapter.getGroup(groupPosition);
        if (group.isEnabled() && group.valueId != groupValueId) {
            // if the camera is shooting interval photo now, user should not change the photo mode
            if (isShootingIntervalPhoto) {
                return true;
            }

            if (isFlatCameraModeSupported) {
                FlatCameraMode mode = FlatCameraMode.find(group.valueId);
                if (!isFlying && mode == FlatCameraMode.PHOTO_PANORAMA) {
                    Toast.makeText(getContext(), getResources().getString(R.string.uxsdk_set_panorama_mode_failed), Toast.LENGTH_LONG).show();
                    return true;
                }
                int defaultValue = 0;
                if (group.childValueId != ExpandableGroupListItem.INVALID_VALUE) {
                    defaultValue = group.childValueId;
                } else if (!group.childs.isEmpty()) {
                    defaultValue = group.childs.get(0).valueId;
                }

                int child = getPhotoTypeValueByMode(CameraUtil.toShootPhotoMode(mode), defaultValue);
                updateFlatModeToCamera(mode, child);
                updateSelected(groupValueId, group.valueId, child);
                groupValueId = group.valueId;
                childValueId = child;
            } else {
                ShootPhotoMode type = ShootPhotoMode.find(group.valueId);
                int defaultValue = 0;
                if (group.childValueId != ExpandableGroupListItem.INVALID_VALUE) {
                    defaultValue = group.childValueId;
                } else if (!group.childs.isEmpty()) {
                    defaultValue = group.childs.get(0).valueId;
                }

                int child = getPhotoTypeValueByMode(type, defaultValue);
                updateShootPhotoModeToCamera(type, child);
                updateSelected(groupValueId, group.valueId, child);
                groupValueId = group.valueId;
                childValueId = child;
            }
        }
        return true;
    }

    @Override
    protected void onChildViewClick(final Object tag) {
        if (tag instanceof ExpandableChildListItem) {
            final ExpandableChildListItem model = (ExpandableChildListItem) tag;
            if (groupValueId == model.groupValueId && model.valueId == childValueId) {
                return;
            }

            // if the camera is shooting interval photo now, user should not change the photo mode
            if (isShootingIntervalPhoto) {
                return;
            }

            if (isFlatCameraModeSupported) {
                FlatCameraMode mode = FlatCameraMode.find(model.groupValueId);
                updateFlatModeToCamera(mode, model.valueId);
            } else {
                updateShootPhotoModeToCamera(ShootPhotoMode.find(model.groupValueId), model.valueId);
            }
            updateSelected(groupValueId, model.groupValueId, model.valueId);
            groupValueId = model.groupValueId;
            childValueId = model.valueId;
        }
    }
}
