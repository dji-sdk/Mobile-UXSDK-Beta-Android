package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.Aperture;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Created by Robert on 9/12/16.
 */

public class CameraApertureSettingWidget extends FrameLayoutWidget
        implements OnWheelChangedListener, OnWheelScrollListener {
    //region Constant properites
    private static final String TAG = "CameraApertureSettingWidget";
    private static final int DISABLE_ITEM_NUM = 1;
    private static final int ENABLE_ITEM_NUM = 7;

    //endregion

    //region Properties
    private WheelHorizontalView apertureWheel;
    private ImageView position_mark;
    private Aperture currentAperture;
    private WheelAdapter<String> adapterAperture;
    private int curAperturePos = 0;
    private String[] apertureNameArray;
    private boolean wheelScrolling;
    private DJIKey apertureKey;
    private Object[] apertureValueArray;
    private DJIKey apertureRangeKey;
    private DJIKey exposureModeKey;
    private SettingsDefinitions.ExposureMode cameraExposureMode;
    private DJIKey currentExposureKey;
    private CameraKey cameraTypeKey;
    private boolean isZ30Camera;
    private DJIKey variableApertureSupportedKey;
    private boolean isVariableApertureSupported;
    //endregion

    //region Default Constructors
    public CameraApertureSettingWidget(Context context) {
        super(context, null, 0);
    }

    public CameraApertureSettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraApertureSettingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //endregion
    private ApertureSettingAppearances widgetAppearances;

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ApertureSettingAppearances();
        }
        return widgetAppearances;
    }

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        apertureWheel = (WheelHorizontalView) findViewById(R.id.wheelview_camera_settings_aperture);
        position_mark = (ImageView) findViewById(R.id.imageview_aperture_wheel_position);
        apertureNameArray = getResources().getStringArray(R.array.uxsdk_camera_aperture_array);

        updateApertureWheel();
    }

    //region Key Life cycle
    @Override
    public void initKey() {

        cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);
        apertureKey = CameraKey.create(CameraKey.APERTURE, keyIndex);
        currentExposureKey = CameraKey.create(CameraKey.EXPOSURE_SETTINGS, keyIndex);
        exposureModeKey = CameraKey.create(CameraKey.EXPOSURE_MODE, keyIndex);
        apertureRangeKey = CameraKey.create(CameraKey.APERTURE_RANGE, keyIndex);
        variableApertureSupportedKey = CameraKey.create(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED);
        addDependentKey(variableApertureSupportedKey);
        addDependentKey(cameraTypeKey);
        addDependentKey(apertureKey);
        addDependentKey(exposureModeKey);
        addDependentKey(currentExposureKey);
        addDependentKey(apertureRangeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(exposureModeKey)) {
            cameraExposureMode = (SettingsDefinitions.ExposureMode) value;
        } else if (key.equals(apertureKey)) {
            currentAperture = (Aperture) value;
            //DJILog.d(TAG, "apertureKey value " + currentAperture);
        } else if (key.equals(currentExposureKey)) {
            ExposureSettings exposureParameters = ((ExposureSettings) value);
            currentAperture = exposureParameters.getAperture();
            //DJILog.d(TAG, "currentExposureKey value " + currentAperture);
        } else if (key.equals(apertureRangeKey)) {
            Object[] apertureArray = (Aperture[]) value;
            //DJILog.d(TAG, "apertureArray value length: " + apertureArray.length);
            updateApertureArray(apertureArray);
        } else if (key.equals(cameraTypeKey)) {
            SettingsDefinitions.CameraType type = (SettingsDefinitions.CameraType) value;
            isZ30Camera = type == SettingsDefinitions.CameraType.DJICameraTypeGD600;
        } else if (key.equals(variableApertureSupportedKey)) {
            isVariableApertureSupported = (boolean) value;
            if (!isVariableApertureSupported) {
                apertureValueArray = null;
                apertureNameArray = null;
            }
        }
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    // Only assign apertureValueArray and apertureNameArray in this method
    // to make sure they are consistent on the index which can mapping each other.
    private void updateApertureArray(Object[] array) {
        apertureValueArray = array;
        String[] newAperture = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            newAperture[i] = CameraUtil.apertureDisplayName(getResources(), (Aperture) array[i]);
        }
        apertureNameArray = newAperture;
    }

    public void updateWidget(DJIKey key) {
        if (key.equals(exposureModeKey)) {
            updateWidgetOnExposureMode();
        } else if (key.equals(apertureRangeKey)) {
            updateApertureWheel();
        } else if (key.equals(apertureKey) || key.equals(currentExposureKey)) {
            // If no apertureRange returned, which means there this camera is fixed aperture.
            if (apertureNameArray == null || apertureNameArray.length == 1) {
                updateApertureArray(new Aperture[]{currentAperture});
                updateApertureWheel();
            }
            //No problem at z30. No restrictions
            updateAperture(currentAperture);
        } else if (key.equals(cameraTypeKey)) {
            if (currentAperture == null && isZ30Camera) {
                Object value = KeyManager.getInstance().getValue(apertureKey);
                if (value != null) {
                    currentAperture = (Aperture) value;
                    updateAperture(currentAperture);
                }
            }
        } else if (key.equals(variableApertureSupportedKey)) {
            enableApertureWheel(isVariableApertureSupported);
        }
    }

    private void updateApertureWheel() {
        if (apertureNameArray != null) {
            //DJILog.d("LWF", "updateApertureWheel " + apertureNameArray.length + " array " + apertureNameArray.toString());
            adapterAperture = new WheelAdapter<>(getContext(), apertureNameArray);
            adapterAperture.setItemResource(R.layout.uxsdk_wheel_item_camera_set_aperture);
            adapterAperture.setItemTextResource(R.id.camera_settings_wheel_text);
            adapterAperture.setCurPos(curAperturePos);

            apertureWheel.removeChangingListener(this);
            apertureWheel.addChangingListener(this);
            apertureWheel.removeScrollingListener(this);
            apertureWheel.addScrollingListener(this);

            apertureWheel.setViewAdapter(adapterAperture);
            apertureWheel.setCurrentItem(curAperturePos);
        }

        if (apertureNameArray != null && apertureNameArray.length <= 1) {
            enableApertureWheel(false);
        }
    }

    private void updateWidgetOnExposureMode() {
        if (cameraExposureMode == SettingsDefinitions.ExposureMode.PROGRAM
                || cameraExposureMode == SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY) {
            enableApertureWheel(false);
        } else if (cameraExposureMode == SettingsDefinitions.ExposureMode.APERTURE_PRIORITY
                || cameraExposureMode == SettingsDefinitions.ExposureMode.MANUAL) {
            enableApertureWheel(true);
        }
    }

    private void enableApertureWheel(boolean state) {
        if (state && isVariableApertureSupported) {
            apertureWheel.setEnabled(true);
            position_mark.setVisibility(VISIBLE);
            apertureWheel.setVisibleItems(ENABLE_ITEM_NUM);
        } else {
            apertureWheel.setEnabled(false);
            position_mark.setVisibility(INVISIBLE);
            apertureWheel.setVisibleItems(DISABLE_ITEM_NUM);
        }
        // If only one item is shown, just show the text in disable color.
        adapterAperture.setEnable(apertureWheel.getVisibleItems() > 1);
    }

    private int positionOfAperture(Aperture aperture) {
        int position = 0;
        if (apertureValueArray != null) {
            for (int i = 0; i < apertureValueArray.length; i++) {
                if (apertureValueArray[i] == aperture) {
                    return i;
                }
            }
        } else if (apertureNameArray != null) {
            String displayName = CameraUtil.apertureDisplayName(getResources(), aperture);
            for (int i = 0; i < apertureNameArray.length; i++) {
                if (apertureNameArray[i].equalsIgnoreCase(displayName)) {
                    return i;
                }
            }
        }

        return position;
    }

    public void updateAperture(Aperture aperture) {
        if (!wheelScrolling) {
            //DJILog.d(TAG, "cur aperture to set is " + aperture.name());
            curAperturePos = positionOfAperture(aperture);
            apertureWheel.setCurrentItem(curAperturePos);
            adapterAperture.setCurPos(curAperturePos);
        }
    }
    //endregion

    //region OnWheel listeners

    @Override
    public void onScrollingStarted(AbstractWheel wheel) {
        wheelScrolling = true;
        //DJILog.d(TAG, "Aperture scroll start");
    }

    @Override
    public void onScrollingFinished(AbstractWheel wheel) {
        //DJILog.d(TAG, "Aperture scroll finish");
        wheelScrolling = false;
        handleApertureChanged(wheel.getCurrentItem());
    }

    @Override
    public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
        if (wheelScrolling) {
            adapterAperture.setCurPos(newValue);
            apertureWheel.setCurrentItem(newValue);
            //DJILog.d(TAG, "shutter scroll changed");
        }
    }

    private void handleApertureChanged(final int pos) {
        if (KeyManager.getInstance() == null) return;

        if (apertureValueArray != null && pos < apertureValueArray.length) {
            AudioUtil.playSimpleSound(getContext());

            curAperturePos = pos;
            final Aperture newAperture = (Aperture) apertureValueArray[curAperturePos];
            updateAperture(newAperture);

            KeyManager.getInstance().setValue(apertureKey, newAperture, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera Aperture " + newAperture.name() + " set successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            restoreToCurrentAperture();
                        }
                    });

                    DJILog.d(TAG, "Failed to set Camera Aperture");
                }
            });
        }
    }

    private void restoreToCurrentAperture() {
        if (currentAperture != null) {
            updateAperture(currentAperture);
        }
    }

    //endregion
}
