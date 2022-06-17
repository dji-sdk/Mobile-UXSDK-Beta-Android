package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListEditBigItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 100, R.layout.uxsdk_list_item_view);

    private static final TextAppearance TEXT = new TextAppearance(604, -218, 200, 38,
                                                                  R.id.list_item_title,
                                                                  "Atmosphere Transmission Coefficient",
                                                                  "Roboto-Regular", 2);

    private static final TextAppearance RANGE_TEXT = new TextAppearance(604, -180, 122, 46,
                                                                        R.id.list_item_range_big,
                                                                        "50~(100-Window Transmission Coefficient)",
                                                                        "Roboto-Regular", 2);
    private static final TextAppearance EDIT_TEXT = new TextAppearance(730, -180, 50, 25, R.id.list_item_edittext,
                                                                       "100.00", "Roboto-Regular");

    private static final TextAppearance UNIT_TEXT = new TextAppearance(784, -180, 15, 25,
            R.id.list_item_unit,
            "%", "Roboto-Regular");

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -127, 212, 1, R.id.list_item_divider);
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
