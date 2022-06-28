package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions.ExposureCompensation;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.common.camera.SettingsDefinitions.ExposureSensitivityMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Created by Robert on 9/21/16.
 */
public class CameraEVSettingWidget extends FrameLayoutWidget {

    //region Constant Properties
    private static final String TAG = "CameraEVSettingWidget";

    //endregion

    //region Properties
    private TextView evTitle;
    private ImageView evMinusView;
    private TextView evValueText;
    private ImageView evPlusView;
    private StripeView evStatusView;

    private DJIKey cameraEVKey;
    private DJIKey currentExposureKey;
    private ExposureMode exposureMode;
    private ExposureCompensation cameraEV;

    private String[] evNameArray;
    private Object[] evValueArray;

    private int currentEvPos = 0;
    private TextView evStatusValueText;
    private DJIKey compensationRangeKey;
    private DJIKey exposureModeKey;
    private DJIKey exposureSensitivityModeKey;
    //endregion

    private EVSettingAppearances widgetAppearances;
    // EV value when in Manual Mode.
    private ExposureCompensation cameraMMEV;
    private boolean isEvAdjustable;
    private boolean isEIEnabled;

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new EVSettingAppearances();
        }
        return widgetAppearances;
    }

    //region Default Constructors
    public CameraEVSettingWidget(Context context) {
        super(context, null, 0);
    }

    public CameraEVSettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraEVSettingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        evTitle = (TextView) findViewById(R.id.textview_ev_title);
        evMinusView = (ImageView) findViewById(R.id.imagebutton_ev_setting_minus);
        evValueText = (TextView) findViewById(R.id.textview_setting_ev_value);
        evPlusView = (ImageView) findViewById(R.id.imagebutton_ev_setting_plus);
        evStatusView = (StripeView) findViewById(R.id.stripeview_setting_ev_status);
        evStatusValueText = (TextView) findViewById(R.id.textview_setting_ev_status_value);
        initEVWidgetWithInitialValue();
    }

    private void initEVWidgetWithInitialValue() {
        final Resources res = context.getResources();
        int[] valueArray = res.getIntArray(R.array.uxsdk_camera_ev_value_array);
        if (!isInEditMode()){
            ExposureCompensation[] evRange = new ExposureCompensation[valueArray.length];
            for (int i = 0; i < valueArray.length; i++) {
                evRange[i] = ExposureCompensation.find(valueArray[i]);
            }
            updateEvArray(evRange);

            evStatusView.setZeroPosition(evNameArray.length / 2);
            currentEvPos = evNameArray.length / 2;
            updateEVStateView(currentEvPos);
        }
        evTitle.setText(R.string.uxsdk_camera_exposure_ev_title);
        initClickListener();

        enableEditable(true);
    }

    private void updateEVStateView(int curPos) {
        evStatusValueText.setText(evNameArray[curPos]);
        evValueText.setText(evNameArray[curPos]);
        evStatusView.setSelectedPosition(curPos);
    }

    //endregion

    //region Key life cycle
    @Override
    public void initKey() {
        // This key only used for setting.
        cameraEVKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_COMPENSATION, keyIndex, subKeyIndex);
        exposureModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_MODE, keyIndex, subKeyIndex);
        compensationRangeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_COMPENSATION_RANGE, keyIndex, subKeyIndex);
        currentExposureKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_SETTINGS, keyIndex, subKeyIndex);
        exposureSensitivityModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_SENSITIVITY_MODE, keyIndex, subKeyIndex);

        addDependentKey(exposureModeKey);
        addDependentKey(cameraEVKey);
        addDependentKey(currentExposureKey);
        addDependentKey(compensationRangeKey);
        addDependentKey(exposureSensitivityModeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(exposureModeKey)) {
            exposureMode = (ExposureMode) value;
        } else if (key.equals(currentExposureKey)) {
            ExposureSettings exposureParameters = ((ExposureSettings) value);
            cameraMMEV = exposureParameters.getExposureCompensation();
        } else if (key.equals(cameraEVKey)) {
            cameraEV = (ExposureCompensation) value;
        } else if (key.equals(compensationRangeKey)) {
            Object[] evArray = (ExposureCompensation[]) value;
            updateEvArray(evArray);
        }else if (key.equals(exposureSensitivityModeKey)) {
            isEIEnabled = ((ExposureSensitivityMode) value) == ExposureSensitivityMode.EI;
        }
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    // Only assign evValueArray and evNameArray in this method
    // to make sure they are consistent on the index which can mapping each other.
    private void updateEvArray(Object[] array) {
        evValueArray = array;
        String[] newEV = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            String evName = CameraUtil.exposureValueDisplayName((ExposureCompensation) (array[i]));
            newEV[i] = evName;
        }
        evNameArray = newEV;
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(exposureModeKey)) {
            updateEditable(exposureMode, cameraEV);
        } else if (key.equals(compensationRangeKey)) {
            updateEVStatusViewRange();
        } else if (key.equals(exposureSensitivityModeKey)) {
            this.setVisibility(isEIEnabled ? GONE : VISIBLE);
        } else {
            updateEditable(exposureMode, cameraEV);
            if (isEvAdjustable) {
                updateEV(cameraEV);
            } else {
                updateEV(cameraMMEV);
            }
        }
    }

    private void updateEVStatusViewRange() {
        evStatusView.setZeroPosition(evNameArray.length / 2);
    }

    public int getEvValuePos(ExposureCompensation ev) {
        int pos = 0;
        if (evValueArray != null) {
            for (int i = 0, length = evValueArray.length; i < length; i++) {
                if (ev == evValueArray[i]) {
                    return i;
                }
            }
        }
        return pos;
    }

    public void updateEV(ExposureCompensation ev) {
        // By refer to DJIGo both iOS and Android, they show the fixed EV as 0
        if (ev == ExposureCompensation.FIXED) {
            ev = ExposureCompensation.N_0_0;
        }

        currentEvPos = getEvValuePos(ev);
        updateEVStateView(currentEvPos);
    }

    public void enableEditable(boolean isEditable) {
        isEvAdjustable = isEditable;
        if (isEditable) {
            evPlusView.setEnabled(true);
            evPlusView.setVisibility(View.VISIBLE);
            evMinusView.setEnabled(true);
            evMinusView.setVisibility(View.VISIBLE);
            evValueText.setEnabled(true);
            evValueText.setVisibility(View.VISIBLE);

            evStatusView.setVisibility(View.INVISIBLE);
            evStatusValueText.setVisibility(View.INVISIBLE);
        } else {
            evMinusView.setEnabled(false);
            evMinusView.setVisibility(INVISIBLE);
            evPlusView.setEnabled(false);
            evPlusView.setVisibility(INVISIBLE);
            evValueText.setEnabled(false);
            evValueText.setVisibility(View.INVISIBLE);

            evStatusView.setVisibility(View.VISIBLE);
            evStatusValueText.setVisibility(View.VISIBLE);
        }
    }

    private void updateEditable(final ExposureMode mode, ExposureCompensation ev) {
        boolean canEVEditable = (mode != ExposureMode.MANUAL && ev != ExposureCompensation.FIXED);
        if (isEvAdjustable != canEVEditable) {
            enableEditable(canEVEditable);
        }
    }
    //endregion

    //region Click event handler
    private void initClickListener() {
        OnClickListener minusClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEvPos > 0) {
                    currentEvPos--;
                    handleEvChanged(currentEvPos, false);
                }
            }
        };

        OnClickListener plusClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEvPos < (evNameArray.length - 1)) {
                    currentEvPos++;
                    handleEvChanged(currentEvPos, false);
                }
            }
        };

        OnClickListener restoreClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEvPos != (evNameArray.length / 2)) {
                    currentEvPos = (evNameArray.length / 2);
                    handleEvChanged(currentEvPos, true);
                }
            }
        };

        evMinusView.setOnClickListener(minusClickListener);
        evPlusView.setOnClickListener(plusClickListener);
        evValueText.setOnClickListener(restoreClickListener);
    }

    private void handleEvChanged(final int evPos, final boolean isMid) {
        if (KeyManager.getInstance() == null) return;

        if (evValueArray == null || evPos >= evValueArray.length) {
            return;
        }

        if (isMid) {
            playEvCenterSound();
        } else {
            playSimpleSound();
        }

        final ExposureCompensation newEv = (ExposureCompensation) evValueArray[evPos];
        // Update the UI first to improve the response.
        updateEV(newEv);

        KeyManager.getInstance().setValue(cameraEVKey, newEv, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        restoreToCurrentEv();
                    }
                });
            }
        });
    }

    private void restoreToCurrentEv() {
        updateEV(cameraEV);
    }

    private void playEvCenterSound() {
        AudioUtil.playSoundInBackground(getContext(), R.raw.uxsdk_camera_ev_center);
    }

    private void playSimpleSound() {
        AudioUtil.playSoundInBackground(getContext(), R.raw.uxsdk_camera_simple_click);
    }
    //endregion
}
