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

package dji.ux.beta.cameracore.widget.cameracapture.shootphoto;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions.SDCardOperationState;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.common.camera.SettingsDefinitions.StorageLocation;
import dji.log.DJILog;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.Single;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.ui.ProgressRingView;
import dji.ux.beta.cameracore.util.CameraActionSound;
import dji.ux.beta.cameracore.util.CameraResource;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.ProductUtil;

import static dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Shoot Photo Widget
 * <p>
 * Widget can be used for shooting photo. The widget displays the current photo mode. It also
 * displays the storage state and errors associated with it.
 */
public class ShootPhotoWidget extends ConstraintLayoutWidget implements View.OnClickListener {
    //region Fields
    private static final String TAG = "ShootPhotoWidget";
    private ShootPhotoWidgetModel widgetModel;
    private ProgressRingView borderProgressRingView;
    private ImageView centerImageView;
    private ImageView storageStatusOverlayImageView;
    private Drawable startShootPhotoDrawable;
    private Drawable stopShootPhotoDrawable;
    private Drawable startShootPhotoHasselbladDrawable;
    private Drawable stopShootPhotoHasselbladDrawable;
    @ColorInt
    private int progressRingHasselbladColor;
    @ColorInt
    private int progressRingColor;
    private Map<StorageIconState, Drawable> storageInternalIconMap;
    private Map<StorageIconState, Drawable> storageSSDIconMap;
    private Map<StorageIconState, Drawable> storageSDCardIconMap;
    private CameraActionSound cameraActionSound;
    //endregion

    //region Lifecycle
    public ShootPhotoWidget(Context context) {
        super(context);
    }

    public ShootPhotoWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShootPhotoWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_shoot_photo, this);
        borderProgressRingView = findViewById(R.id.progress_ring_view_border);
        centerImageView = findViewById(R.id.image_view_center);
        storageStatusOverlayImageView = findViewById(R.id.image_view_storage_status_overlay);
        storageInternalIconMap = new HashMap<>();
        storageSSDIconMap = new HashMap<>();
        storageSDCardIconMap = new HashMap<>();
        cameraActionSound = new CameraActionSound(context);
        if (!isInEditMode()) {
            centerImageView.setOnClickListener(this);
            widgetModel =
                    new ShootPhotoWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

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
                widgetModel.isShootingPhoto()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(
                                this::onIsShootingPhotoChange,
                                logErrorConsumer(TAG, "isShootingPhoto: ")));
        addReaction(reactToCanStartOrStopShootingPhoto());
        addReaction(reactToPhotoStateAndPhotoStorageState());
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getContext().getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        DJILog.d(TAG, "onClick");
        if (v.equals(centerImageView)) {
            Single<Boolean> stop = widgetModel.canStopShootingPhoto().firstOrError();
            Single<Boolean> start = widgetModel.canStartShootingPhoto().firstOrError();

            addDisposable(Single.zip(stop, start, Pair::new)
                    .flatMapCompletable(pairs -> {
                        if (pairs.first) {
                            return widgetModel.stopShootPhoto();
                        } else if (pairs.second) {
                            return widgetModel.startShootPhoto();
                        }
                        return Completable.complete();
                    }).observeOn(SchedulerProvider.ui())
                    .subscribe(
                            () -> {
                            }, logErrorConsumer(TAG, "Start Stop Shoot Photo")));
        }
    }
    //endregion

    //region private helpers
    private Drawable getCameraResourceDrawable(int resourceId) {
        return getResources().getDrawable(resourceId);
    }

    private void updateCameraForegroundResource(@NonNull CameraPhotoState cameraPhotoState, @NonNull CameraPhotoStorageState cameraPhotoStorageState) {
        Drawable foregroundDrawable = null;
        if (cameraPhotoState instanceof CameraPanoramaPhotoState) {
            foregroundDrawable = getCameraResourceDrawable(CameraResource.getPhotoModeImgResId(cameraPhotoState.getShootPhotoMode().value(),
                    ((CameraPanoramaPhotoState) cameraPhotoState).getPhotoPanoramaMode().value()));
            cameraActionSound.setShutterCount(CameraActionSound.ShutterSoundCount.ONE);
        } else if (cameraPhotoState instanceof CameraAEBPhotoState) {
            int photoCount = ((CameraAEBPhotoState) cameraPhotoState).getPhotoAEBCount().value();
            foregroundDrawable = getCameraResourceDrawable(CameraResource.getPhotoModeImgResId(cameraPhotoState.getShootPhotoMode().value(),
                    photoCount));
            cameraActionSound.setShutterCount(CameraActionSound.ShutterSoundCount.find(photoCount));
        } else if (cameraPhotoState instanceof CameraBurstPhotoState) {
            int photoCount = ((CameraBurstPhotoState) cameraPhotoState).getPhotoBurstCount().value();
            foregroundDrawable = getCameraResourceDrawable(CameraResource.getPhotoModeImgResId(cameraPhotoState.getShootPhotoMode().value(),
                    photoCount));
            cameraActionSound.setShutterCount(CameraActionSound.ShutterSoundCount.find(photoCount));
        } else if (cameraPhotoState instanceof CameraIntervalPhotoState) {
            foregroundDrawable = getCameraResourceDrawable(CameraResource.getPhotoModeImgResId(cameraPhotoState.getShootPhotoMode().value(),
                    ((CameraIntervalPhotoState) cameraPhotoState).getTimeIntervalInSeconds()));
            cameraActionSound.setShutterCount(CameraActionSound.ShutterSoundCount.ONE);
        } else {
            if (cameraPhotoState.getShootPhotoMode() != ShootPhotoMode.SINGLE) {
                foregroundDrawable = getCameraResourceDrawable(CameraResource.getPhotoModeImgResId(cameraPhotoState.getShootPhotoMode().value(),
                        0));
                cameraActionSound.setShutterCount(CameraActionSound.ShutterSoundCount.ONE);
            }
        }

        if (cameraPhotoStorageState instanceof CameraSDPhotoStorageState) {
            CameraSDPhotoStorageState sdStorageState = (CameraSDPhotoStorageState) cameraPhotoStorageState;
            if (cameraPhotoStorageState.getStorageLocation() == StorageLocation.SDCARD) {
                if (sdStorageState.getStorageOperationState() == SDCardOperationState.NOT_INSERTED) {
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.NOT_INSERTED);
                } else if (sdStorageState.getStorageOperationState() == SDCardOperationState.FULL) {
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.FULL);
                } else if (sdStorageState.getStorageOperationState() == SDCardOperationState.SLOW) {
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.SLOW);
                }
            } else if (cameraPhotoStorageState.getStorageLocation() == StorageLocation.INTERNAL_STORAGE) {
                if (sdStorageState.getStorageOperationState() == SDCardOperationState.NOT_INSERTED) {
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.NOT_INSERTED);
                } else if (sdStorageState.getStorageOperationState() == SDCardOperationState.FULL) {
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.FULL);
                } else if (sdStorageState.getStorageOperationState() == SDCardOperationState.SLOW) {
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.SLOW);
                }
            }
        } else if (cameraPhotoStorageState instanceof CameraSSDPhotoStorageState) {
            CameraSSDPhotoStorageState ssdStorageState = (CameraSSDPhotoStorageState) cameraPhotoStorageState;
            if (ssdStorageState.getStorageOperationState() == SSDOperationState.NOT_FOUND) {
                foregroundDrawable = getSSDStorageIcon(StorageIconState.NOT_INSERTED);
            } else if (ssdStorageState.getStorageOperationState() == SSDOperationState.FULL) {
                foregroundDrawable = getSSDStorageIcon(StorageIconState.FULL);
            }
        }

        storageStatusOverlayImageView.setImageDrawable(foregroundDrawable);
    }


    private void onIsShootingPhotoChange(boolean isShootingPhoto) {
        DJILog.d(TAG, "onIsShootingPhotoChange " + isShootingPhoto);
        borderProgressRingView.setIndeterminate(isShootingPhoto);
        if (isShootingPhoto) {
            addDisposable(cameraActionSound.playCapturePhoto());
        }
    }

    private Disposable reactToPhotoStateAndPhotoStorageState() {
        return Flowable.combineLatest(widgetModel.getCameraPhotoState(),
                widgetModel.getCameraStorageState(),
                Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateCameraForegroundResource(values.first, values.second),
                        logErrorConsumer(TAG, "reactToPhotoStateAndPhotoStorageState "));
    }

    private Disposable reactToCanStartOrStopShootingPhoto() {
        return Flowable.combineLatest(
                widgetModel.canStartShootingPhoto(),
                widgetModel.canStopShootingPhoto(),
                Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateImages(values.first, values.second),
                        logErrorConsumer(TAG, "reactToCanStartOrStopShootingPhoto: "));
    }

    private void checkAndUpdatePhotoStateAndPhotoStorageState() {
        if (!isInEditMode()) {
            addDisposable(Flowable.combineLatest(widgetModel.getCameraPhotoState(),
                    widgetModel.getCameraStorageState(),
                    Pair::new)
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(values -> updateCameraForegroundResource(values.first, values.second),
                            logErrorConsumer(TAG, "checkAndUpdatePhotoStateAndPhotoStorageState ")));
        }
    }

    private void checkAndUpdateCanStartOrStopShootingPhoto() {
        if (!isInEditMode()) {
            addDisposable(Flowable.combineLatest(
                    widgetModel.canStartShootingPhoto(),
                    widgetModel.canStopShootingPhoto(),
                    Pair::new)
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(values -> updateImages(values.first, values.second),
                            logErrorConsumer(TAG, "checkAndUpdateCanStartOrStopShootingPhoto: ")));
        }
    }

    private void updateImages(boolean canStartShootingPhoto, boolean canStopShootingPhoto) {
        DJILog.d(TAG, "reactToCanStartOrStopShootingPhoto "
                + canStartShootingPhoto + " --- " + canStopShootingPhoto);

        if (!canStopShootingPhoto) {
            enableAction(canStartShootingPhoto);
            centerImageView.setImageDrawable(ProductUtil.isHasselbladCamera() ? startShootPhotoHasselbladDrawable : startShootPhotoDrawable);
        } else {
            enableAction(true);
            centerImageView.setImageDrawable(ProductUtil.isHasselbladCamera() ? stopShootPhotoHasselbladDrawable : stopShootPhotoDrawable);
        }

        storageStatusOverlayImageView.setVisibility(canStopShootingPhoto ? View.GONE : View.VISIBLE);
        if (ProductUtil.isHasselbladCamera()) {
            borderProgressRingView.setRingColor(progressRingHasselbladColor);
        } else {
            borderProgressRingView.setRingColor(progressRingColor);
        }

        if (canStartShootingPhoto) {
            // This for bug where burst mode sometimes does not return value for onIsShootingPhotoChange
            // and ring spins forever
            borderProgressRingView.setIndeterminate(false);
        }
    }

    private void enableAction(boolean isEnabled) {
        centerImageView.setEnabled(isEnabled);
    }

    private void initDefaults() {
        startShootPhotoDrawable = getResources().getDrawable(R.drawable.uxsdk_shape_circle);
        stopShootPhotoDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_shutter_stop);
        startShootPhotoHasselbladDrawable = getResources().getDrawable(R.drawable.uxsdk_selector_hasselblad_shoot_photo);
        stopShootPhotoHasselbladDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_hasselblad_shutter_stop);
        setProgressRingColor(Color.WHITE);
        setProgressRingHasselbladColor(getResources().getColor(R.color.uxsdk_shoot_photo_hasselblad_ring));
        setInternalStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_internal_storage_not_inserted);
        setInternalStorageIcon(StorageIconState.SLOW, R.drawable.uxsdk_ic_internal_storage_slow);
        setInternalStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_internal_storage_full);
        setSDCardStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_sdcard_not_inserted);
        setSDCardStorageIcon(StorageIconState.SLOW, R.drawable.uxsdk_ic_sdcard_slow);
        setSDCardStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_sdcard_full);
        setSSDStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_ssd_not_inserted);
        setSSDStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_ssd_full);
        setCameraIndex(CameraIndex.CAMERA_INDEX_0);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShootPhotoWidget);
        setCameraIndex(CameraIndex.find(typedArray.getInt(R.styleable.ShootPhotoWidget_uxsdk_cameraIndex, 0)));

        setForegroundIconBackground(typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_foregroundIconBackground));
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStartIcon) != null) {
            startShootPhotoDrawable = typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStartIcon);
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStopIcon) != null) {
            stopShootPhotoDrawable = typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStopIcon);
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStartHasselbladIcon) != null) {
            startShootPhotoHasselbladDrawable = typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStartHasselbladIcon);
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStopHasselbladIcon) != null) {
            stopShootPhotoHasselbladDrawable = typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_shootPhotoStopHasselbladIcon);
        }

        setProgressRingColor(typedArray.getColor(R.styleable.ShootPhotoWidget_uxsdk_progressRingColor, Color.WHITE));
        setProgressRingHasselbladColor(typedArray.getColor(R.styleable.ShootPhotoWidget_uxsdk_progressRingHasselbladColor, getResources().getColor(R.color.uxsdk_shoot_photo_hasselblad_ring)));
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageNotInsertedIcon) != null) {
            setInternalStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageFullIcon) != null) {
            setInternalStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageSlowIcon) != null) {
            setInternalStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_internalStorageSlowIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardNotInsertedIcon) != null) {
            setSDCardStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardFullIcon) != null) {
            setSDCardStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardSlowIcon) != null) {
            setSDCardStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_sdCardSlowIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdNotInsertedIcon) != null) {
            setSSDStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdFullIcon) != null) {
            setSSDStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdSlowIcon) != null) {
            setSSDStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.ShootPhotoWidget_uxsdk_ssdSlowIcon));
        }

        typedArray.recycle();
    }
    //endregion

    //region customizations

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return {@link CameraIndex}
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex {@link CameraIndex}
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the current start shooting photo icon
     *
     * @return Drawable currently used
     */
    public Drawable getStartShootPhotoDrawable() {
        return startShootPhotoDrawable;
    }

    /**
     * Set the start shoot photo icon
     *
     * @param resourceId to be used
     */
    public void setStartShootPhotoDrawable(@DrawableRes int resourceId) {
        setStartShootPhotoDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the start shoot photo icon
     *
     * @param drawable to be used
     */
    public void setStartShootPhotoDrawable(@Nullable Drawable drawable) {
        startShootPhotoDrawable = drawable;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }

    /**
     * Get the current stop shooting photo icon
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getStopShootPhotoDrawable() {
        return stopShootPhotoDrawable;
    }

    /**
     * Set stop shoot photo icon
     *
     * @param resourceId to be used
     */
    public void setStopShootPhotoDrawable(@DrawableRes int resourceId) {
        setStopShootPhotoDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set stop shoot photo icon
     *
     * @param drawable to be used
     */
    public void setStopShootPhotoDrawable(@Nullable Drawable drawable) {
        stopShootPhotoDrawable = drawable;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }

    /**
     * Get the current icon for start shooting photo for Hasselblad camera
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getStartShootHasselbladPhotoDrawable() {
        return startShootPhotoHasselbladDrawable;
    }

    /**
     * Set the current icon for start shooting photo for Hasselblad camera
     *
     * @param resourceId to be used
     */
    public void setStartShootHasselbladPhotoDrawable(@DrawableRes int resourceId) {
        setStartShootHasselbladPhotoDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the current icon for start shooting photo for Hasselblad camera
     *
     * @param drawable to be used
     */
    public void setStartShootHasselbladPhotoDrawable(@Nullable Drawable drawable) {
        startShootPhotoHasselbladDrawable = drawable;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }

    /**
     * Get the current icon for stop shooting photo for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getStopShootHasselbladPhotoDrawable() {
        return stopShootPhotoHasselbladDrawable;
    }

    /**
     * Set the icon for stop shooting photo for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @param resourceId to be used
     */
    public void setStopShootHasselbladPhotoDrawable(@DrawableRes int resourceId) {
        setStopShootHasselbladPhotoDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for stop shooting photo for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @param drawable to be used
     */
    public void setStopShootHasselbladPhotoDrawable(@Nullable Drawable drawable) {
        stopShootPhotoHasselbladDrawable = drawable;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param resourceId       to be used
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setInternalStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param drawable         to be used
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageInternalIconMap.put(storageIconState, drawable);
        checkAndUpdatePhotoStateAndPhotoStorageState();
    }

    /**
     * Get the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getInternalStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageInternalIconMap.get(storageIconState);
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param resourceId       to be used
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setSDCardStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param drawable         to be used
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageSDCardIconMap.put(storageIconState, drawable);
        checkAndUpdatePhotoStateAndPhotoStorageState();
    }

    /**
     * Get the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getSDCardStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageSDCardIconMap.get(storageIconState);
    }

    /**
     * Set the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon should be used
     * @param resourceId       to be used
     */
    public void setSSDStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setSSDStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon should be used
     * @param drawable         Drawable to be used
     */
    public void setSSDStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageSSDIconMap.put(storageIconState, drawable);
        checkAndUpdatePhotoStateAndPhotoStorageState();
    }

    /**
     * Get the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getSSDStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageSSDIconMap.get(storageIconState);
    }

    /**
     * Get current background of foreground icon
     *
     * @return Drawable being used
     */
    @Nullable
    public Drawable getForegroundIconBackground() {
        return storageStatusOverlayImageView.getBackground();
    }

    /**
     * Set the background of the foreground icon
     *
     * @param resourceId to be used as background
     */
    public void setForegroundIconBackground(@DrawableRes int resourceId) {
        storageStatusOverlayImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set background of the foreground icon
     *
     * @param drawable to be used as background
     */
    public void setForegroundIconBackground(@Nullable Drawable drawable) {
        storageStatusOverlayImageView.setBackground(drawable);
    }

    /**
     * Get the color of the progress ring
     *
     * @return integer representing color
     */
    @ColorInt
    public int getProgressRingColor() {
        return progressRingColor;
    }

    /**
     * Set the color of the progress ring
     *
     * @param color integer value
     */
    public void setProgressRingColor(@ColorInt int color) {
        progressRingColor = color;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }

    /**
     * Get the color of the progress ring when the camera is Hasselblad
     * Currently on Mavic 2 Pro
     *
     * @return integer representing color
     */
    @ColorInt
    public int getProgressRingHasselbladColor() {
        return progressRingHasselbladColor;
    }

    /**
     * Set the color of the progress ring when the camera is Hasselblad
     * Currently on Mavic 2 Pro
     *
     * @param color integer value
     */
    public void setProgressRingHasselbladColor(@ColorInt int color) {
        progressRingHasselbladColor = color;
        checkAndUpdateCanStartOrStopShootingPhoto();
    }
    //endregion

    /**
     * Enum indicating storage error state
     */
    public enum StorageIconState {
        /**
         * The storage is slow
         */
        SLOW,

        /**
         * The storage is full
         */
        FULL,

        /**
         * The storage is not inserted
         */
        NOT_INSERTED
    }
}
