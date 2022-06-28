package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Created by Robert on 9/12/16.
 */
public class CameraExposureModeSettingWidget extends FrameLayoutWidget implements View.OnClickListener {

    //region Properties
    static final String TAG = "CameraExposureModeSettingWidget";
    private ExposureMode exposureMode;

    private FrameLayout modePLayout;
    private DJIKey exposureModeKey;
    private FrameLayout modeSLayout;
    private FrameLayout modeALayout;
    private FrameLayout modeMLayout;
    private DJIKey exposureModeRangeKey;
    private ExposureMode[] exposureModeRange;
    //endregion

    //region Default Constructors
    public CameraExposureModeSettingWidget(Context context) {
        super(context, null, 0);
    }

    public CameraExposureModeSettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraExposureModeSettingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //endregion
    private ExposureModeSettingAppearances widgetAppearances;

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ExposureModeSettingAppearances();
        }
        return widgetAppearances;
    }

    //region UI Life Cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        setBackgroundResource(R.drawable.uxsdk_camera_exposure_mode_setting_background);
        modePLayout = (FrameLayout) findViewById(R.id.layout_camera_mode_p);
        modeSLayout = (FrameLayout) findViewById(R.id.layout_camera_mode_s);
        modeALayout = (FrameLayout) findViewById(R.id.layout_camera_mode_a);
        modeMLayout = (FrameLayout) findViewById(R.id.layout_camera_mode_m);
        modePLayout.setOnClickListener(this);
        modeALayout.setOnClickListener(this);
        modeSLayout.setOnClickListener(this);
        modeMLayout.setOnClickListener(this);
        modePLayout.setSelected(true);
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    //endregion

    //region UI Logic
    @Override
    public void initKey() {
        exposureModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_MODE, keyIndex, subKeyIndex);
        addDependentKey(exposureModeKey);
        exposureModeRangeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_MODE_RANGE, keyIndex, subKeyIndex);
        addDependentKey(exposureModeRangeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(exposureModeKey)) {
            exposureMode = (ExposureMode) value;
        } else if (key.equals(exposureModeRangeKey)) {
            exposureModeRange = (ExposureMode[]) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(exposureModeKey)) {
            updateExposureMode(exposureMode);
        } else if (key.equals(exposureModeRangeKey)) {
            updateExposureModeRange(exposureModeRange);
        }
    }

    private boolean rangeContains(ExposureMode[] range, ExposureMode value) {

        if (range == null){
            return false;
        }

        for (ExposureMode item : range) {
            if (item.equals(value)) {
                return true;
            }
        }

        return false;
    }

    private void updateExposureModeRange(ExposureMode[] range) {
        modeALayout.setEnabled(rangeContains(range, ExposureMode.APERTURE_PRIORITY));
        modeSLayout.setEnabled(rangeContains(range, ExposureMode.SHUTTER_PRIORITY));
        modeMLayout.setEnabled(rangeContains(range, ExposureMode.MANUAL));
        modePLayout.setEnabled(rangeContains(range, ExposureMode.PROGRAM));
    }

    private void updateExposureMode(ExposureMode mode) {
        modePLayout.setSelected(false);
        modeMLayout.setSelected(false);
        modeSLayout.setSelected(false);
        modeALayout.setSelected(false);

        if (mode == ExposureMode.PROGRAM) {
            if (!modePLayout.isSelected()) {
                modePLayout.setSelected(true);
            }
        } else if (mode == ExposureMode.SHUTTER_PRIORITY) {
            if (!modeSLayout.isSelected()) {
                modeSLayout.setSelected(true);
            }
        } else if (mode == ExposureMode.APERTURE_PRIORITY) {
            if (!modeALayout.isSelected()) {
                modeALayout.setSelected(true);
            }
        } else if (mode == ExposureMode.MANUAL) {
            if (!modeMLayout.isSelected()) {
                modeMLayout.setSelected(true);
            }
        }
    }

    @Override
    public void onClick(View v) {

        ExposureMode previousExposureMode = exposureMode;
        int resId = v.getId();
        if (resId == R.id.layout_camera_mode_p) {
            exposureMode = ExposureMode.PROGRAM;
        } else if (resId == R.id.layout_camera_mode_a) {
            exposureMode = ExposureMode.APERTURE_PRIORITY;
        } else if (resId == R.id.layout_camera_mode_s) {
            exposureMode = ExposureMode.SHUTTER_PRIORITY;
        } else if (resId == R.id.layout_camera_mode_m) {
            exposureMode = ExposureMode.MANUAL;
        }

        if (exposureMode.equals(previousExposureMode)) {
            return;
        }

        updateExposureMode(exposureMode);

        if (KeyManager.getInstance() == null) return;
        KeyManager.getInstance().setValue(exposureModeKey, exposureMode, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        restoreToCurrentExposureMode();
                    }
                });
            }
        });
    }

    private void restoreToCurrentExposureMode() {
        if (KeyManager.getInstance() == null) return;

        Object evValue = KeyManager.getInstance().getValue(exposureModeKey);
        if (evValue != null) {
            exposureMode = (ExposureMode) evValue;
            updateExposureMode(exposureMode);
        }
    }
    //endregion
}
