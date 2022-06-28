package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class ExposureSettingAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance EXPOSURE_MODE =
        new ViewAppearance(487, 1250, 159, 30, R.id.widget_camera_exposure_mode);
    private static final ViewAppearance ISO_EI_SETTING =
        new ViewAppearance(462, 1287, 211, 60, R.id.widget_camera_iso_ei_setting);
    private static final ViewAppearance APERTURE_SETTING =
        new ViewAppearance(462, 1356, 211, 60, R.id.widget_camera_aperture_setting);
    private static final ViewAppearance SHUTTER_SETTING =
        new ViewAppearance(462, 1426, 211, 60, R.id.widget_camera_shutter_setting);
    private static final ViewAppearance EV_SETTING =
        new ViewAppearance(462, 1495, 211, 64, R.id.widget_camera_ev_setting);

    private static final ViewAppearance WIDGET =
        new ViewAppearance(462, 1243, 211, 316, R.layout.uxsdk_widget_camera_exposure_setting);
    private static final ViewAppearance BACKGROUND =
        new ViewAppearance(462, 1243, 211, 316, R.id.background_exposure_setting);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        EXPOSURE_MODE, ISO_EI_SETTING, APERTURE_SETTING, SHUTTER_SETTING, EV_SETTING, BACKGROUND
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
