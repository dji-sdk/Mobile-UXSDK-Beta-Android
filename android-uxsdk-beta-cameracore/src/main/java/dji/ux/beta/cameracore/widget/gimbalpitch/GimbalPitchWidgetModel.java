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

package dji.ux.beta.cameracore.widget.gimbalpitch;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dji.common.gimbal.Attitude;
import dji.common.gimbal.CapabilityKey;
import dji.common.util.DJIParamMinMaxCapability;
import dji.keysdk.DJIKey;
import dji.keysdk.GimbalKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

public class GimbalPitchWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "GimbalPitchWidgetModel";
    private static final int GIMBAL_INDEX_DEFAULT = 0;
    private static final int WIDGET_FADE_DELAY_IN_SECONDS = 3;
    //endregion
    //region Fields
    private final DataProcessor<Attitude> attitudeProcessor;
    private final DataProcessor<Map> capabilitiesMapProcessor;
    private final DataProcessor<PitchState> pitchStateProcessor;
    private final DataProcessor<Boolean> shouldFadeProcessor;
    private final Flowable<Long> alphaTimer;
    private int gimbalIndex;
    private Disposable widgetAlphaTimer;
    private boolean isNewPitchValue;
    //endregion

    //region Constructor
    public GimbalPitchWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        attitudeProcessor = DataProcessor.create(new Attitude(0, 0, 0));
        capabilitiesMapProcessor = DataProcessor.create(new HashMap());
        pitchStateProcessor = DataProcessor.create(new PitchState(0, -90, 30));
        shouldFadeProcessor = DataProcessor.create(true);
        gimbalIndex = GIMBAL_INDEX_DEFAULT;
        alphaTimer = generateWidgetAlphaTimer();
    }
    //endregion

    //region Data
    public Flowable<PitchState> getPitchState() {
        return pitchStateProcessor.toFlowable();
    }

    public Flowable<Boolean> shouldFade() {
        return shouldFadeProcessor.toFlowable();
    }
    //endregion

    //region Actions
    public void setGimbalIndex(@IntRange(from = 0) int gimbalIndex) {
        this.gimbalIndex = gimbalIndex;
        restart();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey attitudeInDegreesKey = GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES, gimbalIndex);
        DJIKey capabilitiesKey = GimbalKey.create(GimbalKey.CAPABILITIES, gimbalIndex);
        bindDataProcessor(attitudeInDegreesKey, attitudeProcessor, newValue -> onGimbalPitchChanged((Attitude) newValue));
        bindDataProcessor(capabilitiesKey, capabilitiesMapProcessor);
    }

    @Override
    protected void inCleanup() {
        stopWidgetAlphaTimer();
    }

    //endregion

    //region Updates
    @Override
    protected void updateStates() {
        DJIParamMinMaxCapability minMaxCapability = getMinMaxCapability();
        attitudeProcessor.getValue();
        if (minMaxCapability == null) {
            return;
        }

        if (minMaxCapability.isSupported() && isNewPitchValue) {
            int pitch = Math.round(attitudeProcessor.getValue().getPitch());
            PitchState pitchState = new PitchState(
                    pitch,
                    minMaxCapability.getMin().intValue(),
                    minMaxCapability.getMax().intValue());
            startWidgetAlphaTimer();
            pitchStateProcessor.onNext(pitchState);
        }
    }
    //endregion

    //region Helpers
    private DJIParamMinMaxCapability getMinMaxCapability() {
        if (capabilitiesMapProcessor.getValue() == null) {
            return null;
        }

        Object capability = capabilitiesMapProcessor.getValue().get(CapabilityKey.ADJUST_PITCH);
        if (capability instanceof DJIParamMinMaxCapability
                && ((DJIParamMinMaxCapability) capability).isSupported()) {
            return (DJIParamMinMaxCapability) capability;
        }
        return null;
    }

    private void onGimbalPitchChanged(Attitude attitude) {
        float newPitch = attitude.getPitch();
        isNewPitchValue = roundPitchValue(newPitch) != roundPitchValue(attitudeProcessor.getValue().getPitch());
    }

    private float roundPitchValue(float pitchValue) {
        return Math.round(pitchValue);
    }

    private void startWidgetAlphaTimer() {
        stopWidgetAlphaTimer();
        widgetAlphaTimer = alphaTimer.subscribe();
    }

    private void stopWidgetAlphaTimer() {
        if (widgetAlphaTimer != null && !widgetAlphaTimer.isDisposed()) {
            widgetAlphaTimer.dispose();
            shouldFadeProcessor.onNext(false);
        }
    }

    private Flowable<Long> generateWidgetAlphaTimer() {
        return Flowable.timer(WIDGET_FADE_DELAY_IN_SECONDS, TimeUnit.SECONDS)
                .doOnNext(aLong -> shouldFadeProcessor.onNext(true));
    }
    //endregion

    //region State
    public class PitchState {
        private final int minRange;
        private final int maxRange;
        private final int pitch;

        PitchState(int pitch, int minRange, int maxRange) {
            this.pitch = pitch;
            this.minRange = minRange;
            this.maxRange = maxRange;
        }

        public int getMinRange() {
            return minRange;
        }

        public int getMaxRange() {
            return maxRange;
        }

        public int getPitch() {
            return pitch;
        }
    }
    //endregion
}
