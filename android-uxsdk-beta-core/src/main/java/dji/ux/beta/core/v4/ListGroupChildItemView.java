package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

public class ListGroupChildItemView extends BaseFrameLayout {

    private BaseWidgetAppearances widgetAppearances;

    public ListGroupChildItemView(Context context) {
        this(context, null, 0);
    }

    public ListGroupChildItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ListGroupChildItemView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ListGroupChildItemAppearances();
        }
        return widgetAppearances;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: Address this workaround
        // HeightMeasureSpec value should be 0, but it is not correct when measureChild is called
        super.onMeasure(widthMeasureSpec, MeasureSpec.UNSPECIFIED);
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }
}