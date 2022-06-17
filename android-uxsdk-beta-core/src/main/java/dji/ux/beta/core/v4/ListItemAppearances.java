package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 42, R.layout.uxsdk_list_item_view);

    private static final ViewAppearance TEXT_ICON = new ViewAppearance(613, -220, 30, 30,
            R.id.list_item_title_icon);
    private static final TextAppearance TEXT = new TextAppearance(604, -214, 120, 18,
            R.id.list_item_title,
            "MidRangeWhiteHotIso", "Roboto-Regular");
    private static final ViewAppearance SUB_ICON = new ViewAppearance(754, -220, 30, 30,
            R.id.list_item_value_icon);
    private static final TextAppearance SUB_TEXT = new TextAppearance(685, -212, 99, 18,
            R.id.list_item_value,
            "MidRangeWhiteHotIso", "Roboto-Regular");
    private static final ViewAppearance ARROW = new ViewAppearance(791, -210, 6, 10,
            R.id.list_item_arrow);
    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -185, 212, 1, R.id.list_item_divider);


    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[]{
            TEXT_ICON, TEXT, SUB_ICON, SUB_TEXT, ARROW, DIVIDER
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
