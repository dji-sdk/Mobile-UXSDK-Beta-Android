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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.remotecontroller.ProfessionalRC;
import dji.log.DJILog;
import dji.ux.beta.accessory.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

public class MFIOConfigWidget extends ConstraintLayoutWidget
        implements View.OnClickListener, Switch.OnCheckedChangeListener {

    //region Fields
    private static final String TAG = "MFIOConfigWidget";
    private static final int PORT_1_OPEN_DUTY_RATIO = 60;
    private static final int PORT_2_OPEN_DUTY_RATIO = 76;
    private static final int PORT_3_OPEN_DUTY_RATIO = 70;
    private static final int PORT_1_CLOSE_DUTY_RATIO = 40;
    private static final int PORT_2_CLOSE_DUTY_RATIO = 58;
    private static final int PORT_3_CLOSE_DUTY_RATIO = 54;
    private MFIOConfigWidgetModel widgetModel;
    private Switch powerSupplyEnabledSwitch;
    private Button port1Button;
    private Button port2Button;
    private Button port3Button;
    private boolean isPort1Open;
    private boolean isPort2Open;
    private boolean isPort3Open;
    //endregion

    //region Constructor
    public MFIOConfigWidget(@NonNull Context context) {
        super(context);
    }

    public MFIOConfigWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MFIOConfigWidget(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_mfio_config, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);

        powerSupplyEnabledSwitch = findViewById(R.id.switch_enable_power_supply);
        powerSupplyEnabledSwitch.setOnCheckedChangeListener(this);
        port1Button = findViewById(R.id.button_port_one);
        port1Button.setOnClickListener(this);
        port2Button = findViewById(R.id.button_port_two);
        port2Button.setOnClickListener(this);
        port3Button = findViewById(R.id.button_port_three);
        port3Button.setOnClickListener(this);

        if (!isInEditMode()) {
            widgetModel = new MFIOConfigWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            initializePowerSupplyEnabled();
            initializeIOPortsToClosed();
            initializeCustomizableRCButtons();
        }
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_port_one) {
            togglePort1State();
        } else if (v.getId() == R.id.button_port_two) {
            togglePort2State();
        } else if (v.getId() == R.id.button_port_three) {
            togglePort3State();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        addDisposable(widgetModel.setPowerSupplyEnabled(isChecked).subscribe(() -> {
        }, throwable -> {
            powerSupplyEnabledSwitch.setChecked(!isChecked);
            DJILog.e(TAG, "setPowerSupplyEnabled: " + throwable.getLocalizedMessage());
        }));
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getRCButtonEvents()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updatePortState));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_mfio_config_ratio);
    }
    //endregion

    //region Helpers
    private void initializePowerSupplyEnabled() {
        addDisposable(widgetModel.getPowerSupplyEnabled()
                .observeOn(SchedulerProvider.ui())
                .subscribe(enabled -> powerSupplyEnabledSwitch.setChecked((boolean) enabled),
                        logErrorConsumer("MFIOConfigWidget", "PowerSupplyEnabled:")));
    }

    private void initializeIOPortsToClosed() {
        closePort(PORT_1_CLOSE_DUTY_RATIO, 0, port1Button);
        isPort1Open = false;
        closePort(PORT_2_CLOSE_DUTY_RATIO, 1, port2Button);
        isPort2Open = false;
        closePort(PORT_3_CLOSE_DUTY_RATIO, 2, port3Button);
        isPort3Open = false;
    }

    private void initializeCustomizableRCButtons() {
        addDisposable(widgetModel.rcCustomizeBGButton().subscribe(() -> {
        }, logErrorConsumer(TAG, "setBGCustomization: ")));
        addDisposable(widgetModel.rcCustomizeC3Button().subscribe(() -> {
        }, logErrorConsumer(TAG, "setC3Customization: ")));
        addDisposable(widgetModel.rcCustomizeC4Button().subscribe(() -> {
        }, logErrorConsumer(TAG, "setC4Customization: ")));
    }

    private void updatePortState(@NonNull ProfessionalRC.Event event) {
        switch (event.getFunctionID()) {
            case CUSTOM150:
                togglePort1State();
                break;
            case CUSTOM151:
                togglePort2State();
                break;
            case CUSTOM152:
                togglePort3State();
                break;
            default:
                // Do nothing
                break;
        }
    }

    private void togglePort1State() {
        if (isPort1Open) {
            closePort(PORT_1_CLOSE_DUTY_RATIO, 0, port1Button);
            isPort1Open = false;
        } else {
            openPort(PORT_1_OPEN_DUTY_RATIO, 0, port1Button);
            isPort1Open = true;
        }
    }

    private void togglePort2State() {
        if (isPort2Open) {
            closePort(PORT_2_CLOSE_DUTY_RATIO, 1, port2Button);
            isPort2Open = false;
        } else {
            openPort(PORT_2_OPEN_DUTY_RATIO, 1, port2Button);
            isPort2Open = true;
        }
    }

    private void togglePort3State() {
        if (isPort3Open) {
            closePort(PORT_3_CLOSE_DUTY_RATIO, 2, port3Button);
            isPort3Open = false;
        } else {
            openPort(PORT_3_OPEN_DUTY_RATIO, 2, port3Button);
            isPort3Open = true;
        }
    }

    private void closePort(int dutyRatio, int index, Button port) {
        addDisposable(widgetModel.initOnboardIO(dutyRatio, index).subscribe(() -> {
        }, logErrorConsumer(TAG, "setPortClosed: " + index)));

        port.setText(R.string.uxsdk_port_open);
    }

    private void openPort(int dutyRatio, int index, Button port) {
        addDisposable(widgetModel.initOnboardIO(dutyRatio, index).subscribe(() -> {
        }, logErrorConsumer(TAG, "setPortOpened: " + index)));

        port.setText(R.string.uxsdk_port_close);
    }
    //endregion
}
