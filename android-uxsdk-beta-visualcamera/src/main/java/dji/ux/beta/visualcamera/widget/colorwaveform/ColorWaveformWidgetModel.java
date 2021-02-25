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

package dji.ux.beta.visualcamera.widget.colorwaveform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.ColorWaveformSettings.ColorWaveformDisplayState;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKey;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Color Waveform Widget Model
 * <p>
 * Widget Model for the {@link ColorWaveformWidget} used to define the
 * underlying logic and communication
 */
public class ColorWaveformWidgetModel extends WidgetModel {

    //region Fields
    private final GlobalPreferencesInterface preferencesManager;
    private final DataProcessor<Boolean> colorWaveformEnabledProcessor;
    private final DataProcessor<ColorWaveformDisplayState> colorWaveformDisplayStateProcessor;
    private final ObservableInMemoryKeyedStore keyedStore;
    private UXKey colorWaveformDisplayStateKey;
    private UXKey colorWaveformEnabledKey;
    //endregion

    //region Constructor
    public ColorWaveformWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                    @NonNull ObservableInMemoryKeyedStore keyedStore,
                                    @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
        colorWaveformEnabledProcessor = DataProcessor.create(false);
        colorWaveformDisplayStateProcessor = DataProcessor.create(ColorWaveformDisplayState.UNKNOWN);
        if (preferencesManager != null) {
            colorWaveformEnabledProcessor.onNext(preferencesManager.isColorWaveformEnabled());
            colorWaveformDisplayStateProcessor.onNext(preferencesManager.getColorWaveformDisplayState());
        }
    }
    //endregion

    //region Data

    /**
     * Get whether the color waveform is enabled.
     *
     * @return A Flowable that will emit a boolean when the enabled state of the color waveform
     * changes.
     */
    @NonNull
    public Flowable<Boolean> getColorWaveformEnabled() {
        return colorWaveformEnabledProcessor.toFlowable();
    }

    /**
     * Get the current display state of the color waveform.
     *
     * @return A Flowable that will emit the current display state of the color waveform.
     */
    @NonNull
    public Flowable<ColorWaveformDisplayState> getColorWaveformDisplayState() {
        return colorWaveformDisplayStateProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        colorWaveformDisplayStateKey = UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE);
        colorWaveformEnabledKey = UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED);

        bindDataProcessor(colorWaveformDisplayStateKey, colorWaveformDisplayStateProcessor);
        bindDataProcessor(colorWaveformEnabledKey, colorWaveformEnabledProcessor);

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

    //region User interaction

    /**
     * Set whether the color waveform is enabled.
     *
     * @param enabled `true` if the color waveform is enabled, `false` otherwise.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable setColorWaveformEnabled(boolean enabled) {
        if (preferencesManager != null) {
            preferencesManager.setColorWaveformEnabled(enabled);
        }
        return keyedStore.setValue(colorWaveformEnabledKey, enabled);
    }

    /**
     * Set the display state of the color waveform.
     *
     * @param displayState The display state of the color waveform.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable setColorWaveformDisplayState(@NonNull ColorWaveformDisplayState displayState) {
        if (preferencesManager != null) {
            preferencesManager.setColorWaveformDisplayState(displayState);
        }
        return keyedStore.setValue(colorWaveformDisplayStateKey, displayState);
    }
    //endregion
}
