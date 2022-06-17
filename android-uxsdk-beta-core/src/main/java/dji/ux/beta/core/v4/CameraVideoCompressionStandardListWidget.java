package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.VideoFileCompressionStandard;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * This widget shows video compression standards
 */
public class CameraVideoCompressionStandardListWidget extends ListViewWidget {

    //region fields
    private static final String TAG = CameraVideoCompressionStandardListWidget.class.getName();
    private DJIKey cameraVideoCompressionRangeKey;
    private DJIKey cameraVideoCompressionKey;
    private VideoFileCompressionStandard[] videoFileCompressionStandardsRangeArray;
    private VideoFileCompressionStandard videoFileCompressionStandardValue;
    //endregion

    //region Default Constructors
    public CameraVideoCompressionStandardListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraVideoCompressionStandardListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraVideoCompressionStandardListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion


    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_compression_standard_name_array);
        videoFileCompressionStandardsRangeArray = VideoFileCompressionStandard.getValues();

        initAdapter(videoFileCompressionStandardsRangeArray);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }
        disableItemsWhenCameraBusy();
    }

    protected void initAdapter(VideoFileCompressionStandard[] range) {
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
            Integer nameIndex = CameraUtil.getVideoCompressionStandardIndex(range[i]);
            if (nameIndex != null && nameIndex < itemNameArray.length) {
                model.setTitle(itemNameArray[nameIndex]);
                //DJILog.d("LWF", "set title " + itemNameArray[nameIndex]);
            }
            adapter.add(model);
        }

        contentList.setAdapter(adapter);
    }

    @Override
    public void updateSelectedItem(ListItem item, View stateView) {
        int position = adapter.findIndexByItem(item);
        if (videoFileCompressionStandardsRangeArray != null) {
            final VideoFileCompressionStandard newVideoFileCompressionStandard = videoFileCompressionStandardsRangeArray[position];
            updateItemSelection(newVideoFileCompressionStandard);
            KeyManager.getInstance().setValue(cameraVideoCompressionKey, newVideoFileCompressionStandard, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Video Compression Standard " + newVideoFileCompressionStandard.name() + " set successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(videoFileCompressionStandardValue);
                    DJILog.d(TAG, "Failed to set Video Compression Standard");
                }
            });
        }
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_video_compression_standard_label);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        cameraVideoCompressionRangeKey = CameraKey.create(CameraKey.VIDEO_COMPRESSION_STANDARD_RANGE);
        addDependentKey(cameraVideoCompressionRangeKey);
        cameraVideoCompressionKey = CameraKey.create(CameraKey.VIDEO_FILE_COMPRESSION_STANDARD);
        addDependentKey(cameraVideoCompressionKey);

    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(cameraVideoCompressionRangeKey)) {
            videoFileCompressionStandardsRangeArray = (VideoFileCompressionStandard[]) value;
        } else if (key.equals(cameraVideoCompressionKey)) {
            videoFileCompressionStandardValue = (VideoFileCompressionStandard) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(cameraVideoCompressionRangeKey)) {
            initAdapter(videoFileCompressionStandardsRangeArray);
            updateItemSelection(videoFileCompressionStandardValue);
        } else if (key.equals(cameraVideoCompressionKey)) {
            updateItemSelection(videoFileCompressionStandardValue);
        }
    }
    //endregion

    private void updateItemSelection(VideoFileCompressionStandard videoFileCompressionStandard) {
        if (videoFileCompressionStandard != null) {
            int position = adapter.findIndexByValueID(videoFileCompressionStandard.value());
            adapter.onItemClick(position);
        }
    }
}
