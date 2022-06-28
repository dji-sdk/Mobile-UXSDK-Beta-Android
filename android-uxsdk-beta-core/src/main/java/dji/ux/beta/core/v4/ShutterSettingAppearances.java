package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ShutterSettingAppearances extends BaseWidgetAppearances {
    private static final ViewAppearance WIDGET = new ViewAppearance(0, 0, 211, 60, R.layout.uxsdk_widget_shutter_setting);

    private static final TextAppearance SHUTTER_TITLE =
        new TextAppearance(8, 0, 210, 10,R.id.textview_shutter_title, "Shutter (Underexposed)", "Roboto-Regular");
    private static final ViewAppearance SHUTTER_SETTING_BACKGROUND =
        new ViewAppearance(1, 14, 210, 46,R.id.imageview_camera_settings_shutter_background);
    private static final ViewAppearance SHUTTER_WHEEL_VIEW =
        new ViewAppearance(1, 0, 210, 60, R.id.wheelview_camera_settings_shutter); //8, 26, 200, 20);
    private static final ViewAppearance SHUTTER_POSITION_MARK =
        new ViewAppearance(100, 50, 10, 7,R.id.imageview_shutter_wheel_position);

    private static final Appearance[] ELEMENT_APPEARANCES =
        new Appearance[] { SHUTTER_TITLE, SHUTTER_SETTING_BACKGROUND, SHUTTER_WHEEL_VIEW, SHUTTER_POSITION_MARK };

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
