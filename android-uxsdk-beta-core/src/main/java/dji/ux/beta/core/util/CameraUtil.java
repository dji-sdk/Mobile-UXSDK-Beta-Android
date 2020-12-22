/*
 * Copyright (c) 2018-2020 DJI
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

package dji.ux.beta.core.util;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dji.common.camera.CameraVideoStreamSource;
import dji.common.camera.SettingsDefinitions;
import dji.sdk.camera.Camera;
import dji.ux.beta.core.R;

/**
 * Utility class for displaying camera information.
 */
public final class CameraUtil {

    private static final String SHUTTER_SUBSTITUENT1 = "SHUTTER_SPEED_1_";
    private static final String SHUTTER_SUPPLANTER1 = "";

    private static final String SHUTTER_WIFI_REGEX_2 = "(\\d+)_DOT_(\\d+)";
    private static final String SHUTTER_WIFI_REPLACE_2 = "$1.$2";

    private static final String SHUTTER_SUBSTITUENT3 = "SHUTTER_SPEED_([0-9.]+)";
    private static final String SHUTTER_SUPPLANTER3 = "$1\"";

    private static final String SHUTTER_SUBSTITUENT4 = "DOT_(\\d+)";
    private static final String SHUTTER_SUPPLANTER4 = "1.$1\"";

    // Convert the  N_5_0 to -5.0, N_0_0 to 0, P_1_0 to +1.0
    private static final String EV_SUBSTITUENT1 = "N_0_0";
    private static final String EV_SUPPLANTER1 = "0";

    private static final String EV_SUBSTITUENT2 = "N_";
    private static final String EV_SUPPLANTER2 = "-";

    private static final String EV_SUBSTITUENT3 = "P_";
    private static final String EV_SUPPLANTER3 = "+";

    private static final String EV_SUBSTITUENT4 = "_";
    private static final String EV_SUPPLANTER4 = ".";

    private CameraUtil() {
        // prevent instantiation of util class
    }

    @NonNull
    public static String resolutionShortDisplayName(@NonNull SettingsDefinitions.VideoResolution resolution) {

        String shortName;

        switch (resolution) {
            case RESOLUTION_336x256:
                shortName = "256P";
                break;
            case RESOLUTION_640x480:
                shortName = "480P";
                break;
            case RESOLUTION_640x512:
                shortName = "512P";
                break;
            case RESOLUTION_1280x720:
                shortName = "720P";
                break;
            case RESOLUTION_1920x1080:
                shortName = "1080P";
                break;
            case RESOLUTION_2048x1080:
                shortName = "2K";
                break;
            case RESOLUTION_2688x1512:
            case RESOLUTION_2704x1520:
            case RESOLUTION_2720x1530:
                shortName = "2.7K";
                break;
            case RESOLUTION_3712x2088:
            case RESOLUTION_3840x1572:
            case RESOLUTION_3840x2160:
            case RESOLUTION_3944x2088:
            case RESOLUTION_4096x2160:
                shortName = "4K";
                break;
            case RESOLUTION_4608x2160:
            case RESOLUTION_4608x2592:
                shortName = "4.5K";
                break;
            case RESOLUTION_5280x2160:
            case RESOLUTION_5280x2972:
                shortName = "5K";
                break;
            case RESOLUTION_5760X3240:
            case RESOLUTION_6016X3200:
                shortName = "6K";
                break;
            default:
                shortName = "Unknown";
        }

        return shortName;
    }

    @NonNull
    public static String frameRateDisplayName(@NonNull SettingsDefinitions.VideoFrameRate frameRate) {
        final String originalFrameRateString = frameRate.toString();

        String processedFrameRateString;
        Matcher matcher = Pattern.compile("FRAME_RATE_(\\d{2,3})_DOT_.*").matcher(originalFrameRateString);
        if (matcher.find()) {
            String tempRate = matcher.group(1);
            processedFrameRateString = Integer.toString(Integer.valueOf(tempRate) + 1);
        } else {
            matcher = Pattern.compile("FRAME_RATE_(\\d{2,3})_FPS").matcher(originalFrameRateString);
            if (matcher.find()) {
                processedFrameRateString = matcher.group(1);
            } else {
                processedFrameRateString = "Null";
            }
        }

        return processedFrameRateString;

    }

    @NonNull
    public static String convertPhotoFileFormatToString(@NonNull Resources resources, @NonNull SettingsDefinitions.PhotoFileFormat photoFileFormat) {
        String formatString;
        if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.RAW.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_raw);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_jpeg);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.RAW_AND_JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_jpegraw);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_tiff);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.RADIOMETRIC_JPEG.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_radiometic_jpeg);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_low_tiff);
        } else if (photoFileFormat.value() == SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION.value()) {
            formatString = resources.getString(R.string.uxsdk_camera_picture_format_high_tiff);
        } else {
            formatString = photoFileFormat.toString();
        }

        return formatString;
    }

    @NonNull
    public static String formatVideoTime(@NonNull Resources resources, int flyTime) {
        String result;
        final int[] time = UnitConversionUtil.formatSecondToHour(flyTime);
        if (time[2] > 0) {
            result = resources.getString(R.string.uxsdk_video_time_hours, time[2], time[1], time[0]);
        } else {
            result = resources.getString(R.string.uxsdk_video_time, time[1], time[0]);
        }
        return result;

    }


    /**
     * Convert the Aperture enum name to the string to show on the UI.
     *
     * @param resources A resources object.
     * @param aperture  The aperture value to convert.
     * @return A String to display.
     */
    public static String apertureDisplayName(@NonNull Resources resources, @NonNull SettingsDefinitions.Aperture aperture) {
        String displayName;
        if (aperture == SettingsDefinitions.Aperture.UNKNOWN) {
            displayName = resources.getString(R.string.uxsdk_string_default_value);
        } else {
            // Convert the F_1p7 to 1.7, F_4p0 to 4, F_4p5 to 4.5
            int apertureValue = aperture.value();
            int apertureInteger = apertureValue / 100;
            // Just keep one decimal number, so divide by 10.
            int apertureDecimal = apertureValue % 100 / 10;
            if (apertureDecimal == 0) {
                displayName = String.format(Locale.US, "%d", apertureInteger);
            } else {
                displayName = String.format(Locale.US, "%d.%d", apertureInteger, apertureDecimal);
            }
        }

        return displayName;
    }

    /**
     * Convert the Shutter Speed enum name to the string to show on the UI.
     *
     * @param shutterSpeed The shutter speed value to convert.
     * @return A String to display.
     */
    @NonNull
    public static String shutterSpeedDisplayName(@NonNull SettingsDefinitions.ShutterSpeed shutterSpeed) {

        String shutterName = shutterSpeed.name();
        shutterName = shutterName.replace(SHUTTER_SUBSTITUENT1, SHUTTER_SUPPLANTER1).
                replaceAll(SHUTTER_WIFI_REGEX_2, SHUTTER_WIFI_REPLACE_2).
                replaceAll(SHUTTER_SUBSTITUENT3, SHUTTER_SUPPLANTER3).
                replaceAll(SHUTTER_SUBSTITUENT4, SHUTTER_SUPPLANTER4);

        return shutterName;
    }

    /**
     * Convert the Exposure Compensation enum name to the string to show on the UI.
     *
     * @param ev The exposure compensation value to convert.
     * @return A String to display.
     */
    @NonNull
    public static String exposureValueDisplayName(@NonNull SettingsDefinitions.ExposureCompensation ev) {
        String enumName = ev.name();
        return enumName.replace(EV_SUBSTITUENT1, EV_SUPPLANTER1).
                replaceAll(EV_SUBSTITUENT2, EV_SUPPLANTER2).
                replaceAll(EV_SUBSTITUENT3, EV_SUPPLANTER3).
                replaceAll(EV_SUBSTITUENT4, EV_SUPPLANTER4);
    }

    public static int convertISOToInt(@NonNull SettingsDefinitions.ISO iso) {
        if (iso == SettingsDefinitions.ISO.AUTO || iso == SettingsDefinitions.ISO.UNKNOWN) {
            return 0;
        }

        String name = iso.toString();
        String[] isoValue = name.split("_");
        return Integer.parseInt(isoValue[1]);
    }

    @NonNull
    public static SettingsDefinitions.ISO convertIntToISO(int isoValue) {
        if (isoValue > 0 && isoValue < 200) {
            return SettingsDefinitions.ISO.ISO_100;
        } else if (isoValue >= 200 && isoValue < 400) {
            return SettingsDefinitions.ISO.ISO_200;
        } else if (isoValue >= 400 && isoValue < 800) {
            return SettingsDefinitions.ISO.ISO_400;
        } else if (isoValue >= 800 && isoValue < 1600) {
            return SettingsDefinitions.ISO.ISO_800;
        } else if (isoValue >= 1600 && isoValue < 3200) {
            return SettingsDefinitions.ISO.ISO_1600;
        } else if (isoValue >= 3200 && isoValue < 6400) {
            return SettingsDefinitions.ISO.ISO_3200;
        } else if (isoValue >= 6400 && isoValue < 12800) {
            return SettingsDefinitions.ISO.ISO_6400;
        } else if (isoValue >= 12800 && isoValue < 25600) {
            return SettingsDefinitions.ISO.ISO_12800;
        } else if (isoValue >= 25600) {
            return SettingsDefinitions.ISO.ISO_25600;
        } else {
            return SettingsDefinitions.ISO.UNKNOWN;
        }
    }

    /**
     * Get the lens index based on the given stream source and camera name.
     *
     * @param streamSource The streamSource
     * @param cameraName   The name of the camera
     * @return The lens index
     */
    public static int getLensIndex(CameraVideoStreamSource streamSource, String cameraName) {
        if (streamSource == CameraVideoStreamSource.WIDE) {
            return SettingsDefinitions.LensType.WIDE.value();
        } else if (streamSource == CameraVideoStreamSource.INFRARED_THERMAL) {
            if (Camera.DisplayNameXT2_VL.equals(cameraName) ||
                    Camera.DisplayNameMavic2EnterpriseDual_VL.equals(cameraName)) {
                return Camera.XT2_IR_CAMERA_INDEX;
            } else {
                return SettingsDefinitions.LensType.INFRARED_THERMAL.value();
            }
        } else {
            return SettingsDefinitions.LensType.ZOOM.value();
        }
    }
}
