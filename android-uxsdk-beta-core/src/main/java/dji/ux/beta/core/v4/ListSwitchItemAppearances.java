package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListSwitchItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 242, 42, R.layout.uxsdk_list_item_view);

    private static final TextAppearance TEXT = new TextAppearance(604, -214, 175, 18,
                                                                  R.id.list_item_title,
                                                                  "Lock Gimbal While Shooting", "Roboto-Regular");
    private static final ViewAppearance SWITCH_BUTTON = new ViewAppearance(790, -216, 41, 21,
                                                                           R.id.list_item_value_switch_button);
    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -185, 242, 1, R.id.list_item_divider);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        TEXT, SWITCH_BUTTON, DIVIDER
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
