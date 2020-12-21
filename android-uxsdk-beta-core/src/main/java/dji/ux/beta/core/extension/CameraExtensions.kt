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

@file:JvmName("CameraExtensions")

package dji.ux.beta.core.extension

import dji.common.camera.SettingsDefinitions.FlatCameraMode
import dji.common.camera.SettingsDefinitions.ShootPhotoMode

/**
 * Convert [FlatCameraMode] to [ShootPhotoMode]
 */
fun FlatCameraMode.toShootPhotoMode(): ShootPhotoMode {
    return when (this) {
        FlatCameraMode.PHOTO_SINGLE -> ShootPhotoMode.SINGLE
        FlatCameraMode.PHOTO_HDR -> ShootPhotoMode.HDR
        FlatCameraMode.PHOTO_BURST -> ShootPhotoMode.BURST
        FlatCameraMode.PHOTO_AEB -> ShootPhotoMode.AEB
        FlatCameraMode.PHOTO_INTERVAL -> ShootPhotoMode.INTERVAL
        FlatCameraMode.PHOTO_PANORAMA -> ShootPhotoMode.PANORAMA
        FlatCameraMode.PHOTO_EHDR -> ShootPhotoMode.EHDR
        FlatCameraMode.PHOTO_HYPER_LIGHT -> ShootPhotoMode.HYPER_LIGHT
        FlatCameraMode.PHOTO_TIME_LAPSE -> ShootPhotoMode.TIME_LAPSE
        else -> ShootPhotoMode.UNKNOWN
    }
}

/**
 * Convert [ShootPhotoMode] to [FlatCameraMode]
 */
fun ShootPhotoMode.toFlatCameraMode(): FlatCameraMode {
    return when (this) {
        ShootPhotoMode.SINGLE -> FlatCameraMode.PHOTO_SINGLE
        ShootPhotoMode.HDR -> FlatCameraMode.PHOTO_HDR
        ShootPhotoMode.BURST -> FlatCameraMode.PHOTO_BURST
        ShootPhotoMode.AEB -> FlatCameraMode.PHOTO_AEB
        ShootPhotoMode.INTERVAL -> FlatCameraMode.PHOTO_INTERVAL
        ShootPhotoMode.PANORAMA -> FlatCameraMode.PHOTO_PANORAMA
        ShootPhotoMode.EHDR -> FlatCameraMode.PHOTO_EHDR
        ShootPhotoMode.HYPER_LIGHT -> FlatCameraMode.PHOTO_HYPER_LIGHT
        ShootPhotoMode.TIME_LAPSE -> FlatCameraMode.PHOTO_TIME_LAPSE
        else -> FlatCameraMode.UNKNOWN
    }
}

/**
 * Check if flat camera mode is picture mode
 */
fun FlatCameraMode.isPictureMode(): Boolean {
    return this == FlatCameraMode.PHOTO_TIME_LAPSE
            || this == FlatCameraMode.PHOTO_AEB
            || this == FlatCameraMode.PHOTO_SINGLE
            || this == FlatCameraMode.PHOTO_BURST
            || this == FlatCameraMode.PHOTO_HDR
            || this == FlatCameraMode.PHOTO_INTERVAL
            || this == FlatCameraMode.PHOTO_HYPER_LIGHT
            || this == FlatCameraMode.PHOTO_PANORAMA
            || this == FlatCameraMode.PHOTO_EHDR
}