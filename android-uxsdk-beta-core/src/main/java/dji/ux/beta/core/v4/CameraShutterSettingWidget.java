package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.common.camera.SettingsDefinitions.ExposureState;
import dji.common.camera.SettingsDefinitions.ShutterSpeed;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.functions.Action1;
import dji.thirdparty.rx.schedulers.Schedulers;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Camera Shutter Speed Setting widget.
 */
public class CameraShutterSettingWidget extends FrameLayoutWidget
    implements OnWheelScrollListener, OnWheelChangedListener {

    //region Constant properties
    private static final String TAG = "CameraShutterSettingWidget";
    private static final int DISABLE_ITEM_NUM = 1;
    private static final int ENABLE_ITEM_NUM = 7;
    //endregion

    //region Properties
    private ShutterSettingAppearances widgetAppearances;
    private WheelHorizontalView shutterWheel;
    private ImageView position_mark;
    private DJIKey cameraShutterKey;
    private ShutterSpeed currentShutter;
    private ShutterWheelAdapter adapterShutter;
    private int curShutterPos = 0;
    private boolean wheelScrolling;
    private ExposureMode exposureMode;
    private Object[] shutterValueArray;
    private String[] shutterNameArray;
    private DJIKey currentExposureValueKey;
    private DJIKey exposureModeKey;
    private DJIKey shutterSpeedRangeKey;
    private DJIKey exposureStateKey;
    private ExposureState exposureState;
    private TextView shutterTitle;
    //endregion

    private class ShutterWheelAdapter extends WheelAdapter<String> {

        public ShutterWheelAdapter(Context context, String[] items) {
            super(context, items);
        }

        @Override
        protected CharSequence getItemText(int index) {
            String text = (String) super.getItemText(index);
            if (!(CameraShutterSettingWidget.this.shutterWheel.isEnabled())) {
                if (!text.contains("\"")) {
                    text = "1/" + text + "\"";
                }
            }

            return text;
        }
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ShutterSettingAppearances();
        }
        return widgetAppearances;
    }

    //region Default Constructors
    public CameraShutterSettingWidget(Context context) {
        super(context, null, 0);
    }

    public CameraShutterSettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraShutterSettingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        shutterWheel = (WheelHorizontalView) findViewById(R.id.wheelview_camera_settings_shutter);
        position_mark = (ImageView) findViewById(R.id.imageview_shutter_wheel_position);
        shutterTitle = (TextView) findViewById(R.id.textview_shutter_title);
        shutterNameArray = getResources().getStringArray(R.array.uxsdk_camera_shutter_names);

        updateShutterWheel();
    }
    //endregion

    //region Key life cycle
    @Override
    public void initKey() {

        cameraShutterKey = CameraUtil.createCameraKeys(CameraKey.SHUTTER_SPEED, keyIndex,subKeyIndex);
        currentExposureValueKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_SETTINGS, keyIndex,subKeyIndex);
        exposureModeKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_MODE, keyIndex,subKeyIndex);
        shutterSpeedRangeKey = CameraUtil.createCameraKeys(CameraKey.SHUTTER_SPEED_RANGE, keyIndex,subKeyIndex);
        exposureStateKey = CameraUtil.createCameraKeys(CameraKey.EXPOSURE_STATE, keyIndex,subKeyIndex);

        addDependentKey(currentExposureValueKey);
        addDependentKey(cameraShutterKey);
        addDependentKey(exposureModeKey);
        addDependentKey(shutterSpeedRangeKey);
        addDependentKey(exposureStateKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(exposureModeKey)) {
            exposureMode = (ExposureMode) value;
        } else if (key.equals(cameraShutterKey)) {
            currentShutter = (ShutterSpeed) value;
        } else if (key.equals(currentExposureValueKey)) {
            ExposureSettings exposureParameters = ((ExposureSettings) value);
            currentShutter = exposureParameters.getShutterSpeed();
        } else if (key.equals(shutterSpeedRangeKey)) {
            updateShutterArray((ShutterSpeed[]) value);
        } else if (key.equals(exposureStateKey)) {
            exposureState = (ExposureState) value;
        }
    }

    // Only assign shutterValueArray and shutterNameArray in this method
    // to make sure they are consistent on the index which can mapping each other.
    private void updateShutterArray(Object[] array) {
        shutterValueArray = array;
        String[] newShutter = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            String shutterName = CameraUtil.shutterSpeedDisplayName((ShutterSpeed) array[i]);
            newShutter[i] = shutterName;
        }
        shutterNameArray = newShutter;
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(exposureModeKey)) {
            updateWidgetOnExposureMode();
            updateTitleAndShutterWheelColour();
        } else if (key.equals(currentExposureValueKey) || key.equals(cameraShutterKey)) {
            updateShutterSpeed(currentShutter);
        } else if (key.equals(shutterSpeedRangeKey)) {
            updateShutterWheel();
        } else if (key.equals(exposureStateKey)) {
            updateTitleAndShutterWheelColour();
        }
    }

    private void updateTitleAndShutterWheelColour() {
        if (exposureMode == ExposureMode.SHUTTER_PRIORITY) {
            if (exposureState == ExposureState.UNDEREXPOSED) {
                shutterTitle.setText(R.string.uxsdk_camera_exposure_shutter_underexposed_title);
                adapterShutter.setSelectColor(getResources().getColor(R.color.uxsdk_red));
                adapterShutter.setInterval(curShutterPos, Integer.MAX_VALUE);
                return;
            } else if (exposureState == ExposureState.OVEREXPOSED) {
                shutterTitle.setText(R.string.uxsdk_camera_exposure_shutter_overexposed_title);
                adapterShutter.setSelectColor(getResources().getColor(R.color.uxsdk_red));
                adapterShutter.setInterval(Integer.MIN_VALUE, curShutterPos);
                return;
            }
        }
        shutterTitle.setText(R.string.uxsdk_camera_exposure_shutter_title);
        adapterShutter.setSelectColor(); //Resets to default select colour
        adapterShutter.resetInterval();

    }

    private void updateShutterWheel() {
        adapterShutter = new ShutterWheelAdapter(getContext(), shutterNameArray);
        adapterShutter.setItemResource(R.layout.uxsdk_wheel_item_camera_set_shutter);
        adapterShutter.setItemTextResource(R.id.camera_settings_wheel_text);
        adapterShutter.setCurPos(curShutterPos);

        shutterWheel.removeChangingListener(this);
        shutterWheel.addChangingListener(this);
        shutterWheel.removeScrollingListener(this);
        shutterWheel.addScrollingListener(this);

        shutterWheel.setViewAdapter(adapterShutter);
        shutterWheel.setCurrentItem(curShutterPos);
    }

    private int positionOfShutterSpeed(ShutterSpeed speed) {
        int position = 0;
        if (shutterValueArray != null) {
            for (int i = 0; i < shutterValueArray.length; i++) {
                if (shutterValueArray[i] == speed) {
                    return i;
                }
            }
        } else if (shutterNameArray != null) {
            String displayName = CameraUtil.shutterSpeedDisplayName(speed);
            for (int i = 0; i < shutterNameArray.length; i++) {
                if (shutterNameArray[i].equalsIgnoreCase(displayName)) {
                    return i;
                }
            }
        }

        return position;
    }

    public void updateShutterSpeed(ShutterSpeed shutterSpeed) {
        if (shutterSpeed != null && !wheelScrolling) {
            curShutterPos = positionOfShutterSpeed(shutterSpeed);
            shutterWheel.setCurrentItem(curShutterPos);
            adapterShutter.setCurPos(curShutterPos);
        }
    }

    private void updateWidgetOnExposureMode() {
        if (exposureMode == ExposureMode.PROGRAM || exposureMode == ExposureMode.APERTURE_PRIORITY) {
            shutterWheel.setEnabled(false);
            position_mark.setVisibility(INVISIBLE);
            shutterWheel.setVisibleItems(DISABLE_ITEM_NUM);
        } else if (exposureMode == ExposureMode.SHUTTER_PRIORITY || exposureMode == ExposureMode.MANUAL) {
            shutterWheel.setEnabled(true);
            position_mark.setVisibility(VISIBLE);
            shutterWheel.setVisibleItems(ENABLE_ITEM_NUM);
        }

        // If only one item is shown, just show the text in disable color.
        adapterShutter.setEnable(shutterWheel.getVisibleItems() > 1);
    }
    //endregion

    //region OnWheel listeners

    @Override
    public void onScrollingStarted(AbstractWheel wheel) {
        wheelScrolling = true;
    }

    @Override
    public void onScrollingFinished(final AbstractWheel wheel) {
        wheelScrolling = false;
        Observable.just(true).subscribeOn(Schedulers.computation()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                handleShutterChanged(wheel.getCurrentItem());
            }
        });
    }

    @Override
    public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
        if (wheelScrolling) {
            adapterShutter.setCurPos(newValue);
            shutterWheel.setCurrentItem(newValue);
        }
    }

    private AtomicBoolean canSendCommand = new AtomicBoolean(true);

    private void handleShutterChanged(final int pos) {
        if (KeyManager.getInstance() == null) return;

        if (shutterValueArray != null) {
            AudioUtil.playSimpleSound(getContext());

            curShutterPos = pos;
            final ShutterSpeed newShutter = (ShutterSpeed) shutterValueArray[curShutterPos];
            updateShutterSpeed(newShutter);

            if (canSendCommand.compareAndSet(true, false)) {
                KeyManager.getInstance().setValue(cameraShutterKey, newShutter, new SetCallback() {
                    @Override
                    public void onSuccess() {
                        canSendCommand.set(true);
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        canSendCommand.set(true);
                        post(new Runnable() {
                            @Override
                            public void run() {
                                restoreToCurrentShutter();
                            }
                        });
                        DJILog.d(TAG, "Failed to set Camera shutter: " + error.getDescription());
                    }
                });
            }
        }
    }

    private void restoreToCurrentShutter() {
        if (currentShutter != null) {
            updateShutterSpeed(currentShutter);
        }
    }

    //endregion
}
