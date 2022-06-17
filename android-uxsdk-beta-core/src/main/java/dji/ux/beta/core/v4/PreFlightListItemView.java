package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

public class PreFlightListItemView extends BaseFrameLayout {

    private BaseWidgetAppearances widgetAppearances;

    public PreFlightListItemView(Context context) {
        super(context, null, 0);
    }

    public PreFlightListItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new PreCheckItemAppearances();
        }
        return widgetAppearances;
    }

    public PreFlightListItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    public void setButtonVisible(boolean isVisible) {
        ((PreCheckItemAppearances)widgetAppearances).setUseSmallText(isVisible);
    }

    public void setItemEditable(boolean isEditable){
        ((PreCheckItemAppearances)widgetAppearances).setItemEditable(isEditable);
    }

    public void setFullButtonItem(boolean useFullActionButton) {
        ((PreCheckItemAppearances)widgetAppearances).setUseFullButton(useFullActionButton);
    }
}
