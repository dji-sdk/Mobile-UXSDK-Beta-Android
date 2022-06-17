package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by Eleven Chen
 */
public class ListSectionItemView extends BaseFrameLayout {

    private ListSectionItemAppearances widgetAppearances;

    public ListSectionItemView(Context context) {
        super(context, null, 0);
    }

    public ListSectionItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListSectionItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListSectionItemAppearances();
        }
        return widgetAppearances;
    }
}