package dji.ux.beta.cameracore.widget.cameracontrols.palette;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

public class PaletteModel  extends WidgetModel implements ICameraIndex {
    private final DataProcessor<Integer> rangeMaxDataProcessor;
    private final DataProcessor<Integer> currentFocusRingDataProcessor;
    private int cameraIndex = 0;
    private int lensIndex = 0;

    public CameraKey getCameraFocusRingKey() {
        return cameraFocusRingKey;
    }

    private CameraKey cameraFocusRingKey;

    public PaletteModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        rangeMaxDataProcessor = DataProcessor.create(0);   //都是默认值
        currentFocusRingDataProcessor = DataProcessor.create(0);
    }

    @Override
    protected void inSetup() {
        CameraKey cameraFocusRingUpperBoundKey = djiSdkModel.createLensKey(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, cameraIndex, lensIndex);
        cameraFocusRingKey = djiSdkModel.createLensKey(CameraKey.FOCUS_RING_VALUE, cameraIndex, lensIndex);
        CameraKey focusModeKey = djiSdkModel.createLensKey(CameraKey.FOCUS_MODE, cameraIndex, lensIndex);
        bindDataProcessor(cameraFocusRingKey, currentFocusRingDataProcessor);
        bindDataProcessor(cameraFocusRingUpperBoundKey, rangeMaxDataProcessor);
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        if (this.cameraIndex == cameraIndex.getIndex() && this.lensIndex == lensType.value()) {
            return;
        }
        this.cameraIndex = cameraIndex.getIndex();
        this.lensIndex = lensType.value();
        restart();
    }


    @Override
    protected void inCleanup() {

    }



    @NonNull
    @Override
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    @NonNull
    @Override
    public SettingsDefinitions.LensType getLensType() {
        return SettingsDefinitions.LensType.find(lensIndex);
    }

}