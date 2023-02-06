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
import io.reactivex.rxjava3.core.Flowable;

public class PaletteBackgroundModel extends WidgetModel implements ICameraIndex {
    private final DataProcessor<SettingsDefinitions.ThermalPalette> paletteProcessor;
    private final DataProcessor<SettingsDefinitions.ThermalGainMode> thermalGainModeDataProcessor;
    private final DataProcessor<Float[]> currentGainModeTemperatureRangeProcessor;
    private final DataProcessor<Integer> thermalIsoThermUpperValueProcessor;
    private final DataProcessor<Integer> thermalIsoThermLowerValueProcessor;

    public final int defaultValue = -100000;
    private int cameraIndex = 0;
    private int lensIndex = 0;
    private final DJISDKModel djiSdkModel;
    private CameraKey cameraPaletteKey;

    private CameraKey gainModeTemperatureRangeKey;
    private CameraKey currentGainModeTemperatureRangeKey;
    private CameraKey thermalGainModeKey;

    private CameraKey thermalIsoThermUpperValueKey;

    private CameraKey thermalIsoThermLowerValueKey;
    public CameraKey getThermalGainModeKey() {
        return thermalGainModeKey;
    }
    public CameraKey getThermalIsoThermLowerValueKey() {
        return thermalIsoThermLowerValueKey;
    }

    public CameraKey getThermalIsoThermUpperValueKey() {
        return thermalIsoThermUpperValueKey;
    }

    public CameraKey getCameraPaletteKey() {
        return cameraPaletteKey;
    }

    public CameraKey getGainModeTemperatureRangeKey() {
        return gainModeTemperatureRangeKey;
    }

    public CameraKey getCurrentGainModeTemperatureRangeKey() {
        return currentGainModeTemperatureRangeKey;
    }

    public PaletteBackgroundModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        this.djiSdkModel = djiSdkModel;
        paletteProcessor = DataProcessor.create(SettingsDefinitions.ThermalPalette.UNKNOWN);
        thermalGainModeDataProcessor = DataProcessor.create(SettingsDefinitions.ThermalGainMode.UNKNOWN);
        DataProcessor<SettingsDefinitions.GainModeTemperatureRange> gainModeTemperatureRangeProcessor = DataProcessor.create(new SettingsDefinitions.GainModeTemperatureRange.Builder().build());
        Float[] currentValue = new Float[0];
        currentGainModeTemperatureRangeProcessor = DataProcessor.create(currentValue);
        thermalIsoThermUpperValueProcessor = DataProcessor.create(defaultValue);
        thermalIsoThermLowerValueProcessor = DataProcessor.create(defaultValue);
    }

    @Override
    protected void inSetup() {
        cameraPaletteKey = djiSdkModel.createLensKey(CameraKey.THERMAL_PALETTE,cameraIndex,lensIndex);
        currentGainModeTemperatureRangeKey = djiSdkModel.createLensKey(CameraKey.CURRENT_THERMAL_GAIN_MODE_TEMPERATURE_RANGE,cameraIndex,lensIndex);
        thermalGainModeKey = djiSdkModel.createLensKey(CameraKey.THERMAL_GAIN_MODE,cameraIndex,lensIndex);
        thermalIsoThermUpperValueKey = djiSdkModel.createLensKey(CameraKey.THERMAL_ISOTHERM_UPPER_VALUE,cameraIndex,lensIndex);
        thermalIsoThermLowerValueKey = djiSdkModel.createLensKey(CameraKey.THERMAL_ISOTHERM_LOWER_VALUE,cameraIndex,lensIndex);

        bindDataProcessor(cameraPaletteKey, paletteProcessor);
        bindDataProcessor(currentGainModeTemperatureRangeKey,currentGainModeTemperatureRangeProcessor);
        bindDataProcessor(thermalGainModeKey,thermalGainModeDataProcessor);
        bindDataProcessor(thermalIsoThermUpperValueKey,thermalIsoThermUpperValueProcessor);
        bindDataProcessor(thermalIsoThermLowerValueKey,thermalIsoThermLowerValueProcessor);
    }

    @Override
    public void updateCameraSource(@NonNull SettingDefinitions.CameraIndex cameraIndex, @NonNull SettingsDefinitions.LensType lensType) {
        this.cameraIndex = cameraIndex.getIndex();
        this.lensIndex = lensType.value();
        restart();
    }

    @Override
    protected void inCleanup() {
    }

    public Flowable<SettingsDefinitions.ThermalPalette> getPaletteProcessor() {
        return paletteProcessor.toFlowable();
    }

    public Flowable<Integer> getThermalIsoThermUpperValueProcessor() {
        return thermalIsoThermUpperValueProcessor.toFlowable();
    }

    public Flowable<Integer> getThermalIsoThermLowerValueProcessor() {
        return thermalIsoThermLowerValueProcessor.toFlowable();
    }

    public Flowable<Float[]> getCurrentGainTemperatureRangeProcessor() {
        return currentGainModeTemperatureRangeProcessor.toFlowable();
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
