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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Spotlight Control Widget Model
 * <p>
 * Widget Model for the {@link SpotlightControlWidget} used to define the
 * underlying logic and communication
 */
public class SpotlightControlWidgetModel extends WidgetModel {

    //region Fields
    private static final int DEFAULT_BRIGHTNESS_PERCENTAGE = 0;
    private static final float DEFAULT_TEMPERATURE = 0.0f;
    private final DataProcessor<Boolean> spotlightConnectedDataProcessor;
    private final DataProcessor<Boolean> spotlightEnabledDataProcessor;
    private final DataProcessor<Integer> spotlightBrightnessPercentageDataProcessor;
    private final DataProcessor<Float> spotlightTemperatureDataProcessor;
    private final DataProcessor<SpotlightState> spotlightStateProcessor;
    private DJIKey spotlightEnabledKey;
    private DJIKey spotlightBrightnessPercentageKey;

    //endregion


    //region Lifecycle
    public SpotlightControlWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        spotlightConnectedDataProcessor = DataProcessor.create(false);
        spotlightEnabledDataProcessor = DataProcessor.create(false);
        spotlightBrightnessPercentageDataProcessor = DataProcessor.create(0);
        spotlightTemperatureDataProcessor = DataProcessor.create(0.0f);
        spotlightStateProcessor = DataProcessor.create(new SpotlightState(false, DEFAULT_BRIGHTNESS_PERCENTAGE, DEFAULT_TEMPERATURE));
    }

    @Override
    protected void inSetup() {
        DJIKey spotlightConnectedKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION);
        bindDataProcessor(spotlightConnectedKey, spotlightConnectedDataProcessor);
        spotlightEnabledKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED);
        bindDataProcessor(spotlightEnabledKey, spotlightEnabledDataProcessor);
        spotlightBrightnessPercentageKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS);
        bindDataProcessor(spotlightBrightnessPercentageKey, spotlightBrightnessPercentageDataProcessor);
        DJIKey spotlightTemperatureKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE);
        bindDataProcessor(spotlightTemperatureKey, spotlightTemperatureDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // Empty Function
    }

    @Override
    protected void updateStates() {
        int percentage = spotlightEnabledDataProcessor.getValue() ? spotlightBrightnessPercentageDataProcessor.getValue() : DEFAULT_BRIGHTNESS_PERCENTAGE;
        float temperature = spotlightEnabledDataProcessor.getValue() ? spotlightTemperatureDataProcessor.getValue() : DEFAULT_TEMPERATURE;
        spotlightStateProcessor.onNext(new SpotlightState(spotlightEnabledDataProcessor.getValue(), percentage, temperature));
    }

    //endregion

    //region Data

    /**
     * Check if spotlight is connected
     *
     * @return Flowable with boolean value true - connected false - not connected
     */
    public Flowable<Boolean> isSpotlightConnected() {
        return spotlightConnectedDataProcessor.toFlowable();
    }

    /**
     * Get the current state of the spotlight
     *
     * @return Flowable with {@link SpotlightState}
     */
    public Flowable<SpotlightState> getSpotlightState() {
        return spotlightStateProcessor.toFlowable();
    }
    //endregion

    //region

    /**
     * Switch the spotlight ON and OFF
     *
     * @return Completable representing the state of the action
     */
    public Completable toggleSpotlight() {
        return djiSdkModel.setValue(spotlightEnabledKey, !spotlightEnabledDataProcessor.getValue());
    }

    /**
     * Set the brightness of the spotlight
     *
     * @param value integer value of brightness percentage
     * @return Completable representing the state of the action
     */
    public Completable setSpotlightBrightnessPercentage(@IntRange(from = 0, to = 100) int value) {
        return djiSdkModel.setValue(spotlightBrightnessPercentageKey, value);
    }
    //endregion
}
