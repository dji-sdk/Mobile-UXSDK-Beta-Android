/*
 * Copyright (c) 2018-2021 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.cameracore.widget.cameraisoandeisetting;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.log.DJILog;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.FlatCameraModule;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.ProductUtil;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraISOAndEISettingWidget} used to define
 * the underlying logic and communication
 */
public class CameraISOAndEISettingWidgetModel extends WidgetModel {

    //region Fields
    private static final String LOCKED_ISO_VALUE = "500";
    private static final int DEFAULT_ISO_RANGE_LENGTH = 9;
    private static final String TAG = "ISOAndEISettingWidMod";
    private final DataProcessor<SettingsDefinitions.ISO> isoDataProcessor;
    private final DataProcessor<ExposureSettings> exposureSettingsDataProcessor;
    private final DataProcessor<SettingsDefinitions.ISO[]> isoRangeDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureSensitivityMode> exposureSensitivityModeDataProcessor;
    private final DataProcessor<Integer> eiValueDataProcessor;
    private final DataProcessor<Integer> recommendedEIValueDataProcessor;
    private final DataProcessor<int[]> eiValueRangeDataProcessor;
    private final DataProcessor<SettingsDefinitions.ExposureMode> exposureModeDataProcessor;
    private final DataProcessor<ISOInformation> isoInformationDataProcessor;
    private final DataProcessor<EIInformation> eiInformationDataProcessor;
    private final DataProcessor<Boolean> isRecordVideoEIModeDataProcessor;
    private final DataProcessor<Boolean> isAutoISOSupportedDataProcessor;
    private int cameraIndex = 0;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private CameraKey isoKey;
    private CameraKey eiValueKey;
    private FlatCameraModule flatCameraModule;
    private LensModule lensModule;
    //endregion

    //region Lifecycle
    public CameraISOAndEISettingWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        isoDataProcessor = DataProcessor.create(SettingsDefinitions.ISO.UNKNOWN);
        exposureSettingsDataProcessor = DataProcessor.create(new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        isoRangeDataProcessor = DataProcessor.create(getDefaultExposureCompensationArray());
        exposureSensitivityModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureSensitivityMode.UNKNOWN);
        eiValueDataProcessor = DataProcessor.create(0);
        recommendedEIValueDataProcessor = DataProcessor.create(0);
        eiValueRangeDataProcessor = DataProcessor.create(new int[0]);
        exposureModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN);

        isoInformationDataProcessor = DataProcessor.create(new ISOInformation(
                new SettingsDefinitions.ISO[]{},
                SettingsDefinitions.ISO.UNKNOWN.value(),
                false,
                false));
        eiInformationDataProcessor = DataProcessor.create(new EIInformation(new int[]{}, 0, 0));
        isRecordVideoEIModeDataProcessor = DataProcessor.create(false);
        isAutoISOSupportedDataProcessor = DataProcessor.create(false);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        isoKey = lensModule.createLensKey(CameraKey.ISO, cameraIndex, lensType.value());
        bindDataProcessor(isoKey, isoDataProcessor, newValue ->
                updateISOInformation((SettingsDefinitions.ISO) newValue));
        CameraKey exposureSettingsKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        bindDataProcessor(exposureSettingsKey, exposureSettingsDataProcessor, newValue ->
                updateISOValue(((ExposureSettings) newValue).getISO()));
        CameraKey isoRangeKey = lensModule.createLensKey(CameraKey.ISO_RANGE, cameraIndex, lensType.value());
        bindDataProcessor(isoRangeKey, isoRangeDataProcessor);
        CameraKey exposureSensitivityModeKey = CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex);
        bindDataProcessor(exposureSensitivityModeKey, exposureSensitivityModeDataProcessor);
        eiValueKey = CameraKey.create(CameraKey.EI_VALUE, cameraIndex);
        bindDataProcessor(eiValueKey, eiValueDataProcessor, newValue ->
                updateEIInformation(eiValueRangeDataProcessor.getValue(), (int) newValue, recommendedEIValueDataProcessor.getValue()));
        CameraKey recommendedEIValueKey = CameraKey.create(CameraKey.RECOMMENDED_EI_VALUE, cameraIndex);
        bindDataProcessor(recommendedEIValueKey, recommendedEIValueDataProcessor, newValue ->
                updateEIInformation(eiValueRangeDataProcessor.getValue(), eiValueDataProcessor.getValue(), (int) newValue));
        CameraKey eiValueRangeKey = CameraKey.create(CameraKey.EI_VALUE_RANGE, cameraIndex);
        bindDataProcessor(eiValueRangeKey, eiValueRangeDataProcessor, newValue ->
                updateEIInformation((int[]) newValue, eiValueDataProcessor.getValue(), recommendedEIValueDataProcessor.getValue()));
        CameraKey exposureModeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(value -> {
                    if (value) {
                        restart();
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")));
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        updateRecordVideoEIMode(flatCameraModule.getCameraModeDataProcessor().getValue(), exposureSensitivityModeDataProcessor.getValue());
        updateAutoISO(isoRangeDataProcessor.getValue(), isRecordVideoEIModeDataProcessor.getValue(), exposureModeDataProcessor.getValue());
    }
    //endregion

    //region data

    /**
     * Get the current index of the camera the widget model is reacting to
     *
     * @return current camera index
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    /**
     * Set the index of the camera for which the widget model should react
     *
     * @param cameraIndex camera index
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        flatCameraModule.setCameraIndex(cameraIndex);
        lensModule.setCameraIndex(this, cameraIndex);
        restart();
    }

    /**
     * Get the current type of the lens the widget model is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return lensType;
    }

    /**
     * Set the type of the lens for which the widget model should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        this.lensType = lensType;
        restart();
    }

    /**
     * Get the ISO information.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<ISOInformation> getISOInformation() {
        return isoInformationDataProcessor.toFlowable();
    }

    /**
     * Get the EI information.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<EIInformation> getEIInformation() {
        return eiInformationDataProcessor.toFlowable();
    }

    /**
     * Get whether the camera is in both Record Video mode and EI mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<Boolean> isRecordVideoEIMode() {
        return isRecordVideoEIModeDataProcessor.toFlowable();
    }

    /**
     * Get whether the camera supports automatic ISO
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<Boolean> isAutoISOSupported() {
        return isAutoISOSupportedDataProcessor.toFlowable();
    }
    //endregion

    //region actions

    /**
     * Set the ISO to auto or the value at the given index within the ISO range.
     *
     * @param isAuto   Whether the ISO is auto
     * @param isoIndex The index of the ISO within the ISO range to set when isAuto is false
     * @return Completable representing success or failure of action
     */
    @NonNull
    public Completable setISO(boolean isAuto, int isoIndex) {
        SettingsDefinitions.ISO newISO = null;
        if (isAuto) {
            newISO = SettingsDefinitions.ISO.AUTO;
        } else {
            if (isoIndex < isoRangeDataProcessor.getValue().length) {
                newISO = isoRangeDataProcessor.getValue()[isoIndex];
            }
        }
        if (newISO != null) {
            return djiSdkModel.setValue(isoKey, newISO);
        } else {
            return Completable.complete();
        }
    }

    /**
     * Set the EI to the value at the given index within the EI range.
     *
     * @param eiIndex The index of the EI within the EI range
     * @return Completable representing success or failure of action
     */
    @NonNull
    public Completable setEI(int eiIndex) {
        int ei = 0;
        if (eiIndex < eiValueRangeDataProcessor.getValue().length) {
            ei = eiValueRangeDataProcessor.getValue()[eiIndex];
        }
        return djiSdkModel.setValue(eiValueKey, ei);
    }

    /**
     * Get the value of the ISO as String when the ISO is the value at the given index within the
     * ISO range.
     *
     * @param isoIndex The index of the ISO within the ISO range
     * @return The value of the ISO as a String
     */
    @NonNull
    public String getISOText(int isoIndex) {
        if (isoInformationDataProcessor.getValue().isISOLocked()) {
            // By referring to DJIGo4 in both iOS and Android version
            // Showing the ISO_FIXED as locked value 500
            return LOCKED_ISO_VALUE;
        } else {
            if (isoIndex < isoRangeDataProcessor.getValue().length) {
                int cameraISO = CameraUtil.convertISOToInt(isoRangeDataProcessor.getValue()[isoIndex]);
                return String.valueOf(cameraISO);
            } else {
                return "";
            }
        }
    }

    /**
     * Get the value of the EI as String when the EI is the value at the given index within the
     * EI range.
     *
     * @param eiIndex The index of the EI within the EI range
     * @return The value of the EI as a String
     */
    @NonNull
    public String getEIText(int eiIndex) {
        if (eiIndex < eiValueRangeDataProcessor.getValue().length) {
            return String.valueOf(eiValueRangeDataProcessor.getValue()[eiIndex]);
        } else {
            return "";
        }
    }
    //endregion

    //region helpers
    @NonNull
    private SettingsDefinitions.ISO[] getDefaultExposureCompensationArray() {
        SettingsDefinitions.ISO[] evRange = new SettingsDefinitions.ISO[DEFAULT_ISO_RANGE_LENGTH];
        for (int i = 0; i < DEFAULT_ISO_RANGE_LENGTH; i++) {
            evRange[i] = SettingsDefinitions.ISO.find(i + 3);
        }
        return evRange;
    }

    private void updateISOValue(int isoValue) {
        SettingsDefinitions.ISO iso = CameraUtil.convertIntToISO(isoValue);
        updateISOInformation(iso);
    }

    private void updateISOInformation(@NonNull SettingsDefinitions.ISO iso) {
        SettingsDefinitions.ISO[] isoRange = updateISORangeValue(isoRangeDataProcessor.getValue());
        int isoValue = CameraUtil.convertISOToInt(iso);
        isoInformationDataProcessor.onNext(new ISOInformation(
                isoRange,
                isoValue,
                iso == SettingsDefinitions.ISO.FIXED,
                iso == SettingsDefinitions.ISO.AUTO));
    }

    private void updateRecordVideoEIMode(@NonNull SettingsDefinitions.CameraMode cameraMode,
                                         @NonNull SettingsDefinitions.ExposureSensitivityMode exposureSensitivityMode) {
        isRecordVideoEIModeDataProcessor.onNext(cameraMode == SettingsDefinitions.CameraMode.RECORD_VIDEO
                && exposureSensitivityMode == SettingsDefinitions.ExposureSensitivityMode.EI);
    }

    private void updateAutoISO(@NonNull SettingsDefinitions.ISO[] isoRange,
                               boolean isRecordVideoEIMode,
                               @NonNull SettingsDefinitions.ExposureMode exposureMode) {
        isAutoISOSupportedDataProcessor.onNext(isoRangeContainsAuto(isoRange)
                && !isRecordVideoEIMode
                && ProductUtil.isAutoISOSupportedProduct());

        if (!ProductUtil.isAutoISOSupportedProduct() && exposureMode != SettingsDefinitions.ExposureMode.MANUAL) {
            addDisposable(setISO(true, getIndexOfISO())
                    .subscribe(() -> DJILog.d(TAG, "set auto iso success"),
                            error -> DJILog.d(TAG, "set auto iso fail " + error.toString())));
        }
    }

    private boolean isoRangeContainsAuto(@NonNull SettingsDefinitions.ISO[] array) {
        for (SettingsDefinitions.ISO iso : array) {
            if (iso == SettingsDefinitions.ISO.AUTO) {
                return true;
            }
        }
        return false;
    }

    private int getIndexOfISO() {
        for (int i = 0; i < isoRangeDataProcessor.getValue().length; i++) {
            if (isoRangeDataProcessor.getValue()[i] == isoDataProcessor.getValue()) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    private SettingsDefinitions.ISO[] updateISORangeValue(@NonNull SettingsDefinitions.ISO[] array) {

        SettingsDefinitions.ISO[] newISOValues;

        if (isoRangeContainsAuto(array)) {
            newISOValues = new SettingsDefinitions.ISO[array.length - 1];
        } else {
            newISOValues = new SettingsDefinitions.ISO[array.length];
        }

        // remove the auto value
        for (int i = 0, j = 0; i < array.length; i++) {
            if (array[i] != SettingsDefinitions.ISO.AUTO) {
                newISOValues[j] = array[i];
                j++;
            }
        }

        return newISOValues;
    }

    private void updateEIInformation(@NonNull int[] eiRange, int eiValue, int eiRecommendedValue) {
        eiInformationDataProcessor.onNext(new EIInformation(
                eiRange,
                eiValue,
                eiRecommendedValue));
    }
    //endregion

    //region classes

    /**
     * The information related to the camera's ISO
     */
    public static class ISOInformation {
        private final SettingsDefinitions.ISO[] isoRange;
        private final int isoValue;
        private final boolean isISOLocked;
        private final boolean isISOAuto;

        @VisibleForTesting
        protected ISOInformation(@NonNull SettingsDefinitions.ISO[] isoRange,
                                 int isoValue,
                                 boolean isISOLocked,
                                 boolean isISOAuto) {
            this.isoRange = isoRange;
            this.isoValue = isoValue;
            this.isISOLocked = isISOLocked;
            this.isISOAuto = isISOAuto;
        }

        /**
         * Get the ISO range
         *
         * @return The ISO range
         */
        @NonNull
        public SettingsDefinitions.ISO[] getIsoRange() {
            return isoRange;
        }

        /**
         * Get the ISO value
         *
         * @return The ISO value
         */
        public int getIsoValue() {
            return isoValue;
        }

        /**
         * Get whether the ISO is locked
         *
         * @return True if the ISO is {@link SettingsDefinitions.ISO#FIXED}, false otherwise
         */
        public boolean isISOLocked() {
            return isISOLocked;
        }

        /**
         * Get whether the ISO is auto
         *
         * @return True if the ISO is {@link SettingsDefinitions.ISO#AUTO}, false otherwise
         */
        public boolean isISOAuto() {
            return isISOAuto;
        }
    }

    /**
     * The information related to the camera's EI
     */
    public static class EIInformation {
        private final int[] eiRange;
        private final int eiValue;
        private final int eiRecommendedValue;

        @VisibleForTesting
        protected EIInformation(@NonNull int[] eiRange,
                                int eiValue,
                                int eiRecommendedValue) {
            this.eiRange = eiRange;
            this.eiValue = eiValue;
            this.eiRecommendedValue = eiRecommendedValue;
        }

        /**
         * Get the EI range
         *
         * @return The EI range
         */
        public int[] getEiRange() {
            return eiRange;
        }

        /**
         * Get the EI value
         *
         * @return The EI value
         */
        public int getEiValue() {
            return eiValue;
        }

        /**
         * Get the EI recommended value
         *
         * @return The EI recommended value
         */
        public int getEiRecommendedValue() {
            return eiRecommendedValue;
        }
    }
    //endregion
}
