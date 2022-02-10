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

package dji.ux.beta.cameracore.widget.autoexposurelock;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Auto Exposure Lock Widget Model
 * <p>
 * Widget Model for the {@link AutoExposureLockWidget} used to define the
 * underlying logic and communication
 */
public class AutoExposureLockWidgetModel extends WidgetModel implements ICameraIndex {
    //region Fields
    private final DataProcessor<Boolean> autoExposureLockBooleanProcessor;
    private DJIKey autoExposureLockKey;
    private int cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    //endregion

    public AutoExposureLockWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        autoExposureLockBooleanProcessor = DataProcessor.create(false);
    }

    //region Data

    /**
     * Check if the auto exposure lock is enabled
     *
     * @return Flowable with boolean true - enabled  false - disabled
     */
    public Flowable<Boolean> isAutoExposureLockOn() {
        return autoExposureLockBooleanProcessor.toFlowable();
    }

    //endregion

    //region Actions

    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    @NonNull
    @Override
    public SettingsDefinitions.LensType getLensType() {
        return lensType;
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        this.cameraIndex = cameraIndex.getIndex();
        this.lensType = lensType;
        restart();
    }

    /**
     * Set auto exposure lock the opposite of its current state
     *
     * @return Completable representing success and failure of action
     */
    public Completable toggleAutoExposureLock() {
        return djiSdkModel.setValue(autoExposureLockKey, !autoExposureLockBooleanProcessor.getValue());
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        autoExposureLockKey = djiSdkModel.createLensKey(CameraKey.AE_LOCK, cameraIndex, lensType.value());
        bindDataProcessor(autoExposureLockKey, autoExposureLockBooleanProcessor);
    }

    @Override
    protected void inCleanup() {
        // nothing to clean
    }

    @Override
    protected void updateStates() {
        // No States
    }

    //endregion
}
