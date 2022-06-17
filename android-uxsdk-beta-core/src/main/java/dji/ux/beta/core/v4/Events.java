package dji.ux.beta.core.v4;

import androidx.annotation.ColorRes;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.FocusMode;

public class Events {

    /**
     * Event that controls visibility of PreFlightChecklist
     */
    public static final class PreFlightCheckListControlEvent {

    }

    /**
     * Event that controls visibility of RTK Panel
     */
    public static final class RTKPanelControlEvent {

    }

    /**
     * Event that controls visibility of Spotlight Panel
     */
    public static final class SpotlightPanelControlEvent {

    }

    /**
     * Event that controls visibility of Simulator Panel
     */
    public static final class SimulatorPanelControlEvent {

    }

    /**
     * Event that controls the change of Auto Focus Mode
     */
    public static final class AutoFocusTypeEvent {

    }

    public static final class OverexposureWarningStatus {
        private final boolean isEnabled;
        private final int index;

        public OverexposureWarningStatus(boolean isEnabled, int index) {
            this.isEnabled = isEnabled;
            this.index = index;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * Event will load the preset values in the
     * Simulator Panel
     */
    public static final class SimulatorLoadPresetEvent {
        private double latitude;
        private double longitude;
        private int satelliteCount;
        private int frequency;

        public SimulatorLoadPresetEvent(double latitude, double longitude, int satelliteCount, int frequency) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.satelliteCount = satelliteCount;
            this.frequency = frequency;
        }

        public double getLatitude() {
            return this.latitude;
        }

        public double getLongitude() {
            return this.longitude;
        }

        public int getSatelliteCount() {
            return this.satelliteCount;
        }

        public int getFrequency() {
            return this.frequency;
        }

    }

    /**
     * Event that controls visibility of CameraSettingExposurePanelControl
     */
    public static final class CameraSettingExposurePanelControlEvent {
        private final boolean shouldShow;
        private final int index;

        public CameraSettingExposurePanelControlEvent(boolean shouldShow, int index) {
            this.shouldShow = shouldShow;
            this.index = index;
        }

        public boolean shouldShow() {
            return shouldShow;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * Event that controls visibility of CameraSettingAdvancedPanelControl
     */
    public static final class CameraSettingAdvancedPanelControlEvent {
        private final boolean shouldShow;
        private final int index;

        public CameraSettingAdvancedPanelControlEvent(boolean shouldShow, int index) {
            this.shouldShow = shouldShow;
            this.index = index;
        }

        public boolean shouldShow() {
            return shouldShow;
        }

        public int getIndex() {
            return index;
        }
    }

    public static final class DualCameraSyncCaptureEvent {
        private final int index;
        private final boolean isRecording;
        private final SettingsDefinitions.CameraMode cameraMode;

        public DualCameraSyncCaptureEvent(int index, SettingsDefinitions.CameraMode mode, boolean isRecording) {
            this.index = index;
            this.cameraMode = mode;
            this.isRecording = isRecording;
        }

        public int getIndex() {
            return index;
        }

        public SettingsDefinitions.CameraMode getCameraMode() {
            return cameraMode;
        }

        public boolean isRecording() {
            return isRecording;
        }

        @Override
        public String toString() {
            return "DualCameraSyncCaptureEvent{"
                    + "index="
                    + index
                    + ", isRecording="
                    + isRecording
                    + ", cameraMode="
                    + cameraMode
                    + '}';
        }
    }

    public static final class DualCameraSyncModeEvent {
        private final int index;
        private final boolean videoMode;

        public DualCameraSyncModeEvent(int index, boolean videoMode) {
            this.index = index;
            this.videoMode = videoMode;
        }

        public int getIndex() {
            return index;
        }

        public boolean isVideoMode() {
            return videoMode;
        }

        @Override
        public String toString() {
            return "DualCameraSyncModeEvent{" + "index=" + index + ", videoMode=" + videoMode + '}';
        }
    }

    public static final class CameraBusyEvent {
        private final boolean isBusy;

        public CameraBusyEvent(boolean isBusy) {
            this.isBusy = isBusy;
        }

        public boolean isBusy() {
            return isBusy;
        }
    }

    public static final class CameraModeChangedEvent {
        private final int index;
        private final SettingsDefinitions.CameraMode mode;

        public CameraModeChangedEvent(int index, SettingsDefinitions.CameraMode mode) {
            this.index = index;
            this.mode = mode;
        }

        public int getIndex() {
            return index;
        }

        public SettingsDefinitions.CameraMode getMode() {
            return mode;
        }

        @Override
        public String toString() {
            return "CameraModeChangedEvent{" + "index=" + index + ", mode=" + mode.name() + '}';
        }
    }

    /**
     * Focus mode event
     */
    public static final class FocusModeSetEvent {
        public final FocusMode focusMode;

        public FocusModeSetEvent(FocusMode mode) {
            focusMode = mode;
        }
    }

    public static final class ThermalCameraSceneSettingPanelEvent {
        private final int keyIndex;
        private final boolean shouldShow;

        public ThermalCameraSceneSettingPanelEvent(int keyIndex, boolean shouldShow) {
            this.keyIndex = keyIndex;
            this.shouldShow = shouldShow;
        }

        public int getKeyIndex() {
            return keyIndex;
        }

        public boolean isShouldShow() {
            return shouldShow;
        }
    }

    public static final class TempAlertEvent {
        private final boolean enable;
        private final float threshold;

        public TempAlertEvent(boolean enable, float threshold) {
            this.enable = enable;
            this.threshold = threshold;
        }

        public boolean isEnable() {
            return enable;
        }

        public float getThreshold() {
            return threshold;
        }
    }

    public static final class TemperatureUnitChangedEvent {

    }

    public static final class XT2PIPModeValueChangedEvent {

    }

    /**
     * Event that loads Page Change event of Multi page Dialog
     */
    public static final class MultiPageDialogPageChangeEvent {

        private final Object object;
        private Page changePage;
        private int pageIndex;

        public MultiPageDialogPageChangeEvent(Object object, Page changePage) {
            this(object, changePage, 0);
        }

        public MultiPageDialogPageChangeEvent(Object object, Page changePage, int index) {

            this.object = object;
            this.changePage = changePage;
            this.pageIndex = index;
        }

        public Page getChangePageTo() {
            return changePage;
        }

        public int getPageIndex() {
            return this.pageIndex;
        }

        public Object getObject() {
            return object;
        }

        public enum Page {
            NEXT,
            PREVIOUS,
            INDEX
        }
    }

    /**
     * Event that Dismiss event of Multi page Dialog
     */
    public static final class MultiPageDialogDismissEvent {

    }

    public static final class CenterPointColorEvent {

        @ColorRes
        private final int color;

        public CenterPointColorEvent(@ColorRes int color) {
            this.color = color;
        }

        @ColorRes
        public int getColor() {
            return color;
        }
    }

    /**
     * Event that controls visibility of Speaker Panel
     */
    public static final class SpeakerPanelControlEvent {

    }
}
