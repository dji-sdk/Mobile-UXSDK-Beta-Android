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

package dji.ux.beta.cameracore.widget.manualfocus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.functions.Function;
import dji.thirdparty.org.reactivestreams.Publisher;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKey;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Manual Focus Widget Model
 * <p>
 * Widget Model for the {@link ManualFocusWidget} used to define the
 * underlying logic and communication
 */
public class ManualFocusWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "ManualFocusWidgetModel";
    //endregion

    //region Fields
    private final DataProcessor<Integer> focusRingProcessor;
    private final DataProcessor<Integer> focusRingUpperBoundProcessor;
    private final DataProcessor<SettingDefinitions.ControlMode> controlModeProcessor;
    private DJIKey cameraFocusRingKey;
    private final GlobalPreferencesInterface preferencesManager;
    private int cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private LensModule lensModule;
    //endregion

    //region life-cycle
    public ManualFocusWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                  @NonNull ObservableInMemoryKeyedStore keyedStore,
                                  @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        focusRingProcessor = DataProcessor.create(0);
        focusRingUpperBoundProcessor = DataProcessor.create(0);
        controlModeProcessor = DataProcessor.create(SettingDefinitions.ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            controlModeProcessor.onNext(preferencesManager.getControlMode());
        }
        this.preferencesManager = preferencesManager;
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        cameraFocusRingKey = lensModule.createLensKey(CameraKey.FOCUS_RING_VALUE, cameraIndex, lensType.value());
        bindDataProcessor(cameraFocusRingKey, focusRingProcessor);
        DJIKey cameraFocusRingUpperBoundKey = lensModule.createLensKey(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, cameraIndex, lensType.value());
        bindDataProcessor(cameraFocusRingUpperBoundKey, focusRingUpperBoundProcessor);
        UXKey controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeProcessor);
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(value -> {
                    if (value) {
                        restart();
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")));

        if (preferencesManager != null) {
            preferencesManager.setUpListener();
        }
    }

    @Override
    protected void inCleanup() {
        if (preferencesManager != null) {
            preferencesManager.cleanup();
        }
    }

    @Override
    protected void updateStates() {
        // No states to update
    }
    //endregion

    //region Data

    /**
     * Get the current focus ring value
     *
     * @return Flowable with integer
     */
    public Flowable<Integer> getFocusRingValue() {
        return focusRingProcessor.toFlowable();
    }

    /**
     * Get the upper bound of the focus ring value
     *
     * @return Flowable with integer
     */
    public Flowable<Integer> getFocusRingUpperBoundValue() {
        return focusRingUpperBoundProcessor.toFlowable();
    }

    /**
     * Check if the camera is currently in manual focus mode
     *
     * @return Flowable with boolean value
     * true - Manual Focus Mode
     * false - Not in Manual Focus Mode
     */
    public Flowable<Boolean> isManualFocusMode() {
        return controlModeProcessor.toFlowable()
                .concatMap((Function<SettingDefinitions.ControlMode, Publisher<Boolean>>) controlMode ->
                        Flowable.just(controlMode == SettingDefinitions.ControlMode.MANUAL_FOCUS));
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

    //region Action

    /**
     * Set the focus ring value
     *
     * @param focusRing integer value representing focus ring
     * @return Completable determining the success or failure of the action
     */
    public Completable setFocusRingValue(int focusRing) {
        return djiSdkModel.setValue(cameraFocusRingKey, focusRing);
    }

    //endregion
}
