package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListEditItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 42, R.layout.uxsdk_list_item_view);

    private static final TextAppearance TEXT = new TextAppearance(604, -214, 85, 18,
                                                                 R.id.list_item_title,
                                                                 "BlackAndWhite", "Roboto-Regular");

    private static final TextAppearance RANGE_TEXT = new TextAppearance(637, -214, 100, 18,
                                                                            R.id.list_item_range,
                                                                 "BlackAndWhite", "Roboto-Regular");
    private static final ViewAppearance EDIT_TEXT = new ViewAppearance(740, -218, 41, 25, R.id.list_item_edittext);

    private static final TextAppearance UNIT_TEXT = new TextAppearance(784, -218, 15, 25,
            R.id.list_item_unit,
            "%", "Roboto-Regular");

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -185, 212, 1, R.id.list_item_divider);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        TEXT, RANGE_TEXT,  EDIT_TEXT, DIVIDER, UNIT_TEXT
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
