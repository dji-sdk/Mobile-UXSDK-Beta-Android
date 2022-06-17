package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;


/**
 * A switch widget shown in ListView
 */
public class ListSwitchItemView extends ListItemView {
    //region Properties
    private ListSwitchItemAppearances widgetAppearances;
    //endregion

    public ListSwitchItemView(Context context) {
        super(context, null, 0);
    }

    public ListSwitchItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListSwitchItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListSwitchItemAppearances();
        }
        return widgetAppearances;
    }
}
