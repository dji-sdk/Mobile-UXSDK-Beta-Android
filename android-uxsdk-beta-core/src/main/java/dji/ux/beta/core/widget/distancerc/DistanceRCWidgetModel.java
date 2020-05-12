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

package dji.ux.beta.core.widget.distancerc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.remotecontroller.GPSData;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.RemoteControllerKey;
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
 * Widget Model for the {@link DistanceRCWidget} used to define
 * the underlying logic and communication
 */
public class DistanceRCWidgetModel extends WidgetModel {

    //region Fields
    private final GlobalPreferencesInterface preferencesManager;
    private final DataProcessor<Float> distanceFromRCProcessor;
    private final DataProcessor<Double> aircraftLatitudeProcessor;
    private final DataProcessor<Double> aircraftLongitudeProcessor;
    private final DataProcessor<GPSData> rcGPSDataProcessor;
    private final DataProcessor<UnitConversionUtil.UnitType> unitTypeProcessor;

    //endregion

    //region Constructor
    public DistanceRCWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                 @NonNull ObservableInMemoryKeyedStore keyedStore,
                                 @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        this.preferencesManager = preferencesManager;
        distanceFromRCProcessor = DataProcessor.create(0.0f);
        aircraftLatitudeProcessor = DataProcessor.create(0.0);
        aircraftLongitudeProcessor = DataProcessor.create(0.0);
        rcGPSDataProcessor = DataProcessor.create(new GPSData.Builder().build());
        unitTypeProcessor = DataProcessor.create(UnitConversionUtil.UnitType.METRIC);
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.getUnitType());
        }
    }
    //endregion

    //region Data

    /**
     * Get the distance of the aircraft from the RC (pilot).
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Float> getDistanceFromRC() {
        return distanceFromRCProcessor.toFlowable();
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
        RemoteControllerKey rcGPSDataKey = RemoteControllerKey.create(RemoteControllerKey.GPS_DATA);

        bindDataProcessor(aircraftLatitudeKey, aircraftLatitudeProcessor);
        bindDataProcessor(aircraftLongitudeKey, aircraftLongitudeProcessor);
        bindDataProcessor(rcGPSDataKey, rcGPSDataProcessor);

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
        //Check if the GPS data is valid. The data is not valid if there are too few
        //satellites or the signal strength is too low.
        if (rcGPSDataProcessor.getValue().isValid()) {
            double rcLatitude = rcGPSDataProcessor.getValue().getLocation().getLatitude();
            double rcLongitude = rcGPSDataProcessor.getValue().getLocation().getLongitude();
            if (LocationUtil.checkLatitude(aircraftLatitudeProcessor.getValue())
                    && LocationUtil.checkLongitude(aircraftLongitudeProcessor.getValue())
                    && LocationUtil.checkLatitude(rcLatitude)
                    && LocationUtil.checkLongitude(rcLongitude)) {
                convertValueByUnit(LocationUtil.distanceBetween(rcLatitude,
                        rcLongitude,
                        aircraftLatitudeProcessor.getValue(),
                        aircraftLongitudeProcessor.getValue()));
            }
        }
    }
    //endregion

    //region Helpers
    private void convertValueByUnit(float distanceFromRC) {
        if (unitTypeProcessor.getValue() == UnitConversionUtil.UnitType.IMPERIAL) {
            distanceFromRCProcessor.onNext(UnitConversionUtil.convertMetersToFeet(distanceFromRC));
        } else {
            distanceFromRCProcessor.onNext(distanceFromRC);
        }
    }
    //endregion
}
