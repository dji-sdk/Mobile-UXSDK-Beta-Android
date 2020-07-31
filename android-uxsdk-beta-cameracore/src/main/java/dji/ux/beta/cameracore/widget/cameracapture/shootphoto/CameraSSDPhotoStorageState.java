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

package dji.ux.beta.cameracore.widget.cameracapture.shootphoto;

import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions;

/**
 * Class represents SSD Storage state for Shoot Photo Mode
 */
public class CameraSSDPhotoStorageState extends CameraPhotoStorageState {
    private SSDOperationState storageOperationState;

    public CameraSSDPhotoStorageState(SettingsDefinitions.StorageLocation storageLocation, long availableCaptureCount, SSDOperationState storageOperationState) {
        super(storageLocation, availableCaptureCount);
        this.storageOperationState = storageOperationState;
    }

    /**
     * Get operation state of SSD
     *
     * @return {@link SSDOperationState}
     */
    public SSDOperationState getStorageOperationState() {
        return storageOperationState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraSSDPhotoStorageState)) return false;
        if (!super.equals(o)) return false;
        CameraSSDPhotoStorageState that = (CameraSSDPhotoStorageState) o;
        return storageOperationState == that.storageOperationState;
    }

    @Override
    public int hashCode() {
        return 31 * storageOperationState.value();
    }
}
