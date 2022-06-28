package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

import dji.common.bus.UXSDKEventBus;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;

// Doc key: CameraSettingExposurePanel

/**
 *  Display:
 *  This panel shows all the camera settings that are related to exposure. It allows
 *  exposure mode selection (auto, aperture priority, shutter priority and manual)
 *  depending on the connected camera. Depending on the mode, the ISO, aperture,
 *  shutter speed and exposure compensation value can also be set.
 *  
 *  Usage:
 *  Preferred Aspect Ratio: 211:316. To allow user to toggle hide and show this
 *  panel, use in conjunction with `CameraControlsWidget`
 *  
 *  Interaction:
 *  All the settings are presented in ListView hierarchy.
 */
public class CameraSettingExposurePanelV4 extends BaseFrameLayout implements KeyIndexManager {
    //region Properties
    private BaseWidgetAppearances widgetAppearances;
    FrameLayoutWidget exposeMode;
    FrameLayoutWidget isoSetting;
    FrameLayoutWidget aperture;
    FrameLayoutWidget shutter;
    FrameLayoutWidget evSetting;
    protected int keyIndex;
    protected int subKeyIndex;
    //endregion

    //region Default Constructors
    public CameraSettingExposurePanelV4(Context context) {
        this(context, null, 0);
    }

    public CameraSettingExposurePanelV4(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSettingExposurePanelV4(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        // Association with CameraControlsWidget button click
        subscription.add(UXSDKEventBus.getInstance()
                                      .register(Events.CameraSettingExposurePanelControlEvent.class)
                                      .observeOn(AndroidSchedulers.mainThread())
                                      .subscribe(new Action1<Events.CameraSettingExposurePanelControlEvent>() {
                                          @Override
                                          public void call(Events.CameraSettingExposurePanelControlEvent event) {
                                              if (event.getIndex() == keyIndex) {
                                                  if (event.shouldShow()) {
                                                      setVisibility(VISIBLE);
                                                  } else {
                                                      setVisibility(INVISIBLE);
                                                  }
                                              }
                                          }
                                      }));
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new ExposureSettingAppearances();
        }
        return widgetAppearances;
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        //This widget is clickable which will not transfer the user click event to next layer.
        ViewUtils.setClickableView(this);

        exposeMode = findViewById(R.id.widget_camera_exposure_mode);
        isoSetting = findViewById(R.id.widget_camera_iso_ei_setting);
        aperture = findViewById(R.id.widget_camera_aperture_setting);
        shutter = findViewById(R.id.widget_camera_shutter_setting);
        evSetting = findViewById(R.id.widget_camera_ev_setting);
    }

    @Override
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    @Override
    public int getKeyIndex() {
        return keyIndex;
    }

    @Override
    public int getSubKeyIndex() {
        return subKeyIndex;
    }

    @Override
    public void updateKeyOnIndex(int keyIndex ,int subKeyIndex) {
        this.keyIndex = keyIndex;
        this.subKeyIndex = subKeyIndex;
        exposeMode.updateKeyOnIndex(keyIndex,subKeyIndex);
        isoSetting.updateKeyOnIndex(keyIndex,subKeyIndex);
        aperture.updateKeyOnIndex(keyIndex,subKeyIndex);
        shutter.updateKeyOnIndex(keyIndex,subKeyIndex);
        evSetting.updateKeyOnIndex(keyIndex,subKeyIndex);
    }
}
