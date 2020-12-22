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

package dji.ux.beta.visualcamera.widget.cameraconfig.shutter;

import androidx.annotation.NonNull;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraConfigShutterWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigShutterWidgetModel extends WidgetModel {
    //region Fields
    private DataProcessor<ExposureSettings> exposureSettingsProcessor;
    private DataProcessor<SettingsDefinitions.ShutterSpeed> shutterSpeedProcessor;
    private int cameraIndex;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    //endregion

    //region Constructor
    public CameraConfigShutterWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        exposureSettingsProcessor = DataProcessor.create(new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        shutterSpeedProcessor = DataProcessor.create(SettingsDefinitions.ShutterSpeed.UNKNOWN);
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
     * Get the shutter speed.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.ShutterSpeed> getShutterSpeed() {
        return shutterSpeedProcessor.toFlowable();
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        DJIKey exposureSettingsKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        bindDataProcessor(exposureSettingsKey, exposureSettingsProcessor, exposureSettings -> {
            ExposureSettings settings = (ExposureSettings) exposureSettings;
            if (settings.getShutterSpeed() != null) {
                shutterSpeedProcessor.onNext(settings.getShutterSpeed());
            }
        });
    }

    @Override
    protected void inCleanup() {
        //Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        //Nothing to update
    }
    //endregion
}
