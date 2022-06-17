package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.AntiFlickerFrequency;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Widget to select anti-flicker frequency
 */
public class CameraAntiFlickerListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraAntiFlickerListWidget";
    private int[] antiFlickerRange;
    private DJIKey antiFlickerRangeKey;
    private DJIKey antiFlickerKey;
    private DJIKey cameraTypeKey;
    private boolean isZ30Camera;
    private AntiFlickerFrequency currentAntiFlicker;
    private AntiFlickerFrequency mSelectAntiFlicker;
    private AntiFlickerFrequency antiFlickerFrequency;
    private SlidingDialogV4 mSlidingDialog;
    //endregion

    //region Default Constructors
    public CameraAntiFlickerListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraAntiFlickerListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraAntiFlickerListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_anti_flicker_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_anti_flicker_img_array);
        antiFlickerRange = getResources().getIntArray(R.array.uxsdk_camera_anti_flicker_default_value_array);

        initAdapter(antiFlickerRange);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_anti_flick_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        antiFlickerRangeKey = CameraUtil.createCameraKeys(CameraKey.ANTI_FLICKER_RANGE, keyIndex, subKeyIndex);
        antiFlickerKey = CameraUtil.createCameraKeys(CameraKey.ANTI_FLICKER_FREQUENCY, keyIndex, subKeyIndex);
        cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);
        addDependentKey(antiFlickerRangeKey);
        addDependentKey(antiFlickerKey);
        addDependentKey(cameraTypeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(antiFlickerKey)) {
            antiFlickerFrequency = (AntiFlickerFrequency) value;
        } else if (key.equals(antiFlickerRangeKey)) {

            Object[] array = (AntiFlickerFrequency[]) value;
            antiFlickerRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                antiFlickerRange[i] = ((AntiFlickerFrequency) array[i]).value();
            }
        } else if (key.equals(cameraTypeKey)) {
            SettingsDefinitions.CameraType type = (SettingsDefinitions.CameraType) value;
            isZ30Camera = type == SettingsDefinitions.CameraType.DJICameraTypeGD600;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(antiFlickerRangeKey)) {
            initAdapter(antiFlickerRange);
            updateItemSelection(antiFlickerFrequency);
        } else if (key.equals(antiFlickerKey)) {
            updateItemSelection(antiFlickerFrequency);
        }
    }

    private void updateItemSelection(AntiFlickerFrequency antiFlickerFrequency) {
        if (antiFlickerFrequency != null) {
            if (currentAntiFlicker != antiFlickerFrequency) {
                currentAntiFlicker = antiFlickerFrequency;
                int position = adapter.findIndexByValueID(antiFlickerFrequency.value());
                adapter.onItemClick(position);
            }
        }
    }
    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (KeyManager.getInstance() == null) return;
        if (item.getSelected()) {
            return;
        }
        if (antiFlickerRange != null) {
            mSelectAntiFlicker = AntiFlickerFrequency.find(item.valueId);

            if (isZ30Camera) {
                showOperateDlg(item.getTitle());
            } else {
                updateAntiFlicker();
            }
        }

    }

    private void showOperateDlg( String newAntiFlickerName) {
        if (mSlidingDialog != null) {
            if (!mSlidingDialog.isShowing()) {
                mSlidingDialog.setTitleStr(context.getString(R.string.uxsdk_camera_anti_flicker_reset));
                mSlidingDialog.setDesc(context.getString(R.string.uxsdk_camera_anti_flicker_reset_desc, newAntiFlickerName));
                mSlidingDialog.show();
            }
            return;
        }
        SlidingDialogV4.OnEventListener listener = new SlidingDialogV4.OnEventListener() {
            @Override
            public void onRightBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
                updateAntiFlicker();
            }

            @Override
            public void onLeftBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
            }

            @Override
            public void onCbChecked(final DialogInterface dialog, boolean checked, int arg) {

            }
        };
        mSlidingDialog = ViewUtils.showOperateDlg(context, context.getString(R.string.uxsdk_camera_anti_flicker_reset),
                                 context.getString(R.string.uxsdk_camera_anti_flicker_reset_desc, newAntiFlickerName),
                                 listener);
    }

    private void updateAntiFlicker() {
        if (mSelectAntiFlicker == null){
            return;
        }
        KeyManager.getInstance().setValue(antiFlickerKey, mSelectAntiFlicker, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateItemSelection(antiFlickerFrequency);
                    }
                });
                DJILog.d(TAG, "Failed to set camera AntiFlicker " + error.getDescription());
            }
        });
    }
}
