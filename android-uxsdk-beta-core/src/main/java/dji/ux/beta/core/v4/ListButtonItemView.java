package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

public class ListButtonItemView extends ListItemView {

    private CameraListButtonItemAppearances widgetAppearances;

    public ListButtonItemView(Context context) {
        super(context, null, 0);
    }

    public ListButtonItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListButtonItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new CameraListButtonItemAppearances();
        }
        return widgetAppearances;
    }
}
