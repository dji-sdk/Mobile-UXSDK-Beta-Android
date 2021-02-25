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

package dji.ux.beta.cameracore.widget.cameraaperturesetting;

import androidx.annotation.NonNull;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.Aperture;
import dji.common.camera.SettingsDefinitions.CameraType;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Camera Aperture Setting Widget Model
 * <p>
 * Widget Model for the {@link CameraApertureSettingWidget} used to define the
 * underlying logic and communication
 */
public class CameraApertureSettingWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "ApertureSettingWidMod";
    //endregion

    //region Fields
    private final DataProcessor<ExposureMode> exposureModeDataProcessor;
    private final DataProcessor<Boolean> isVariableApertureSupportedDataProcessor;
    private final DataProcessor<Aperture[]> apertureRangeArrayDataProcessor;
    private final DataProcessor<ExposureSettings> exposureSettingsDataProcessor;
    private final DataProcessor<Aperture> currentApertureDataProcessor;
    private final DataProcessor<CameraType> cameraTypeDataProcessor;
    private final DataProcessor<Boolean> changeApertureSupported;
    private int cameraIndex = 0;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private CameraKey currentApertureKey;
    private LensModule lensModule;
    //endregion

    //region public methods
    public CameraApertureSettingWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);

        exposureModeDataProcessor = DataProcessor.create(ExposureMode.UNKNOWN);
        isVariableApertureSupportedDataProcessor = DataProcessor.create(false);
        apertureRangeArrayDataProcessor = DataProcessor.create(new Aperture[0]);
        exposureSettingsDataProcessor = DataProcessor.create(new ExposureSettings(Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        currentApertureDataProcessor = DataProcessor.create(Aperture.UNKNOWN);
        cameraTypeDataProcessor = DataProcessor.create(CameraType.OTHER);
        changeApertureSupported = DataProcessor.create(false);
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        CameraKey exposureModeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
        CameraKey isVariableApertureSupportedKey = lensModule.createLensKey(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED, cameraIndex, lensType.value());
        bindDataProcessor(isVariableApertureSupportedKey, isVariableApertureSupportedDataProcessor);
        CameraKey apertureRangeKey = lensModule.createLensKey(CameraKey.APERTURE_RANGE, cameraIndex, lensType.value());
        bindDataProcessor(apertureRangeKey, apertureRangeArrayDataProcessor);
        CameraKey exposureSettingsKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        bindDataProcessor(exposureSettingsKey, exposureSettingsDataProcessor, newValue -> {
            ExposureSettings exposureParameters = ((ExposureSettings) newValue);
            currentApertureDataProcessor.onNext(exposureParameters.getAperture());
        });
        currentApertureKey = lensModule.createLensKey(CameraKey.APERTURE, cameraIndex, lensType.value());
        bindDataProcessor(currentApertureKey, currentApertureDataProcessor);
        CameraKey cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, cameraIndex);
        bindDataProcessor(cameraTypeKey, cameraTypeDataProcessor);
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
        // Empty block
    }

    @Override
    protected void updateStates() {
        changeApertureSupported.onNext(isVariableApertureSupportedDataProcessor.getValue() &&
                (exposureModeDataProcessor.getValue() == ExposureMode.MANUAL
                        || exposureModeDataProcessor.getValue() == ExposureMode.APERTURE_PRIORITY));
    }
    //endregion

    //region Data


    /**
     * Get the camera index for which the model is reacting.
     *
     * @return int representing {@link CameraIndex}.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return CameraIndex.find(cameraIndex);
    }

    /**
     * Set camera index to which the model should react.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
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
     * Is aperture change supported by the current camera and camera mode
     * combination
     *
     * @return Flowable with boolean value
     */
    public Flowable<Boolean> isChangeApertureSupported() {
        return changeApertureSupported.toFlowable();
    }

    /**
     * Get the current aperture value
     *
     * @return Flowable with instance of {@link Aperture} representing current value
     */
    public Flowable<Aperture> getCurrentApertureValue() {
        return currentApertureDataProcessor.toFlowable();
    }

    /**
     * Get the range of aperture values supported by the camera
     *
     * @return Flowable with Array of {@link Aperture}
     */
    public Flowable<Aperture[]> getApertureRange() {
        return apertureRangeArrayDataProcessor.toFlowable();
    }

    //endregion

    //region Actions

    /**
     * Set the aperture value to camera
     *
     * @param aperture instance of {@link Aperture}
     * @return Completable representing success and failure of action
     */
    public Completable setApertureValue(@NonNull Aperture aperture) {
        return djiSdkModel.setValue(currentApertureKey, aperture);
    }


    //endregion

}
