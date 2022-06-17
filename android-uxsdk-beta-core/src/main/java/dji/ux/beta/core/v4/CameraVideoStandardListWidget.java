package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.VideoStandard;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Widget to select video standard
 */
public class CameraVideoStandardListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraVideoStandardListWidget";
    private DJIKey videoStandardKey;
    private int[] videoStandardRange;
    private int currentPosition;
    private SlidingDialogV4 tipDlg;
    //endregion

    //region Default Constructors
    public CameraVideoStandardListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraVideoStandardListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraVideoStandardListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_standard_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_video_standard_img_array);
        videoStandardRange = getResources().getIntArray(R.array.uxsdk_camera_video_standard_default_value_array);

        initAdapter(videoStandardRange);
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
            textTitle.setText(R.string.uxsdk_camera_video_standard_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        videoStandardKey = CameraKey.create(CameraKey.VIDEO_STANDARD, keyIndex);
        addDependentKey(videoStandardKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(videoStandardKey)) {
            VideoStandard videoStandard = (VideoStandard) value;
            currentPosition = adapter.findIndexByValueID(videoStandard.value());
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(videoStandardKey)) {
            adapter.onItemClick(currentPosition);
        }
    }
    //endregion

    private void showIconDlg(final String content, final int index) {
        if (tipDlg != null) {
            if (!tipDlg.isShowing()) {
                tipDlg.show();
            }
            return;
        }
        SlidingDialogV4.OnEventListener listener = new SlidingDialogV4.OnEventListener() {
            @Override
            public void onRightBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
                updateVideoStandardToCamera(index);
            }

            @Override
            public void onLeftBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
            }

            @Override
            public void onCbChecked(final DialogInterface dialog, boolean checked, int arg) {

            }
        };

        ViewUtils.showOperateDlg(context,
                                 context.getString(R.string.uxsdk_camera_video_standard_setting_tip),
                                 context.getString(R.string.uxsdk_camera_video_standard_setting_tip_desc, content),
                                 listener);
    }

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        showIconDlg(itemNameArray[item.valueId], adapter.findIndexByItem(item));
    }

    private void updateVideoStandardToCamera(int position) {
        if (KeyManager.getInstance() == null) return;

        ListItem item = adapter.getItemByPos(position);
        item.setSelected(true);

        if (videoStandardRange != null) {
            final VideoStandard newVideoStandard = VideoStandard.find(item.valueId);
            KeyManager.getInstance().setValue(videoStandardKey, newVideoStandard, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera setting " + newVideoStandard.name() + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    ListItem item = adapter.getItemByPos(currentPosition);
                    item.setSelected(true);
                    DJILog.d(TAG, "Failed to set camera file format");
                }
            });
        }
    }
}
