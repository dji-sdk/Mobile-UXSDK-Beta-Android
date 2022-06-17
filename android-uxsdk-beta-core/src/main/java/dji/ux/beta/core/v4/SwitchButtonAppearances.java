package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

/**
 * Appearance setting for DULSwitchButton
 */
public class SwitchButtonAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(0, 0, 40, 20, R.layout.uxsdk_switch_button);
    private static final ImageAppearance VIEW_LEFT = new ImageAppearance(0, 0, 20, 20, R.id.imageview_left);
    private static final ViewAppearance VIEW = new ViewAppearance(0, 0, 40, 20, R.id.layout_track);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] { VIEW_LEFT, VIEW };

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
