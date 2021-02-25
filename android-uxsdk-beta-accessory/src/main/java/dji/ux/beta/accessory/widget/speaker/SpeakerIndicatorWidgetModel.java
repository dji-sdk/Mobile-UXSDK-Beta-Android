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

package dji.ux.beta.accessory.widget.speaker;

import androidx.annotation.NonNull;

import dji.common.accessory.SettingsDefinitions;
import dji.common.accessory.SpeakerState;
import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Speaker Indicator Widget Model
 * <p>
 * Widget Model for the {@link SpeakerIndicatorWidget} used to define the
 * underlying logic and communication
 */
public class SpeakerIndicatorWidgetModel extends WidgetModel {

    //region private fields
    private final DataProcessor<Boolean> speakerConnectedProcessor;
    private final DataProcessor<SpeakerState> speakerStateProcessor;
    private final DataProcessor<SpeakerIndicatorState> speakerIndicatorStateProcessor;
    //endregion

    //region Lifecycle
    public SpeakerIndicatorWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        speakerConnectedProcessor = DataProcessor.create(false);
        speakerStateProcessor = DataProcessor.create(new SpeakerState.Builder().index(0)
                .playingState(SettingsDefinitions.SpeakerPlayingState.UNKNOWN)
                .playingMode(SettingsDefinitions.PlayMode.UNKNOWN)
                .storageLocation(SettingsDefinitions.AudioStorageLocation.UNKNOWN)
                .volume(0)
                .build());
        speakerIndicatorStateProcessor = DataProcessor.create(SpeakerIndicatorState.HIDDEN);
    }

    @Override
    protected void inSetup() {
        DJIKey speakerConnectedKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.CONNECTION);
        bindDataProcessor(speakerConnectedKey, speakerConnectedProcessor);
        DJIKey speakerActiveKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.SPEAKER_STATE);
        bindDataProcessor(speakerActiveKey, speakerStateProcessor);
    }

    @Override
    protected void inCleanup() {
        // No clean up
    }

    @Override
    protected void updateStates() {
        if (speakerConnectedProcessor.getValue()) {
            if (speakerStateProcessor.getValue().getPlayingState() == SettingsDefinitions.SpeakerPlayingState.PLAYING) {
                speakerIndicatorStateProcessor.onNext(SpeakerIndicatorState.ACTIVE);
            } else {
                speakerIndicatorStateProcessor.onNext(SpeakerIndicatorState.INACTIVE);
            }
        } else {
            speakerIndicatorStateProcessor.onNext(SpeakerIndicatorState.HIDDEN);
        }


    }
    //endregion

    //region Data

    /**
     * Get the speaker indicator state
     *
     * @return Flowable with instance of {@link SpeakerIndicatorState}
     */
    public Flowable<SpeakerIndicatorState> getSpeakerIndicatorState() {
        return speakerIndicatorStateProcessor.toFlowable();
    }

    //endregion

    /**
     * Enum representing the state of the indicator widget
     */
    public enum SpeakerIndicatorState {
        /**
         * Speaker is not connected
         */
        HIDDEN,

        /**
         * Speaker is connected and playing audio
         */
        ACTIVE,

        /**
         * Speaker is connected but not playing audio
         */
        INACTIVE
    }

}
