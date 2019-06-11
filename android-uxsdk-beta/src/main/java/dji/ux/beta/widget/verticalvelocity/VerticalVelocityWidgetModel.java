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

package dji.ux.beta.widget.verticalvelocity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
 * Widget Model for the {@link VerticalVelocityWidget} used to define
 * the underlying logic and communication
 */
public class VerticalVelocityWidgetModel extends WidgetModel {
    //region Fields
    private final GlobalPreferencesInterface preferencesManager;
    private final DataProcessor<Float> rawVerticalVelocityProcessor;
    private final DataProcessor<Float> verticalVelocityProcessor;
    private final DataProcessor<UnitConversionUtil.UnitType> unitTypeProcessor;
    //endregion

    //region Constructor
    public VerticalVelocityWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore,
                                       @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        this.preferencesManager = preferencesManager;
        rawVerticalVelocityProcessor = DataProcessor.create(0.0f);
        verticalVelocityProcessor = DataProcessor.create(0.0f);
        unitTypeProcessor = DataProcessor.create(UnitConversionUtil.UnitType.METRIC);
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.getUnitType());
        }
    }
    //endregion

    //region Data
    /**
     * Get the value of the vertical velocity of the aircraft
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Float> getVerticalVelocity() {
        return verticalVelocityProcessor.toFlowable();
    }

    /**
     * Get the unit type of the vertical velocity value received.
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
        FlightControllerKey aircraftVelocityZKey = FlightControllerKey.create(FlightControllerKey.VELOCITY_Z);
        bindDataProcessor(aircraftVelocityZKey,
                          rawVerticalVelocityProcessor,
                          verticalVelocity -> convertValueByUnit((float) verticalVelocity));

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
    private void convertValueByUnit(float verticalVelocity) {
        if (unitTypeProcessor.getValue() == UnitConversionUtil.UnitType.IMPERIAL) {
            verticalVelocityProcessor.onNext(UnitConversionUtil.convertMetersPerSecToMilesPerHr(verticalVelocity));
        } else {
            verticalVelocityProcessor.onNext(verticalVelocity);
        }
    }
    //endregion
}
