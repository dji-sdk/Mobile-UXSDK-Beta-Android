package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ListGroupChildImageAppearances extends BaseWidgetAppearances {

    private static final ImageAppearance CHILD_IMAGE1 = new ImageAppearance(1553, 228, 44, 40, R.id.child_image1);
    private static final ImageAppearance CHILD_IMAGE2 = new ImageAppearance(1627, 228, 44, 40, R.id.child_image2);
    private static final ImageAppearance CHILD_IMAGE3 = new ImageAppearance(1701, 228, 44, 40, R.id.child_image3);

    private static final ViewAppearance WIDGET = new ViewAppearance(1540, 222, 212, 46, R.layout.uxsdk_expandable_view_child_image);
    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        CHILD_IMAGE1, CHILD_IMAGE2, CHILD_IMAGE3
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
