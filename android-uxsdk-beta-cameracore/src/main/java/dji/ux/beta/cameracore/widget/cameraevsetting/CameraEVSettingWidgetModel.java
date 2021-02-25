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

package dji.ux.beta.cameracore.widget.cameraevsetting;

import androidx.annotation.NonNull;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.functions.Function;
import dji.thirdparty.org.reactivestreams.Publisher;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraEVSettingWidget} used to define
 * the underlying logic and communication
 */
public class CameraEVSettingWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "EVSettingWidgetModel";
    //endregion

    //region Fields
    private static final int DEFAULT_EV_RANGE_LENGTH = 31;
    private final DataProcessor<SettingsDefinitions.ExposureCompensation> exposureCompensationDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureMode> exposureModeDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureCompensation[]> exposureCompensationRangeDataProcessor;
    private final DataProcessor<ExposureSettings> exposureSettingsDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureSensitivityMode> exposureSensitivityModeDataProcessor;
    private final DataProcessor<Boolean> editableDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureCompensation> currentEVValueDataProcessor;
    private final DataProcessor<Integer> currentEVPosDataProcessor;
    private int cameraIndex = 0;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private CameraKey exposureCompensationKey;
    private LensModule lensModule;
    //endregion

    //region Lifecycle
    public CameraEVSettingWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                      @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        exposureCompensationDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureCompensation.UNKNOWN);
        exposureModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN);
        exposureCompensationRangeDataProcessor = DataProcessor.create(getDefaultExposureCompensationArray());
        exposureSettingsDataProcessor = DataProcessor.create(new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        exposureSensitivityModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureSensitivityMode.UNKNOWN);

        editableDataProcessor = DataProcessor.create(true);
        currentEVValueDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureCompensation.UNKNOWN);
        currentEVPosDataProcessor = DataProcessor.create(exposureCompensationRangeDataProcessor.getValue().length / 2);
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        exposureCompensationKey = lensModule.createLensKey(CameraKey.EXPOSURE_COMPENSATION, cameraIndex, lensType.value());
        bindDataProcessor(exposureCompensationKey, exposureCompensationDataProcessor);
        CameraKey exposureModeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
        CameraKey compensationRangeKey = lensModule.createLensKey(CameraKey.EXPOSURE_COMPENSATION_RANGE, cameraIndex, lensType.value());
        bindDataProcessor(compensationRangeKey, exposureCompensationRangeDataProcessor);
        CameraKey exposureSettingsKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        bindDataProcessor(exposureSettingsKey, exposureSettingsDataProcessor);
        CameraKey exposureSensitivityModeKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex);
        bindDataProcessor(exposureSensitivityModeKey, exposureSensitivityModeDataProcessor);
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
        updateEditable();
        updateEV();
        updateEvPos();
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
     * Get whether the EV can be set.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<Boolean> isEditable() {
        return editableDataProcessor.toFlowable();
    }

    /**
     * Get the EV range.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<SettingsDefinitions.ExposureCompensation[]> getEVRange() {
        return exposureCompensationRangeDataProcessor.toFlowable();
    }

    /**
     * Get the index of the current EV within the EV range array.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<Integer> getCurrentEVPosition() {
        return currentEVPosDataProcessor.toFlowable();
    }

    /**
     * Get whether the camera is in EI mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<Boolean> isEIMode() {
        return exposureSensitivityModeDataProcessor.toFlowable()
                .concatMap((Function<SettingsDefinitions.ExposureSensitivityMode, Publisher<Boolean>>) exposureSensitivityMode ->
                        Flowable.just(exposureSensitivityMode == SettingsDefinitions.ExposureSensitivityMode.EI));
    }

    //endregion

    //region actions

    /**
     * Increment the EV to the next value in the EV range array. If the EV is already at the
     * maximum, returns an error state.
     *
     * @return Completable representing success or failure of action
     */
    @NonNull
    public Completable incrementEV() {
        SettingsDefinitions.ExposureCompensation[] evValueArray = exposureCompensationRangeDataProcessor.getValue();
        int currentEvPos = currentEVPosDataProcessor.getValue();

        return setExposureCompensation(evValueArray, currentEvPos + 1);
    }

    /**
     * Decrement the EV to the previous value in the EV range array. If the EV is already at the
     * minimum, returns an error state.
     *
     * @return Completable representing success or failure of action
     */
    @NonNull
    public Completable decrementEV() {
        SettingsDefinitions.ExposureCompensation[] evValueArray = exposureCompensationRangeDataProcessor.getValue();
        int currentEvPos = currentEVPosDataProcessor.getValue();

        return setExposureCompensation(evValueArray, currentEvPos - 1);
    }

    /**
     * Sets the EV to the middle value in the EV range array.
     *
     * @return Completable representing success or failure of action
     */
    @NonNull
    public Completable restoreEV() {
        SettingsDefinitions.ExposureCompensation[] evValueArray = exposureCompensationRangeDataProcessor.getValue();

        return setExposureCompensation(evValueArray, evValueArray.length / 2);
    }
    //endregion

    //region helpers
    @NonNull
    private Completable setExposureCompensation(SettingsDefinitions.ExposureCompensation[] evValueArray, int evPos) {
        if (evPos >= evValueArray.length || evPos < 0) {
            return Completable.error(new Throwable("Exposure compensation is out of range"));
        }

        SettingsDefinitions.ExposureCompensation newEv = evValueArray[evPos];
        return djiSdkModel.setValue(exposureCompensationKey, newEv);
    }

    @NonNull
    private SettingsDefinitions.ExposureCompensation[] getDefaultExposureCompensationArray() {
        SettingsDefinitions.ExposureCompensation[] evRange = new SettingsDefinitions.ExposureCompensation[DEFAULT_EV_RANGE_LENGTH];
        for (int i = 0; i < DEFAULT_EV_RANGE_LENGTH; i++) {
            evRange[i] = SettingsDefinitions.ExposureCompensation.find(i + 1);
        }
        return evRange;
    }

    private void updateEditable() {
        editableDataProcessor.onNext(exposureModeDataProcessor.getValue() != SettingsDefinitions.ExposureMode.MANUAL
                && exposureCompensationDataProcessor.getValue() != SettingsDefinitions.ExposureCompensation.FIXED);
    }

    private void updateEV() {
        if (editableDataProcessor.getValue()) {
            currentEVValueDataProcessor.onNext(exposureCompensationDataProcessor.getValue());
        } else {
            SettingsDefinitions.ExposureCompensation ev = exposureSettingsDataProcessor.getValue().getExposureCompensation();
            if (ev == SettingsDefinitions.ExposureCompensation.FIXED) {
                currentEVValueDataProcessor.onNext(SettingsDefinitions.ExposureCompensation.N_0_0);
            } else {
                currentEVValueDataProcessor.onNext(ev);
            }
        }
    }

    private void updateEvPos() {
        SettingsDefinitions.ExposureCompensation ev = currentEVValueDataProcessor.getValue();
        SettingsDefinitions.ExposureCompensation[] evValueArray = exposureCompensationRangeDataProcessor.getValue();
        for (int i = 0, length = evValueArray.length; i < length; i++) {
            if (ev == evValueArray[i]) {
                currentEVPosDataProcessor.onNext(i);
            }
        }
    }
    //endregion
}
