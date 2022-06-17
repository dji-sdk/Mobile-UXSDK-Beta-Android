package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListGroupChildButtonAppearances extends BaseWidgetAppearances {

    private static final TextAppearance CHILD_BUTTON1 = new TextAppearance(1553, 228, 62, 36,
                                                                           R.id.child_button1,
                                                                           "TIFF Linear", "Roboto-Regular");
    private static final TextAppearance CHILD_BUTTON2 = new TextAppearance(1624, 228, 62, 36,
                                                                           R.id.child_button2,
                                                                           "TIFF Linear", "Roboto-Regular");
    private static final TextAppearance CHILD_BUTTON3 = new TextAppearance(1696, 228, 62, 36,
                                                                           R.id.child_button3,
                                                                           "TIFF Linear", "Roboto-Regular");


    private static final ViewAppearance WIDGET = new ViewAppearance(1540, 222, 212, 46, R.layout.uxsdk_expandable_view_child_button);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        CHILD_BUTTON1, CHILD_BUTTON2, CHILD_BUTTON3
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

