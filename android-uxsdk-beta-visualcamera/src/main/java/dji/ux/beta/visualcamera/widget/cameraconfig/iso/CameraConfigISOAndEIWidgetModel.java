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

package dji.ux.beta.visualcamera.widget.cameraconfig.iso;

import androidx.annotation.NonNull;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.functions.Function;
import dji.thirdparty.org.reactivestreams.Publisher;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraConfigISOAndEIWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigISOAndEIWidgetModel extends WidgetModel {

    //region Constants
    /**
     * The value to display when the ISO is locked.
     */
    protected static final String LOCKED_ISO_VALUE = "500";
    //endregion

    //region Fields
    private final DataProcessor<ExposureSettings> exposureSettingsProcessor;
    private final DataProcessor<SettingsDefinitions.ISO> isoProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureSensitivityMode> exposureSensitivityModeProcessor;
    private final DataProcessor<Integer> eiValueProcessor;
    private final DataProcessor<String> isoAndEIValueProcessor;
    private int cameraIndex;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    //endregion

    //region Constructor
    public CameraConfigISOAndEIWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                           @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        exposureSettingsProcessor = DataProcessor.create(new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        isoProcessor = DataProcessor.create(SettingsDefinitions.ISO.UNKNOWN);
        exposureSensitivityModeProcessor = DataProcessor.create(SettingsDefinitions.ExposureSensitivityMode.UNKNOWN);
        eiValueProcessor = DataProcessor.create(0);
        isoAndEIValueProcessor = DataProcessor.create("");
    }
    //endregion

    //region Data

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
     * Get the ISO.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.ISO> getISO() {
        return isoProcessor.toFlowable();
    }

    /**
     * Get either the ISO or the exposure index value as a displayable String.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<String> getISOAndEIValue() {
        return isoAndEIValueProcessor.toFlowable();
    }

    /**
     * Get whether the camera is in EI mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> isEIMode() {
        return exposureSensitivityModeProcessor.toFlowable()
                .concatMap((Function<SettingsDefinitions.ExposureSensitivityMode, Publisher<Boolean>>) exposureSensitivityMode ->
                        Flowable.just(exposureSensitivityMode == SettingsDefinitions.ExposureSensitivityMode.EI));
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        DJIKey exposureSettingsKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        DJIKey isoKey = djiSdkModel.createLensKey(CameraKey.ISO, cameraIndex, lensType.value());
        DJIKey exposureSensitivityModeKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex);
        DJIKey eiValueKey = CameraKey.create(CameraKey.EI_VALUE, cameraIndex);

        bindDataProcessor(exposureSettingsKey, exposureSettingsProcessor);
        bindDataProcessor(isoKey, isoProcessor);
        bindDataProcessor(exposureSensitivityModeKey, exposureSensitivityModeProcessor);
        bindDataProcessor(eiValueKey, eiValueProcessor);
    }

    @Override
    protected void inCleanup() {
        //Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        updateConsolidatedISOValue();
    }
    //endregion

    //region Helpers
    private void updateConsolidatedISOValue() {
        if (exposureSensitivityModeProcessor.getValue() == SettingsDefinitions.ExposureSensitivityMode.EI) {
            isoAndEIValueProcessor.onNext(String.valueOf(eiValueProcessor.getValue()));
        } else {
            if (isoProcessor.getValue() == SettingsDefinitions.ISO.FIXED && exposureSettingsProcessor.getValue().getISO() == 0) {
                isoAndEIValueProcessor.onNext(LOCKED_ISO_VALUE);
            } else {
                isoAndEIValueProcessor.onNext(String.valueOf(exposureSettingsProcessor.getValue().getISO()));
            }
        }
    }
    //endregion
}
