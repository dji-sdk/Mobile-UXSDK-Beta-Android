package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ApertureSettingAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET =
        new ViewAppearance(0, 0, 211, 60, R.layout.uxsdk_widget_aperture_setting);

    private static final TextAppearance APERTURE_TITLE =
        new TextAppearance(8, 0, 44, 10, R.id.textview_aperture_title, "APERTURE", "Roboto-Regular");
    private static final ViewAppearance APERTURE_SETTING_BACKGROUND =
        new ViewAppearance(1, 14, 210, 46, R.id.imageview_camera_settings_aperture_background);
    private static final ViewAppearance APERTURE_WHEEL_VIEW =
        new ViewAppearance(8, 0, 200, 60, R.id.wheelview_camera_settings_aperture); //(8, 26, 200, 20);
    private static final ViewAppearance APERTURE_POSITION_MARK =
        new ViewAppearance(100, 50, 10, 7, R.id.imageview_aperture_wheel_position);

    private static final Appearance[] ELEMENT_APPEARANCES =
        new Appearance[] { APERTURE_TITLE, APERTURE_SETTING_BACKGROUND, APERTURE_WHEEL_VIEW, APERTURE_POSITION_MARK };

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
