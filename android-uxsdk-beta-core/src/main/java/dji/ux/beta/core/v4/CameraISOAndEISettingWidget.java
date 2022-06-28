package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.CameraUtils;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.common.camera.SettingsDefinitions.ExposureSensitivityMode;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.camera.SettingsDefinitions.ISO;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.ProductUtil;

/**
 * Horizontal seek bar that displays ISO value as well allow changing ISO
 */
public class CameraISOAndEISettingWidget extends FrameLayoutWidget
        implements Button.OnClickListener, SeekBar.OnSeekBarChangeListener {

    //region Properties
    private static final String LOCKED_ISO_VALUE = "500";
    private static final String TAG = "ISOAndEISettingWidget";
    private BaseWidgetAppearances widgetAppearances;
    private DJIKey cameraISOKey;
    private int cameraISO;
    private ISO[] isoValueArray;
    private SeekBar seekBarISO;
    private ImageView autoISOButton;
    private DJIKey isoRangeKey;
    private DJIKey currentExposureValueKey;
    private boolean isSeekBarTracking;
    private boolean isISOSeekBarEnabled;
    private boolean isISOAutoSupported;
    private boolean isISOAutoSelected;
    private boolean isISOLocked;
    private DJIKey exposureSensitivityModeKey;
    private DJIKey eiValueKey;
    private DJIKey eiRecommendedValueKey;
    private DJIKey eiValueRangeKey;
    private boolean isEIEnabled;
    private int eiValue;
    private int eiRecommendedValue;
    private int[] eiValueArray;
    private DJIKey cameraModeKey;
    private CameraMode cameraMode;
    private DJIKey flatCameraModeKey;
    private FlatCameraMode flatCameraMode;
    private TextView title;
    private SeekBar seekBarEI;
    private LinearLayout seekBarISOLayout;
    private DJIKey exposureModeKey;

    //endregion

    //region Widget UI Life Cycle
    public CameraISOAndEISettingWidget(Context context) {
        super(context, null, 0);
    }

    public CameraISOAndEISettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraISOAndEISettingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private static int getISOIndex(ISO[] array, int isoValue) {
        int index = -1;
        ISO iso = CameraUtils.convertIntToISO(isoValue);
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (iso == array[i]) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Initialize ISO array
        final Resources res = context.getResources();
        int[] valueArray = res.getIntArray(R.array.uxsdk_iso_values);
        isoValueArray = new ISO[valueArray.length];

        if(!isInEditMode()){
            for (int i = 0; i < valueArray.length; i++) {
                isoValueArray[i] = ISO.find(valueArray[i]);
            }
            updateISORangeValue(isoValueArray);
        }

        title = (TextView) findViewById(R.id.textview_iso_title);
        seekBarISOLayout = (LinearLayout) findViewById(R.id.seekbar_layout);
        seekBarISO = (SeekBar) findViewById(R.id.seekbar_iso);
        autoISOButton = (ImageView) findViewById(R.id.button_iso_auto);
        isISOSeekBarEnabled = false;
        seekBarISO.setProgress(0);
        seekBarISO.enable(false);
        seekBarISO.setOnSeekBarChangeListener(this);
        seekBarISO.setBaselineVisibility(false);
        seekBarISO.setMinValueVisibility(true);
        seekBarISO.setMaxValueVisibility(true);
        seekBarISO.setMinusVisibility(false);
        seekBarISO.setPlusVisibility(false);
        autoISOButton.setOnClickListener(this);

        // EI seekBar
        seekBarEI = (SeekBar) findViewById(R.id.seekbar_ei);
        seekBarEI.setOnSeekBarChangeListener(this);
        seekBarEI.setVisibility(GONE);
        seekBarEI.setMinValueVisibility(true);
        seekBarEI.setMaxValueVisibility(true);
        seekBarEI.setMinusVisibility(false);
        seekBarEI.setPlusVisibility(false);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ISOAndEISettingAppearances();
        }
        return widgetAppearances;
    }
    //endregion

    @Override
    public boolean shouldTrack() {
        return false;
    }

    //region Widget Key logic
    @Override
    public void initKey() {

        cameraISOKey = CameraUtil.createCameraKeys(CameraKey.ISO, keyIndex, subKeyIndex);

        addDependentKey(cameraISOKey);

        currentExposureValueKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_SETTINGS, keyIndex ,subKeyIndex);
        addDependentKey(currentExposureValueKey);

        isoRangeKey = CameraUtil.createCameraKeys(CameraKey.ISO_RANGE, keyIndex , subKeyIndex);
        addDependentKey(isoRangeKey);

        exposureSensitivityModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_SENSITIVITY_MODE, keyIndex ,subKeyIndex);
        addDependentKey(exposureSensitivityModeKey);

        eiValueKey = CameraUtil.createCameraKeys(CameraKey.EI_VALUE, keyIndex ,subKeyIndex);
        addDependentKey(eiValueKey);

        eiRecommendedValueKey = CameraUtil.createCameraKeys(CameraKey.RECOMMENDED_EI_VALUE, keyIndex , subKeyIndex);
        addDependentKey(eiRecommendedValueKey);

        eiValueRangeKey = CameraUtil.createCameraKeys(CameraKey.EI_VALUE_RANGE, keyIndex ,subKeyIndex);
        addDependentKey(eiValueRangeKey);

        cameraModeKey = CameraUtil.createCameraKeys(CameraKey.MODE, keyIndex , subKeyIndex);
        addDependentKey(cameraModeKey);

        flatCameraModeKey = CameraUtil.createCameraKeys(CameraKey.FLAT_CAMERA_MODE, keyIndex , subKeyIndex);
        addDependentKey(flatCameraModeKey);

        exposureModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_MODE, keyIndex ,subKeyIndex);
        addDependentKey(exposureModeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(cameraISOKey)) {
            ISO iso = (ISO) value;
            isISOAutoSelected = (iso == ISO.AUTO);
            isISOLocked = (iso == ISO.FIXED);
        } else if (key.equals(isoRangeKey)) {
            updateISORangeValue((ISO[]) value);
        } else if (key.equals(currentExposureValueKey)) {
            ExposureSettings exposureParameters = (ExposureSettings) value;
            cameraISO = exposureParameters.getISO();
        } else if (key.equals(exposureSensitivityModeKey)) {
            isEIEnabled = ((ExposureSensitivityMode) value) == ExposureSensitivityMode.EI;
        } else if (key.equals(eiValueKey)) {
            eiValue = (int) value;
        } else if (key.equals(eiRecommendedValueKey)) {
            eiRecommendedValue = (int) value;
        } else if (key.equals(eiValueRangeKey)) {
            eiValueArray = (int[]) value;
        } else if (key.equals(cameraModeKey)) {
            cameraMode = (CameraMode) value;
        } else if (key.equals(flatCameraModeKey)) {
            flatCameraMode = (FlatCameraMode) value;
        } else if (key.equals(exposureModeKey)) {
            SettingsDefinitions.ExposureMode exposureMode = (SettingsDefinitions.ExposureMode) value;
            if (!isAutoISOSupportedByProduct()) {
                if (exposureMode != SettingsDefinitions.ExposureMode.MANUAL) {
                    isISOAutoSelected = true;
                    setAutoISO(isISOAutoSelected);
                } else {
                    isISOAutoSelected = false;
                }
            }
        }
    }

    private boolean checkAutoISO(ISO[] array) {

        for (ISO iso : array) {
            if (iso == ISO.AUTO) {
                return true;
            }
        }
        return false;
    }

    private void updateISORangeValue(ISO[] array) {

        isISOAutoSupported = checkAutoISO(array);
        ISO[] newISOValues;

        if (isISOAutoSupported) {
            newISOValues = new ISO[array.length - 1];
        } else {
            newISOValues = new ISO[array.length];
        }

        // remove the auto value
        for (int i = 0, j = 0; i < array.length; i++) {
            if (array[i] != ISO.AUTO) {
                newISOValues[j] = array[i];
                j++;
            }
        }

        isoValueArray = newISOValues;
    }

    private void updateISORangeUI() {
        // Workaround where ISO range updates to single value in AUTO mode
        if (isoValueArray != null && isoValueArray.length != 0) {

            int minCameraISO = CameraUtils.convertISOToInt(isoValueArray[0]);
            seekBarISO.setLeftBoundaryText(String.valueOf(minCameraISO));
            int maxCameraISO = CameraUtils.convertISOToInt(isoValueArray[isoValueArray.length - 1]);
            seekBarISO.setRightBoundaryText(String.valueOf(maxCameraISO));

            seekBarISO.setMax(isoValueArray.length - 1);
            isISOSeekBarEnabled = true;

            updateISOValue(isoValueArray, cameraISO);
            // Auto button has relationship with ISO range, so need update this button here.
            updateAutoISOButton();


        } else {
            isISOSeekBarEnabled = false;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(cameraISOKey) || key.equals(isoRangeKey) || key.equals(currentExposureValueKey) || key.equals(exposureModeKey)) {
            updateISOWidget(key);
        } else if (key.equals(exposureSensitivityModeKey) || key.equals(cameraModeKey) || key.equals(flatCameraModeKey)) {
            updateWidgetUI();
        } else {
            updateEIWidget(key);
        }
    }

    private void updateWidgetUI() {
        if (isRecordVideoEIMode()) {
            title.setText(R.string.uxsdk_camera_ei);
            seekBarISOLayout.setVisibility(GONE);
            seekBarEI.setVisibility(VISIBLE);
        } else {
            title.setText(R.string.uxsdk_camera_exposure_iso_title);
            seekBarISOLayout.setVisibility(VISIBLE);
            seekBarEI.setVisibility(GONE);
        }
    }

    private void updateISOWidget(DJIKey key) {
        if (isISOLocked) {
            updateISOLocked();
        } else {
            if (key.equals(currentExposureValueKey) || key.equals(cameraISOKey)) {
                autoISOButton.setSelected(isISOAutoSelected);
                if (!isSeekBarTracking) {
                    updateISOValue(isoValueArray, cameraISO);
                }
            } else if (key.equals(isoRangeKey)) {
                updateISORangeUI();
            }

            seekBarISO.enable(!isISOAutoSelected && isISOSeekBarEnabled);
        }
    }

    private void updateEIWidget(DJIKey key) {

        if (eiValueArray == null || eiValueArray.length == 0) {
            seekBarEI.enable(false);
            return;
        } else {
            seekBarEI.enable(true);
        }

        if (key.equals(eiValueKey)) {
            if (!isSeekBarTracking) {
                updateEIValue(eiValueArray, eiValue);
            }
        } else if (key.equals(eiValueRangeKey)) {
            updateEIRangeUI(eiValueArray);
        } else if (key.equals(eiRecommendedValueKey)) {
            updateEIBaseline(eiValueArray, eiRecommendedValue);
        }

    }

    private void updateAutoISOButton() {
        // Update auto ISO button
        if (isISOAutoSupported && isISOSeekBarEnabled
                && !isRecordVideoEIMode()
                && isAutoISOSupportedByProduct()) {
            autoISOButton.setVisibility(VISIBLE);
        } else {
            autoISOButton.setVisibility(GONE);
        }
    }

    private boolean isRecordVideoEIMode() {
        return (!CameraUtil.isPictureMode(flatCameraMode) || cameraMode == CameraMode.RECORD_VIDEO)
                && isEIEnabled;
    }

    private void updateISOValue(ISO[] array, int value) {
        int progress = getISOIndex(array, value);
        seekBarISO.setProgress(progress);
    }

    // By referring to DJIGo4 in both iOS and Android version
    // Showing the ISO_FIXED  as locked value 500
    private void updateISOLocked() {
        autoISOButton.setVisibility(GONE);
        seekBarISO.enable(false);
        seekBarISO.setProgress(seekBarISO.getMax() / 2 - 1);
    }

    private int getEIIndex(int[] array, int eiValue) {
        int index = -1;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == eiValue) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    private void updateEIRangeUI(int[] array) {
        // Workaround where ISO range updates to single value in AUTO mode
        if (array != null && array.length > 0) {
            seekBarEI.setMax(array.length - 1);
            seekBarEI.setLeftBoundaryText(String.valueOf(array[0]));
            seekBarEI.setRightBoundaryText(String.valueOf(array[array.length - 1]));
            updateEIValue(array, eiValue);
            updateEIBaseline(array, eiRecommendedValue);
        }
    }

    private void updateEIValue(int[] array, int eiValue) {
        int progress = getEIIndex(array, eiValue);
        seekBarEI.setProgress(progress);
    }

    private void updateEIBaseline(int[] array, int eiRecommendedValue) {
        int progress = getEIIndex(array, eiRecommendedValue);

        if (progress >= 0) {
            seekBarEI.setBaselineProgress(progress);
            seekBarEI.setBaselineVisibility(true);
        } else {
            seekBarEI.setBaselineVisibility(false);
        }

    }
    //endregion

    //region OnClick Listener
    @Override
    public void onClick(View v) {
        if (v == autoISOButton) {
            isISOAutoSelected = !isISOAutoSelected;
            setAutoISO(isISOAutoSelected);
        }
    }

    //endregion

    //region OnSeekBarChangeListener
    @Override
    public void onProgressChanged(SeekBar object, int progress) {
        if (object == seekBarISO) {
            if (isISOLocked) {
                seekBarISO.setText(LOCKED_ISO_VALUE);
            } else {
                if (isoValueArray != null && isoValueArray.length != 0 && progress < isoValueArray.length) {
                    cameraISO = CameraUtils.convertISOToInt(isoValueArray[progress]);
                    seekBarISO.setText(String.valueOf(cameraISO));
                }
            }
        } else {
            if (eiValueArray != null && isoValueArray.length != 0 && progress < eiValueArray.length) {
                seekBarEI.setText(String.valueOf(eiValueArray[progress]));
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar object, int progress) {
        isSeekBarTracking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar object, int progress) {
        isSeekBarTracking = false;
        AudioUtil.playSimpleSound(getContext());
        if (object == seekBarISO) {
            if (isoValueArray != null && isoValueArray.length != 0) {
                final ISO newISO = isoValueArray[progress];
                updateISOToCamera(newISO);
            }
        } else {
            if (eiValueArray != null && progress < eiValueArray.length) {
                updateEIToCamera(eiValueArray[progress]);
            }
        }
    }

    @Override
    public void onPlusClicked(SeekBar seekBar) {
        //Do nothing as not displayed - implement when displayed as required
    }

    @Override
    public void onMinusClicked(SeekBar seekBar) {
        //Do nothing as not displayed - implement when displayed as required
    }

    private void updateISOToCamera(final ISO newISO) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(cameraISOKey, newISO, new SetCallback() {
            @Override
            public void onSuccess() {
                //DJILog.d(TAG, "Camera ISO " + newISO.name() + " set successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                seekBarISO.restorePreviousProgress();
                //DJILog.d(TAG, "Failed to set Camera Exposure Mode");
            }
        });
    }

    private void updateEIToCamera(final int newEI) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(eiValueKey, newEI, new SetCallback() {
            @Override
            public void onSuccess() {
                //DJILog.d(TAG, "Camera EI " + newEI + " set successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                seekBarEI.restorePreviousProgress();
                //DJILog.d(TAG, "Failed to set Camera EI: " + error.getDescription());
            }
        });
    }
    //endregion

    //region Reference code
    private void setAutoISO(boolean isAuto) {

        ISO newISO = null;
        if (isAuto) {
            newISO = ISO.AUTO;
        } else {
            if (isoValueArray != null && seekBarISO.getProgress() < isoValueArray.length) {
                newISO = isoValueArray[seekBarISO.getProgress()];
            }
        }
        if (newISO != null) {
            updateISOToCamera(newISO);
        }
    }

    private boolean isAutoISOSupportedByProduct() {
        return (!ProductUtil.isMavicAir())
                && (!ProductUtil.isMavicPro()
                && (!ProductUtil.isMavicMini()));
    }
    //endregion

}