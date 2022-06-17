package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListSeekBarItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 80, R.layout.uxsdk_list_item_view);

    private static final TextAppearance TEXT = new TextAppearance(604, -214, 192, 23,
            R.id.list_item_title,
            "BlackAndWhite", "Roboto-Regular");
    private static final ViewAppearance SEEK_BAR =
            new ViewAppearance(604, -199, 192, 40, R.id.list_item_seek_bar);

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -147, 212, 1, R.id.list_item_divider);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        TEXT, SEEK_BAR, DIVIDER
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
