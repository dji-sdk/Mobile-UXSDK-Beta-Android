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

package dji.ux.beta.accessory.widget.spotlight;

import androidx.annotation.NonNull;

import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Spotlight Indicator Widget Model
 * <p>
 * Widget Model for the {@link SpotlightIndicatorWidget} used to define the
 * underlying logic and communication
 */
public class SpotlightIndicatorWidgetModel extends WidgetModel {

    //region
    private final DataProcessor<Boolean> spotlightConnectedDataProcessor;
    private final DataProcessor<Boolean> spotlightEnabledDataProcessor;
    private final DataProcessor<SpotlightIndicatorState> spotlightStateDataProcessor;

    //endregion

    // region Lifecycle
    public SpotlightIndicatorWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                         @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        spotlightConnectedDataProcessor = DataProcessor.create(false);
        spotlightEnabledDataProcessor = DataProcessor.create(false);
        spotlightStateDataProcessor = DataProcessor.create(SpotlightIndicatorState.HIDDEN);

    }

    @Override
    protected void inSetup() {
        DJIKey spotlightConnectedKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION);
        bindDataProcessor(spotlightConnectedKey, spotlightConnectedDataProcessor);
        DJIKey spotlightEnabledKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED);
        bindDataProcessor(spotlightEnabledKey, spotlightEnabledDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // No clean up required
    }

    @Override
    protected void updateStates() {
        if (spotlightConnectedDataProcessor.getValue()) {
            if (spotlightEnabledDataProcessor.getValue()) {
                spotlightStateDataProcessor.onNext(SpotlightIndicatorState.ACTIVE);
            } else {
                spotlightStateDataProcessor.onNext(SpotlightIndicatorState.INACTIVE);
            }
        } else {
            spotlightStateDataProcessor.onNext(SpotlightIndicatorState.HIDDEN);
        }
    }
    //endregion

    //region Data

    /**
     * Get the current state that the indicator should display
     *
     * @return Flowable with instance of {@link SpotlightIndicatorState}
     */
    public Flowable<SpotlightIndicatorState> getSpotlightState() {
        return spotlightStateDataProcessor.toFlowable();
    }

    //endregion

    /**
     * Enum representing the state of the indicator widget
     */
    public enum SpotlightIndicatorState {
        /**
         * Spotlight is not connected
         */
        HIDDEN,
        /**
         * Spotlight is currently connected and switched on
         */
        ACTIVE,
        /**
         * Spotlight is currently connected and switched off
         */
        INACTIVE
    }

}
