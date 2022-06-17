package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;


public class PreCheckItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance TITLE_ICON = new ViewAppearance(4572, 3568, 20, 20, R.id.list_item_title_icon);
    private static final TextAppearance TITLE =
        new TextAppearance(4608, 3567, 200, 22, R.id.list_item_title, "Aircraft Battery Temperature", "Roboto-Regular");
    private static final ViewAppearance ACTION_BUTTON = new ViewAppearance(4886, 3558, 98, 40, R.id.action_button);
    // Use the longer place holder string to make the text size is similar with the Title
    private static final TextAppearance STATE_TEXT = new TextAppearance(4876,
                                                                        3567,
                                                                        291,
                                                                        22,
                                                                        R.id.list_item_value,
                                                                        "Aircraft Battery Temperature Temperature",
                                                                        "Roboto-Regular");
    private static final TextAppearance STATE_EDIT_TEXT =
        new TextAppearance(5086, 3562, 85, 34, R.id.list_item_value_editable, "Unknown ", "Roboto-Regular");
    // Even longer to make the text the same size when it's two lines
    private static final TextAppearance STATE_TEXT_LEFT =
        new TextAppearance(4986, 3560, 81, 40, R.id.list_item_value, " (20~5000m) ", "Roboto-Regular", 1);

    private static final TextAppearance STATE_TEXT_SMALL = new TextAppearance(5016,
                                                                              3556,
                                                                              151,
                                                                              44,
                                                                              R.id.list_item_value,
                                                                              "Aircraft Battery Temperature Temperature    ",
                                                                              "Roboto-Regular",
                                                                              2);
    private static final ViewAppearance FULL_ACTION_BUTTON =
        new ViewAppearance(4581, 3558, 601, 40, R.id.list_item_full_button);
    private static final ViewAppearance DIVIDER = new ViewAppearance(4548, 3599, 667, 1, R.id.list_item_divider);
    private static final ViewAppearance WIDGET =
        new ViewAppearance(4548, 3556, 667, 44, R.layout.uxsdk_precheck_list_item_view);
    private static final Appearance[] ELEMENT_APPEARANCES =
        new Appearance[] { TITLE_ICON, TITLE, ACTION_BUTTON, STATE_TEXT, STATE_EDIT_TEXT, FULL_ACTION_BUTTON, DIVIDER };
    private static final Appearance[] ELEMENT_APPEARANCES_SMALL_TEXT =
        new Appearance[] { TITLE_ICON, TITLE, ACTION_BUTTON, STATE_TEXT_SMALL, STATE_EDIT_TEXT, FULL_ACTION_BUTTON, DIVIDER };
    private static final Appearance[] ELEMENT_APPEARANCES_EDITABLE =
        new Appearance[] { TITLE_ICON, TITLE, ACTION_BUTTON, STATE_TEXT_LEFT, STATE_EDIT_TEXT, FULL_ACTION_BUTTON, DIVIDER };
    private static final Appearance[] ELEMENT_APPEARANCES_ONLY_BUTTON =
        new Appearance[] { TITLE_ICON, TITLE, ACTION_BUTTON, STATE_TEXT_LEFT, STATE_EDIT_TEXT, FULL_ACTION_BUTTON, DIVIDER };

    private boolean useSmallText;
    private boolean isEditable;
    private boolean useFullActionButton;

    @NonNull
    @Override
    public ViewAppearance getMainAppearance() {
        return WIDGET;
    }

    @NonNull
    @Override
    public Appearance[] getElementAppearances() {
        if (isEditable) {
            return ELEMENT_APPEARANCES_EDITABLE;
        } else if (useSmallText) {
            return ELEMENT_APPEARANCES_SMALL_TEXT;
        } else if (useFullActionButton) {
            return ELEMENT_APPEARANCES_ONLY_BUTTON;
        } else {
            return ELEMENT_APPEARANCES;
        }
    }

    public void setItemEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    public void setUseSmallText(boolean useSmallText) {
        this.useSmallText = useSmallText;
    }

    public void setUseFullButton(boolean useOnlyFullButton) {
        this.useFullActionButton = useOnlyFullButton;
    }
}
