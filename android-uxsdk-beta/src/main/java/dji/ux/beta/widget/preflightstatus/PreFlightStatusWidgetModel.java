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

package dji.ux.beta.widget.preflightstatus;

import android.support.annotation.NonNull;
import dji.common.logics.warningstatuslogic.WarningStatusItem;
import dji.keysdk.DJIKey;
import dji.keysdk.DiagnosticsKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DataProcessor;

/**
 * Widget Model for the {@link PreFlightStatusWidget} used to define
 * the underlying logic and communication
 */
public class PreFlightStatusWidgetModel extends WidgetModel {
    //region Constants
    private static final String TAG = PreFlightStatusWidgetModel.class.getName();
    //endregion
    //region Fields
    private DataProcessor<WarningStatusItem> preFlightStatusProcessor;
    //endregion

    //region Constructor
    public PreFlightStatusWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                      @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        preFlightStatusProcessor = DataProcessor.create(WarningStatusItem.getDefaultItem());
    }
    //endregion

    //region Data
    /**
     * Get the pre-flight status of the aircraft as a WarningStatusItem.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<WarningStatusItem> getPreFlightStatus() {
        return preFlightStatusProcessor.toFlowable();
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        DJIKey preFlightStatusKey = DiagnosticsKey.create(DiagnosticsKey.PRE_FLIGHT_STATUS);
        bindDataProcessor(preFlightStatusKey, preFlightStatusProcessor);
    }

    @Override
    protected void inCleanup() {
        // Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        //Nothing to update
    }
    //endregion
}
