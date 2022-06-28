package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ISOAndEISettingAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(0, 0, 211, 60, R.layout.uxsdk_widget_iso_ei_setting_v4);

    private static final TextAppearance ISO_TITLE =
        new TextAppearance(8, 0, 16, 10,R.id.textview_iso_title,  "ISO", "Roboto-Regular");
    private static final ViewAppearance ISO_SEEK_BAR_BACKGROUND =
        new ViewAppearance(0, 14, 211, 46, R.id.seekbar_background);
    private static final ViewAppearance ISO_SEEK_BAR_LAYOUT =
        new ViewAppearance(0, 14, 211, 46, R.id.seekbar_layout);
    private static final ViewAppearance EI_SEEK_BAR =
        new ViewAppearance(0, 14, 210, 46, R.id.seekbar_ei);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        ISO_TITLE, ISO_SEEK_BAR_BACKGROUND, ISO_SEEK_BAR_LAYOUT, EI_SEEK_BAR
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