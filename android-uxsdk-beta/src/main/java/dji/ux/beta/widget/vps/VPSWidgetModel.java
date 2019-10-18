/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.widget.vps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.keysdk.FlightControllerKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.GlobalPreferencesInterface;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.GlobalPreferenceKeys;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.base.uxsdkkeys.UXKey;
import dji.ux.beta.base.uxsdkkeys.UXKeys;
import dji.ux.beta.util.DataProcessor;
import dji.ux.beta.util.UnitConversionUtil;

/**
 * Widget Model for the Vision Positioning System widget {@link VPSWidget}
 * used to define the underlying logic and communication
 */
public class VPSWidgetModel extends WidgetModel {
    //region Fields
    private final GlobalPreferencesInterface preferencesManager;
    private final DataProcessor<Boolean> visionPositioningEnabledProcessor;
    private final DataProcessor<Boolean> ultrasonicBeingUsedProcessor;
    private final DataProcessor<Float> rawUltrasonicHeightProcessor;
    private final DataProcessor<Float> ultrasonicHeightProcessor;
    private final DataProcessor<UnitConversionUtil.UnitType> unitTypeProcessor;
    //endregion

    //region Constructor
    public VPSWidgetModel(@NonNull DJISDKModel djiSdkModel,
                          @NonNull ObservableInMemoryKeyedStore keyedStore,
                          @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        this.preferencesManager = preferencesManager;
        visionPositioningEnabledProcessor = DataProcessor.create(false);
        ultrasonicBeingUsedProcessor = DataProcessor.create(false);
        ultrasonicHeightProcessor = DataProcessor.create(0.0f);
        rawUltrasonicHeightProcessor = DataProcessor.create(0.0f);
        unitTypeProcessor = DataProcessor.create(UnitConversionUtil.UnitType.METRIC);
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.getUnitType());
        }
    }
    //endregion

    //region Data

    /**
     * Get the height of the aircraft as returned by the ultrasonic sensor.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Float> getUltrasonicHeight() {
        return ultrasonicHeightProcessor.toFlowable();
    }

    /**
     * Get if the vision positioning is enabled or disabled.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> getVisionPositioningEnabled() {
        return visionPositioningEnabledProcessor.toFlowable();
    }

    public Flowable<Boolean> getUltrasonicBeingUsed() {
        return ultrasonicBeingUsedProcessor.toFlowable();
    }

    /**
     * Get the unit type of the ultrasonic height value received.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<UnitConversionUtil.UnitType> getUnitType() {
        return unitTypeProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        FlightControllerKey visionPositioningEnabledKey =
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_ASSISTED_POSITIONING_ENABLED);
        FlightControllerKey isUltrasonicBeingUsedKey =
                FlightControllerKey.create(FlightControllerKey.IS_ULTRASONIC_BEING_USED);
        FlightControllerKey ultrasonicHeightKey =
                FlightControllerKey.create(FlightControllerKey.ULTRASONIC_HEIGHT_IN_METERS);

        bindDataProcessor(visionPositioningEnabledKey, visionPositioningEnabledProcessor);
        bindDataProcessor(isUltrasonicBeingUsedKey, ultrasonicBeingUsedProcessor);
        bindDataProcessor(ultrasonicHeightKey,
                rawUltrasonicHeightProcessor,
                ultrasonicHeight -> convertValueByUnit((float) ultrasonicHeight));

        UXKey unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE);
        bindDataProcessor(unitKey, unitTypeProcessor);

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
        //Nothing to update
    }
    //endregion

    //region Helpers
    private void convertValueByUnit(float ultrasonicHeight) {
        if (unitTypeProcessor.getValue() == UnitConversionUtil.UnitType.IMPERIAL) {
            ultrasonicHeightProcessor.onNext(UnitConversionUtil.convertMetersToFeet(ultrasonicHeight));
        } else {
            ultrasonicHeightProcessor.onNext(ultrasonicHeight);
        }
    }
    //endregion
}
