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

package dji.ux.beta.media.widget.accesslocker.dialogwidgets;

import androidx.annotation.NonNull;

import dji.common.flightcontroller.accesslocker.UserAccountInfo;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Access Locker Remove Password Widget Model
 * <p>
 * Widget Model for the {@link AccessLockerRemovePasswordWidget} used to define the
 * underlying logic and communication
 */
public class AccessLockerRemovePasswordWidgetModel extends WidgetModel {

    //region Fields
    private final DataProcessor<String> accessLockerUsernameProcessor;
    //endregion

    public AccessLockerRemovePasswordWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                                 @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        accessLockerUsernameProcessor = DataProcessor.create("");
    }

    @Override
    protected void inSetup() {
        DJIKey accessLockerUsernameKey = FlightControllerKey.createAccessLockerKey(FlightControllerKey.ACCESS_LOCKER_USERNAME);
        bindDataProcessor(accessLockerUsernameKey, accessLockerUsernameProcessor);

    }

    @Override
    protected void inCleanup() {
        //Empty
    }

    @Override
    protected void updateStates() {
        //Empty
    }


    //region action

    /**
     * Remove aircraft password. This will make the aircraft
     * usable without a password.
     *
     * @param password string with current password to verify action
     * @return Completable representing success and failure of action
     */
    public Completable removePasswordFromDevice(@NonNull String password) {
        DJIKey accessLockerResetUserAccountKey =
                FlightControllerKey.createAccessLockerKey(FlightControllerKey.RESET_USER_ACCOUNT);
        return djiSdkModel.performAction(accessLockerResetUserAccountKey, new UserAccountInfo(accessLockerUsernameProcessor.getValue(), password));
    }
    //endregion

}
