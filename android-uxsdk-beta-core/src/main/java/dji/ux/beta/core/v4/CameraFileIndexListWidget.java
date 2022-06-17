package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.FileIndexMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Widget to reset file numbering
 */
public class CameraFileIndexListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraFileIndexListWidget";
    private int[] fileIndexRange;
    private DJIKey fileIndexKey;
    private int curPosition;
    //endregion

    //region Default Constructors
    public CameraFileIndexListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraFileIndexListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraFileIndexListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_file_index_name_array);
        itemImageIdArray = null;
        fileIndexRange = getResources().getIntArray(R.array.uxsdk_camera_file_index_default_value_array);
        initAdapter(fileIndexRange);
    }
    //endregion

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_file_index_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        fileIndexKey = CameraKey.create(CameraKey.FILE_INDEX_MODE, keyIndex); // getKey
        addDependentKey(fileIndexKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(fileIndexKey)) {
            FileIndexMode fileIndex = (FileIndexMode) value;
            curPosition = adapter.findIndexByValueID(fileIndex.value());
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(fileIndexKey)) {
            adapter.onItemClick(curPosition);
        }
    }

    //endregion

    //region User action
    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (KeyManager.getInstance() == null) return;

        adapter.onItemClick(adapter.findIndexByItem(item));
        if (fileIndexRange != null) {
            final FileIndexMode newFileIndex = FileIndexMode.find(item.valueId);
            KeyManager.getInstance().setValue(fileIndexKey, newFileIndex, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJISDKModelV4.getInstance().getValueOfKey(fileIndexKey, CameraFileIndexListWidget.this);
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    adapter.onItemClick(curPosition);
                    DJILog.d(TAG, "Failed to set camera file format");
                    DJISDKModelV4.getInstance().getValueOfKey(fileIndexKey, CameraFileIndexListWidget.this);
                }
            });
        }
    }
    //endregion
}
