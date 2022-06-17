package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.VideoFileFormat;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Widget to select video format
 */
public class CameraVideoFormatListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraVideoFormatListWidget";
    private int[] videoFormatRange;
    private DJIKey videoFormatRangeKey;
    private DJIKey videoFormatKey;
    private VideoFileFormat videoFormat, currentVideoFormat;
    //endregion

    //region Default Constructors
    public CameraVideoFormatListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraVideoFormatListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraVideoFormatListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_format_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_video_format_img_array);
        videoFormatRange = getResources().getIntArray(R.array.uxsdk_camera_video_format_default_value_array);

        initAdapter(videoFormatRange);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        disableItemsWhenCameraBusy();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_video_format_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        videoFormatRangeKey = CameraUtil.createCameraKeys(CameraKey.VIDEO_FILE_FORMAT_RANGE, keyIndex, subKeyIndex);
        videoFormatKey = CameraUtil.createCameraKeys(CameraKey.VIDEO_FILE_FORMAT, keyIndex, subKeyIndex);
        addDependentKey(videoFormatRangeKey);
        addDependentKey(videoFormatKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(videoFormatKey)) {
            videoFormat = (VideoFileFormat) value;
        } else if (key.equals(videoFormatRangeKey)) {
            Object[] array = (VideoFileFormat[]) value;
            videoFormatRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                videoFormatRange[i] = ((VideoFileFormat) array[i]).value();
            }
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(videoFormatRangeKey)) {
            initAdapter(videoFormatRange);
            updateItemSelection(videoFormat);
        } else if (key.equals(videoFormatKey)) {

            updateItemSelection(videoFormat);

        }
    }

    private void updateItemSelection(VideoFileFormat videoFormat) {
        if (videoFormat != null) {
            if (currentVideoFormat != videoFormat) {
                currentVideoFormat = videoFormat;
                int position = adapter.findIndexByValueID(videoFormat.value());
                adapter.onItemClick(position);
            }
        }
    }
    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        if (KeyManager.getInstance() == null) return;

        if (videoFormatRange != null) {
            final VideoFileFormat newVideoFormat = VideoFileFormat.find(item.valueId);
            updateItemSelection(newVideoFormat);
            KeyManager.getInstance().setValue(videoFormatKey, newVideoFormat, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera setting " + newVideoFormat.name() + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(videoFormat);
                    DJILog.d(TAG, "Failed to set camera file format");
                }
            });
        }
    }
}
