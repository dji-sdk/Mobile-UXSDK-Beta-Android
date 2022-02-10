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

package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettingsindicator;

import androidx.annotation.NonNull;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.ux.beta.core.base.ICameraIndex;
import io.reactivex.rxjava3.core.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Exposure Settings Indicator Widget Model
 * <p>
 * Widget Model for the {@link ExposureSettingsIndicatorWidget} used to define the
 * underlying logic and communication
 */
public class ExposureSettingsIndicatorWidgetModel extends WidgetModel implements ICameraIndex {

    //region Fields
    private final DataProcessor<ExposureMode> exposureModeDataProcessor;
    private int cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    //endregion

    //region Lifecycle
    public ExposureSettingsIndicatorWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                                @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        exposureModeDataProcessor = DataProcessor.create(ExposureMode.UNKNOWN);
    }

    @Override
    protected void inSetup() {
        DJIKey exposureModeKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // No Clean up needed
    }

    @Override
    protected void updateStates() {
        // No states
    }
    //endregion

    //region Data

    /**
     * Get the current exposure mode
     *
     * @return {@link ExposureMode}
     */
    public Flowable<ExposureMode> getExposureMode() {
        return exposureModeDataProcessor.toFlowable();
    }
    //endregion

    @Override
    public void updateCameraSource(@NonNull CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        this.cameraIndex = cameraIndex.getIndex();
        this.lensType = lensType;
        restart();
    }

    @NonNull
    public CameraIndex getCameraIndex() {
        return CameraIndex.find(cameraIndex);
    }

    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return lensType;
    }
}
