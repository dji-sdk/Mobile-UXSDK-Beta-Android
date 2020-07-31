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

package dji.ux.beta.core.base;

import dji.common.error.DJIError;

/**
 * Class containing UXSDKErrors with their descriptions
 */
public final class UXSDKErrorDescription extends DJIError {
    /**
     * The value type of the object does not match the value type of the key.
     */
    public static final UXSDKErrorDescription VALUE_TYPE_MISMATCH =
            new UXSDKErrorDescription("The value type of the object does not match the value type of the key.");

    /**
     * Simulator not running. Wind simulation can only be achieved while the simulator is active.
     */
    public static final UXSDKErrorDescription SIMULATOR_WIND_ERROR =
            new UXSDKErrorDescription("Simulator not running. Wind simulation can only be achieved while the simulator is active.");

    /**
     * Fly zone error
     */
    public static final UXSDKErrorDescription FLYZONE_ERROR =
            new UXSDKErrorDescription("FlyZoneManager not available.");

    /**
     * User account manager error
     */
    public static final UXSDKErrorDescription USER_ACCOUNT_MANAGER_ERROR =
            new UXSDKErrorDescription("UserAccountManager not available.");

    private UXSDKErrorDescription(String description) {
        super(description);
    }
}
