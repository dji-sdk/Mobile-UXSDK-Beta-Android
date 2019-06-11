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

package dji.ux.beta.util;

import dji.ux.beta.categories.UnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Test:
 * This class tests all the location utility methods in the {@link LocationUtil} class.
 * - Check if given coordinates (latitude, longitude) in decimal values are valid or invalid
 * - Check if the distance between 2 locations is calculated accurately
 * - Check if the angle between 2 locations is calculated accurately
 */
@Category(UnitTest.class)
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class LocationUtilTest {
    @Test
    public void location_checkLatitude_valid() {
        Assert.assertTrue(LocationUtil.checkLatitude(37.46581));
    }

    @Test
    public void location_checkLatitude_invalid() {
        Assert.assertFalse(LocationUtil.checkLatitude(95.6814));
    }

    @Test
    public void location_checkLongitude_valid() {
        Assert.assertTrue(LocationUtil.checkLongitude(-167.2398));
    }

    @Test
    public void location_checkLongitude_invalid() {
        Assert.assertFalse(LocationUtil.checkLongitude(194.2398));
    }

    @Test
    public void location_calculate_distanceBetween() {
        Assert.assertEquals("Distance between given coordinates",
                            250.55477f,
                            LocationUtil.distanceBetween(37.421791, -122.137648, 37.421401, -122.134860),
                            0.0001);
    }

    @Test
    public void location_calculate_distanceAndAngle() {
        float[] angleAndDistance =
            LocationUtil.calculateAngleAndDistance(37.421401, -122.134860, 37.421791, -122.137648);
        Assert.assertEquals("Angle", 170.052f, angleAndDistance[0], 0.0001);
        Assert.assertEquals("Distance", 250.55477f, angleAndDistance[1], 0.0001);
    }
}
