package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.CameraSSDVideoLicense;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Widget to select SSD Video license
 */
public class CameraSSDVideoLicenseListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraSSDVideoLicenseListWidget";
    private CameraSSDVideoLicense[] licenseRange;
    private DJIKey ssdVideoLicenseRangeKey;
    private DJIKey ssdVideoLicenseKey;
    private CameraSSDVideoLicense videoLicense,currentVideoLicense;
    //endregion

    //region Default Constructors
    public CameraSSDVideoLicenseListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraSSDVideoLicenseListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraSSDVideoLicenseListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_ssd_license_type);
        licenseRange = CameraSSDVideoLicense.getValues();
        //initAdapter(licenseRange);
    }

    private void initAdapter(CameraSSDVideoLicense[] licenseRange) {
        if (licenseRange == null || licenseRange.length == 0) {
            return;
        }
        
        int[] itemRange = new int[licenseRange.length];
        for (int i = 0; i < licenseRange.length; i++) {
            itemRange[i] = licenseRange[i].value();
        }
        initAdapter(itemRange);
    }


    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_ssd_video_license);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter.onItemClick(0);
    }

    //region Key life cycle
    @Override
    public void initKey() {
        ssdVideoLicenseRangeKey = CameraKey.create(CameraKey.SSD_VIDEO_LICENSES, keyIndex);
        ssdVideoLicenseKey = CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, keyIndex);
        addDependentKey(ssdVideoLicenseRangeKey);
        addDependentKey(ssdVideoLicenseKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(ssdVideoLicenseKey)) {
            videoLicense = (CameraSSDVideoLicense) value;
        } else if (key.equals(ssdVideoLicenseRangeKey)) {
            licenseRange = (CameraSSDVideoLicense[]) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(ssdVideoLicenseRangeKey)) {
            initAdapter(licenseRange);
            updateItemSelection(videoLicense);
        } else if (key.equals(ssdVideoLicenseKey)) {
            updateItemSelection(videoLicense);
        }
    }

    private void updateItemSelection(CameraSSDVideoLicense videoLicense){
        if (videoLicense != null) {
            if(currentVideoLicense!=videoLicense){
                currentVideoLicense=videoLicense;
                int position = adapter.findIndexByValueID(videoLicense.value());
                adapter.onItemClick(position);
            }
        }
    }

    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        int position = adapter.findIndexByItem(item);
        if (licenseRange != null && licenseRange.length > position) {
            final CameraSSDVideoLicense newLicense = licenseRange[position];
            updateItemSelection((newLicense));

            KeyManager.getInstance().setValue(ssdVideoLicenseKey, newLicense, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "SSD Video License " + newLicense.name() + " set successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(videoLicense);
                    DJILog.d(TAG, "Failed to set SSD Video License");
                }
            });
        }
    }
}
