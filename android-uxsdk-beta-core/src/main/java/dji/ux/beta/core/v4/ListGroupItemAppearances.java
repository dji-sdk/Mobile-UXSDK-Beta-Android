package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListGroupItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET =
        new ViewAppearance(594, -226, 324, 65, R.layout.uxsdk_expandable_view_group);
    private static final ViewAppearance ITEM_VIEW =
        new ViewAppearance(594, -226, 324, 65, R.id.expendable_group_layout);
    private static final ViewAppearance ITEM_ICON =
        new ViewAppearance(613, -220, 30, 50, R.id.expandable_group_icon);
    private static final TextAppearance ITEM_NAME = new TextAppearance(651,
                                                                       -223,
                                                                       220,
                                                                       58,
                                                                       R.id.expandable_group_name,
                                                                       "Save original hyperlapse",
                                                                       "Roboto-Regular");
    private static final TextAppearance ITEM_NAME_SWITCH = new TextAppearance(613,
                                                                       -223,
                                                                       220,
                                                                       58,
                                                                       R.id.expandable_group_name,
                                                                       "Save original hyperlapse",
                                                                       "Roboto-Regular");
    private static final ViewAppearance ITEM_VALUE_BG =
        new ViewAppearance(858, -216, 54, 47, R.id.expandable_group_value_bg);
    private static final TextAppearance ITEM_VALUE =
        new TextAppearance(860, -214, 50, 40, R.id.expandable_group_value, "120.000", "Roboto-Italic");
    private static final ViewAppearance ITEM_SWITCH_BUTTON =
        new ViewAppearance(870, -204, 45, 35, R.id.expandable_group_switch_button);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        ITEM_VIEW, ITEM_ICON, ITEM_NAME, ITEM_VALUE, ITEM_VALUE_BG, ITEM_SWITCH_BUTTON
    };

    private static final Appearance[] ELEMENT_APPEARANCES_SWITCH = new Appearance[] {
        ITEM_VIEW, ITEM_ICON, ITEM_NAME_SWITCH, ITEM_VALUE, ITEM_VALUE_BG, ITEM_SWITCH_BUTTON
    };

    private boolean isSwitchType;

    @NonNull
    @Override
    public ViewAppearance getMainAppearance() {
        return WIDGET;
    }

    @NonNull
    @Override
    public Appearance[] getElementAppearances() {
        if (isSwitchType) {
            return ELEMENT_APPEARANCES_SWITCH;
        } else {
            return ELEMENT_APPEARANCES;
        }
    }

    public void setItemSwitchType(boolean isSwitchType) {
        this.isSwitchType = isSwitchType;
    }
}

