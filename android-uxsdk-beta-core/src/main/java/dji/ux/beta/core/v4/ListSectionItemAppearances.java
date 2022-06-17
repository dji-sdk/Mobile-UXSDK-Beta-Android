package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListSectionItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 30, R.layout.uxsdk_list_item_view);

    private static final TextAppearance TEXT = new TextAppearance(604, -214, 85, 18,
            R.id.list_item_title,
            "Thermal Camera", "Roboto-Regular");
    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -197, 212, 1, R.id.list_item_divider);


    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[]{
            TEXT, DIVIDER
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
