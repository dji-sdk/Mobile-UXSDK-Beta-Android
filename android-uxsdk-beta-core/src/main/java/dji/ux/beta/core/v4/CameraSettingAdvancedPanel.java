package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.bus.UXSDKEventBus;
import dji.common.camera.CameraUtils;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.sdk.camera.Camera;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static dji.common.camera.SettingsDefinitions.CameraType.DJICameraTypeFC1705;

// Doc key: CameraSettingAdvancedPanel

/**
 * Display:
 * This panel shows all the camera settings that are not related to exposure. It is
 * segmented in three parts:
 * - Picture Settings
 * - Video Settings
 * - Common Settings
 * <p>
 * Usage:
 * Preferred Aspect Ratio: 53:79. To allow user to toggle hide and show this panel,
 * use in conjunction with `CameraControlsWidget`
 * <p>
 * Interaction:
 * All the settings are presented in ListView hierarchy.
 */
public class CameraSettingAdvancedPanel extends FrameLayoutWidget
        implements ParentChildrenViewAnimator.RootViewCallback {
    //region Properties
    private final static int SHOOT_PHOTO_INDEX = 0;
    private final static int RECORD_VIDEO_INDEX = 1;

    private DJIKey cameraModeKey;
    private DJIKey isFlatCameraModeSupportedKey;
    private DJIKey flatCameraModeKey;
    private CameraMode cameraMode;
    private boolean isFlatCameraModeSupported;
    private FlatCameraMode flatCameraMode;
    private ParentChildrenViewAnimator contentAnimator;
    private TabBarView tabBar;
    private BaseWidgetAppearances widgetAppearance;
    private ImageView imageBackArrow;
    private TextView textTitle;
    private FrameLayout titleBar;

    private CameraSettingListView cameraPhotoSettingList;
    private CameraSettingListView cameraVideoSettingList;
    private CameraSettingListView cameraOtherSettingList;
    //endregion

    //region Default Constructors
    public CameraSettingAdvancedPanel(Context context) {
        this(context, null, 0);
    }

    public CameraSettingAdvancedPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSettingAdvancedPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearance == null) {
            /*if (isXT2Camera()) {
                widgetAppearance = new ThermalAdvancedSettingAppearancesForXT2();
            } else if (isThermalCamera()) {
                widgetAppearance = new ThermalAdvancedSettingAppearances();
            } else if (isPayloadCamera()){
                widgetAppearance = new PayloadCameraSettingScenePanelAppearances();
            } else {*/
            widgetAppearance = new AdvancedSettingAppearances();
            //}
        }
        return widgetAppearance;
    }

    private void initTabBar() {
        tabBar = (TabBarView) findViewById(R.id.camera_advsetting_tab);
        ImageView photoIcon = (ImageView) findViewById(R.id.camera_tab_photo);
        ImageView videoIcon = (ImageView) findViewById(R.id.camera_tab_video);
        ImageView otherIcon = (ImageView) findViewById(R.id.camera_tab_other);
        ImageView tabIndicator = (ImageView) findViewById(R.id.camera_tab_indicator);

        tabBar.initTabBar(new ImageView[]{photoIcon, videoIcon, otherIcon}, tabIndicator, true);
        tabBar.setStageChangedCallback(new TabBarView.OnStageChangeCallback() {
            @Override
            public void onStageChange(int stage) {
                showView(stage);
            }
        });
    }

    private void initTitleBar() {
        titleBar = (FrameLayout) findViewById(R.id.camera_setting_title_bar);
        imageBackArrow = (ImageView) findViewById(R.id.imageview_back);
        textTitle = (TextView) findViewById(R.id.textview_title);
        imageBackArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v == imageBackArrow) {
                    if (event.getAction() == ACTION_DOWN) {
                        imageBackArrow.setPressed(true);
                        onBackButtonClicked();
                    } else if (event.getAction() == ACTION_UP) {
                        imageBackArrow.setPressed(false);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    protected void onBackButtonClicked() {
        CameraSettingListView currentSettingList = (CameraSettingListView) contentAnimator.getCurrentView();
        if (currentSettingList != null) {
            currentSettingList.onBackButtonClicked();
        }
    }

    /**
     * Inflates layout file and initialize view
     */
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        //This widget is clickable which will not transfer the user click event to next layer.
        ViewUtils.setClickableView(this);

        initTabBar();
        initTitleBar();

        contentAnimator = findViewById(R.id.camera_setting_content);

        cameraPhotoSettingList = findViewById(R.id.camera_setting_content_photo);
        cameraPhotoSettingList.setRootViewCallback(this);
        cameraPhotoSettingList.setTitleTextView(textTitle);

        cameraVideoSettingList = findViewById(R.id.camera_setting_content_video);
        cameraVideoSettingList.setRootViewCallback(this);
        cameraVideoSettingList.setTitleTextView(textTitle);

        cameraOtherSettingList = findViewById(R.id.camera_setting_content_other);
        cameraOtherSettingList.setRootViewCallback(this);
        cameraOtherSettingList.setTitleTextView(textTitle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        // Association with CameraControlsWidget button click
        subscription.add(UXSDKEventBus.getInstance()
                .register(Events.CameraSettingAdvancedPanelControlEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Events.CameraSettingAdvancedPanelControlEvent>() {
                    @Override
                    public void call(Events.CameraSettingAdvancedPanelControlEvent event) {
                        if (event.getIndex() == keyIndex || (event.getIndex() == 0 && keyIndex == Camera.XT2_IR_CAMERA_INDEX)) {
                            if (event.shouldShow()) {
                                setVisibility(VISIBLE);
                            } else {
                                setVisibility(INVISIBLE);
                            }
                        }
                    }
                }));
    }

    //endregion

    //region Key life cycle
    @Override
    public void initKey() {
        cameraModeKey = CameraKey.create(CameraKey.MODE, keyIndex);
        isFlatCameraModeSupportedKey = CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, keyIndex);
        flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, keyIndex);
        addDependentKey(cameraModeKey);
        addDependentKey(isFlatCameraModeSupportedKey);
        addDependentKey(flatCameraModeKey);
    }

    @Override
    public void updateKeyOnIndex(int keyIndex, int subKeyIndex) {
        super.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraPhotoSettingList.updateKeyOnIndex(keyIndex,subKeyIndex);
        cameraVideoSettingList.updateKeyOnIndex(keyIndex,subKeyIndex);
        cameraOtherSettingList.updateKeyOnIndex(keyIndex,subKeyIndex);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(cameraModeKey)) {
            cameraMode = (CameraMode) value;
        } else if (key.equals(isFlatCameraModeSupportedKey)) {
            isFlatCameraModeSupported = (Boolean) value;
        } else if (key.equals(flatCameraModeKey)) {
            flatCameraMode = (FlatCameraMode) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(cameraModeKey)) {
            onCameraModeUpdate(cameraMode);
        } else if (key.equals(isFlatCameraModeSupportedKey)) {
            if (isFlatCameraModeSupported) {
                onFlatCameraModeUpdate(flatCameraMode);
            } else {
                onCameraModeUpdate(cameraMode);
            }
        } else if (key.equals(flatCameraModeKey) && isFlatCameraModeSupported) {
            onFlatCameraModeUpdate(flatCameraMode);
        }
    }

    private void onCameraModeUpdate(CameraMode cameraMode) {
        int index = 0;
        if (cameraMode == CameraMode.SHOOT_PHOTO) {
            index = SHOOT_PHOTO_INDEX;
        } else if (cameraMode == CameraMode.RECORD_VIDEO) {
            index = RECORD_VIDEO_INDEX;
        }
        // Back to parent view first before tab changing.
        onBackButtonClicked();

        tabBar.handleTabChanged(index);
    }

    private void onFlatCameraModeUpdate(FlatCameraMode flatCameraMode) {
        int index = 0;
        if (CameraUtil.isPictureMode(flatCameraMode)) {
            if (tabBar.getCurrentTabIndex() == SHOOT_PHOTO_INDEX) {
                return;
            }
            index = SHOOT_PHOTO_INDEX;
        } else {
            if (tabBar.getCurrentTabIndex() == RECORD_VIDEO_INDEX) {
                return;
            }
            index = RECORD_VIDEO_INDEX;
        }
        // Back to parent view first before tab changing.
        onBackButtonClicked();

        tabBar.handleTabChanged(index);
    }

    private boolean isThermalCamera() {
        DJIKey key = CameraKey.create(CameraKey.IS_THERMAL_CAMERA, keyIndex);
        if (KeyManager.getInstance() != null) {
            Object value = KeyManager.getInstance().getValue(key);
            return value != null && (Boolean) value;
        }
        return false;
    }

    private boolean isXT2Camera() {
        DJIKey key = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);
        if (KeyManager.getInstance() != null) {
            Object value = KeyManager.getInstance().getValue(key);
            return value != null && value == DJICameraTypeFC1705;
        }
        return false;
    }

    private boolean isPayloadCamera() {
        DJIKey key = CameraKey.create(CameraKey.CAMERA_TYPE, keyIndex);
        if (KeyManager.getInstance() != null) {
            Object value = KeyManager.getInstance().getValue(key);
            return CameraUtils.isPayloadCamera((SettingsDefinitions.CameraType) value);
        }
        return false;
    }

    //endregion

    //region User Action

    private void performCameraModeAction(final CameraMode cameraMode) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(cameraModeKey, cameraMode, new SetCallback() {
            @Override
            public void onSuccess() {
                // Do nothing.
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                //DJILog.d(TAG, "Failed to set " + cameraMode + " with error: " + error.getDescription());
            }
        });
    }

    private void performFlatCameraModeAction(final FlatCameraMode flatCameraMode) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(flatCameraModeKey, flatCameraMode, new SetCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
            }
        });
    }

    private void showView(int index) {
        if (isFlatCameraModeSupported) {
            if (index == SHOOT_PHOTO_INDEX && !CameraUtil.isPictureMode(flatCameraMode)) {
                performFlatCameraModeAction(FlatCameraMode.PHOTO_SINGLE);
            } else if (index == RECORD_VIDEO_INDEX && CameraUtil.isPictureMode(flatCameraMode)) {
                performFlatCameraModeAction(FlatCameraMode.VIDEO_NORMAL);
            }
        } else {
            if (index == SHOOT_PHOTO_INDEX && cameraMode != CameraMode.SHOOT_PHOTO) {
                performCameraModeAction(CameraMode.SHOOT_PHOTO);
            } else if (index == RECORD_VIDEO_INDEX && cameraMode != CameraMode.RECORD_VIDEO) {
                performCameraModeAction(CameraMode.RECORD_VIDEO);
            }
        }

        contentAnimator.setDisplayedChild(index);
    }

    @Override
    public void onRootViewIsShown(boolean isShown) {
        if (isShown) {
            tabBar.setVisibility(VISIBLE);
            titleBar.setVisibility(GONE);
        } else {
            tabBar.setVisibility(INVISIBLE);
            titleBar.setVisibility(VISIBLE);
        }
    }

    //endregion
}
