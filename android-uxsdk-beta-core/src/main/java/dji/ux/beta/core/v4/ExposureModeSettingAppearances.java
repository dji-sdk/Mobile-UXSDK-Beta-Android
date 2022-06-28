package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ExposureModeSettingAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET =
        new ViewAppearance(8, 1250, 160, 30, R.layout.uxsdk_widget_exposure_mode_setting_v4);

    private static final ViewAppearance AUTO_MODE = new ViewAppearance(8, 1250, 40, 30, R.id.layout_camera_mode_p);
    private static final TextAppearance AUTO_TEXT =
        new TextAppearance(17, 1254, 21, 9, R.id.textview_camera_mode_p, "AUTO", "Roboto-Medium");
    private static final ImageAppearance AUTO_ICON =
        new ImageAppearance(20, 1264, 14, 11, R.id.imageview_exposure_automode_icon);

    private static final ViewAppearance S_MODE = new ViewAppearance(48, 1250, 40, 30, R.id.layout_camera_mode_s);
    private static final TextAppearance S_TEXT =
        new TextAppearance(22, 1255, 11, 20, R.id.textview_camera_mode_s, "S", "Roboto-Regular");

    private static final ViewAppearance A_MODE = new ViewAppearance(88, 1250, 40, 30, R.id.layout_camera_mode_a);
    private static final TextAppearance A_TEXT =
        new TextAppearance(22, 1255, 11, 20, R.id.textview_camera_mode_a, "A", "Roboto-Regular");

    private static final ViewAppearance M_MODE = new ViewAppearance(128, 1250, 40, 30, R.id.layout_camera_mode_m);
    private static final TextAppearance M_TEXT =
        new TextAppearance(22, 1255, 15, 20, R.id.textview_camera_mode_m, "M", "Roboto-Regular");

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        AUTO_MODE, AUTO_TEXT, AUTO_ICON, S_MODE, S_TEXT, A_MODE, A_TEXT, M_MODE, M_TEXT
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
