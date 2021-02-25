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

/**
 * Access Locker Set Password Widget Model
 * <p>
 * Widget Model for the {@link AccessLockerSetPasswordWidget} used to define the
 * underlying logic and communication
 */
public class AccessLockerSetPasswordWidgetModel extends WidgetModel {
    //region Fields
    private static final String USERNAME = "UserName";
    //endregion

    //region Lifecycle
    public AccessLockerSetPasswordWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                              @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
    }

    @Override
    protected void inSetup() {
        //empty
    }

    @Override
    protected void inCleanup() {
        //empty
    }

    @Override
    protected void updateStates() {
        //empty
    }

    //endregion

    //region Actions

    /**
     * Set password to aircraft.
     *
     * @param password String to be used as password
     * @return Completable representing success and failure of action
     */
    public Completable setPasswordToDevice(@NonNull String password) {
        DJIKey accessLockerSetUpUserKey = FlightControllerKey.createAccessLockerKey(FlightControllerKey.SET_UP_USER);
        return djiSdkModel.performAction(accessLockerSetUpUserKey, new UserAccountInfo(USERNAME, password));
    }
    //endregion
}
