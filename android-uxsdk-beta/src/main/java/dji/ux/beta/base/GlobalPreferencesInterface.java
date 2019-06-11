/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.base;


import android.support.annotation.NonNull;
import dji.ux.beta.util.UnitConversionUtil;

/**
 * Interface to be implemented for functions included under
 * global preferences. These settings will persist across app restarts.
 */
public interface GlobalPreferencesInterface {
    /**
     * Set up the listeners for the global preferences interface
     */
    void setUpListener();

    /**
     * Clean up the listeners for the global preferences interface
     */
    void cleanup();

    //region global Settings interface
    /**
     * Get the global unit type from {@link dji.ux.beta.util.UnitConversionUtil.UnitType}
     *
     * @return Current UnitType value if saved or default value to be used if null.
     */
    @NonNull
    UnitConversionUtil.UnitType getUnitType();

    /**
     * Get the global unit type from {@link dji.ux.beta.util.UnitConversionUtil.UnitType}
     *
     * @param unitType UnitType value
     */
    void setUnitType(@NonNull UnitConversionUtil.UnitType unitType);
    //endregion
}

