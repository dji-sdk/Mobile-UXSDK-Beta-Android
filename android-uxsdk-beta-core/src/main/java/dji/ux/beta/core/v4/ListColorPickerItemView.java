package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import dji.common.bus.UXSDKEventBus;
import dji.ux.beta.core.R;


public class ListColorPickerItemView extends BaseFrameLayout implements View.OnClickListener {

    private ListColorPickerItemAppearances widgetAppearances;
    private static final int[] RES_IDS = {R.id.list_item_white, R.id.list_item_yellow, R.id.list_item_red,
        R.id.list_item_blue, R.id.list_item_green, R.id.list_item_black};
    private TypedArray colorArray;

    public ListColorPickerItemView(Context context) {
        this(context, null, 0);
    }

    public ListColorPickerItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ListColorPickerItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        colorArray = getResources().obtainTypedArray(R.array.uxsdk_camera_center_point_color_array);

        for (int resId : RES_IDS) {
            findViewById(resId).setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {
        for (int i = 0; i < RES_IDS.length; i++) {
            View item = findViewById(RES_IDS[i]);
            if (RES_IDS[i] == v.getId()) {
                item.setSelected(true);
                UXSDKEventBus.getInstance().post(new Events.CenterPointColorEvent(colorArray.getResourceId(i, R.color.uxsdk_white)));
            } else {
                item.setSelected(false);
            }
        }
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListColorPickerItemAppearances();
        }
        return widgetAppearances;
    }
}
