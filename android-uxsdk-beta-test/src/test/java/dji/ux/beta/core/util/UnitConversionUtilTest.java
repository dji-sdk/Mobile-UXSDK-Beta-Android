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

import dji.ux.beta.categories.UnitTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test:
 * This class tests all the unit conversion methods in the {@link UnitConversionUtil} class.
 * - Convert length units between meters and feet
 * - Convert speed units between m/s, km/hr and mph
 * - Convert time units from seconds to seconds, min, hours
 */
@Category(UnitTest.class)
public class UnitConversionUtilTest {

    @Test
    public void unitConversion_length_metersToFeet() {
        Assert.assertEquals("Meters to Feet conversion", 3.2808f, UnitConversionUtil.convertMetersToFeet(1.0f), 0.0001);
    }

    @Test
    public void unitConversion_length_feetToMeters() {
        Assert.assertEquals("Feet to Meters conversion", 1.524f, UnitConversionUtil.convertFeetToMeters(5.0f), 0.0001);
    }

    @Test
    public void unitConversion_speed_meterPerSecToMilesPerHour() {
        Assert.assertEquals("Meter per sec to miles per hour conversion",
                            2.23694f,
                            UnitConversionUtil.convertMetersPerSecToMilesPerHr(1.0f),
                            0.0001);
    }

    @Test
    public void unitConversion_speed_milesPerHourToMeterPerSec() {
        Assert.assertEquals("Miles per hour to meter per sec conversion",
                            2.2352f,
                            UnitConversionUtil.convertMilesPerHrToMetersPerSec(5.0f),
                            0.0001);
    }

    @Test
    public void unitConversion_speed_meterPerSecToKmPerHour() {
        Assert.assertEquals("Meter per sec to kilometers per hour conversion",
                            3.6f,
                            UnitConversionUtil.convertMetersPerSecToKmPerHr(1.0f),
                            0.0001);
    }

    @Test
    public void unitConversion_speed_kmPerHourToMeterPerSec() {
        Assert.assertEquals("Kilometers per hour to meter per sec conversion",
                            1.3888f,
                            UnitConversionUtil.convertKmPerHrToMetersPerSec(5.0f),
                            0.0001);
    }

    @Test
    public void unitConversion_time_secondToHour() {
        int[] testTime = UnitConversionUtil.formatSecondToHour(8967);
        Assert.assertEquals("Seconds to sec,min,hr conversion", 27, testTime[0]);
        Assert.assertEquals("Seconds to sec,min,hr conversion", 29, testTime[1]);
        Assert.assertEquals("Seconds to sec,min,hr conversion", 2, testTime[2]);
    }
}
