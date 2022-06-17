package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;


/**
 * Created by Robert.Liu on 03/11/2016.
 */

public class ListImageItemView extends ListItemView {

    private ListImageItemAppearances widgetAppearances;

    public ListImageItemView(Context context) {
        super(context, null, 0);
    }

    public ListImageItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListImageItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }


    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListImageItemAppearances();
        }
        return widgetAppearances;
    }
}
