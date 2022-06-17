package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListGroupChildItemAppearances extends BaseWidgetAppearances {

    private static final TextAppearance CHILD_VALUE1 = new TextAppearance(1553, 223, 44, 40,
            R.id.child_value1,
            "119.880", "Roboto-Regular");
    private static final TextAppearance CHILD_TAG1 = new TextAppearance(1553, 253, 44, 14, R.id.child_tag1, "SLOW",
                                                                        "Roboto-Regular");

    private static final TextAppearance CHILD_VALUE2 = new TextAppearance(1624, 223, 44, 40,
            R.id.child_value2,
            "119.880", "Roboto-Regular");
    private static final TextAppearance CHILD_TAG2 = new TextAppearance(1624, 253, 44, 14, R.id.child_tag2, "SLOW",
                                                                        "Roboto-Regular");

    private static final TextAppearance CHILD_VALUE3 = new TextAppearance(1696, 223, 44, 40,
            R.id.child_value3,
            "119.880", "Roboto-Regular");
    private static final TextAppearance CHILD_TAG3 = new TextAppearance(1696, 253, 44, 14, R.id.child_tag3, "SLOW",
                                                                        "Roboto-Regular");
    private static final ViewAppearance WIDGET = new ViewAppearance(1540, 222, 212, 46, R.layout.uxsdk_expandable_view_child);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        CHILD_VALUE1, CHILD_TAG1, CHILD_VALUE2, CHILD_TAG2, CHILD_VALUE3, CHILD_TAG3
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
