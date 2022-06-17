package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.CameraColor;
import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.common.camera.SettingsDefinitions.EIColor;
import dji.common.camera.SettingsDefinitions.ExposureSensitivityMode;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Widget to select filter for images
 */
public class CameraFilterListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraFilterListWidget";
    private int[] filterRange;
    private DJIKey filterRangeKey;
    private DJIKey filterKey;
    private CameraColor cameraColor;
    private DJIKey ssdEnabledKey;
    private boolean isSSDEnabled;
    private WeakReference<TextView> titleView;
    private CameraKey eiModeKey;
    private CameraKey eiColorKey;
    private String[] eiColorNameArray;
    private boolean isEIEnabled;
    private EIColor eiColor;
    private CameraKey cameraModeKey;
    private CameraMode cameraMode;
    private DJIKey flatCameraModeKey;
    private FlatCameraMode flatCameraMode;
    //endregion

    //region Default Constructors
    public CameraFilterListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraFilterListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraFilterListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_filter_type);
        eiColorNameArray = getResources().getStringArray(R.array.uxsdk_camera_ei_color_type);

        filterRange = getResources().getIntArray(R.array.uxsdk_camera_filter_default);

        initAdapter(filterRange);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        titleView = new WeakReference<>(textTitle);
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_filter);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //TODO: toBeSelectedItem = adapter.getItemByValueId(CameraDigitalFilter.None.value());
        adapter.onItemClick(0);
    }

    protected void initEIColorAdapter(EIColor[] range) {
        if (range == null) {
            return;
        }

        if (itemNameArray == null) {
            return;
        }

        adapter = new RecyclerAdapter(this);

        for (int i = 0; i < range.length; i++) {
            ListItem model = new ListItem();
            model.valueId = range[i].value();
            if (i < eiColorNameArray.length) {
                model.setTitle(eiColorNameArray[i]);
            }
            adapter.add(model);
        }

        contentList.setAdapter(adapter);
    }

    //region Key life cycle
    @Override
    public void initKey() {
        filterRangeKey = CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR_RANGE, keyIndex, subKeyIndex);
        filterKey = CameraUtil.createCameraKeys(CameraKey.CAMERA_COLOR, keyIndex, subKeyIndex);
        ssdEnabledKey = CameraKey.create(CameraKey.SSD_VIDEO_RECORDING_ENABLED, keyIndex);
        eiModeKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, keyIndex);
        eiColorKey = CameraKey.create(CameraKey.EI_COLOR, keyIndex);
        cameraModeKey = CameraKey.create(CameraKey.MODE, keyIndex);
        flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, keyIndex);

        addDependentKey(filterRangeKey);
        addDependentKey(filterKey);
        addDependentKey(ssdEnabledKey);
        addDependentKey(eiModeKey);
        addDependentKey(eiColorKey);
        addDependentKey(cameraModeKey);
        addDependentKey(flatCameraModeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(filterKey)) {
            cameraColor = (CameraColor) value;
            //DJILog.d("LWF", "transformValue filterKey is " + value);
        } else if (key.equals(filterRangeKey)) {
            Object[] array = (CameraColor[]) value;
            filterRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                filterRange[i] = ((CameraColor) array[i]).value();
            }
        } else if (key.equals(ssdEnabledKey)) {
            isSSDEnabled = (boolean) value;
        } else if (key.equals(eiModeKey)) {
            isEIEnabled = (value == ExposureSensitivityMode.EI);
        } else if (key.equals(eiColorKey)) {
            eiColor = (EIColor) value;
        } else if (key.equals(cameraModeKey)) {
            cameraMode = (CameraMode) value;
        } else if (key.equals(flatCameraModeKey)) {
            flatCameraMode = (FlatCameraMode) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(filterRangeKey)) {
            updateColorAdapter();
            updateSelection();
        } else if (key.equals(ssdEnabledKey)) {
            updateViewTitle();
            updateSelection();
        } else if (key.equals(eiModeKey)) {
            updateColorAdapter();
            updateSelection();
        } else if (key.equals(filterKey) || key.equals(eiColorKey)) {
            updateSelection();
        } else if (key.equals(cameraModeKey)) {
            updateViewTitle();
        } else if (key.equals(flatCameraModeKey)) {
            updateViewTitle();
        }
    }

    private void updateSelection() {
        if (isSSDEnabled && isEIEnabled) {
            updateEIItemSelection(eiColor);
        } else {
            updateItemSelection(cameraColor);
        }
    }

    private void updateColorAdapter() {
        if (isEIEnabled) {
            EIColor[] eiColorRange = EIColor.getValues();
            initEIColorAdapter(eiColorRange);
            updateEIItemSelection(eiColor);
        } else {
            initAdapter(filterRange);
            updateItemSelection(cameraColor);
        }
    }

    private void updateEIItemSelection(EIColor eiColor) {
        if (eiColor != null) {
            int position = adapter.findIndexByValueID(eiColor.value());
            //DJILog.d("LWF", "Select EI item " + eiColor + " at position " + position);
            adapter.onItemClick(position);
        }
    }


    private void updateItemSelection(CameraColor cameraColor) {
        if (cameraColor != null) {
            int position = adapter.findIndexByValueID(cameraColor.value());
            //DJILog.d("LWF", "Select Color item " + cameraColor + " at position " + position);
            adapter.onItemClick(position);
        }
    }

    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {

        if (isEIEnabled && isSSDEnabled) {
            updateSelectedEIColorItem(view, adapter.findIndexByItem(item));
        } else {
            updateSelectedColorItem(view, adapter.findIndexByItem(item));
        }
    }

    private void updateSelectedEIColorItem(View view, int position) {
        EIColor[] eiColorRange = EIColor.getValues();
        final EIColor newEIColor = eiColorRange[position];
        updateEIItemSelection(newEIColor);
        KeyManager.getInstance().setValue(eiColorKey, newEIColor, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera EI Color " + newEIColor.name() + " set successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                updateEIItemSelection(eiColor);
                DJILog.d(TAG, "Failed to set EI Color");
            }
        });

    }

    private void updateSelectedColorItem(View view, int position) {
        if (filterRange != null) {
            final CameraColor newFilter = CameraColor.find(filterRange[position]);
            updateItemSelection(newFilter);
            KeyManager.getInstance().setValue(filterKey, newFilter, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera ISO " + newFilter.name() + " set successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(cameraColor);
                    DJILog.d(TAG, "Failed to set Camera Exposure Mode");
                }
            });
        }
    }

    private void updateViewTitle() {
        int titleId = R.string.uxsdk_camera_filter;
        if (isSSDEnabled && (cameraMode == CameraMode.RECORD_VIDEO || !CameraUtil.isPictureMode(flatCameraMode))) {
            titleId = R.string.uxsdk_camera_looks;
        }

        if (titleView != null && titleView.get() != null) {
            titleView.get().setText(titleId);
        }
    }

}
