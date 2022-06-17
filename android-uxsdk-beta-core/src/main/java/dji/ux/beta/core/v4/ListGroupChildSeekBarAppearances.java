package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListGroupChildSeekBarAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance CHILD_SEEK_BAR =
        new ViewAppearance(1560, 220, 210, 35, R.id.expandable_child_sb);

    private static final ViewAppearance WIDGET =
        new ViewAppearance(1540, 222, 212, 42, R.layout.uxsdk_expandable_view_child_seekbar);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] { CHILD_SEEK_BAR };

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
