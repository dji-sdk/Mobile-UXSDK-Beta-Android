package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.SSDColor;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

/**
 * Widget to select SSD Color for images
 */
public class CameraSSDColorListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraSSDColorListWidget";
    private SSDColor[] ssdColorValueArray;
    private DJIKey ssdColorRangeKey;
    private DJIKey ssdColorKey;
    private SSDColor ssdColor;
    //endregion

    //region Default Constructors
    public CameraSSDColorListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraSSDColorListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraSSDColorListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_ssd_color_array);
        ssdColorValueArray = SSDColor.getValues();
        initAdapter(ssdColorValueArray);
    }

    protected void initAdapter(SSDColor[] range) {
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
            Integer nameIndex = CameraUtil.getSSDColorIndex(range[i]);
            if (nameIndex != null && nameIndex < itemNameArray.length) {
                model.setTitle(itemNameArray[nameIndex]);
                //DJILog.d("LWF", "set title " + itemNameArray[nameIndex]);
            }
            adapter.add(model);
        }

        contentList.setAdapter(adapter);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_ssd_color_title);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        ssdColorRangeKey = CameraKey.create(CameraKey.SSD_COLOR_RANGE);
        ssdColorKey = CameraKey.create(CameraKey.SSD_COLOR);
        addDependentKey(ssdColorRangeKey);
        addDependentKey(ssdColorKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(ssdColorKey)) {
            ssdColor = (SSDColor) value;
            //DJILog.d("LWF", "ssdColor is " + ssdColor);
        } else if (key.equals(ssdColorRangeKey)) {
            ssdColorValueArray = (SSDColor[]) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(ssdColorRangeKey)) {
            initAdapter(ssdColorValueArray);
            updateItemSelection(ssdColor);
        } else if (key.equals(ssdColorKey)) {
            updateItemSelection(ssdColor);
        }
    }

    private void updateItemSelection(SSDColor ssdColor) {
        if (ssdColor != null) {
            int position = adapter.findIndexByValueID(ssdColor.value());
            adapter.onItemClick(position);
        }
    }

    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        int position = adapter.findIndexByItem(item);
        if (ssdColorValueArray != null) {
            final SSDColor newSSDColor = ssdColorValueArray[position];
            updateItemSelection(newSSDColor);
            KeyManager.getInstance().setValue(ssdColorKey, newSSDColor, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera SSDColor " + newSSDColor.name() + " set successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(ssdColor);
                    DJILog.d(TAG, "Failed to set SSDColor");
                }
            });
        }
    }

}
