package dji.ux.beta.core.v4;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

/**
 * Appearance setting for DULSeekBar
 */
public class SeekBarAppearances extends BaseWidgetAppearances {
    private static final ViewAppearance WIDGET = new ViewAppearance(0, 0, 220, 46, R.layout.uxsdk_seek_bar_v4);
    private static final TextAppearance TEXT_MIN_VIEW =
        new TextAppearance(0, 25, 22, 11, R.id.textview_min_value, "320", "Roboto-Medium");
    private static final ImageAppearance MINUS_VIEW = new ImageAppearance(0, 14, 28, 28, R.id.imageview_minus);
    private static final TextAppearance TEXT_VIEW =
        new TextAppearance(20, 4, 44, 19, R.id.textview_value, "32000K", "Roboto-Medium");
    private static final TextAppearance TEXT_MAX_VIEW =
        new TextAppearance(183, 25, 32, 11, R.id.textview_max_value, "32000", "Roboto-Medium");
    private static final ImageAppearance PLUS_VIEW = new ImageAppearance(192, 14, 28, 28, R.id.imageview_plus);
    private static final ImageAppearance THUMB_VIEW = new ImageAppearance(34, 24, 16, 16, R.id.imageview_thumb);
    private static final ImageAppearance DISABLE_THUMB_VIEW =
        new ImageAppearance(37, 27, 10, 10, R.id.imageview_thumb_disable);
    private static final ImageAppearance RECOMMENDED_BAR = new ImageAppearance(100, 27, 2, 12, R.id.imageview_recommended_bar);
    private static final ImageAppearance TRACK_VIEW = new ImageAppearance(37, 31, 146, 4, R.id.imageview_track);

    private static final Appearance[] ELEMENT_APPEARANCES = new Appearance[] {
        THUMB_VIEW, DISABLE_THUMB_VIEW, RECOMMENDED_BAR, TRACK_VIEW, TEXT_MIN_VIEW, MINUS_VIEW, TEXT_VIEW, PLUS_VIEW, TEXT_MAX_VIEW
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
