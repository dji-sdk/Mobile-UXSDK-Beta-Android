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

package dji.ux.beta.accessory.widget.mfioconfig;

import androidx.annotation.NonNull;

import dji.common.flightcontroller.IOStateOnBoard;
import dji.common.remotecontroller.ProfessionalRC;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.RemoteControllerKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.Single;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;

public class MFIOConfigWidgetModel extends WidgetModel {

    //region Fields
    private static final int FREQUENCY_50_HZ = 50;
    private final DataProcessor<ProfessionalRC.Event> proButtonProcessor;
    //endregion

    //region Constructor
    public MFIOConfigWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                 @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        proButtonProcessor =
                DataProcessor.create(new ProfessionalRC.Event(ProfessionalRC.ButtonAction.OTHER));
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey buttonEventKey =
                RemoteControllerKey.create(RemoteControllerKey.BUTTON_EVENT_OF_PROFESSIONAL_RC);
        bindDataProcessor(buttonEventKey, proButtonProcessor);
    }

    @Override
    protected void inCleanup() {
        // Do nothing
    }

    @Override
    protected void updateStates() {
        // Do nothing
    }
    //endregion

    //region Data
    @NonNull
    public Flowable<ProfessionalRC.Event> getRCButtonEvents() {
        return proButtonProcessor.toFlowable();
    }

    @NonNull
    public Single<Object> getPowerSupplyEnabled() {
        DJIKey powerSupplyEnabledKey =
                FlightControllerKey.create(FlightControllerKey.POWER_SUPPLY_PORT_ENABLED);
        return djiSdkModel.getValue(powerSupplyEnabledKey);
    }

    @NonNull
    public Completable setPowerSupplyEnabled(boolean enabled) {
        DJIKey powerSupplyEnabledKey =
                FlightControllerKey.create(FlightControllerKey.POWER_SUPPLY_PORT_ENABLED);
        return djiSdkModel.setValue(powerSupplyEnabledKey, enabled)
                .subscribeOn(SchedulerProvider.io());
    }

    @NonNull
    public Completable initOnboardIO(int dutyRatio, int index) {
        DJIKey initIO = FlightControllerKey.create(FlightControllerKey.INIT_IO);
        IOStateOnBoard ioStateOnBoard =
                IOStateOnBoard.Builder.createInitialParams(dutyRatio, FREQUENCY_50_HZ);
        return djiSdkModel.performAction(initIO, index, ioStateOnBoard)
                .subscribeOn(SchedulerProvider.io());
    }

    @NonNull
    public Completable rcCustomizeBGButton() {
        DJIKey setCustomizableButtons =
                RemoteControllerKey.create(RemoteControllerKey.CUSTOMIZE_BUTTON);
        return djiSdkModel.performAction(setCustomizableButtons,
                ProfessionalRC.CustomizableButton.BG,
                ProfessionalRC.ButtonAction.CUSTOM150)
                .subscribeOn(SchedulerProvider.io());
    }

    @NonNull
    public Completable rcCustomizeC3Button() {
        DJIKey setCustomizableButtons =
                RemoteControllerKey.create(RemoteControllerKey.CUSTOMIZE_BUTTON);
        return djiSdkModel.performAction(setCustomizableButtons,
                ProfessionalRC.CustomizableButton.C3,
                ProfessionalRC.ButtonAction.CUSTOM151)
                .subscribeOn(SchedulerProvider.io());
    }

    @NonNull
    public Completable rcCustomizeC4Button() {
        DJIKey setCustomizableButtons =
                RemoteControllerKey.create(RemoteControllerKey.CUSTOMIZE_BUTTON);
        return djiSdkModel.performAction(setCustomizableButtons,
                ProfessionalRC.CustomizableButton.C4,
                ProfessionalRC.ButtonAction.CUSTOM152)
                .subscribeOn(SchedulerProvider.io());
    }
    //endregion
}
