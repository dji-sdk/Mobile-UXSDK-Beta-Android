package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by Robert.Liu on 10/18/16.
 */

public class ListSeekBarItemView extends BaseFrameLayout {

    private ListSeekBarItemAppearances widgetAppearances;

    public ListSeekBarItemView(Context context) {
        super(context, null, 0);
    }

    public ListSeekBarItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    public ListSeekBarItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListSeekBarItemAppearances();
        }
        return widgetAppearances;
    }
}