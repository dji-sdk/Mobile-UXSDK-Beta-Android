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

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

public final class SettingDefinitions {

    private SettingDefinitions() {
        //Empty Constructor
    }

    /**
     * The index of the Map Provider
     */
    public enum MapProvider {
        UNKNOWN(-1),
        GOOGLE(0),
        HERE(1),
        AMAP(2),
        MAPBOX(3);

        private int index;

        MapProvider(int index) {
            this.index = index;
        }

        @NonNull
        public static MapProvider find(@IntRange(from = -1, to = 3) int index) {
            for (MapProvider mapProvider : MapProvider.values()) {
                if (mapProvider.getIndex() == index) {
                    return mapProvider;
                }
            }
            return UNKNOWN;
        }

        public int getIndex() {
            return index;
        }
    }
}
