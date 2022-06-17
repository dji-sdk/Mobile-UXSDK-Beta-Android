package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by Luca.Wu on 06/14/2017.
 */

public class ListTextItemView extends ListItemView {

    private CameraListTextItemAppearances widgetAppearances;

    public ListTextItemView(Context context) {
        super(context, null, 0);
    }

    public ListTextItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListTextItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new CameraListTextItemAppearances();
        }
        return widgetAppearances;
    }
}
