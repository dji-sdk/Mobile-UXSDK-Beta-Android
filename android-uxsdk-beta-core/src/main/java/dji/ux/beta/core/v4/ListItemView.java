package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

public class ListItemView extends BaseFrameLayout {

    private ListItemAppearances widgetAppearances;

    public ListItemView(Context context) {
        super(context, null, 0);
    }

    public ListItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListItemAppearances();
        }
        return widgetAppearances;
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }
}