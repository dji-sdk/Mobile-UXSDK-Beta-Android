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

package dji.ux.beta.widget.simulator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.Group;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.simulator.SimulatorWindData;
import dji.common.model.LocationCoordinate2D;
import dji.log.DJILog;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.OnStateChangeCallback;
import dji.ux.beta.base.SchedulerProvider;
import dji.ux.beta.base.UXSDKError;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DisplayUtil;
import dji.ux.beta.util.EditTextNumberInputFilter;
import dji.ux.beta.widget.simulator.preset.OnLoadPresetListener;
import dji.ux.beta.widget.simulator.preset.PresetListDialog;
import dji.ux.beta.widget.simulator.preset.SavePresetDialog;
import dji.ux.beta.widget.simulator.preset.SimulatorPresetData;

/**
 * Simulator Control Widget
 * <p>
 * Widget can be used for quick simulation of the aircraft flight without flying it.
 * Aircraft should be connected to run the simulation.
 * User can enter the location coordinates, satellite count and
 * data frequency. The user has the option to save presets to reuse simulation
 * configuration.
 */
public class SimulatorControlWidget extends ConstraintLayoutWidget
        implements OnClickListener, OnStateChangeCallback, OnLoadPresetListener {

    //region fields
    private static final int WIND_DIRECTION_X = 0;
    private static final int WIND_DIRECTION_Y = 1;
    private static final int WIND_DIRECTION_Z = 2;
    private static final int DEFAULT_FREQUENCY = 20;
    private static final int DEFAULT_SATELLITE_COUNT = 0;
    private static final int SIMULATION_MIN_WIND_SPEED = -2000;
    private static final int SIMULATION_MAX_WIND_SPEED = 20000;
    private SimulatorControlWidgetModel widgetModel;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private EditText frequencyEditText;
    private EditText satelliteCountEditText;
    private TextView simulatorTitleTextView;
    private Switch simulatorSwitch;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView satelliteTextView;
    private TextView worldXTextView;
    private TextView worldYTextView;
    private TextView worldZTextView;
    private TextView motorsStartedTextView;
    private TextView aircraftFlyingTextView;
    private TextView pitchTextView;
    private TextView yawTextView;
    private TextView rollTextView;
    private TextView frequencyTextView;
    private TextView loadPresetTextView;
    private TextView savePresetTextView;
    private TextView windXTextView;
    private TextView windYTextView;
    private TextView windZTextView;

    private TextView latitudeLabelTextView;
    private TextView longitudeLabelTextView;
    private TextView satelliteLabelTextView;
    private TextView worldXLabelTextView;
    private TextView worldYLabelTextView;
    private TextView worldZLabelTextView;
    private TextView motorsStartedLabelTextView;
    private TextView aircraftFlyingLabelTextView;
    private TextView pitchLabelTextView;
    private TextView yawLabelTextView;
    private TextView rollLabelTextView;
    private TextView frequencyLabelTextView;
    private TextView windXLabelTextView;
    private TextView windYLabelTextView;
    private TextView windZLabelTextView;

    private TextView positionSectionHeaderTextView;
    private TextView windSectionHeaderTextView;
    private TextView attitudeSectionHeaderTextView;
    private TextView aircraftSectionHeaderTextView;

    private Group attitudeGroup;
    private Group aircraftStatusGroup;
    private Group realWorldPositionGroup;
    private Group windSimulationGroup;
    private Group buttonGroup;

    private DecimalFormat df;
    private int worldPositionSectionVisible;
    private int attitudeSectionVisible;
    private int aircraftStatusSectionVisible;
    private int windSectionVisible;
    private Drawable activeSimulatorDrawable;
    private Drawable inActiveSimulatorDrawable;
    private SchedulerProvider schedulerProvider;
    private boolean shouldReactToCheckChange;
    //endregion

    //region lifecycle
    public SimulatorControlWidget(Context context) {
        super(context);
    }

    public SimulatorControlWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimulatorControlWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        inflate(context, R.layout.uxsdk_widget_simulator_control, this);
        schedulerProvider = SchedulerProvider.getInstance();
        initViewElements();
        if (!isInEditMode()) {
            widgetModel = new SimulatorControlWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    schedulerProvider);
        }
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getSatelliteCount()
                .observeOn(schedulerProvider.ui())
                .subscribe(this::updateSatelliteCount));
        addReaction(widgetModel.getSimulatorWindData()
                .observeOn(schedulerProvider.ui())
                .subscribe(this::updateWindValues));
        addReaction(widgetModel.getSimulatorState()
                .observeOn(schedulerProvider.ui())
                .subscribe(this::updateWidgetValues));
        addReaction(widgetModel.isSimulatorActive()
                .observeOn(schedulerProvider.ui())
                .subscribe(this::updateUI));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_simulator_control_ratio);
    }

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
    public void onLoadPreset(SimulatorPresetData simulatorPresetData) {
        if (simulatorPresetData != null) {
            latitudeEditText.setText(String.valueOf(simulatorPresetData.getLatitude()));
            longitudeEditText.setText(String.valueOf(simulatorPresetData.getLongitude()));
            satelliteCountEditText.setText(String.valueOf(simulatorPresetData.getSatelliteCount()));
            frequencyEditText.setText(String.valueOf(simulatorPresetData.getUpdateFrequency()));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.textview_load_preset) {
            showPresetListDialog();
        } else if (id == R.id.textview_save_preset) {
            showSavePresetDialog();
        } else if (id == R.id.imageview_btn_plus_x) {
            setWindSpeedUI(WIND_DIRECTION_X, true);
        } else if (id == R.id.imageview_btn_plus_y) {
            setWindSpeedUI(WIND_DIRECTION_Y, true);
        } else if (id == R.id.imageview_btn_plus_z) {
            setWindSpeedUI(WIND_DIRECTION_Z, true);
        } else if (id == R.id.imageview_btn_minus_x) {
            setWindSpeedUI(WIND_DIRECTION_X, false);
        } else if (id == R.id.imageview_btn_minus_y) {
            setWindSpeedUI(WIND_DIRECTION_Y, false);
        } else if (id == R.id.imageview_btn_minus_z) {
            setWindSpeedUI(WIND_DIRECTION_Z, false);
        }
    }

    @Override
    public void onStateChange(@Nullable Object state) {
        toggleVisibility();
    }

    //endregion

    //region private methods
    private void toggleVisibility() {
        if (getVisibility() == VISIBLE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimulatorControlWidget);

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_simulatorActiveDrawable) != null) {
            activeSimulatorDrawable =
                    typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_simulatorActiveDrawable);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_simulatorInactiveDrawable) != null) {
            inActiveSimulatorDrawable =
                    typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_simulatorInactiveDrawable);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_buttonBackground) != null) {
            setButtonBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_buttonBackground));
        }
        if (typedArray.getColorStateList(R.styleable.SimulatorControlWidget_uxsdk_buttonTextColor) != null) {
            setButtonTextColor(typedArray.getColorStateList(R.styleable.SimulatorControlWidget_uxsdk_buttonTextColor));
        }
        float buttonTextSize =
                typedArray.getDimension(R.styleable.SimulatorControlWidget_uxsdk_buttonTextSize, INVALID_RESOURCE);
        if (buttonTextSize != INVALID_RESOURCE) {
            setButtonTextSize(DisplayUtil.pxToSp(context, buttonTextSize));
        }
        int textAppearance =
                typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_buttonTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_labelsBackground) != null) {
            setLabelBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_labelsBackground));
        }

        setLabelTextColor(typedArray.getColor(R.styleable.SimulatorControlWidget_uxsdk_labelsTextColor,
                getResources().getColor(R.color.uxsdk_white_85_percent)));

        setLabelTextSize(typedArray.getInt(R.styleable.SimulatorControlWidget_uxsdk_labelsTextSize, 12));
        textAppearance =
                typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_labelsTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setLabelTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_valueBackground) != null) {
            setValueBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_valueBackground));
        }

        setValueTextColor(typedArray.getColor(R.styleable.SimulatorControlWidget_uxsdk_valueTextColor,
                getResources().getColor(R.color.uxsdk_blue)));

        float valueTextSize =
                typedArray.getDimension(R.styleable.SimulatorControlWidget_uxsdk_valueTextSize, INVALID_RESOURCE);
        if (valueTextSize != INVALID_RESOURCE) {
            setValueTextSize(DisplayUtil.pxToSp(context, valueTextSize));
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_valueTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setValueTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_inputBackground) != null) {
            setInputBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_inputBackground));
        }

        setInputTextColor(typedArray.getColor(R.styleable.SimulatorControlWidget_uxsdk_inputTextColor,
                getResources().getColor(R.color.uxsdk_black)));
        setInputTextSize(typedArray.getInt(R.styleable.SimulatorControlWidget_uxsdk_inputTextSize, 12));
        textAppearance =
                typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_inputTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setInputTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_headerBackground) != null) {
            setHeaderBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_headerBackground));
        }

        setHeaderTextColor(typedArray.getColor(R.styleable.SimulatorControlWidget_uxsdk_headerTextColor,
                getResources().getColor(R.color.uxsdk_white)));
        setHeaderTextSize(typedArray.getInt(R.styleable.SimulatorControlWidget_uxsdk_headerTextSize, 12));
        textAppearance =
                typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_headerTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setHeaderTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleBackground) != null) {
            setTitleBackground(typedArray.getDrawable(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleBackground));
        }

        setTitleTextColor(typedArray.getColor(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextColor,
                getResources().getColor(R.color.uxsdk_white)));
        setTitleTextSize(typedArray.getInt(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextSize, 12));
        textAppearance = typedArray.getResourceId(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextAppearance,
                INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(textAppearance);
        }

        typedArray.recycle();
    }

    private void setWindSpeedUI(int windDirection, boolean isPositive) {
        int change = isPositive ? 1 : -1;
        TextView textView = windXTextView;
        switch (windDirection) {
            case WIND_DIRECTION_X:
                textView = windXTextView;
                break;
            case WIND_DIRECTION_Y:
                textView = windYTextView;
                break;
            case WIND_DIRECTION_Z:
                textView = windZTextView;
                break;
        }
        int val = Integer.parseInt(textView.getText().toString());
        val = val + change;
        if (val > SIMULATION_MAX_WIND_SPEED) {
            val = SIMULATION_MAX_WIND_SPEED;
        } else if (val < SIMULATION_MIN_WIND_SPEED) {
            val = SIMULATION_MIN_WIND_SPEED;
        }
        textView.setText(String.valueOf(val));
        addDisposable(widgetModel.setSimulatorWindData(new SimulatorWindData.Builder().windSpeedX(Integer.parseInt(
                windXTextView.getText().toString()))
                .windSpeedY(Integer.parseInt(
                        windYTextView.getText()
                                .toString()))
                .windSpeedZ(Integer.parseInt(
                        windZTextView.getText()
                                .toString()))
                .build())
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(() -> {
                }, error -> {
                    if (error instanceof UXSDKError) {
                        DJILog.e(TAG, error.toString());
                        checkAndUpdateWind();
                    }
                }));
    }

    private void checkAndUpdateWind() {
        addDisposable(widgetModel.getSimulatorWindData()
                .lastOrError()
                .observeOn(schedulerProvider.ui())
                .subscribe(this::updateWindValues));
    }

    private void updateSatelliteCount(Integer integer) {
        satelliteTextView.setText(String.valueOf(integer));
    }

    private void updateUI(boolean isActive) {
        shouldReactToCheckChange = false;
        if (isActive) {
            updateWidgetToStartedState();
        } else {
            updateWidgetToStoppedState();
        }
        shouldReactToCheckChange = true;
    }

    private void initViewElements() {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');
        df = new DecimalFormat("#.######", otherSymbols);
        simulatorTitleTextView = findViewById(R.id.textview_simulator_title);
        simulatorSwitch = findViewById(R.id.switch_simulator);
        frequencyEditText = findViewById(R.id.edit_text_frequency);
        frequencyTextView = findViewById(R.id.textview_simulator_frequency_value);
        latitudeEditText = findViewById(R.id.edit_text_simulator_lat);
        longitudeEditText = findViewById(R.id.edit_text_simulator_lng);
        latitudeTextView = findViewById(R.id.textview_simulator_latitude_value);
        longitudeTextView = findViewById(R.id.textview_simulator_longitude_value);
        satelliteCountEditText = findViewById(R.id.edit_text_satellite_count);
        satelliteTextView = findViewById(R.id.textview_simulator_satellite_value);
        worldXTextView = findViewById(R.id.textview_simulator_world_x_value);
        worldYTextView = findViewById(R.id.textview_simulator_world_y_value);
        worldZTextView = findViewById(R.id.textview_simulator_world_z_value);
        motorsStartedTextView = findViewById(R.id.textview_simulator_motors_value);
        aircraftFlyingTextView = findViewById(R.id.textview_simulator_aircraft_flying_value);
        pitchTextView = findViewById(R.id.textview_simulator_aircraft_pitch_value);
        yawTextView = findViewById(R.id.textview_simulator_aircraft_yaw_value);
        rollTextView = findViewById(R.id.textview_simulator_aircraft_roll_value);
        frequencyEditText.setFilters(new InputFilter[]{new EditTextNumberInputFilter("2", "150")});
        latitudeEditText.setFilters(new InputFilter[]{new EditTextNumberInputFilter("-90", "90")});
        longitudeEditText.setFilters(new InputFilter[]{new EditTextNumberInputFilter("-180", "180")});
        satelliteCountEditText.setFilters(new InputFilter[]{new EditTextNumberInputFilter("1", "20")});
        loadPresetTextView = findViewById(R.id.textview_load_preset);
        savePresetTextView = findViewById(R.id.textview_save_preset);
        loadPresetTextView.setOnClickListener(this);
        savePresetTextView.setOnClickListener(this);
        windXTextView = findViewById(R.id.textview_wind_x);
        windYTextView = findViewById(R.id.textview_wind_y);
        windZTextView = findViewById(R.id.textview_wind_z);
        latitudeLabelTextView = findViewById(R.id.textview_simulator_latitude_label);
        longitudeLabelTextView = findViewById(R.id.textview_simulator_longitude_label);
        satelliteLabelTextView = findViewById(R.id.textview_simulator_satellite_label);
        worldXLabelTextView = findViewById(R.id.textview_simulator_world_x_label);
        worldYLabelTextView = findViewById(R.id.textview_simulator_world_y_label);
        worldZLabelTextView = findViewById(R.id.textview_simulator_world_z_label);
        motorsStartedLabelTextView = findViewById(R.id.textview_simulator_motors_label);
        aircraftFlyingLabelTextView = findViewById(R.id.textview_simulator_aircraft_flying_label);
        pitchLabelTextView = findViewById(R.id.textview_simulator_pitch_label);
        yawLabelTextView = findViewById(R.id.textview_simulator_yaw_label);
        rollLabelTextView = findViewById(R.id.textview_simulator_roll_label);
        frequencyLabelTextView = findViewById(R.id.textview_simulator_frequency_label);
        windXLabelTextView = findViewById(R.id.textview_wind_speed_x_label);
        windYLabelTextView = findViewById(R.id.textview_wind_speed_y_label);
        windZLabelTextView = findViewById(R.id.textview_wind_speed_z_label);

        positionSectionHeaderTextView = findViewById(R.id.textview_location_section_header);
        aircraftSectionHeaderTextView = findViewById(R.id.textview_status_section_header);
        attitudeSectionHeaderTextView = findViewById(R.id.textview_attitude_section_header);
        windSectionHeaderTextView = findViewById(R.id.textview_wind_section_header);

        findViewById(R.id.imageview_btn_plus_x).setOnClickListener(this);
        findViewById(R.id.imageview_btn_plus_y).setOnClickListener(this);
        findViewById(R.id.imageview_btn_plus_z).setOnClickListener(this);
        findViewById(R.id.imageview_btn_minus_x).setOnClickListener(this);
        findViewById(R.id.imageview_btn_minus_y).setOnClickListener(this);
        findViewById(R.id.imageview_btn_minus_z).setOnClickListener(this);
        attitudeGroup = findViewById(R.id.constraint_group_attitude);
        aircraftStatusGroup = findViewById(R.id.constraint_group_aircraft_state);
        windSimulationGroup = findViewById(R.id.constraint_group_wind);
        realWorldPositionGroup = findViewById(R.id.constraint_group_real_world);
        buttonGroup = findViewById(R.id.constraint_group_buttons);
        simulatorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleSwitchChange(isChecked));
        inActiveSimulatorDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_simulator);
        activeSimulatorDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_simulator_active);
    }

    private void handleSwitchChange(boolean isChecked) {
        if (shouldReactToCheckChange) {
            if (isChecked) {
                startSimulator();
            } else {
                stopSimulator();
            }
        }
    }

    private void stopSimulator() {
        addDisposable(widgetModel.stopSimulator().subscribe(() -> {
        }, error -> {
            if (error instanceof UXSDKError) {
                DJILog.e(TAG, error.toString());
            }
        }));
    }

    private void startSimulator() {
        LocationCoordinate2D locationCoordinate2D = getSimulatedLocation();
        if (locationCoordinate2D != null) {
            setSimulatorStatus(true);
            int updateFrequency = DEFAULT_FREQUENCY;
            if (null != frequencyEditText.getText().toString() && !frequencyEditText.getText().toString().isEmpty()) {
                updateFrequency = Integer.parseInt(frequencyEditText.getText().toString());
            }
            int satelliteCount = DEFAULT_SATELLITE_COUNT;
            if (null != satelliteCountEditText.getText().toString() && !satelliteCountEditText.getText()
                    .toString()
                    .isEmpty()) {
                satelliteCount = Integer.parseInt(satelliteCountEditText.getText().toString());
            }
            InitializationData initializationData =
                    InitializationData.createInstance(locationCoordinate2D, updateFrequency, satelliteCount);
            addDisposable(widgetModel.startSimulator(initializationData)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(() -> {
                    }, error -> {
                        if (error instanceof UXSDKError) {
                            DJILog.e(TAG, error.toString());
                            setSimulatorStatus(false);
                        }
                    }));
        } else {
            setSimulatorStatus(false);
            Toast.makeText(getContext(),
                    getResources().getString(R.string.uxsdk_simulator_input_val_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setSimulatorStatus(boolean simulatorActive) {
        shouldReactToCheckChange = false;
        if (simulatorActive) {
            updateWidgetToStartedState();
        } else {
            updateWidgetToStoppedState();
        }
        shouldReactToCheckChange = true;
    }

    private void updateWidgetToStartedState() {
        simulatorSwitch.setChecked(true);
        latitudeTextView.setText(latitudeEditText.getText());
        longitudeTextView.setText(longitudeEditText.getText());
        frequencyTextView.setText(frequencyEditText.getText().toString());
        latitudeEditText.setVisibility(View.INVISIBLE);
        longitudeEditText.setVisibility(View.INVISIBLE);
        satelliteCountEditText.setVisibility(View.INVISIBLE);
        frequencyEditText.setVisibility(INVISIBLE);
        frequencyTextView.setVisibility(VISIBLE);
        latitudeTextView.setVisibility(View.VISIBLE);
        longitudeTextView.setVisibility(View.VISIBLE);
        satelliteTextView.setVisibility(View.VISIBLE);
        simulatorTitleTextView.setCompoundDrawablesWithIntrinsicBounds(activeSimulatorDrawable, null, null, null);
        realWorldPositionGroup.setVisibility(worldPositionSectionVisible);
        windSimulationGroup.setVisibility(windSectionVisible);
        attitudeGroup.setVisibility(attitudeSectionVisible);
        aircraftStatusGroup.setVisibility(aircraftStatusSectionVisible);
        buttonGroup.setVisibility(GONE);
    }

    private void updateWidgetToStoppedState() {
        simulatorSwitch.setChecked(false);
        latitudeEditText.setVisibility(View.VISIBLE);
        longitudeEditText.setVisibility(View.VISIBLE);
        satelliteCountEditText.setVisibility(View.VISIBLE);
        frequencyEditText.setVisibility(View.VISIBLE);
        frequencyTextView.setVisibility(INVISIBLE);
        latitudeTextView.setVisibility(View.INVISIBLE);
        longitudeTextView.setVisibility(View.INVISIBLE);
        satelliteTextView.setVisibility(View.INVISIBLE);
        worldXTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        worldYTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        worldZTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        pitchTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        yawTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        rollTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        motorsStartedTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        aircraftFlyingTextView.setText(getResources().getString(R.string.uxsdk_simulator_null_string));
        windXTextView.setText(getResources().getString(R.string.uxsdk_simulator_zero_string));
        windYTextView.setText(getResources().getString(R.string.uxsdk_simulator_zero_string));
        windZTextView.setText(getResources().getString(R.string.uxsdk_simulator_zero_string));
        simulatorTitleTextView.setCompoundDrawablesWithIntrinsicBounds(inActiveSimulatorDrawable, null, null, null);
        realWorldPositionGroup.setVisibility(GONE);
        windSimulationGroup.setVisibility(GONE);
        attitudeGroup.setVisibility(GONE);
        aircraftStatusGroup.setVisibility(GONE);
        buttonGroup.setVisibility(VISIBLE);
    }

    private void updateWidgetValues(SimulatorState simulatorState) {
        if (simulatorState != null) {
            latitudeTextView.setText(df.format(simulatorState.getLocation().getLatitude()));
            longitudeTextView.setText(df.format(simulatorState.getLocation().getLongitude()));
            worldXTextView.setText(df.format(simulatorState.getPositionX()));
            worldYTextView.setText(df.format(simulatorState.getPositionY()));
            worldZTextView.setText(df.format(simulatorState.getPositionZ()));
            pitchTextView.setText(df.format(simulatorState.getPitch()));
            yawTextView.setText(df.format(simulatorState.getYaw()));
            rollTextView.setText(df.format(simulatorState.getRoll()));
            motorsStartedTextView.setText(simulatorState.areMotorsOn()
                    ? R.string.uxsdk_app_yes
                    : R.string.uxsdk_app_no);
            aircraftFlyingTextView.setText(simulatorState.isFlying()
                    ? R.string.uxsdk_app_yes
                    : R.string.uxsdk_app_no);
        }
    }

    private void updateWindValues(SimulatorWindData simulatorWindData) {
        windXTextView.setText(String.valueOf(simulatorWindData.getWindSpeedX()));
        windYTextView.setText(String.valueOf(simulatorWindData.getWindSpeedY()));
        windZTextView.setText(String.valueOf(simulatorWindData.getWindSpeedZ()));
    }

    private LocationCoordinate2D getSimulatedLocation() {
        LocationCoordinate2D locationCoordinate2D = null;
        if (!latitudeEditText.getText().toString().isEmpty() && !longitudeEditText.getText().toString().isEmpty()) {

            double latCoordinates = Double.parseDouble(latitudeEditText.getText().toString());
            double lngCoordinates = Double.parseDouble(longitudeEditText.getText().toString());
            if (!Double.isNaN(latCoordinates) && !Double.isNaN(lngCoordinates)) {
                locationCoordinate2D = new LocationCoordinate2D(latCoordinates, lngCoordinates);
            }
        }
        return locationCoordinate2D;
    }

    private void showSavePresetDialog() {
        if (TextUtils.isEmpty(latitudeEditText.getText().toString())
                || TextUtils.isEmpty(longitudeEditText.getText().toString())
                || TextUtils.isEmpty(frequencyEditText.getText().toString())
                || TextUtils.isEmpty(satelliteCountEditText.getText().toString())) return;

        SimulatorPresetData presetData =
                new SimulatorPresetData(Double.parseDouble(latitudeEditText.getText().toString()),
                        Double.parseDouble(longitudeEditText.getText().toString()),
                        Integer.parseInt(satelliteCountEditText.getText().toString()),
                        Integer.parseInt(frequencyEditText.getText().toString()));
        new SavePresetDialog(getContext(), true, presetData).show();
    }

    private void showPresetListDialog() {
        new PresetListDialog(getContext(), this).show();
    }

    //endregion

    //region customizations

    /**
     * Show or hide the wind simulation section when the simulator is active
     *
     * @param windSectionVisibility true - visible when active false - gone when active
     */
    public void setWindSectionVisibility(boolean windSectionVisibility) {
        windSectionVisible = windSectionVisibility ? VISIBLE : GONE;
    }

    /**
     * Is wind section configured to be visible
     *
     * @return true if visible
     */
    public boolean isWindSectionVisible() {
        return windSectionVisible == VISIBLE;
    }

    /**
     * Show or hide the world position section when the simulator is active
     *
     * @param worldPositionSectionVisibility true - visible when active false - gone when active
     */
    public void setWorldPositionSectionVisibility(boolean worldPositionSectionVisibility) {
        worldPositionSectionVisible = worldPositionSectionVisibility ? VISIBLE : GONE;
    }

    /**
     * Is world position section configured to be visible
     *
     * @return true if visible
     */
    public boolean isWorldPositionSectionVisible() {
        return worldPositionSectionVisible == VISIBLE;
    }

    /**
     * Show or hide the aircraft status section when the simulator is Active
     *
     * @param aircraftStatusSectionVisibility true - visible when active false - gone when active
     */
    public void setAircraftSectionVisibility(boolean aircraftStatusSectionVisibility) {
        aircraftStatusSectionVisible = aircraftStatusSectionVisibility ? VISIBLE : GONE;
    }

    /**
     * Is aircraft status section configured to be visible
     *
     * @return true if visible
     */
    public boolean isAircraftSectionVisible() {
        return aircraftStatusSectionVisible == VISIBLE;
    }

    /**
     * Show or hide the attitude simulation section when the simulator is active
     *
     * @param attitudeSectionVisibility true - visible when active false - gone when active
     */
    public void setAttitudeSectionVisibility(boolean attitudeSectionVisibility) {
        attitudeSectionVisible = attitudeSectionVisibility ? VISIBLE : GONE;
    }

    /**
     * Is aircraft attitude section configured to be visible
     *
     * @return true if visible
     */
    public boolean isAttitudeSectionVisible() {
        return attitudeSectionVisible == VISIBLE;
    }

    /**
     * Set background to title text
     *
     * @param resourceId of resource to be used as background of title
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     *
     * @param drawable representing the background of title
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        simulatorTitleTextView.setBackground(drawable);
    }

    /**
     * Get current background of title text
     *
     * @return Drawable representing the background of title
     */
    @Nullable
    public Drawable getTitleBackground() {
        return simulatorTitleTextView.getBackground();
    }

    /**
     * Set resource for simulator active icon
     *
     * @param simulatorActiveIcon resource id of active state icon
     */
    public void setSimulatorActiveIcon(@DrawableRes int simulatorActiveIcon) {
        setSimulatorActiveIcon(getResources().getDrawable(simulatorActiveIcon));
    }

    /**
     * Set simulator active state drawable
     *
     * @param drawable representing the active state of the simulator
     */
    public void setSimulatorActiveIcon(@NonNull Drawable drawable) {
        this.activeSimulatorDrawable = drawable;
    }

    /**
     * Get current drawable for simulator active state
     *
     * @return Drawable representing simulator active icon
     */
    @Nullable
    public Drawable getSimulatorActiveDrawable() {
        return activeSimulatorDrawable;
    }

    /**
     * Set resource for simulator inactive icon
     *
     * @param inactiveSimulatorIcon resource id of inactive state icon
     */
    public void setSimulatorInactiveIcon(@DrawableRes int inactiveSimulatorIcon) {
        setSimulatorActiveIcon(getResources().getDrawable(inactiveSimulatorIcon));
    }

    /**
     * Set simulator inactive state drawable
     *
     * @param drawable representing the inactive state of the simulator
     */
    public void setSimulatorInactiveIcon(@NonNull Drawable drawable) {
        this.inActiveSimulatorDrawable = drawable;
    }

    /**
     * Get current drawable for simulator inactive state
     *
     * @return Drawable representing simulator inactive icon
     */
    @Nullable
    public Drawable getSimulatorInactiveDrawable() {
        return inActiveSimulatorDrawable;
    }

    /**
     * Set widget title color state list
     *
     * @param colorStateList for title text colors
     */
    public void setTitleTextColor(@NonNull ColorStateList colorStateList) {
        simulatorTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set widget title text color
     *
     * @param color integer value representing text color of title
     */
    public void setTitleTextColor(@ColorInt int color) {
        simulatorTitleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of widget title
     *
     * @return ColorStateList for title text color
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return simulatorTitleTextView.getTextColors();
    }

    /**
     * Get the current text color of title
     *
     * @return integer value representing text color of title
     */
    @ColorInt
    public int getTitleTextColor() {
        return simulatorTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance resourceId for text appearance for title
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        simulatorTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text size of the title
     *
     * @param textSize float value that represents text size
     */
    public void setTitleTextSize(@Dimension float textSize) {
        simulatorTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of title text
     *
     * @return float value representing title  field text size
     */
    @Dimension
    public float getTitleTextSize() {
        return simulatorTitleTextView.getTextSize();
    }

    /**
     * Set text color state list to the value fields
     *
     * @param colorStateList for value text colors
     */
    public void setValueTextColor(@NonNull ColorStateList colorStateList) {
        latitudeTextView.setTextColor(colorStateList);
        longitudeTextView.setTextColor(colorStateList);
        satelliteTextView.setTextColor(colorStateList);
        worldXTextView.setTextColor(colorStateList);
        worldYTextView.setTextColor(colorStateList);
        worldZTextView.setTextColor(colorStateList);
        motorsStartedTextView.setTextColor(colorStateList);
        aircraftFlyingTextView.setTextColor(colorStateList);
        pitchTextView.setTextColor(colorStateList);
        yawTextView.setTextColor(colorStateList);
        rollTextView.setTextColor(colorStateList);
        frequencyTextView.setTextColor(colorStateList);
        windXTextView.setTextColor(colorStateList);
        windYTextView.setTextColor(colorStateList);
        windZTextView.setTextColor(colorStateList);
    }

    /**
     * Set text colors of value fields
     *
     * @param color integer value that represents text color of value fields
     */
    public void setValueTextColor(@ColorInt int color) {
        latitudeTextView.setTextColor(color);
        longitudeTextView.setTextColor(color);
        satelliteTextView.setTextColor(color);
        worldXTextView.setTextColor(color);
        worldYTextView.setTextColor(color);
        worldZTextView.setTextColor(color);
        motorsStartedTextView.setTextColor(color);
        aircraftFlyingTextView.setTextColor(color);
        pitchTextView.setTextColor(color);
        yawTextView.setTextColor(color);
        rollTextView.setTextColor(color);
        frequencyTextView.setTextColor(color);
        windXTextView.setTextColor(color);
        windYTextView.setTextColor(color);
        windZTextView.setTextColor(color);
    }

    /**
     * Get current color state list of the value fields
     *
     * @return ColorStateList for value fields text color
     */
    @Nullable
    public ColorStateList getValueTextColors() {
        return latitudeTextView.getTextColors();
    }

    /**
     * Get current text color of the value fields
     *
     * @return integer value representing text color of the value fields
     */
    @ColorInt
    public int getValueTextColor() {
        return latitudeTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the value fields
     *
     * @param textAppearance resourceId for text appearance of value fields
     */
    public void setValueTextAppearance(@StyleRes int textAppearance) {
        latitudeTextView.setTextAppearance(getContext(), textAppearance);
        longitudeTextView.setTextAppearance(getContext(), textAppearance);
        satelliteTextView.setTextAppearance(getContext(), textAppearance);
        worldXTextView.setTextAppearance(getContext(), textAppearance);
        worldYTextView.setTextAppearance(getContext(), textAppearance);
        worldZTextView.setTextAppearance(getContext(), textAppearance);
        motorsStartedTextView.setTextAppearance(getContext(), textAppearance);
        aircraftFlyingTextView.setTextAppearance(getContext(), textAppearance);
        pitchTextView.setTextAppearance(getContext(), textAppearance);
        yawTextView.setTextAppearance(getContext(), textAppearance);
        rollTextView.setTextAppearance(getContext(), textAppearance);
        frequencyTextView.setTextAppearance(getContext(), textAppearance);
        windXTextView.setTextAppearance(getContext(), textAppearance);
        windYTextView.setTextAppearance(getContext(), textAppearance);
        windZTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text size of the value fields
     *
     * @param textSize float value that represents text size
     */
    public void setValueTextSize(@Dimension float textSize) {
        latitudeTextView.setTextSize(textSize);
        longitudeTextView.setTextSize(textSize);
        satelliteTextView.setTextSize(textSize);
        worldXTextView.setTextSize(textSize);
        worldYTextView.setTextSize(textSize);
        worldZTextView.setTextSize(textSize);
        motorsStartedTextView.setTextSize(textSize);
        aircraftFlyingTextView.setTextSize(textSize);
        pitchTextView.setTextSize(textSize);
        yawTextView.setTextSize(textSize);
        rollTextView.setTextSize(textSize);
        frequencyTextView.setTextSize(textSize);
        windXTextView.setTextSize(textSize);
        windYTextView.setTextSize(textSize);
        windZTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of value fields
     *
     * @return float value representing value field text size
     */
    @Dimension
    public float getValueTextSize() {
        return windZTextView.getTextSize();
    }

    /**
     * Set background resource to value text
     *
     * @param resourceId to use as background for value fields
     */
    public void setValueBackground(@DrawableRes int resourceId) {
        setValueBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to value text
     *
     * @param drawable to use as background for value fields
     */
    public void setValueBackground(@Nullable Drawable drawable) {
        latitudeTextView.setBackground(drawable);
        longitudeTextView.setBackground(drawable);
        satelliteTextView.setBackground(drawable);
        worldXTextView.setBackground(drawable);
        worldYTextView.setBackground(drawable);
        worldZTextView.setBackground(drawable);
        motorsStartedTextView.setBackground(drawable);
        aircraftFlyingTextView.setBackground(drawable);
        pitchTextView.setBackground(drawable);
        yawTextView.setBackground(drawable);
        rollTextView.setBackground(drawable);
        frequencyTextView.setBackground(drawable);
        windXTextView.setBackground(drawable);
        windYTextView.setBackground(drawable);
        windZTextView.setBackground(drawable);
    }

    /**
     * Get current background of value text
     *
     * @return Drawable background of the value fields
     */
    @Nullable
    public Drawable getValueBackground() {
        return latitudeTextView.getBackground();
    }

    /**
     * Set text color state list to all the labels
     *
     * @param colorStateList for labels text colors
     */
    public void setLabelTextColor(@NonNull ColorStateList colorStateList) {
        latitudeLabelTextView.setTextColor(colorStateList);
        longitudeLabelTextView.setTextColor(colorStateList);
        satelliteLabelTextView.setTextColor(colorStateList);
        worldXLabelTextView.setTextColor(colorStateList);
        worldYLabelTextView.setTextColor(colorStateList);
        worldZLabelTextView.setTextColor(colorStateList);
        motorsStartedLabelTextView.setTextColor(colorStateList);
        aircraftFlyingLabelTextView.setTextColor(colorStateList);
        pitchLabelTextView.setTextColor(colorStateList);
        yawLabelTextView.setTextColor(colorStateList);
        rollLabelTextView.setTextColor(colorStateList);
        frequencyLabelTextView.setTextColor(colorStateList);
        windXLabelTextView.setTextColor(colorStateList);
        windYLabelTextView.setTextColor(colorStateList);
        windZLabelTextView.setTextColor(colorStateList);
    }

    /**
     * Set text color to all labels
     *
     * @param color integer value representing color of text of labels
     */
    public void setLabelTextColor(@ColorInt int color) {
        latitudeLabelTextView.setTextColor(color);
        longitudeLabelTextView.setTextColor(color);
        satelliteLabelTextView.setTextColor(color);
        worldXLabelTextView.setTextColor(color);
        worldYLabelTextView.setTextColor(color);
        worldZLabelTextView.setTextColor(color);
        motorsStartedLabelTextView.setTextColor(color);
        aircraftFlyingLabelTextView.setTextColor(color);
        pitchLabelTextView.setTextColor(color);
        yawLabelTextView.setTextColor(color);
        rollLabelTextView.setTextColor(color);
        frequencyLabelTextView.setTextColor(color);
        windXLabelTextView.setTextColor(color);
        windYLabelTextView.setTextColor(color);
        windZLabelTextView.setTextColor(color);
    }

    /**
     * Set text size of the all the labels
     *
     * @param textSize float value that represents text size
     */
    public void setLabelTextSize(@Dimension float textSize) {
        latitudeLabelTextView.setTextSize(textSize);
        longitudeLabelTextView.setTextSize(textSize);
        satelliteLabelTextView.setTextSize(textSize);
        worldXLabelTextView.setTextSize(textSize);
        worldYLabelTextView.setTextSize(textSize);
        worldZLabelTextView.setTextSize(textSize);
        motorsStartedLabelTextView.setTextSize(textSize);
        aircraftFlyingLabelTextView.setTextSize(textSize);
        pitchLabelTextView.setTextSize(textSize);
        yawLabelTextView.setTextSize(textSize);
        rollLabelTextView.setTextSize(textSize);
        frequencyLabelTextView.setTextSize(textSize);
        windXLabelTextView.setTextSize(textSize);
        windYLabelTextView.setTextSize(textSize);
        windZLabelTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of labels
     *
     * @return float value representing labels text size
     */
    @Dimension
    public float getLabelTextSize() {
        return latitudeLabelTextView.getTextSize();
    }

    /**
     * Get current text color state list of widget labels
     *
     * @return ColorStateList for labels text color
     */
    @Nullable
    public ColorStateList getLabelTextColors() {
        return latitudeLabelTextView.getTextColors();
    }

    /**
     * Get label text color
     *
     * @return int value representing text color of labels
     */
    @ColorInt
    public int getLabelTextColor() {
        return latitudeLabelTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget labels
     *
     * @param textAppearance resource id of text appearance for label text
     */
    public void setLabelTextAppearance(@StyleRes int textAppearance) {
        latitudeLabelTextView.setTextAppearance(getContext(), textAppearance);
        longitudeLabelTextView.setTextAppearance(getContext(), textAppearance);
        satelliteLabelTextView.setTextAppearance(getContext(), textAppearance);
        worldXLabelTextView.setTextAppearance(getContext(), textAppearance);
        worldYLabelTextView.setTextAppearance(getContext(), textAppearance);
        worldZLabelTextView.setTextAppearance(getContext(), textAppearance);
        motorsStartedLabelTextView.setTextAppearance(getContext(), textAppearance);
        aircraftFlyingLabelTextView.setTextAppearance(getContext(), textAppearance);
        pitchLabelTextView.setTextAppearance(getContext(), textAppearance);
        yawLabelTextView.setTextAppearance(getContext(), textAppearance);
        rollLabelTextView.setTextAppearance(getContext(), textAppearance);
        frequencyLabelTextView.setTextAppearance(getContext(), textAppearance);
        windXLabelTextView.setTextAppearance(getContext(), textAppearance);
        windYLabelTextView.setTextAppearance(getContext(), textAppearance);
        windZLabelTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background to label text
     *
     * @param resourceId to use as background for all labels
     */
    public void setLabelBackground(@DrawableRes int resourceId) {
        setLabelBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to label fields text
     *
     * @param drawable to use as background for all labels
     */
    public void setLabelBackground(@Nullable Drawable drawable) {
        latitudeLabelTextView.setBackground(drawable);
        longitudeLabelTextView.setBackground(drawable);
        satelliteLabelTextView.setBackground(drawable);
        worldXLabelTextView.setBackground(drawable);
        worldYLabelTextView.setBackground(drawable);
        worldZLabelTextView.setBackground(drawable);
        motorsStartedLabelTextView.setBackground(drawable);
        aircraftFlyingLabelTextView.setBackground(drawable);
        pitchLabelTextView.setBackground(drawable);
        yawLabelTextView.setBackground(drawable);
        rollLabelTextView.setBackground(drawable);
        frequencyLabelTextView.setBackground(drawable);
        windXLabelTextView.setBackground(drawable);
        windYLabelTextView.setBackground(drawable);
        windZLabelTextView.setBackground(drawable);
    }

    /**
     * Get current background of label text
     *
     * @return Drawable used as label's background
     */
    @Nullable
    public Drawable getLabelBackground() {
        return latitudeLabelTextView.getBackground();
    }

    /**
     * Set text color state list to the widget inputs
     *
     * @param colorStateList ColorStateList to use as input text color
     */
    public void setInputTextColor(@NonNull ColorStateList colorStateList) {
        latitudeEditText.setTextColor(colorStateList);
        longitudeEditText.setTextColor(colorStateList);
        satelliteCountEditText.setTextColor(colorStateList);
        frequencyEditText.setTextColor(colorStateList);
    }

    /**
     * Set input text color of input fields
     *
     * @param color integer value of input text fields
     */
    public void setInputTextColor(@ColorInt int color) {
        latitudeEditText.setTextColor(color);
        longitudeEditText.setTextColor(color);
        satelliteCountEditText.setTextColor(color);
        frequencyEditText.setTextColor(color);
    }

    /**
     * Set text size of the widget input text fields
     *
     * @param textSize float value representing text size of input fields
     */
    public void setInputTextSize(@Dimension float textSize) {
        latitudeEditText.setTextSize(textSize);
        longitudeEditText.setTextSize(textSize);
        satelliteCountEditText.setTextSize(textSize);
        frequencyEditText.setTextSize(textSize);
    }

    /**
     * Get current text size of input text fields
     *
     * @return float value representing input fields text size
     */
    @Dimension
    public float getInputTextSize() {
        return frequencyEditText.getTextSize();
    }

    /**
     * Get current text color state list of input text fields
     *
     * @return ColorStateList for input text color
     */
    @Nullable
    public ColorStateList getInputTextColors() {
        return latitudeEditText.getTextColors();
    }

    /**
     * Get text color of input text fields
     *
     * @return integer value representing the color of text
     */
    @ColorInt
    public int getInputTextColor() {
        return latitudeEditText.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget input text fields
     *
     * @param textAppearance resourceId for text appearance of input text fields
     */
    public void setInputTextAppearance(@StyleRes int textAppearance) {
        latitudeEditText.setTextAppearance(getContext(), textAppearance);
        longitudeEditText.setTextAppearance(getContext(), textAppearance);
        satelliteCountEditText.setTextAppearance(getContext(), textAppearance);
        frequencyEditText.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background to input text fields
     *
     * @param resourceId to use as background to input fields
     */
    public void setInputBackground(@DrawableRes int resourceId) {
        setInputBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to input text fields
     *
     * @param drawable to use as background to input text fields
     */
    public void setInputBackground(@Nullable Drawable drawable) {
        latitudeEditText.setBackground(drawable);
        longitudeEditText.setBackground(drawable);
        satelliteCountEditText.setBackground(drawable);
        frequencyEditText.setBackground(drawable);
    }

    /**
     * Get current background of input text fields
     *
     * @return Drawable representing background of input text fields
     */
    @Nullable
    public Drawable getInputBackground() {
        return latitudeEditText.getBackground();
    }

    /**
     * Set text color state list to the widget button
     *
     * @param colorStateList for button text colors
     */
    public void setButtonTextColor(@NonNull ColorStateList colorStateList) {
        savePresetTextView.setTextColor(colorStateList);
        loadPresetTextView.setTextColor(colorStateList);
    }

    /**
     * Set text color of buttons
     *
     * @param color integer value representing color of text of buttons
     */
    public void setButtonTextColor(@ColorInt int color) {
        savePresetTextView.setTextColor(color);
        loadPresetTextView.setTextColor(color);
    }

    /**
     * Set text size of the widget button
     *
     * @param textSize float value that represents text size
     */
    public void setButtonTextSize(@Dimension float textSize) {
        savePresetTextView.setTextSize(textSize);
        loadPresetTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of buttons
     *
     * @return float value representing button text size
     */
    @Dimension
    public float getButtonTextSize() {
        return savePresetTextView.getTextSize();
    }

    /**
     * Get current text color state list of widget button
     *
     * @return ColorStateList for buttons text color
     */
    @Nullable
    public ColorStateList getButtonTextColors() {
        return savePresetTextView.getTextColors();
    }

    /**
     * Get current text color of buttons
     *
     * @return int value representing text color of buttons
     */
    @ColorInt
    public int getButtonTextColor() {
        return savePresetTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget button
     *
     * @param textAppearance resourceId for text appearance for buttons
     */
    public void setButtonTextAppearance(@StyleRes int textAppearance) {
        savePresetTextView.setTextAppearance(getContext(), textAppearance);
        loadPresetTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background resource to button
     *
     * @param resourceId to use as background for buttons
     */
    public void setButtonBackground(@DrawableRes int resourceId) {
        setButtonBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to buttons
     *
     * @param drawable to use as background for buttons
     */
    public void setButtonBackground(@Nullable Drawable drawable) {
        savePresetTextView.setBackground(drawable);
        loadPresetTextView.setBackground(drawable);
    }

    /**
     * Get current background of buttons
     *
     * @return Drawable background of the buttons
     */
    @Nullable
    public Drawable getButtonBackground() {
        return savePresetTextView.getBackground();
    }

    /**
     * Set text color state list to the widget header
     *
     * @param colorStateList ColorStateList to use as header text color
     */
    public void setHeaderTextColor(@NonNull ColorStateList colorStateList) {
        positionSectionHeaderTextView.setTextColor(colorStateList);
        aircraftSectionHeaderTextView.setTextColor(colorStateList);
        attitudeSectionHeaderTextView.setTextColor(colorStateList);
        windSectionHeaderTextView.setTextColor(colorStateList);
    }

    /**
     * Set header text color of header fields
     *
     * @param color integer value of header text fields
     */
    public void setHeaderTextColor(@ColorInt int color) {
        positionSectionHeaderTextView.setTextColor(color);
        aircraftSectionHeaderTextView.setTextColor(color);
        attitudeSectionHeaderTextView.setTextColor(color);
        windSectionHeaderTextView.setTextColor(color);
    }

    /**
     * Set text size of the widget header text fields
     *
     * @param textSize float value representing text size of header fields
     */
    public void setHeaderTextSize(@Dimension float textSize) {
        positionSectionHeaderTextView.setTextSize(textSize);
        aircraftSectionHeaderTextView.setTextSize(textSize);
        attitudeSectionHeaderTextView.setTextSize(textSize);
        windSectionHeaderTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of header text fields
     *
     * @return float value representing header fields text size
     */
    @Dimension
    public float getHeaderTextSize() {
        return positionSectionHeaderTextView.getTextSize();
    }

    /**
     * Get current text color state list of header text fields
     *
     * @return ColorStateList for header text color
     */
    @Nullable
    public ColorStateList getHeaderextColors() {
        return positionSectionHeaderTextView.getTextColors();
    }

    /**
     * Get text color of header text fields
     *
     * @return integer value representing the color of text
     */
    @ColorInt
    public int getHeaderTextColor() {
        return positionSectionHeaderTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget header text fields
     *
     * @param textAppearance resourceId for text appearance of header text fields
     */
    public void setHeaderTextAppearance(@StyleRes int textAppearance) {
        positionSectionHeaderTextView.setTextAppearance(getContext(), textAppearance);
        aircraftSectionHeaderTextView.setTextAppearance(getContext(), textAppearance);
        attitudeSectionHeaderTextView.setTextAppearance(getContext(), textAppearance);
        windSectionHeaderTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background to header text fields
     *
     * @param resourceId to use as background to header fields
     */
    public void setHeaderBackground(@DrawableRes int resourceId) {
        setHeaderBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to header text fields
     *
     * @param drawable to use as background to header text fields
     */
    public void setHeaderBackground(@Nullable Drawable drawable) {
        positionSectionHeaderTextView.setBackground(drawable);
        aircraftSectionHeaderTextView.setBackground(drawable);
        attitudeSectionHeaderTextView.setBackground(drawable);
        windSectionHeaderTextView.setBackground(drawable);
    }

    /**
     * Get current background of header text fields
     *
     * @return Drawable representing background of header text fields
     */
    @Nullable
    public Drawable getHeaderBackground() {
        return positionSectionHeaderTextView.getBackground();
    }
    //endregion
}
