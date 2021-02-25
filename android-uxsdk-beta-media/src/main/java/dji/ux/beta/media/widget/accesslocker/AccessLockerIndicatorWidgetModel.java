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

package dji.ux.beta.media.widget.accesslocker;

import androidx.annotation.NonNull;

import dji.common.flightcontroller.accesslocker.AccessLockerState;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
/**
 * Access Locker Indicator Widget Model
 * <p>
 * Widget Model for the {@link AccessLockerIndicatorWidget} used to define the
 * underlying logic and communication
 */
public class AccessLockerIndicatorWidgetModel extends WidgetModel {

    //region Fields
    private final DataProcessor<Boolean> accessLockerSupportedProcessor;
    private final DataProcessor<AccessLockerState> accessLockerStateProcessor;

    //endregion

    //region life-cycle
    public AccessLockerIndicatorWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                            @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        accessLockerSupportedProcessor = DataProcessor.create(false);
        accessLockerStateProcessor = DataProcessor.create(AccessLockerState.UNKNOWN);
    }

    @Override
    protected void inSetup() {
        DJIKey accessLockerSupportedKey = FlightControllerKey.create(FlightControllerKey.ACCESS_LOCKER_SUPPORTED);
        bindDataProcessor(accessLockerSupportedKey, accessLockerSupportedProcessor);
        DJIKey accessLockerStateKey = FlightControllerKey.createAccessLockerKey(FlightControllerKey.ACCESS_LOCKER_STATE);
        bindDataProcessor(accessLockerStateKey, accessLockerStateProcessor);
    }

    @Override
    protected void inCleanup() {
        // No Clean up
    }

    @Override
    protected void updateStates() {
        // No update states
    }
    //endregion

    //region Data
    /**
     * Check if access locker is supported
     *
     * @return Flowable with boolean true - supported false - not supported
     */
    public Flowable<Boolean> isAccessLockerSupported() {
        return accessLockerSupportedProcessor.toFlowable();
    }

    /**
     * Get current state of access locker
     *
     * @return Flowable with instance of {@link AccessLockerState}
     */
    public Flowable<AccessLockerState> getAccessLockerState() {
        return accessLockerStateProcessor.toFlowable();
    }

    //endregion


}
