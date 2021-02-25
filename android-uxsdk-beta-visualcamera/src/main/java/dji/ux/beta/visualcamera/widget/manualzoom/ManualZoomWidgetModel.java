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

package dji.ux.beta.visualcamera.widget.manualzoom;

import androidx.annotation.NonNull;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.OpticalZoomSpec;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.MathUtil;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Manual Zoom Widget Model
 * <p>
 * Widget Model for the {@link ManualZoomWidget} used to define the
 * underlying logic and communication
 */
public class ManualZoomWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "ManualZoomWidgetModel";
    //endregion

    //region Fields
    private final DataProcessor<Boolean> isOpticalZoomProcessor;
    private final DataProcessor<OpticalZoomSpec> opticalZoomSpecDataProcessor;
    private final DataProcessor<Integer> currentValueProcessor;
    private final DataProcessor<String> zoomLevelTextProcessor;
    private final DataProcessor<Integer> opticalFocalLengthProcessor;
    private final DataProcessor<Boolean> isZoomingProcessor;
    private DJIKey opticalZoomFocalLengthKey;
    private int cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private LensModule lensModule;
    //endregion

    //region Lifecycle
    public ManualZoomWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                 @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        isOpticalZoomProcessor = DataProcessor.create(false);
        opticalZoomSpecDataProcessor = DataProcessor.create(new OpticalZoomSpec(
                0,
                0,
                0));
        currentValueProcessor = DataProcessor.create(0);
        opticalFocalLengthProcessor = DataProcessor.create(0);
        zoomLevelTextProcessor = DataProcessor.create("");
        isZoomingProcessor = DataProcessor.create(false);
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        DJIKey isOpticalZoomSupportedKey = lensModule.createLensKey(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED, cameraIndex, lensType.value());
        bindDataProcessor(isOpticalZoomSupportedKey, isOpticalZoomProcessor);
        DJIKey opticalZoomSpecKey = lensModule.createLensKey(CameraKey.OPTICAL_ZOOM_SPEC, cameraIndex, lensType.value());
        bindDataProcessor(opticalZoomSpecKey, opticalZoomSpecDataProcessor);
        opticalZoomFocalLengthKey = lensModule.createLensKey(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH, cameraIndex, lensType.value());
        bindDataProcessor(opticalZoomFocalLengthKey, opticalFocalLengthProcessor);
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(value -> {
                    if (value) {
                        restart();
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")));
    }

    @Override
    protected void inCleanup() {
        // Empty
    }

    @Override
    protected void updateStates() {
        int normalizedValue = MathUtil.normalize(opticalFocalLengthProcessor.getValue(),
                opticalZoomSpecDataProcessor.getValue().getMinFocalLength(),
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength(),
                0,
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength() - opticalZoomSpecDataProcessor.getValue().getMinFocalLength());
        normalizedValue = reverseNumber(normalizedValue,
                0,
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength() - opticalZoomSpecDataProcessor.getValue().getMinFocalLength());
        currentValueProcessor.onNext(normalizedValue);

        zoomLevelTextProcessor.onNext(String.valueOf(opticalFocalLengthProcessor.getValue() / 10));
    }

    //endregion

    //region Actions

    /**
     * Set the zoom level
     *
     * @param zoomLevel target integer value
     * @return Completable to with status of action
     */
    public Completable setZoomLevel(int zoomLevel) {
        int zoomLevelTemp = MathUtil.normalize(zoomLevel, 0,
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength() - opticalZoomSpecDataProcessor.getValue().getMinFocalLength(),
                opticalZoomSpecDataProcessor.getValue().getMinFocalLength(),
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength());
        zoomLevelTemp = reverseNumber(zoomLevelTemp,
                opticalZoomSpecDataProcessor.getValue().getMinFocalLength(),
                opticalZoomSpecDataProcessor.getValue().getMaxFocalLength());
        int zoomLevelCorrected = validateZoomLevel(zoomLevelTemp) / 10 * 10;

        if (zoomLevelCorrected == opticalFocalLengthProcessor.getValue()) {
            return Completable.complete();
        }

        return setZoomToDevice(zoomLevelCorrected).subscribeOn(SchedulerProvider.io());

    }

    /**
     * Decrease zoom level by step size
     *
     * @return Completable to with the status of action
     */
    public Completable increaseZoomLevel() {
        return setZoomToDevice(opticalFocalLengthProcessor.getValue() + opticalZoomSpecDataProcessor.getValue().getFocalLengthStep()).subscribeOn(SchedulerProvider.io());
    }

    /**
     * Decrease zoom level by step size
     *
     * @return Completable to with the status of action
     */
    public Completable decreaseZoomLevel() {
        return setZoomToDevice(opticalFocalLengthProcessor.getValue() - opticalZoomSpecDataProcessor.getValue().getFocalLengthStep()).subscribeOn(SchedulerProvider.io());
    }


    //endregion

    //region Data

    /**
     * Get the current zoom level
     *
     * @return Flowable with integer value of optical focal length
     */
    public Flowable<Integer> getCurrentZoomLevel() {
        return currentValueProcessor.toFlowable();
    }

    /**
     * Check if the camera lens is currently changing optical focal length
     *
     * @return Flowable with boolean
     * true - camera lens is changing optical focal length
     * false - camera lens is not changing optical focal length
     */
    public Flowable<Boolean> isZooming() {
        return isZoomingProcessor.toFlowable();
    }

    /**
     * Check if optical zoom is supported
     *
     * @return Flowable with boolean
     * true - optical zoom is supported
     * false - optical zoom not supported
     */
    public Flowable<Boolean> isSupported() {
        return isOpticalZoomProcessor.toFlowable();
    }

    /**
     * Get the optical zoom specifications for the camera
     *
     * @return Flowable with instance of {@link OpticalZoomSpec}
     */
    public Flowable<OpticalZoomSpec> getOpticalZoomSpec() {
        return opticalZoomSpecDataProcessor.toFlowable();
    }

    /**
     * Get the current zoom level text
     *
     * @return Flowable with string zoom value
     */
    public Flowable<String> getZoomLevelText() {
        return zoomLevelTextProcessor.toFlowable();
    }

    /**
     * Get the current index of the camera the widget model is reacting to
     *
     * @return current camera index
     */
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    /**
     * Set the camera index to which the model should react
     *
     * @param cameraIndex camera index to set
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        lensModule.setCameraIndex(this, cameraIndex);
        restart();
    }

    /**
     * Get the current type of the lens the widget model is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return lensType;
    }

    /**
     * Set the type of the lens for which the widget model should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        this.lensType = lensType;
        restart();
    }

    //endregion

    //region private methods

    private Completable setZoomToDevice(int zoomLevel) {
        isZoomingProcessor.onNext(true);
        return djiSdkModel.setValue(opticalZoomFocalLengthKey, zoomLevel)
                .subscribeOn(SchedulerProvider.io())
                .doOnComplete(() -> isZoomingProcessor.onNext(false))
                .doOnError(throwable -> isZoomingProcessor.onNext(false));
    }

    private int validateZoomLevel(int zoomLevel) {
        if (zoomLevel < opticalZoomSpecDataProcessor.getValue().getMinFocalLength()) {
            return opticalZoomSpecDataProcessor.getValue().getMinFocalLength();
        }
        if (zoomLevel > opticalZoomSpecDataProcessor.getValue().getMaxFocalLength()) {
            return opticalZoomSpecDataProcessor.getValue().getMaxFocalLength();
        }
        return zoomLevel;
    }

    private int reverseNumber(int num, int min, int max) {
        return (max + min) - num;
    }
    //endregion
}
