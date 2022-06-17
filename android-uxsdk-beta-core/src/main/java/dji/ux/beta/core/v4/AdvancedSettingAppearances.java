package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class AdvancedSettingAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET =
        new ViewAppearance(597, -304, 212, 316, R.layout.uxsdk_widget_camera_advanced_setting);

    private static final ViewAppearance TITLE_BAR =
        new ViewAppearance(597, -304, 212, 35, R.id.camera_setting_title_bar);
    private static final ViewAppearance TITLE_ARROW = new ViewAppearance(598, -303, 60, 34, R.id.imageview_back);
    private static final TextAppearance TITLE =
        new TextAppearance(662, -296, 111, 20, R.id.textview_title, "SD/Liveview Looks", "Roboto-Regular");

    private static final ViewAppearance TAB_BAR = new ViewAppearance(597, -304, 212, 35, R.id.camera_advsetting_tab);
    private static final ViewAppearance TAB_PHOTO_ICON = new ViewAppearance(598, -304, 70, 35, R.id.camera_tab_photo);
    private static final ViewAppearance TAB_VIDEO_ICON = new ViewAppearance(668, -304, 70, 35, R.id.camera_tab_video);
    private static final ViewAppearance TAB_OTHER_ICON = new ViewAppearance(738, -304, 70, 35, R.id.camera_tab_other);
    private static final ViewAppearance TAB_INDICATOR =
        new ViewAppearance(598, -271, 70, 2, R.id.camera_tab_indicator);
    //    private static final ViewAppearance HEADER_VIEW_ANIMATOR = new ViewAppearance(598, -304, 212, 35, R.id.camera_setting_header_view_animator);

    private static final ViewAppearance VIEW_ANIMATOR =
        new ViewAppearance(597, -269, 212, 281, R.id.camera_setting_content);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        TITLE_BAR,
        TITLE_ARROW,
        TITLE,
        TAB_BAR,
        TAB_PHOTO_ICON,
        TAB_VIDEO_ICON,
        TAB_OTHER_ICON,
        TAB_INDICATOR,
        VIEW_ANIMATOR
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