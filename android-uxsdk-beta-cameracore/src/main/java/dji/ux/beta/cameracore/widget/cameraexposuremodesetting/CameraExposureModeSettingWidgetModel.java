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

package dji.ux.beta.cameracore.widget.cameraexposuremodesetting;

import androidx.annotation.NonNull;

import dji.common.camera.SettingsDefinitions;
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
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraExposureModeSettingWidget} used to define the
 * underlying logic and communication
 */
public class CameraExposureModeSettingWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "ExpModeSettingWidMod";
    //endregion

    //region Fields
    private final DataProcessor<SettingsDefinitions.ExposureMode> exposureModeDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureMode[]> exposureModeRangeDataProcessor;
    private int cameraIndex = 0;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private DJIKey exposureModeKey;
    private LensModule lensModule;
    //endregion

    //region Lifecycle
    public CameraExposureModeSettingWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                                @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        exposureModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN);
        exposureModeRangeDataProcessor = DataProcessor.create(
                new SettingsDefinitions.ExposureMode[]{SettingsDefinitions.ExposureMode.UNKNOWN});
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        exposureModeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
        DJIKey exposureModeRangeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE_RANGE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeRangeKey, exposureModeRangeDataProcessor);
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
        // do nothing
    }

    @Override
    protected void updateStates() {
        // do nothing
    }
    //endregion

    //region data

    /**
     * Get the current index of the camera the widget model is reacting to
     *
     * @return current camera index
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    /**
     * Set the index of the camera for which the widget model should react
     *
     * @param cameraIndex camera index
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

    /**
     * Get the exposure mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<SettingsDefinitions.ExposureMode> getExposureMode() {
        return exposureModeDataProcessor.toFlowable();
    }
    //endregion

    //region actions

    /**
     * Get the exposure mode range.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<SettingsDefinitions.ExposureMode[]> getExposureModeRange() {
        return exposureModeRangeDataProcessor.toFlowable();
    }

    /**
     * Set the exposure mode.
     *
     * @param exposureMode The exposure mode to set.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable setExposureMode(@NonNull SettingsDefinitions.ExposureMode exposureMode) {
        return djiSdkModel.setValue(exposureModeKey, exposureMode);
    }
    //endregion
}
