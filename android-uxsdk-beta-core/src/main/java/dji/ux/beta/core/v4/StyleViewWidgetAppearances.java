package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public class StyleViewWidgetAppearances extends BaseWidgetAppearances {

    private static final ViewAppearance WIDGET = new ViewAppearance(1060, 0, 212, 281, R.layout.uxsdk_widget_style_view);

    private static final ViewAppearance SHARP_ICON = new ViewAppearance(1156, 6, 30, 30, R.id.camera_sharpness_icon);
    private static final ViewAppearance CONTRAST_ICON = new ViewAppearance(1196, 6, 30, 30, R.id.camera_contrast_icon);
    private static final ViewAppearance SATURATION_ICON =
        new ViewAppearance(1236, 6, 30, 30, R.id.camera_saturation_icon);

    private static final ViewAppearance STAN_VIEW = new ViewAppearance(1060, 42, 212, 42, R.id.camera_style_standard);
    private static final ViewAppearance STAN_VIEW_SEPARATOR =
        new ViewAppearance(1060, 0, 212, 1, R.id.standard_row_separator);
    private static final TextAppearance STAN_ITEM_TITLE =
        new TextAppearance(1079, 11, 60, 20, R.id.camera_style_standard_item_desc, "Landscape", "Roboto-Regular");
    private static final TextAppearance STAN_SHARP_VALUE =
        new TextAppearance(1161, 11, 20, 20, R.id.camera_style_standard_item_sharpness, "+3", "Roboto-Regular");
    private static final TextAppearance STAN_CONTRAST_VALUE =
        new TextAppearance(1201, 11, 20, 20, R.id.camera_style_standard_item_contrast, "+3", "Roboto-Regular");
    private static final TextAppearance STAN_SATURATION_VALUE =
        new TextAppearance(1241, 11, 20, 20, R.id.camera_style_standard_item_saturation, "+3", "Roboto-Regular");

    private static final ViewAppearance LAND_VIEW = new ViewAppearance(1060, 84, 212, 42, R.id.camera_style_landscape);
    private static final ViewAppearance LAND_VIEW_SEPARATOR =
        new ViewAppearance(1060, 0, 212, 1, R.id.landscape_row_separator);
    private static final TextAppearance LAND_ITEM_TITLE =
        new TextAppearance(1079, 11, 60, 20, R.id.camera_style_landscape_item_desc, "Landscape", "Roboto-Regular");
    private static final TextAppearance LAND_SHARP_VALUE =
        new TextAppearance(1161, 11, 20, 20, R.id.camera_style_landscape_item_sharpness, "+3", "Roboto-Regular");
    private static final TextAppearance LAND_CONTRAST_VALUE =
        new TextAppearance(1201, 11, 20, 20, R.id.camera_style_landscape_item_contrast, "+3", "Roboto-Regular");
    private static final TextAppearance LAND_SATURATION_VALUE =
        new TextAppearance(1241, 11, 20, 20, R.id.camera_style_landscape_item_saturation, "+3", "Roboto-Regular");

    private static final ViewAppearance SOFT_VIEW = new ViewAppearance(1060, 126, 212, 42, R.id.camera_style_soft);
    private static final ViewAppearance SOFT_VIEW_SEPARATOR =
        new ViewAppearance(1060, 0, 212, 1, R.id.soft_row_separator);
    private static final TextAppearance SOFT_ITEM_TITLE =
        new TextAppearance(1079, 11, 60, 20, R.id.camera_style_soft_item_desc, "Landscape", "Roboto-Regular");
    private static final TextAppearance SOFT_SHARP_VALUE =
        new TextAppearance(1161, 11, 20, 20, R.id.camera_style_soft_item_sharpness, "+3", "Roboto-Regular");
    private static final TextAppearance SOFT_CONTRAST_VALUE =
        new TextAppearance(1201, 11, 20, 20, R.id.camera_style_soft_item_contrast, "+3", "Roboto-Regular");
    private static final TextAppearance SOFT_SATURATION_VALUE =
        new TextAppearance(1241, 11, 20, 20, R.id.camera_style_soft_item_saturation, "+3", "Roboto-Regular");

    private static final ViewAppearance CUST_VIEW = new ViewAppearance(1060, 168, 212, 42, R.id.camera_style_custom);
    private static final ViewAppearance CUST_VIEW_SEPARATOR =
        new ViewAppearance(1060, 0, 212, 1, R.id.custom_row_separator);
    private static final TextAppearance CUST_ITEM_TITLE =
        new TextAppearance(1079, 11, 60, 20, R.id.camera_style_custom_item_desc, "Landscape", "Roboto-Regular");
    private static final TextAppearance CUST_SHARP_VALUE =
        new TextAppearance(1161, 11, 20, 20, R.id.camera_style_custom_item_sharpness, "+3", "Roboto-Regular");
    private static final TextAppearance CUST_CONTRAST_VALUE =
        new TextAppearance(1201, 11, 20, 20, R.id.camera_style_custom_item_contrast, "+3", "Roboto-Regular");
    private static final TextAppearance CUST_SATURATION_VALUE =
        new TextAppearance(1241, 11, 20, 20, R.id.camera_style_custom_item_saturation, "+3", "Roboto-Regular");

    private static final ViewAppearance CUST_SHARP_BG =
        new ViewAppearance(1158, 8, 26, 26, R.id.camera_style_custom_sharpness);
    private static final ViewAppearance CUST_CONTRAST_BG =
        new ViewAppearance(1198, 8, 26, 26, R.id.camera_style_custom_contrast);
    private static final ViewAppearance CUST_SATURATION_BG =
        new ViewAppearance(1238, 8, 26, 26, R.id.camera_style_custom_saturation);

    private static final ViewAppearance BUTTON_VIEW =
        new ViewAppearance(1060, 210, 212, 42, R.id.camera_style_custom_layout);
    private static final ViewAppearance MINUS_ICON = new ViewAppearance(1096, 14, 28, 28, R.id.custom_min_img);
    private static final ViewAppearance PLUS_ICON = new ViewAppearance(1208, 14, 28, 28, R.id.custom_max_img);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        SHARP_ICON,
        CONTRAST_ICON,
        SATURATION_ICON,
        STAN_VIEW, STAN_VIEW_SEPARATOR,
        STAN_ITEM_TITLE,
        STAN_SHARP_VALUE,
        STAN_CONTRAST_VALUE,
        STAN_SATURATION_VALUE,
        LAND_VIEW,
        LAND_VIEW_SEPARATOR,
        LAND_ITEM_TITLE,
        LAND_SHARP_VALUE,
        LAND_CONTRAST_VALUE,
        LAND_SATURATION_VALUE,
        SOFT_VIEW,
        SOFT_VIEW_SEPARATOR,
        SOFT_ITEM_TITLE,
        SOFT_SHARP_VALUE,
        SOFT_CONTRAST_VALUE,
        SOFT_SATURATION_VALUE,
        CUST_VIEW,
        CUST_VIEW_SEPARATOR,
        CUST_ITEM_TITLE,
        CUST_SHARP_VALUE,
        CUST_CONTRAST_VALUE,
        CUST_SATURATION_VALUE,
        CUST_SHARP_BG,
        CUST_CONTRAST_BG,
        CUST_SATURATION_BG,
        BUTTON_VIEW,
        MINUS_ICON,
        PLUS_ICON
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
