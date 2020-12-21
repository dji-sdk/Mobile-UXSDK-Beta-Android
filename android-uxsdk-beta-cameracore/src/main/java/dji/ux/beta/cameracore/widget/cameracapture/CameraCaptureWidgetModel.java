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

package dji.ux.beta.cameracore.widget.cameracapture;

import androidx.annotation.NonNull;

import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.FlatCameraModule;

/**
 * Camera Capture Widget Model
 * <p>
 * Widget Model for {@link CameraCaptureWidget} used to define underlying logic
 * and communication
 */
public class CameraCaptureWidgetModel extends WidgetModel {

    //region Fields
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Lifecycle
    public CameraCaptureWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                    @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }

    @Override
    protected void inSetup() {
        // do nothing
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        // Empty function
    }
    //endregion

    //region Data

    /**
     * Get the current camera Mode
     *
     * @return Flowable with {@link CameraMode} instance
     */
    public Flowable<CameraMode> getCameraMode() {
        return flatCameraModule.getCameraModeDataProcessor().toFlowable();
    }
    //endregion
}
