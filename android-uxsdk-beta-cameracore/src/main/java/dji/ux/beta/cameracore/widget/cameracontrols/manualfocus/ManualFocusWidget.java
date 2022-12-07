package dji.ux.beta.cameracore.widget.cameracontrols.manualfocus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.base.DJIRulerView;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.SettingDefinitions;
import io.reactivex.rxjava3.core.Observable;

public class ManualFocusWidget extends FrameLayoutWidget implements ICameraIndex, View.OnClickListener {

    private ManualFocusModel widgetModel;
    private int currentRingRangeValue;
    private int maxRingRangeValue;
    private DJIRulerView ringWheel;
    private int currentValue;
    private boolean isSetting = false;
    private int preValue;
    private View associate;
    SettingsDefinitions.FocusMode focusMode;
    private int resourceId;

    public ManualFocusWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_manual_focus_widget, this);
        findViewById(R.id.vertical_wheel_max).setOnClickListener(this);
        findViewById(R.id.vertical_wheel_min).setOnClickListener(this);
        ringWheel = findViewById(R.id.ring_wheel);
        this.setVisibility(GONE);
        if (!isInEditMode()) {
            widgetModel = new ManualFocusModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
        ringWheel.setOnChangeListener((djiRulerView, var2, var3, var4) -> {
            int curSize = djiRulerView.getCurSize();
            int maxSize = djiRulerView.getMaxSize();
            currentValue = Math.round(curSize * 1f / maxSize * maxRingRangeValue);
            setFocusRingValue();
        });
    }

    private synchronized void setFocusRingValue() {
        KeyManager keyManager = KeyManager.getInstance();
        if (keyManager == null) return;
        if (!this.isSetting) {
            if (this.preValue != currentValue) {
                this.preValue = currentValue;
                this.isSetting = true;
                keyManager.setValue(widgetModel.getCameraFocusRingKey(), currentValue, new SetCallback() {
                    public void onSuccess() {
                        ManualFocusWidget.this.isSetting = false;
                        ManualFocusWidget.this.setFocusRingValue();
                    }

                    public void onFailure(@NonNull DJIError var1) {
                        ManualFocusWidget.this.isSetting = false;
                    }
                });
            }
        }
    }

    private void updateCurrentSize() {
        addReaction(Observable.timer(100, TimeUnit.MILLISECONDS).subscribe(aLong -> updateRange()));
    }

    public void updateVisible() {
        updateVisible(focusMode);
    }

    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
        if (resourceId != 0) {
            associate = getRootView().findViewById(resourceId);
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
    protected void reactToModelChanges() {
        addReaction(widgetModel.getCurrentFocusRingDataProcessor().observeOn(SchedulerProvider.ui()).subscribe(currentRingRangeValue -> {
            this.currentRingRangeValue = currentRingRangeValue;
            updateRange();
        }));
        addReaction(widgetModel.getRangeMaxDataProcessor().observeOn(SchedulerProvider.ui()).subscribe(maxRingRangeValue -> {
            this.maxRingRangeValue = maxRingRangeValue;
            updateMaxRange();
        }));
        addReaction(widgetModel.getFocusModeDataProcessor().observeOn(SchedulerProvider.ui()).subscribe(focusMode -> {
            ManualFocusWidget.this.focusMode = focusMode;
            updateVisible(focusMode);
        }));
    }

    private void updateVisible(SettingsDefinitions.FocusMode focusMode) {
        if (associate == null) return;
        int visibility = associate.getVisibility();
        if (visibility == VISIBLE) {
            if (focusMode == SettingsDefinitions.FocusMode.MANUAL) {
                this.setVisibility(VISIBLE);
                updateCurrentSize();
            } else {
                setVisibility(GONE);
            }
        } else {
            this.setVisibility(GONE);
        }
    }


    private void updateMaxRange() {
        int max = maxRingRangeValue / getStep();
        if (max == 0) return;
        ringWheel.setMaxSize(max);
        updateRange();
    }

    private void updateRange() {
        int currentValue = currentRingRangeValue / getStep();
        ringWheel.setCurSizeNow(currentValue);
    }


    private int getStep() {
        if (maxRingRangeValue > 1000) return 10;
        return 1;
    }


    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return "16:9";
    }

    @NonNull
    @Override
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @NonNull
    @Override
    public SettingsDefinitions.LensType getLensType() {
        return widgetModel.getLensType();
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    public void onClick(View var1) {
        if (var1.getId() == R.id.vertical_wheel_max) {
            int maxSize = ringWheel.getMaxSize();
            this.ringWheel.setCurSizeNow(maxSize);
        } else if (var1.getId() == R.id.vertical_wheel_min) {
            this.ringWheel.setCurSizeNow(0);
        }
        currentValue = Math.round(ringWheel.getCurSize() * 1f / ringWheel.getMaxSize() * maxRingRangeValue);
        setFocusRingValue();
    }


}