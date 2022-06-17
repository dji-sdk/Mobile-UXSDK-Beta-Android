package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListSegmentedItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 52, R.layout.uxsdk_list_item_view);

    private static final ViewAppearance SEGMENTED_VIEW = new ViewAppearance(604, -214, 192, 28, R.id.list_item_segmented);

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -175, 212, 1, R.id.list_item_divider);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        SEGMENTED_VIEW, DIVIDER
    };

    @NonNull
    @Override
    public ViewAppearance getMainAppearance() {
        return WIDGET;
    }

    @NonNull
    @Override
    public Appearance[] getElementAppearances() {
        return ELEMENT_APPEARANCES;
    }
}
