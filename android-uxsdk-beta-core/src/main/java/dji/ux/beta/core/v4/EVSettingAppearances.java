package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class EVSettingAppearances extends BaseWidgetAppearances {
    private static final ViewAppearance WIDGET =
        new ViewAppearance(0, 0, 211, 60, R.layout.uxsdk_widget_ev_setting);
    private static final TextAppearance EV_TITLE =
        new TextAppearance(8, 0, 21, 10, R.id.textview_ev_title, "EV", "Roboto-Regular");
    private static final ViewAppearance EV_SETTING_BACKGROUND =
        new ViewAppearance(1, 14, 210, 46, R.id.imageview_camera_settings_ev_background);
    private static final ViewAppearance EV_MINUS =
        new ViewAppearance(40, 25, 28, 28, R.id.imagebutton_ev_setting_minus);
    private static final TextAppearance EV_VALUE =
        new TextAppearance(80, 27, 50, 19, R.id.textview_setting_ev_value, "+0.7", "Roboto-Medium");
    private static final ViewAppearance EV_PLUS =
        new ViewAppearance(143, 25, 28, 28, R.id.imagebutton_ev_setting_plus);
    private static final TextAppearance EV_VALUE_STATUS =
        new TextAppearance(95, 25, 20, 11, R.id.textview_setting_ev_status_value, "+0.7", "Roboto-Regular");
    private static final ViewAppearance EV_STATUS_VIEW =
        new ViewAppearance(63, 40, 84, 10, R.id.stripeview_setting_ev_status);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        EV_SETTING_BACKGROUND, EV_TITLE, EV_MINUS, EV_VALUE, EV_PLUS, EV_VALUE_STATUS, EV_STATUS_VIEW
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
