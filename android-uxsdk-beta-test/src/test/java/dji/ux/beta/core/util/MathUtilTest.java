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

import java.util.Arrays;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test:
 * This class tests all the math utility methods in the {@link MathUtil} class.
 * - Getting the rotation matrix from a given rotation vector
 * - Normalizing a given value to a given range
 */
@Category(UnitTest.class)
public class MathUtilTest {

    @Test
    public void mathUtil_rotationMatrixLength9_FromVectorLength3() {
        float[] testRotationMatrix = new float[] {
            0.92713606f,
            0.37454614f,
            0.011572707f,
            -0.3740419f,
            0.92686856f,
            -0.031739015f,
            -0.022614103f,
            0.02509771f,
            0.9994292f
        };
        float[] testRotationVector = new float[] { 0.014476884f, 0.008707724f, -0.19067287f };

        float[] rotationMatrix = new float[9];
        MathUtil.getRotationMatrixFromVector(rotationMatrix, testRotationVector);
        Assert.assertTrue("Matrix: 9 Vector: 3", Arrays.equals(rotationMatrix, testRotationMatrix));
    }

    @Test
    public void mathUtil_rotationMatrixLength9_FromVectorLength4() {
        float[] testRotationMatrix = new float[] {
            0.92713606f,
            0.3676248f,
            0.01125662f,
            -0.36712053f,
            0.92686856f,
            -0.03121351f,
            -0.022298016f,
            0.024572205f,
            0.9994292f
        };
        float[] testRotationVector = new float[] { 0.014476884f, 0.008707724f, -0.19067287f, 0.96335845f };

        float[] rotationMatrix = new float[9];
        MathUtil.getRotationMatrixFromVector(rotationMatrix, testRotationVector);
        Assert.assertTrue("Matrix: 9 Vector: 4", Arrays.equals(rotationMatrix, testRotationMatrix));
    }

    @Test
    public void mathUtil_rotationMatrixLength16_FromVectorLength3() {
        float[] testRotationMatrix = new float[] {
            0.92713606f,
            0.37454614f,
            0.011572707f,
            0.0f,
            -0.3740419f,
            0.92686856f,
            -0.031739015f,
            0.0f,
            -0.022614103f,
            0.02509771f,
            0.9994292f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1.0f
        };
        float[] testRotationVector = new float[] { 0.014476884f, 0.008707724f, -0.19067287f };

        float[] rotationMatrix = new float[16];
        MathUtil.getRotationMatrixFromVector(rotationMatrix, testRotationVector);
        Assert.assertTrue("Matrix: 16 Vector: 3", Arrays.equals(rotationMatrix, testRotationMatrix));
    }

    @Test
    public void mathUtil_rotationMatrixLength16_FromVectorLength4() {
        float[] testRotationMatrix = new float[] {
            0.92713606f,
            0.3676248f,
            0.01125662f,
            0.0f,
            -0.36712053f,
            0.92686856f,
            -0.03121351f,
            0.0f,
            -0.022298016f,
            0.024572205f,
            0.9994292f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1.0f
        };
        float[] testRotationVector = new float[] { 0.014476884f, 0.008707724f, -0.19067287f, 0.96335845f };

        float[] rotationMatrix = new float[16];
        MathUtil.getRotationMatrixFromVector(rotationMatrix, testRotationVector);
        Assert.assertTrue("Matrix: 16 Vector: 4", Arrays.equals(rotationMatrix, testRotationMatrix));
    }

    @Test
    public void mathUtil_normalizeGivenValueToGivenRange() {
        Assert.assertEquals("Normalize value", 6, MathUtil.normalize(7, 4, 9, 0, 10));
    }

    @Test
    public void mathUtil_isInteger_IntegerValue() {
        Assert.assertEquals("Integer value", true, MathUtil.isInteger("6"));
    }

    @Test
    public void mathUtil_isInteger_NotANumber() {
        Assert.assertEquals("Invalid string", false, MathUtil.isInteger("6S"));
    }

    @Test
    public void mathUtil_isInteger_NullInput() {
        Assert.assertEquals("Null String", false, MathUtil.isInteger(null));
    }
}
