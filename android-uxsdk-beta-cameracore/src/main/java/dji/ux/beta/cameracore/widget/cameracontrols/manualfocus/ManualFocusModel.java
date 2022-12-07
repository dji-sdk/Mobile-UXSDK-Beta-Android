package dji.ux.beta.cameracore.widget.cameracontrols.manualfocus;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.ICameraIndex;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;
import io.reactivex.rxjava3.core.Flowable;

public class ManualFocusModel extends WidgetModel implements ICameraIndex {
    private final DataProcessor<Integer> rangeMaxDataProcessor;
    private final DataProcessor<Integer> currentFocusRingDataProcessor;
    private final DataProcessor<SettingsDefinitions.FocusMode> focusModeDataProcessor;
    private int cameraIndex = 0;
    private int lensIndex = 0;


    public CameraKey getCameraFocusRingKey() {
        return cameraFocusRingKey;
    }

    private CameraKey cameraFocusRingKey;

    public ManualFocusModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        rangeMaxDataProcessor = DataProcessor.create(0);
        currentFocusRingDataProcessor = DataProcessor.create(0);
        focusModeDataProcessor = DataProcessor.create(SettingsDefinitions.FocusMode.UNKNOWN);
    }

    @Override
    protected void inSetup() {

        CameraKey cameraFocusRingUpperBoundKey = djiSdkModel.createLensKey(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, cameraIndex, lensIndex);
        cameraFocusRingKey = djiSdkModel.createLensKey(CameraKey.FOCUS_RING_VALUE, cameraIndex, lensIndex);
        CameraKey focusModeKey = djiSdkModel.createLensKey(CameraKey.FOCUS_MODE, cameraIndex, lensIndex);
        bindDataProcessor(cameraFocusRingKey, currentFocusRingDataProcessor);
        bindDataProcessor(cameraFocusRingUpperBoundKey, rangeMaxDataProcessor);
        bindDataProcessor(focusModeKey, focusModeDataProcessor);

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

    public Flowable<Integer> getRangeMaxDataProcessor() {
        return rangeMaxDataProcessor.toFlowable();
    }

    public Flowable<Integer> getCurrentFocusRingDataProcessor() {
        return currentFocusRingDataProcessor.toFlowable();
    }

    public Flowable<SettingsDefinitions.FocusMode> getFocusModeDataProcessor() {
        return focusModeDataProcessor.toFlowable();
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