package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListColorPickerItemAppearances extends BaseWidgetAppearances {
    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 82, R.layout.uxsdk_list_color_picker_item_view);
    private static final TextAppearance TEXT = new TextAppearance(604, -214, 137, 18,
                                                                  R.id.list_item_title,
                                                                  "Center Point Color", "Roboto-Regular");

    private static final ImageAppearance COLOR_WHITE = new ImageAppearance(604, -184, 27, 27,
                                                                      R.id.list_item_white);
    private static final ImageAppearance COLOR_YELLOW = new ImageAppearance(636, -184, 27, 27,
                                                                      R.id.list_item_yellow);
    private static final ImageAppearance COLOR_RED = new ImageAppearance(668, -184, 27, 27,
                                                                         R.id.list_item_red);
    private static final ImageAppearance COLOR_BLUE = new ImageAppearance(700, -184, 27, 27,
                                                                          R.id.list_item_blue);
    private static final ImageAppearance COLOR_GREEN = new ImageAppearance(732, -184, 27, 27,
                                                                           R.id.list_item_green);
    private static final ImageAppearance COLOR_BLACK = new ImageAppearance(764, -184, 27, 27,
                                                                           R.id.list_item_black);

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -155, 212, 1, R.id.list_item_divider);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] { TEXT, COLOR_WHITE, COLOR_YELLOW,
        COLOR_RED, COLOR_BLUE, COLOR_GREEN, COLOR_BLACK, DIVIDER };

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
