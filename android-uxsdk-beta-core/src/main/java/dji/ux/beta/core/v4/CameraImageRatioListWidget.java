package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.PhotoAspectRatio;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Widget to select image ratio
 */
public class CameraImageRatioListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraImageRatioListWidget";
    private int[] photoRatioRange;
    private DJIKey photoAspectRatioRangeKey;
    private DJIKey photoRatioKey;
    private PhotoAspectRatio photoAspectRatio, currentPhotoAspectRatio;
    //endregion

    //region Default Constructors
    public CameraImageRatioListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraImageRatioListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraImageRatioListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_photo_aspect_ratio_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_photo_aspect_ratio_img_array);
        photoRatioRange = getResources().getIntArray(R.array.uxsdk_camera_photo_aspect_ratio_default_value_array);

        initAdapter(photoRatioRange);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_image_ratio_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        photoAspectRatioRangeKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_ASPECT_RATIO, keyIndex, subKeyIndex);
        photoRatioKey = CameraUtil.createCameraKeys(CameraKey.PHOTO_ASPECT_RATIO, keyIndex, subKeyIndex);
        addDependentKey(photoAspectRatioRangeKey);
        addDependentKey(photoRatioKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(photoRatioKey)) {
            photoAspectRatio = (PhotoAspectRatio) value;
        } else if (key.equals(photoAspectRatioRangeKey)) {
            Object[] array = (PhotoAspectRatio[]) value;
            photoRatioRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                photoRatioRange[i] = ((PhotoAspectRatio) array[i]).value();
            }
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(photoAspectRatioRangeKey)) {
            initAdapter(photoRatioRange);
            // Update selection since range changed.
            updateItemSelection(photoAspectRatio);
        } else if (key.equals(photoRatioKey)) {

            updateItemSelection(photoAspectRatio);

        }
    }

    private void updateItemSelection(PhotoAspectRatio photoAspectRatio) {
        if (photoAspectRatio != null) {
            if (currentPhotoAspectRatio != photoAspectRatio) {
                currentPhotoAspectRatio = photoAspectRatio;
                int position = adapter.findIndexByValueID(photoAspectRatio.value());
                adapter.onItemClick(position);
            }
        }
    }
    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {

        if (KeyManager.getInstance() == null) return;

        if (photoRatioRange != null) {
            final PhotoAspectRatio newPhotoRatio = PhotoAspectRatio.find(item.valueId);
            updateItemSelection(newPhotoRatio);
            KeyManager.getInstance().setValue(photoRatioKey, newPhotoRatio, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera setting " + newPhotoRatio.name() + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    DJILog.d(TAG, "Failed to set camera file format");
                    updateItemSelection(photoAspectRatio);
                }
            });
        }
    }
}
