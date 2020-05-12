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
 */

package dji.ux.beta.core.widget.distancehome;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.keysdk.FlightControllerKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.GlobalPreferencesInterface;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.base.uxsdkkeys.GlobalPreferenceKeys;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.base.uxsdkkeys.UXKey;
import dji.ux.beta.core.base.uxsdkkeys.UXKeys;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.LocationUtil;
import dji.ux.beta.core.util.UnitConversionUtil;

/**
 * Widget Model for the {@link DistanceHomeWidget} used to define
 * the underlying logic and communication
 */
public class DistanceHomeWidgetModel extends WidgetModel {

    //region Fields
    private final GlobalPreferencesInterface preferencesManager;
    private final DataProcessor<Float> distanceFromHomeProcessor;
    private final DataProcessor<Double> aircraftLatitudeProcessor;
    private final DataProcessor<Double> aircraftLongitudeProcessor;
    private final DataProcessor<Double> homeLatitudeProcessor;
    private final DataProcessor<Double> homeLongitudeProcessor;
    private final DataProcessor<UnitConversionUtil.UnitType> unitTypeProcessor;
    //endregion

    //region Constructor
    public DistanceHomeWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                   @NonNull ObservableInMemoryKeyedStore keyedStore,
                                   @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        this.preferencesManager = preferencesManager;
        distanceFromHomeProcessor = DataProcessor.create(0.0f);
        aircraftLatitudeProcessor = DataProcessor.create(0.0);
        aircraftLongitudeProcessor = DataProcessor.create(0.0);
        homeLatitudeProcessor = DataProcessor.create(0.0);
        homeLongitudeProcessor = DataProcessor.create(0.0);
        unitTypeProcessor = DataProcessor.create(UnitConversionUtil.UnitType.METRIC);
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.getUnitType());
        }
    }
    //endregion

    //region Data

    /**
     * Get the distance of the aircraft from the home location
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Float> getDistanceFromHome() {
        return distanceFromHomeProcessor.toFlowable();
    }

    /**
     * Get the unit type of the distance value received.
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
        FlightControllerKey aircraftLatitudeKey =
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE);
        FlightControllerKey aircraftLongitudeKey =
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE);
        FlightControllerKey homeLatitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE);
        FlightControllerKey homeLongitudeKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE);

        bindDataProcessor(aircraftLatitudeKey, aircraftLatitudeProcessor);
        bindDataProcessor(aircraftLongitudeKey, aircraftLongitudeProcessor);
        bindDataProcessor(homeLatitudeKey, homeLatitudeProcessor);
        bindDataProcessor(homeLongitudeKey, homeLongitudeProcessor);

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
        // Check if location coordinates are valid and update
        if (LocationUtil.checkLatitude(aircraftLatitudeProcessor.getValue())
                && LocationUtil.checkLongitude(aircraftLongitudeProcessor.getValue())
                && LocationUtil.checkLatitude(homeLatitudeProcessor.getValue())
                && LocationUtil.checkLongitude(homeLongitudeProcessor.getValue())) {
            convertValueByUnit(LocationUtil.distanceBetween(homeLatitudeProcessor.getValue(),
                    homeLongitudeProcessor.getValue(),
                    aircraftLatitudeProcessor.getValue(),
                    aircraftLongitudeProcessor.getValue()));
        }
    }
    //endregion

    //region Helpers
    private void convertValueByUnit(float distanceFromHome) {
        if (unitTypeProcessor.getValue() == UnitConversionUtil.UnitType.IMPERIAL) {
            distanceFromHomeProcessor.onNext(UnitConversionUtil.convertMetersToFeet(distanceFromHome));
        } else {
            distanceFromHomeProcessor.onNext(distanceFromHome);
        }
    }
    //endregion
}
