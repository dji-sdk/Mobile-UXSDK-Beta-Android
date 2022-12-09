package dji.ux.beta.cameracore.widget.cameracontrols.palette;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.common.camera.SettingsDefinitions;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.SettingDefinitions;

public class PaletteWidget extends ConstraintLayoutWidget implements View.OnClickListener, ICameraIndex {
    //region Fields
    private static final String TAG = "FocusExpoSwitchWidget";
    private ImageView paletteView;
    private PaletteModel widgetModel;
    private PaletteBackgroundWidget backgroundWidget;
    private int sourceResourceId;
    private View associateView;

    //region Lifecycle
    public PaletteWidget(@NonNull Context context) {
        super(context);
    }

    public PaletteWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PaletteWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_palette, this);
        paletteView = findViewById(R.id.palette_view);
        if (!isInEditMode()) {
            widgetModel = new PaletteModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        setOnClickListener(this);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initDefaults() {

    }

    @Override
    protected void reactToModelChanges() {

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == this.getId()) {
            if (paletteView.getTag().equals(getResources().getString(R.string.uxsdk_palette_unselect))) {
                paletteView.setImageDrawable(getResources().getDrawable(R.drawable.palette_select));
                paletteView.setTag(getResources().getString(R.string.uxsdk_palette_select));
                if (associateView != null) {
                    associateView.setVisibility(VISIBLE);
                }
                return;
            }
            if (associateView != null) {
                associateView.setVisibility(GONE);
            }
            updatePaletteViewUnselected();

        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void updatePaletteViewUnselected() {
        paletteView.setImageDrawable(getResources().getDrawable(R.drawable.palette_unselect));
        paletteView.setTag(getResources().getString(R.string.uxsdk_palette_unselect));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
        associateView = getRootView().findViewById(sourceResourceId);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return widgetModel.getLensType();
    }


}