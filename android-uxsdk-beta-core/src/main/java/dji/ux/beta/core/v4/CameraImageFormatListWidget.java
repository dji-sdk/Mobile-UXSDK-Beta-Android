package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.PhotoFileFormat;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Widget to pick image format
 */
public class CameraImageFormatListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraImageFormatListWidget";
    private int[] fileFormatRange;
    private DJIKey fileFormatRangeKey;
    private DJIKey fileFormatKey;
    private PhotoFileFormat fileFormat, currentFileFormat;
    //endregion

    //region Default Constructors
    public CameraImageFormatListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraImageFormatListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraImageFormatListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_picture_format_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_picture_format_img_res_array);
        fileFormatRange = getResources().getIntArray(R.array.uxsdk_camera_picture_format_value_array);

        initAdapter(fileFormatRange);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }
        disableItemsWhenCameraBusy();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_image_format_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        fileFormatRangeKey = CameraKey.create(CameraKey.PHOTO_FILE_FORMAT_RANGE, keyIndex);
        fileFormatKey = CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, keyIndex);
        addDependentKey(fileFormatRangeKey);
        addDependentKey(fileFormatKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(fileFormatKey)) {
            fileFormat = (PhotoFileFormat) value;
        } else if (key.equals(fileFormatRangeKey)) {
            updateFileFormatRange((PhotoFileFormat[]) value);
        }
    }

    private void updateFileFormatRange(PhotoFileFormat[] array) {
        if (array == null || array.length <= 0) {
            return;
        }

        boolean isThermalCamera = isThermalCamera();
        List<Integer> ranges = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            // New Firmware do not support these two type anymore
            if (isThermalCamera && (array[i] == PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION
                || array[i] == PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION)) {
                continue;
            }
            ranges.add((array[i]).value());
        }
        fileFormatRange = new int[ranges.size()];
        for (int j = 0; j < fileFormatRange.length; ++j) {
            fileFormatRange[j] = ranges.get(j);
        }
    }

    private boolean isThermalCamera() {
        DJIKey key = CameraKey.create(CameraKey.IS_THERMAL_CAMERA, keyIndex);
        if (KeyManager.getInstance() != null) {
            Object value = KeyManager.getInstance().getValue(key);
            return value != null && (Boolean) value;
        }
        return false;
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(fileFormatRangeKey)) {
            initAdapter(fileFormatRange);
            updateItemSelection(fileFormat);
        } else if (key.equals(fileFormatKey)) {
            updateItemSelection(fileFormat);
        }
    }

    private void updateItemSelection(PhotoFileFormat fileFormat) {
        if (fileFormat != null) {
            if (currentFileFormat != fileFormat) {
                currentFileFormat = fileFormat;
                int position = adapter.findIndexByValueID(fileFormat.value());
                adapter.onItemClick(position);
            }
        }
    }
    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (fileFormatRange != null) {
            final PhotoFileFormat newFileFormat = PhotoFileFormat.find(item.valueId);
            updateItemSelection(newFileFormat);
            KeyManager.getInstance().setValue(fileFormatKey, newFileFormat, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera setting " + newFileFormat.name() + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(fileFormat);
                    DJILog.d(TAG, "Failed to set camera file format");
                }
            });
        }
    }
}
