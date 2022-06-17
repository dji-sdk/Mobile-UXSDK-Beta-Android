package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import dji.keysdk.DJIKey;
import dji.ux.beta.core.R;

/**
 * Widget to select grid
 */
public class CameraGridListWidget extends ListViewWidget {

    //region Default Constructors
    public CameraGridListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraGridListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraGridListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_grid_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_grid_img_array);
        int[] gridListArray = getResources().getIntArray(R.array.uxsdk_camera_grid_value_array);

        initAdapter(gridListArray);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_grid_name);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
    }

    @Override
    public void updateWidget(DJIKey key) {
    }
    //endregion

    @Override
    public void updateSelectedItem(ListItem item, View view) {
        int position = adapter.findIndexByItem(item);
        adapter.onItemClick(position);
    }
}
