package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.PictureStylePreset;
import dji.common.camera.SettingsDefinitions.PictureStylePresetType;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.AudioUtil;

public class CameraStyleListWidget extends FrameLayoutWidget implements HeaderTitleClient {
    //region Properties
    private static final String TAG = "CameraStyleListWidget";
    private static final int[] RESID_CHILD = new int[] {
        R.id.camera_style_standard, R.id.camera_style_landscape, R.id.camera_style_soft, R.id.camera_style_custom
    };
    private static final int MIN_STYLE_VAL = -3;
    private static final int MAX_STYLE_VAL = 3;
    private static final int ITEM_NONE = -1;
    private static final int ITEM_SHARPNESS = 0;
    private static final int ITEM_CONTRAST = 1;
    private static final int ITEM_SATURATION = 2;
    private static final int ROW_CUSTOM = 3;
    private DJIKey pictureStyleKey;
    private PictureStylePreset pictureStylePreset, currentStyle;
    private StyleViewWidgetAppearances widgetAppearances;
    private FrameLayout[] rowViewHolders;

    private View sharpnessBG;
    private View contrastBG;
    private View saturationBG;
    private FrameLayout customButtonsLayout;

    private OnClickListener widgetClickListener;

    private int customSharpness;
    private int customContrast;
    private int customSaturation;

    private int selectedRow = ITEM_NONE;
    private int curCustomColumnIndex = ITEM_NONE;
    private TextView custStyleSharpnessValue;
    private TextView custStyleContrastValue;
    private TextView custStyleSaturationValue;

    //endregion

    //region Default Constructors
    public CameraStyleListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraStyleListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraStyleListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion
    private static String formatValue(final int value) {
        return String.format("%+d", value);
    }
    private static int getValueByCheckingBoundary(int value) {
        if (value < MIN_STYLE_VAL) {
            value = MIN_STYLE_VAL;
        } else if (value > MAX_STYLE_VAL) {
            value = MAX_STYLE_VAL;
        }

        return value;
    }
    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        initOnClickListener();
        initUIElements();
        initCustomStyleValue();
    }
    private void initUIElements() {
        rowViewHolders = new FrameLayout[RESID_CHILD.length];
        for (int i = 0; i < RESID_CHILD.length; i++) {
            final FrameLayout rowView = (FrameLayout) findViewById(RESID_CHILD[i]);
            rowView.setOnClickListener(widgetClickListener);
            rowViewHolders[i] = rowView;
        }

        custStyleSharpnessValue = (TextView) findViewById(R.id.camera_style_custom_item_sharpness);
        custStyleContrastValue = (TextView) findViewById(R.id.camera_style_custom_item_contrast);
        custStyleSaturationValue = (TextView) findViewById(R.id.camera_style_custom_item_saturation);

        sharpnessBG = findViewById(R.id.camera_style_custom_sharpness);
        contrastBG = findViewById(R.id.camera_style_custom_contrast);
        saturationBG = findViewById(R.id.camera_style_custom_saturation);

        customButtonsLayout = (FrameLayout) findViewById(R.id.camera_style_custom_layout);
        ImageView minusButton = (ImageView) findViewById(R.id.custom_min_img);
        ImageView plusButton = (ImageView) findViewById(R.id.custom_max_img);

        sharpnessBG.setOnClickListener(widgetClickListener);
        contrastBG.setOnClickListener(widgetClickListener);
        saturationBG.setOnClickListener(widgetClickListener);

        minusButton.setOnClickListener(widgetClickListener);
        plusButton.setOnClickListener(widgetClickListener);
    }
    private void initCustomStyleValue() {
        final byte[] values = CameraResource.pictureStyleValue[PictureStylePresetType.CUSTOM.value()];
        updateCustomStyleValue(values[0], values[1], values[2]);
    }
    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new StyleViewWidgetAppearances();
        }
        return widgetAppearances;
    }
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_picture_style);
        }
    }
    @Override
    public boolean shouldTrack() {
        return false;
    }
    //endregion
    private void initOnClickListener() {

        widgetClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                final int id = v.getId();

                if (R.id.camera_style_custom_sharpness == id) {
                    if (curCustomColumnIndex != ITEM_SHARPNESS) {
                        curCustomColumnIndex = ITEM_SHARPNESS;
                        updateSelectionOfCustomStyleColumnView();
                    }
                } else if (R.id.camera_style_custom_contrast == id) {
                    if (curCustomColumnIndex != ITEM_CONTRAST) {
                        curCustomColumnIndex = ITEM_CONTRAST;
                        updateSelectionOfCustomStyleColumnView();
                    }
                } else if (R.id.camera_style_custom_saturation == id) {
                    if (ITEM_SATURATION != curCustomColumnIndex) {
                        curCustomColumnIndex = ITEM_SATURATION;
                        updateSelectionOfCustomStyleColumnView();
                    }
                } else if (R.id.custom_min_img == id) {
                    handleMinusClick();
                } else if (R.id.custom_max_img == id) {
                    handlePlusClick();
                } else {
                    for (int i = 0; i < RESID_CHILD.length; i++) {
                        if (id == RESID_CHILD[i]) {
                            selectRow(i);
                            if (selectedRow == ROW_CUSTOM) {
                                updateCustomStyleToCamera();
                            } else {
                                updateNonCustomStyleToCamera(i);
                            }
                            break;
                        }
                    }
                }
            }
        };
    }
    //region Key life cycle
    @Override
    public void initKey() {
        pictureStyleKey = CameraKey.create(CameraKey.PICTURE_STYLE_PRESET, keyIndex);

        addDependentKey(pictureStyleKey);
    }
    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(pictureStyleKey)) {
            pictureStylePreset = (PictureStylePreset) value;
        }
    }
    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(pictureStyleKey)) {

            updatePictureStyle(pictureStylePreset);

        }
    }

    //endregion

    //region Action methods
    private void updatePictureStyle(PictureStylePreset pictureStylePreset) {
        if (pictureStylePreset != currentStyle) {
            if (pictureStylePreset != null) {
                currentStyle = pictureStylePreset;
                final int sharpness = pictureStylePreset.getSharpness();
                final int contrast = pictureStylePreset.getContrast();
                final int saturation = pictureStylePreset.getSaturation();

                if (selectedRow == ITEM_NONE) {
                    int styleValue = pictureStylePreset.presetType().value();
                    selectRow(styleValue);
                }

                // Modify custom values to match
                updateCustomStyleValue(sharpness, contrast, saturation);
            } else {
                selectRow(ITEM_NONE);
                initCustomStyleValue();
            }
        }
    }
    private void selectRow(final int pos) {
        if (selectedRow != pos) {
            selectedRow = pos;
            if (pos == ROW_CUSTOM) {
                customButtonsLayout.setVisibility(VISIBLE);
                if (ITEM_NONE == curCustomColumnIndex) {
                    curCustomColumnIndex = ITEM_SHARPNESS;
                }
                updateSelectionOfCustomStyleColumnView();
            } else {
                customButtonsLayout.setVisibility(GONE);
            }
            for (FrameLayout rowViewHolder : rowViewHolders) {
                rowViewHolder.setSelected(false);
            }
            if (pos >= 0) {
                rowViewHolders[pos].setSelected(true);
            }
        }
    }
    private void updateCustomStyleValue(final int sharpness, final int contrast, final int saturation) {
        customSharpness = getValueByCheckingBoundary(sharpness);
        customContrast = getValueByCheckingBoundary(contrast);
        customSaturation = getValueByCheckingBoundary(saturation);

        custStyleSharpnessValue.setText(formatValue(customSharpness));
        custStyleContrastValue.setText(formatValue(customContrast));
        custStyleSaturationValue.setText(formatValue(customSaturation));
    }

    private void updateStyleValueToCamera(final PictureStylePreset newStyle) {
        if (KeyManager.getInstance() == null) return;

        if (newStyle != null) {
            updatePictureStyle(newStyle);
            KeyManager.getInstance().setValue(pictureStyleKey, newStyle, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Camera setting " + newStyle + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updatePictureStyle(pictureStylePreset);
                    DJILog.d(TAG, "Failed to set Camera Exposure Mode");
                }
            });
        }
    }

    private void updateCustomStyleToCamera() {
        PictureStylePreset.Builder builder = new PictureStylePreset.Builder();
        builder.sharpness(customSharpness).contrast(customContrast).saturation(customSaturation);
        updateStyleValueToCamera(builder.build());
    }

    private void updateNonCustomStyleToCamera(final int index) {
        final byte[] values = CameraResource.pictureStyleValue[index];
        PictureStylePreset.Builder builder = new PictureStylePreset.Builder();
        builder.sharpness((int) values[0]).contrast((int) values[1]).saturation((int) values[2]);
        updateStyleValueToCamera(builder.build());
    }

    private void handleMinusClick() {
        AudioUtil.playSimpleSound(getContext());

        PictureStylePreset.Builder builder = new PictureStylePreset.Builder();
        builder.sharpness(customSharpness).contrast(customContrast).saturation(customSaturation);

        int value = Integer.MIN_VALUE;
        if (curCustomColumnIndex == ITEM_SHARPNESS) {
            if (customSharpness > MIN_STYLE_VAL) {
                value = customSharpness - 1;
                builder.sharpness(value);
            }
        } else if (curCustomColumnIndex == ITEM_CONTRAST) {
            if (customContrast > MIN_STYLE_VAL) {
                value = customContrast - 1;
                builder.contrast(value);
            }
        } else if (curCustomColumnIndex == ITEM_SATURATION) {
            if (customSaturation > MIN_STYLE_VAL) {
                value = customSaturation - 1;
                builder.saturation(value);
            }
        }

        if (value != Integer.MIN_VALUE) {
            updateStyleValueToCamera(builder.build());
        }
    }

    private void handlePlusClick() {
        AudioUtil.playSimpleSound(getContext());
        int value = Integer.MIN_VALUE;
        PictureStylePreset.Builder builder = new PictureStylePreset.Builder();
        builder.sharpness(customSharpness).contrast(customContrast).saturation(customSaturation);

        if (curCustomColumnIndex == ITEM_SHARPNESS) {
            if (customSharpness < MAX_STYLE_VAL) {
                value = customSharpness + 1;
                builder.sharpness(value);
            }
        } else if (curCustomColumnIndex == ITEM_CONTRAST) {
            if (customContrast < MAX_STYLE_VAL) {
                value = customContrast + 1;
                builder.contrast(value);
            }
        } else if (curCustomColumnIndex == ITEM_SATURATION) {
            if (customSaturation < MAX_STYLE_VAL) {
                value = customSaturation + 1;
                builder.saturation(value);
            }
        }
        if (value != Integer.MIN_VALUE) {
            updateStyleValueToCamera(builder.build());
        }
    }

    private void updateSelectionOfCustomStyleColumnView() {
        sharpnessBG.setSelected(false);
        contrastBG.setSelected(false);
        saturationBG.setSelected(false);

        if (curCustomColumnIndex == ITEM_SHARPNESS) {
            sharpnessBG.setSelected(true);
        } else if (curCustomColumnIndex == ITEM_CONTRAST) {
            contrastBG.setSelected(true);
        } else if (curCustomColumnIndex == ITEM_SATURATION) {
            saturationBG.setSelected(true);
        }
    }

    //endregion
}
