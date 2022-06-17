package dji.ux.beta.core.v4;

import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.camera.SettingsDefinitions.PhotoPanoramaMode;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.ux.beta.core.R;

public class CameraResource {

    public static int[] pictureStyleImgRes = new int[] {
        R.drawable.uxsdk_ic_advanced_more_none, R.drawable.uxsdk_ic_advanced_more_ps_landscape,
        R.drawable.uxsdk_ic_advanced_more_ps_soft, R.drawable.uxsdk_ic_advanced_more_ps_custom
    };

    /**
     * Corresponds to 4 categories: Standard, Landscape, Soft, Custom
     * Each category has 3 numbers: Sharpness, Contrast, Saturation
     */
    public static byte[][] pictureStyleValue = new byte[][] {
        {
            0, 0, 0
        },
        {
            1, 1, 0
        },
        {
            -1, 0, 0
        },
        {
            0, 1, 0
        }
    };

    public static int[] videoFormatImgRes = new int[] {
        R.drawable.uxsdk_ic_advanced_more_vsf_mov, R.drawable.uxsdk_ic_advanced_more_vsf_mp4
    };

    public static int[] thermalVideoFormatImgRes = new int[] {
        R.drawable.uxsdk_ic_advanced_more_vsf_mov,
        R.drawable.uxsdk_ic_advanced_more_vsf_mp4,
        R.drawable.uxsdk_ic_camera_setting_tiffs,
        R.drawable.uxsdk_ic_camera_setting_seq
    };

    public static int[] videoTypeImgRes = new int[] {
        R.drawable.uxsdk_ic_advanced_more_vt_pal, R.drawable.uxsdk_ic_advanced_more_videotype_ntsc
    };

    public static int[][] videoFpsImgResIds = new int[][] {
        {  // 640x480
        }, { // 640x512
            0,
            0,
            R.drawable.uxsdk_ic_advanced_more_vf_640x512_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_640x512_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_640x512_30p
        }, { // 1280x720
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_60p,
            // true 60p
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_96p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_100p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_120p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_180p,
            R.drawable.uxsdk_ic_advanced_more_vf_1280x720_240p
        }, { // 1920x1080
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_24p,
            // true 24p
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_30p,
            // true 30p
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_48p,
            // true 48p
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_60p,
            // true 60p
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_96p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_100p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_120p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_180p,
            R.drawable.uxsdk_ic_advanced_more_vf_1920x1080_240p
        }, { // 2704x1520
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p
            // true60p
        }, { // 2720x1530
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p
            // true60p
        }, { // No 3840x1572 icon, so reuse existing 4K icons.
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_48p,
            // true48p
        }, { // 3840x2160
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_3840x2160_60p
            // true60p
        }, { // 4096x2160
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_4096x2160_60p
            // true60p
        },
        // Below two arrays are not used yet.
        { // 4608x2160
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_30p
            // true30p
        }, { // no icons for 4608x2592, so reuse existing 4.5K icons for now.
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_4608x2160_30p
            // true30p
        }, { // 5280x2160
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_48p,
            // true48p
        }, { // no icons for RESOLUTION_MAX, For X5S and X4S, the maximum resolution is 5280x2972, so reuse existing 5K icons for now.
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p
            // true30p
        }, { // icon for NO_SSD_VIDEO.
            R.drawable.uxsdk_ic_advanced_more_none
        }, { // icon for 5760X3240 16:9  6K.
            R.drawable.uxsdk_ic_advanced_more_vf_6k_24p, //23.976
            R.drawable.uxsdk_ic_advanced_more_vf_6k_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_6k_25p, //
            R.drawable.uxsdk_ic_advanced_more_vf_6k_30p, // 29.970
            R.drawable.uxsdk_ic_advanced_more_vf_6k_30p
        }, { // 6016X3200
            R.drawable.uxsdk_ic_advanced_more_vf_6k_24p, //23.976
            R.drawable.uxsdk_ic_advanced_more_vf_6k_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_6k_25p, //
            R.drawable.uxsdk_ic_advanced_more_vf_6k_30p, // 29.970
            R.drawable.uxsdk_ic_advanced_more_vf_6k_30p
        }, { // 2048x1080
            0,
            0,
            0,
            0,
            0,
            R.drawable.uxsdk_ic_advanced_more_vf_2048x1080_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_2048x1080_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_2048x1080_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_2048x1080_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_2048x1080_60p
        }, { // no icons for 5280x2972, so reuse existing 5K icons for now
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_5280x2160_30p
            // true30p
        }, { // 336x256
            0,
            0,
            R.drawable.uxsdk_ic_advanced_more_vf_336x256_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_336x256_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_336x256_30p
        },{ // 3712x2088
            0,
            0,
            0,
            0,
            0,
            R.drawable.uxsdk_ic_advanced_more_vf_3712x2088_48p, //47.950
            R.drawable.uxsdk_ic_advanced_more_vf_3712x2088_48p, //48
            R.drawable.uxsdk_ic_advanced_more_vf_3712x2088_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_3712x2088_60p, // 59.940
            R.drawable.uxsdk_ic_advanced_more_vf_3712x2088_60p
        },{ // 3944x2088
            0,
            0,
            0,
            0,
            0,
            R.drawable.uxsdk_ic_advanced_more_vf_3944x2088_48p, //47.950
            R.drawable.uxsdk_ic_advanced_more_vf_3944x2088_48p, //48
            R.drawable.uxsdk_ic_advanced_more_vf_3944x2088_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_3944x2088_60p, // 59.940
            R.drawable.uxsdk_ic_advanced_more_vf_3944x2088_60p
        }, { //2588x1512
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_24p,
            // true24p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_25p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_30p,
            // true30p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_48p,
            // true48p
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_50p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p,
            R.drawable.uxsdk_ic_advanced_more_vf_2p7k_60p
        }
    };

    public static int[] getPhotoModeStrId() {
        return new int[] {
            R.string.uxsdk_camera_photomode_single,
            R.string.uxsdk_camera_photomode_hdr,
            R.string.uxsdk_camera_photomode_burst,
            R.string.uxsdk_camera_photomode_aeb,
            R.string.uxsdk_camera_photomode_time,
            R.string.uxsdk_camera_photomode_timelapse,
            R.string.uxsdk_camera_photomode_panorama,
            R.string.uxsdk_camera_photomode_raw_burst,
            R.string.uxsdk_camera_photomode_shallow_focus,
            R.string.uxsdk_camera_photomode_e_hdr,
            R.string.uxsdk_camera_photomode_hyper_light

        };
    }

    public static int[] getFlatModeStrId() {
        return new int[] {
            0,
            0,
            R.string.uxsdk_camera_photomode_timelapse,
            0,
            R.string.uxsdk_camera_photomode_aeb,
            R.string.uxsdk_camera_photomode_single,
            R.string.uxsdk_camera_photomode_burst,
            R.string.uxsdk_camera_photomode_hdr,
            R.string.uxsdk_camera_photomode_time,
            0,
            0,
            R.string.uxsdk_camera_photomode_hyper_light,
            R.string.uxsdk_camera_photomode_panorama,
            0,
            R.string.uxsdk_camera_photomode_e_hdr,
        };
    }

    public static int[] getPhotoModeImageResId() {
        return new int[] {
            R.drawable.uxsdk_ic_photo_mode_nor,
            R.drawable.uxsdk_ic_photo_mode_hdr,
            R.drawable.uxsdk_ic_photo_mode_continuous,
            R.drawable.uxsdk_ic_photo_mode_aeb,
            R.drawable.uxsdk_ic_photo_mode_autotimer,
            R.drawable.uxsdk_ic_photo_mode_autotimer,
            R.drawable.uxsdk_ic_photo_mode_360,
            R.drawable.uxsdk_ic_photo_mode_raw_burst,
            R.drawable.uxsdk_ic_photo_mode_shallow_focus,
            R.drawable.uxsdk_ic_photo_mode_e_hdr,
            R.drawable.uxsdk_ic_photo_mode_hyper_light

        };
    }

    public static int[] getFlatModeImageResId() {
        return new int[] {
                R.drawable.uxsdk_ic_photo_mode_nor,
                R.drawable.uxsdk_ic_photo_mode_nor,
                R.drawable.uxsdk_ic_photo_mode_autotimer,
                R.drawable.uxsdk_ic_photo_mode_hdr,
                R.drawable.uxsdk_ic_photo_mode_aeb,
                R.drawable.uxsdk_ic_photo_mode_nor,
                R.drawable.uxsdk_ic_photo_mode_continuous,
                R.drawable.uxsdk_ic_photo_mode_hdr,
                R.drawable.uxsdk_ic_photo_mode_autotimer,
                R.drawable.uxsdk_ic_photo_mode_autotimer,
                R.drawable.uxsdk_ic_photo_mode_autotimer,
                R.drawable.uxsdk_ic_photo_mode_hyper_light,
                R.drawable.uxsdk_ic_photo_mode_360,
                R.drawable.uxsdk_ic_photo_mode_nor,
                R.drawable.uxsdk_ic_photo_mode_e_hdr
        };
    }

    public static int[] getPhotoModeDefaultCmdId() {
        return new int[] {
            ShootPhotoMode.SINGLE.value(),
            ShootPhotoMode.HDR.value(),
            ShootPhotoMode.BURST.value(),
            ShootPhotoMode.AEB.value(),
            ShootPhotoMode.INTERVAL.value(),
            ShootPhotoMode.TIME_LAPSE.value(),
            ShootPhotoMode.PANORAMA.value(),
            ShootPhotoMode.RAW_BURST.value(),
            ShootPhotoMode.SHALLOW_FOCUS.value(),
            ShootPhotoMode.EHDR.value(),
            ShootPhotoMode.HYPER_LIGHT.value()
        };
    }

    public static int[] getFlatModeDefaultCmdId() {
        return new int[] {
            FlatCameraMode.PHOTO_TIME_LAPSE.value(),
            FlatCameraMode.PHOTO_AEB.value(),
            FlatCameraMode.PHOTO_SINGLE.value(),
            FlatCameraMode.PHOTO_BURST.value(),
            FlatCameraMode.PHOTO_HDR.value(),
            FlatCameraMode.PHOTO_INTERVAL.value(),
            FlatCameraMode.PHOTO_PANORAMA.value()
        };
    }

    public static int[][] getPhotoModeChildDefaultValue() {
        return new int[][] {
            null, null, { 3, 5, 7 }, { 3, 5 }, { 10, 20, 30 }, null, {3, 7}, {3, 5, 7, 10, 14, 255}, null
        };
    }

    public static int[][] getFlatModeChildDefaultValue() {
        return new int[][] {
            null, null, null, null, { 3, 5 }, null, { 3, 5, 7 }, null, { 10, 20, 30 }, null, null, null, {3, 7}
        };
    }

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
        } else if (ShootPhotoMode.SHALLOW_FOCUS.value() == mode){
            return R.drawable.uxsdk_ic_photo_mode_shallow_focus;
        } else if (ShootPhotoMode.PANORAMA.value() == mode) {
            if (PhotoPanoramaMode.PANORAMA_MODE_3X1.value() == value) {
                return R.drawable.uxsdk_advanced_more_photomode_pano3x1;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_1X3.value() == value) {
                return R.drawable.uxsdk_pano_3x1;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_3X3.value() == value) {
                return R.drawable.uxsdk_pano_3x3;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SUPER_RESOLUTION.value() == value) {
                return R.drawable.uxsdk_pano_super_res;
            } else if (PhotoPanoramaMode.PANORAMA_MODE_SPHERE.value() == value) {
                return R.drawable.uxsdk_pano_sphere;
            } else {
                return R.drawable.uxsdk_pano_180;
            }
        } else if (ShootPhotoMode.EHDR.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_e_hdr;
        } else if (ShootPhotoMode.HYPER_LIGHT.value() == mode) {
            return R.drawable.uxsdk_ic_photo_mode_hyper_light;
        } else {
            return R.drawable.uxsdk_ic_photo_mode_nor;
        }
    }
}
