package dji.ux.beta.cameracore.widget.cameracontrols.palette;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.widget.seekbar.OnRangeChangedListener;
import dji.ux.beta.cameracore.widget.seekbar.RangeSeekBar;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.SettingDefinitions;
import io.reactivex.rxjava3.functions.Consumer;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class PaletteBackgroundWidget extends ConstraintLayoutWidget implements View.OnClickListener, ICameraIndex, OnRangeChangedListener {

    private RangeSeekBar rangeSeekBar;
    private ImageView lowTempReduceImage;
    private ImageView lowTempAddImage;
    private ImageView highTempReduceImage;
    private ImageView highTempAddImage;
    private ImageView paletteView;
    private ImageView tempView;
    private ImageView addMaxRangeBtn;
    private ImageView reduceMinRangeBtn;
    private Switch customWidgetSwitch;
    private PaletteBackgroundModel paletteBackgroundModel;
    private int minValue = -40;
    private int maxValue = 150;
    private int recordMinValue = -40;  //更改的最小值
    private int recordMaxValue = 150;  //更改的range最小值
    private int lowTemperature = -40;
    private int highTemperature = 150;

    private ConstraintLayout paletteBackView;
    private ConstraintLayout tempBackView;
    private boolean isSetting = false;
    private boolean isMinTempSetting = false;
    private boolean isMaxTempSetting = false;
    private HashMap<Object, Object> scrollviewImageHashMap;
    private HashMap<Object, Object> textViewHaspMap;
    private ImageView recordImageView;
    private TextView recordTextView;
    private int resourceId;
    private PaletteWidget paletteWidget;

    public PaletteBackgroundWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PaletteBackgroundWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PaletteBackgroundWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_custom_widget_palette_background, this);
        paletteBackView = findViewById(R.id.palette_background_view);
        View topView = findViewById(R.id.custom_palette_background_top);
        tempBackView = findViewById(R.id.temp_background_view);
        scrollviewImageHashMap = new HashMap<>();
        textViewHaspMap = new HashMap<>();
        initPaletteScrollview();
        paletteView = findViewById(R.id.custom_palette_background_image1);
        tempView = findViewById(R.id.custom_palette_background_image2);
        rangeSeekBar = findViewById(R.id.sb_range_3);
        lowTemperature = -40;
        highTemperature = 150;
        rangeSeekBar.setRange(lowTemperature, highTemperature);
        rangeSeekBar.setProgress(lowTemperature, highTemperature);
        rangeSeekBar.setIndicatorTextDecimalFormat("0");
        rangeSeekBar.setOnRangeChangedListener(this);

        lowTempAddImage = findViewById(R.id.low_temp_add_image);
        lowTempReduceImage = findViewById(R.id.low_temp_reduce_image);
        highTempAddImage = findViewById(R.id.high_temp_add_image);
        highTempReduceImage = findViewById(R.id.high_temp_reduce_image);
        addMaxRangeBtn = findViewById(R.id.add_max_range_btn);
        reduceMinRangeBtn = findViewById(R.id.reduce_min_range_btn);
        customWidgetSwitch = findViewById(R.id.custom_widget_switch);

        paletteView.setOnClickListener(this);
        tempView.setOnClickListener(this);

        lowTempAddImage.setOnClickListener(this);
        lowTempReduceImage.setOnClickListener(this);
        highTempAddImage.setOnClickListener(this);
        highTempReduceImage.setOnClickListener(this);

        addMaxRangeBtn.setOnClickListener(this);
        reduceMinRangeBtn.setOnClickListener(this);
        addMaxRangeBtn.setClickable(false);
        reduceMinRangeBtn.setClickable(false);
        customWidgetSwitch.setOnClickListener(this);
        topView.setOnClickListener(view -> {
            this.setVisibility(View.GONE);
            if (paletteWidget != null) {
                paletteWidget.updatePaletteViewUnselected();
            }

        });
        initAttributes(context, attrs);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray obtainStyledAttributes =
                context.obtainStyledAttributes(attrs, R.styleable.PaletteBackgroundWidget);
        resourceId = obtainStyledAttributes.getResourceId(R.styleable.PaletteWidget_uxsdk_associate, INVALID_RESOURCE);
        obtainStyledAttributes.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            paletteBackgroundModel.setup();
        }
        if (resourceId != INVALID_RESOURCE) {
            paletteWidget = getRootView().findViewById(resourceId);
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            paletteBackgroundModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(paletteBackgroundModel.getPaletteProcessor().observeOn(SchedulerProvider.ui()).subscribe(thermalPalette -> {
            if (thermalPalette.value() == SettingsDefinitions.ThermalPalette.UNKNOWN.value())
                return;
            showSelectThermalPalette(thermalPalette);
        }));

        addReaction(paletteBackgroundModel.getThermalIsoThermLowerValueProcessor().observeOn(SchedulerProvider.ui()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Throwable {
                if (integer == paletteBackgroundModel.defaultValue) return;
                rangeSeekBar.setProgress(integer, maxValue);
                minValue = integer;
            }
        }));

        addReaction(paletteBackgroundModel.getThermalIsoThermUpperValueProcessor().observeOn(SchedulerProvider.ui()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                if (integer == paletteBackgroundModel.defaultValue) return;
                maxValue = integer;
                rangeSeekBar.setProgress(minValue, maxValue);
            }
        }));

        addReaction(paletteBackgroundModel.getCurrentGainTemperatureRangeProcessor().observeOn(SchedulerProvider.ui()).subscribe(new Consumer<Float[]>() {
            @Override
            public void accept(Float[] floats) {
                if (floats == null || floats.length != 2) return;
                if (floats[0].equals(floats[1])) return;
                lowTemperature = Math.round(floats[0]);
                highTemperature = Math.round(floats[1]);
                rangeSeekBar.setRange(lowTemperature, highTemperature);
            }
        }));
    }

    private void initPaletteScrollview() {
        ImageView imageView1 = findViewById(R.id.infrared1_image);
        imageView1.setTag(SettingsDefinitions.ThermalPalette.WHITE_HOT);
        TextView textView1 = findViewById(R.id.infrared1_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.WHITE_HOT, imageView1);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.WHITE_HOT, textView1);

        ImageView imageView2 = findViewById(R.id.infrared2_image);
        imageView2.setTag(SettingsDefinitions.ThermalPalette.BLACK_HOT);
        TextView textView2 = findViewById(R.id.infrared2_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.BLACK_HOT, imageView2);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.BLACK_HOT, textView2);

        ImageView imageView3 = findViewById(R.id.infrared3_image);
        imageView3.setTag(SettingsDefinitions.ThermalPalette.RED_HOT);  //描红
        TextView textView3 = findViewById(R.id.infrared3_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.RED_HOT, imageView3);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.RED_HOT, textView3);

        ImageView imageView4 = findViewById(R.id.infrared4_image);   //GREEN_HOT
        imageView4.setTag(SettingsDefinitions.ThermalPalette.GREEN_HOT);  //描红
        TextView textView4 = findViewById(R.id.infrared4_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.GREEN_HOT, imageView4);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.GREEN_HOT, textView4);

        ImageView imageView5 = findViewById(R.id.infrared5_image);
        imageView5.setTag(SettingsDefinitions.ThermalPalette.RAINBOW);
        TextView textView5 = findViewById(R.id.infrared5_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.RAINBOW, imageView5);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.RAINBOW, textView5);

        ImageView imageView6 = findViewById(R.id.infrared6_image);
        imageView6.setTag(SettingsDefinitions.ThermalPalette.IRONBOW_1);
        TextView textView6 = findViewById(R.id.infrared6_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.IRONBOW_1, imageView6);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.IRONBOW_1, textView6);

        ImageView imageView7 = findViewById(R.id.infrared7_image);
        imageView7.setTag(SettingsDefinitions.ThermalPalette.ICE_FIRE);
        TextView textView7 = findViewById(R.id.infrared7_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.ICE_FIRE, imageView7);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.ICE_FIRE, textView7);

        ImageView imageView8 = findViewById(R.id.infrared8_image);
        imageView8.setTag(SettingsDefinitions.ThermalPalette.COLOR_1);
        TextView textView8 = findViewById(R.id.infrared8_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.COLOR_1, imageView8);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.COLOR_1, textView8);

        ImageView imageView9 = findViewById(R.id.infrared9_image);
        imageView9.setTag(SettingsDefinitions.ThermalPalette.COLOR_2);
        TextView textView9 = findViewById(R.id.infrared9_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.COLOR_2, imageView9);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.COLOR_2, textView9);

        ImageView imageView10 = findViewById(R.id.infrared10_image);
        imageView10.setTag(SettingsDefinitions.ThermalPalette.RAIN);
        TextView textView10 = findViewById(R.id.infrared10_text);
        scrollviewImageHashMap.put(SettingsDefinitions.ThermalPalette.RAIN, imageView10);
        textViewHaspMap.put(SettingsDefinitions.ThermalPalette.RAIN, textView10);

        imageView1.setOnClickListener(this::scrollviewClick);
        imageView2.setOnClickListener(this::scrollviewClick);
        imageView3.setOnClickListener(this::scrollviewClick);
        imageView4.setOnClickListener(this::scrollviewClick);
        imageView5.setOnClickListener(this::scrollviewClick);
        imageView6.setOnClickListener(this::scrollviewClick);
        imageView7.setOnClickListener(this::scrollviewClick);
        imageView8.setOnClickListener(this::scrollviewClick);
        imageView9.setOnClickListener(this::scrollviewClick);
        imageView10.setOnClickListener(this::scrollviewClick);

        if (!isInEditMode()) {
            paletteBackgroundModel = new PaletteBackgroundModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
    }

    private void showSelectThermalPalette(SettingsDefinitions.ThermalPalette thermalPalette) {
        if (thermalPalette == null || scrollviewImageHashMap.size() == 0) return;
        ImageView imageView = (ImageView) scrollviewImageHashMap.get(thermalPalette);
        TextView textView = (TextView) textViewHaspMap.get(thermalPalette);
        if (imageView == null || textView == null) return;
        setSelectImage(imageView, textView);
    }

    public void scrollviewClick(View view) {
        if (view.equals(recordImageView)) return;
        SettingsDefinitions.ThermalPalette palette = (SettingsDefinitions.ThermalPalette) view.getTag();
        changePaletteValue(palette);
    }

    private synchronized void changePaletteValue(SettingsDefinitions.ThermalPalette palette) {
        if (!this.isSetting) {
            this.isSetting = true;
            KeyManager.getInstance().setValue(paletteBackgroundModel.getCameraPaletteKey(), palette, new SetCallback() {
                public void onSuccess() {
                    PaletteBackgroundWidget.this.isSetting = false;
                }

                public void onFailure(@NonNull DJIError var1) {
                    PaletteBackgroundWidget.this.isSetting = false;
                }
            });
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setSelectImage(ImageView imageView, TextView textView) {
        imageView.setBackground(getResources().getDrawable(R.drawable.image_rect));
        textView.setTextColor(getResources().getColor(R.color.uxsdk_blue));
        if (recordImageView != null && !imageView.equals(recordImageView)) {
            recordImageView.setBackground(getResources().getDrawable(R.drawable.image_rect_transparent));
        }
        if (recordTextView != null && !recordTextView.equals(textView)) {
            recordTextView.setTextColor(getResources().getColor(R.color.uxsdk_white));
        }
        recordTextView = textView;
        recordImageView = imageView;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onClick(View v) {
        int progress1 = Math.round(rangeSeekBar.getLeftSeekBar().getProgress());
        int progress2 = Math.round(rangeSeekBar.getRightSeekBar().getProgress());
        if (v.equals(lowTempReduceImage)) {
            if (progress1 <= recordMinValue) return;
            rangeSeekBar.setProgress(progress1 - 1, progress2);
            changeTempValue();
        } else if (v.equals(lowTempAddImage)) {
            if (progress1 + 1 >= progress2) {
                return;
            }
            progress1 = progress1 + 1;
            rangeSeekBar.setProgress(progress1, progress2);
            changeTempValue();
        } else if (v.equals(highTempReduceImage)) {
            if (progress2 - 1 <= progress1) return;
            rangeSeekBar.setProgress(progress1, progress2 - 1);
            changeTempValue();
        } else if (v.equals(highTempAddImage)) {
            if (progress2 >= recordMaxValue) return;
            rangeSeekBar.setProgress(progress1, progress2 + 1);
            changeTempValue();
        } else if (v.equals(tempView)) {
            tempView.setImageDrawable(getResources().getDrawable(R.drawable.temp_select));
            paletteView.setImageDrawable(getResources().getDrawable(R.drawable.palette_unselect));
            paletteBackView.setVisibility(View.GONE);
            tempBackView.setVisibility(View.VISIBLE);
        } else if (v.equals(paletteView)) {
            tempView.setImageDrawable(getResources().getDrawable(R.drawable.temp_unselect));
            paletteView.setImageDrawable(getResources().getDrawable(R.drawable.palette_select));
            paletteBackView.setVisibility(View.VISIBLE);
            tempBackView.setVisibility(View.GONE);
        } else if (v.equals(addMaxRangeBtn)) {
            recordMaxValue = maxValue;
            recordMinValue = minValue;
            rangeSeekBar.setRange(minValue, maxValue);
            rangeSeekBar.setProgress(minValue, maxValue);
        } else if (v.equals(reduceMinRangeBtn)) {
            recordMaxValue = highTemperature;
            recordMinValue = lowTemperature;
            rangeSeekBar.setRange(lowTemperature, highTemperature);
            rangeSeekBar.setProgress(minValue, maxValue);
        } else if (v.equals(customWidgetSwitch)) {
            openSwitch(customWidgetSwitch.isChecked());
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void openSwitch(boolean open) {
        rangeSeekBar.setClickable(open);
        lowTempAddImage.setClickable(open);
        lowTempAddImage.setImageDrawable(open ? getResources().getDrawable(R.drawable.add) : getResources().getDrawable(R.drawable.add_grey));
        lowTempReduceImage.setClickable(open);
        lowTempReduceImage.setImageDrawable(open ? getResources().getDrawable(R.drawable.reduce) : getResources().getDrawable(R.drawable.reduce_grey));
        highTempAddImage.setClickable(open);
        highTempAddImage.setImageDrawable(open ? getResources().getDrawable(R.drawable.add) : getResources().getDrawable(R.drawable.add_grey));
        highTempReduceImage.setClickable(open);
        highTempReduceImage.setImageDrawable(open ? getResources().getDrawable(R.drawable.reduce) : getResources().getDrawable(R.drawable.reduce_grey));
        rangeSeekBar.setEnabled(open);
        if (open) {
            rangeState(minValue, maxValue);
            return;
        }
        addMaxRangeBtn.setClickable(false);
        addMaxRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.add_unselect));
        reduceMinRangeBtn.setClickable(false);
        reduceMinRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.reduce_unselect));
    }

    @NonNull
    @Override
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return paletteBackgroundModel.getCameraIndex();
    }

    @NonNull
    @Override
    public SettingsDefinitions.LensType getLensType() {
        return paletteBackgroundModel.getLensType();
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        paletteBackgroundModel.updateCameraSource(cameraIndex, lensType);
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return "1:1";
    }


    @Override
    public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
        minValue = Math.round(leftValue);
        maxValue = Math.round(rightValue);
        rangeState(minValue, maxValue);
    }

    @Override
    public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
    }

    @Override
    public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
        changeTempValue();
    }

    private synchronized void changeTempValue() {
        if (!this.isMinTempSetting && !this.isMaxTempSetting) {
            this.isMinTempSetting = true;
            this.isMaxTempSetting = true;
            KeyManager.getInstance().setValue(paletteBackgroundModel.getThermalIsoThermUpperValueKey(), maxValue, new SetCallback() {
                @Override
                public void onSuccess() {
                    PaletteBackgroundWidget.this.isMinTempSetting = false;
                }

                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    PaletteBackgroundWidget.this.isMinTempSetting = false;
                }
            });
            KeyManager.getInstance().setValue(paletteBackgroundModel.getThermalIsoThermLowerValueKey(), minValue, new SetCallback() {
                @Override
                public void onSuccess() {
                    PaletteBackgroundWidget.this.isMaxTempSetting = false;
                }

                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    PaletteBackgroundWidget.this.isMaxTempSetting = false;
                }
            });
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void rangeState(int minValue, int maxValue) {
        if ((minValue != recordMinValue || maxValue != recordMaxValue) && !addMaxRangeBtn.isClickable()) {
            addMaxRangeBtn.setClickable(true);
            addMaxRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.add_select));
        }
        if ((minValue == recordMinValue && maxValue == recordMaxValue) && addMaxRangeBtn.isClickable()) {
            addMaxRangeBtn.setClickable(false);
            addMaxRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.add_unselect));
        }
        if ((recordMinValue != lowTemperature || recordMaxValue != highTemperature) && !reduceMinRangeBtn.isClickable()) {
            reduceMinRangeBtn.setClickable(true);
            reduceMinRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.reduce_select));
        } else {
            if (!reduceMinRangeBtn.isClickable()) return;
            reduceMinRangeBtn.setClickable(false);
            reduceMinRangeBtn.setImageDrawable(getResources().getDrawable(R.drawable.reduce_unselect));
        }
    }
}