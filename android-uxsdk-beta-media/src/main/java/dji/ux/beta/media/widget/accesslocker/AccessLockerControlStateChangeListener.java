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

package dji.ux.beta.media.widget.accesslocker;

import androidx.annotation.NonNull;

import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerChangePasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerEnterPasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerFormatAircraftWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerRemovePasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerSelectActionWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerSetPasswordWidget;

/**
 * Interface used to communicate the state of the {@link AccessLockerControlWidget}.
 */
public interface AccessLockerControlStateChangeListener {
    enum AccessLockerControlState {
        /**
         * Show {@link AccessLockerSelectActionWidget}
         */
        SELECT_ACTION,
        /**
         * Show {@link AccessLockerSetPasswordWidget}
         */
        SET_PASSWORD,
        /**
         * Show {@link AccessLockerEnterPasswordWidget}
         */
        ENTER_PASSWORD,
        /**
         * Show {@link AccessLockerChangePasswordWidget}
         */
        CHANGE_PASSWORD,
        /**
         * Show {@link AccessLockerRemovePasswordWidget}
         */
        REMOVE_PASSWORD,
        /**
         * Show {@link AccessLockerFormatAircraftWidget}
         */
        FORMAT_AIRCRAFT,
        /**
         * Hide the widget
         */
        CANCEL_DIALOG
    }

    /**
     * Callback to indicate state change
     *
     * @param controlState instance of {@link AccessLockerControlState}
     */
    void onStateChange(@NonNull AccessLockerControlState controlState);
}
