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

package dji.ux.beta.cameracore.util;

import dji.common.camera.SettingsDefinitions.PhotoPanoramaMode;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.ux.beta.cameracore.R;

public class CameraResource {

    public static int getPhotoModeImgResId(final int mode, final int value) {
        if (ShootPhotoMode.HDR.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_hdr;
        } else if (ShootPhotoMode.BURST.value() == mode) {
            if (value == 14) {
                return R.drawable.uxsdk_ic_photo_mode_continuous_14;
            } else if (value == 10) {
                return R.drawable.uxsdk_ic_photo_mode_continuous_10;
            } else if (value == 7) {
                return R.drawable.uxsdk_ic_photo_mode_continuous_7;
            } else if (value == 5) {
                return R.drawable.uxsdk_ic_photo_mode_continuous_5;
            } else {
                return R.drawable.uxsdk_ic_photo_mode_continuous_3;
            }
        } else if (ShootPhotoMode.RAW_BURST.value() == mode) {
            if (value == 255) {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_infinity;
            } else if (value == 14) {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_14;
            } else if (value == 10) {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_10;
            } else if (value == 7) {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_7;
            } else if (value == 5) {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_5;
            } else {
                return R.drawable.uxsdk_ic_photo_mode_raw_burst_3;
            }
        } else if (ShootPhotoMode.AEB.value() == mode) {
            if (value == 7) {
                return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_7;
            } else if (value == 5) {
                return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_5;
            } else {
                return R.drawable.uxsdk_ic_photo_mode_aeb_continuous_3;
            }
        } else if (ShootPhotoMode.INTERVAL.value() == mode) {
            if (value == 60) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_60s;
            } else if (value == 30) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_30s;
            } else if (value == 20) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_20s;
            } else if (value == 15) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_15s;
            } else if (value == 10) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_10s;
            } else if (value == 7) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_7s;
            } else if (value == 4) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_4s;
            } else if (value == 3) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_3s;
            } else if (value == 2) {
                return R.drawable.uxsdk_ic_photo_mode_timepause_2s;
            } else {
                return R.drawable.uxsdk_ic_photo_mode_timepause_5s;
            }
        } else if (ShootPhotoMode.SHALLOW_FOCUS.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_shallow_focus;
        } else if (ShootPhotoMode.PANORAMA.value() == mode) {
            if (PhotoPanoramaMode.PANORAMA_MODE_3X1.value() == value) {
                return R.drawable.uxsdk_ic_photo_mode_pano_3x1;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_1X3.value() == value) {
                return R.drawable.uxsdk_ic_photo_mode_pano_180;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_3X3.value() == value) {
                return R.drawable.uxsdk_ic_photo_mode_pano_3x3;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SUPER_RESOLUTION.value() == value) {
                return R.drawable.uxsdk_ic_photo_mode_nor;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SPHERE.value() == value) {
                return R.drawable.uxsdk_ic_photo_mode_pano_sphere;
            } else {
                return R.drawable.uxsdk_ic_photo_mode_pano_180;
            }
        } else if (ShootPhotoMode.EHDR.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        } else if (ShootPhotoMode.HYPER_LIGHT.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        }
    }
}
