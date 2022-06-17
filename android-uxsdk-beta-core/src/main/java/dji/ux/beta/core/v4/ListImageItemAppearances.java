package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListImageItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 42, R.layout.uxsdk_list_item_view);

    private static final ViewAppearance TEXT_ICON =
        new ViewAppearance(613, -220, 30, 30, R.id.list_item_title_icon);
    private static final TextAppearance TEXT =
        new TextAppearance(651, -214, 100, 18, R.id.list_item_title, "Radiometric JPEG", "Roboto-Regular");
    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -185, 212, 1, R.id.list_item_divider);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        TEXT_ICON, TEXT, DIVIDER
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
