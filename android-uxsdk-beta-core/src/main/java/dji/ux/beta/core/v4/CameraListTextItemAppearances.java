package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class CameraListTextItemAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(594, -226, 212, 42, R.layout.uxsdk_list_item_view);
    private static final TextAppearance TEXT = new TextAppearance(604, -219, 200, 28,
                                                                 R.id.list_item_title,
                                                                 "Profile:  640, <9hz, 13mm, Advanced,",
                                                                  "Roboto-Regular");

    private static final ViewAppearance DIVIDER = new ViewAppearance(594, -185, 212, 1, R.id.list_item_divider);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] { TEXT, DIVIDER };

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
