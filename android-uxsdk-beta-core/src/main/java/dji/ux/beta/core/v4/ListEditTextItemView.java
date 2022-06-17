package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;


/**
 * A switch widget shown in ListView
 */
public class ListEditTextItemView extends ListItemView {
    //region Properties
    private ListEditItemAppearances widgetAppearances;
    //endregion

    public ListEditTextItemView(Context context) {
        super(context, null, 0);
    }

    public ListEditTextItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListEditTextItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListEditItemAppearances();
        }
        return widgetAppearances;
    }
}
