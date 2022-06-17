package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

/**
 * A switch widget shown in ListView
 */
public class ListSegmentedItemView extends ListItemView {
    //region Properties
    private ListSegmentedItemAppearances widgetAppearances;
    //endregion

    public ListSegmentedItemView(Context context) {
        super(context, null, 0);
    }

    public ListSegmentedItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListSegmentedItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListSegmentedItemAppearances();
        }
        return widgetAppearances;
    }
}
