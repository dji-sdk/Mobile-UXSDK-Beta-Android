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

package dji.ux.beta.core.util

import android.os.Build

/**
 * Contains util methods for android devices made by DJI
 */
object DJIDeviceUtil {

    /**
     * Whether the current device is made by DJI
     */
    @JvmStatic
    fun isDJIDevice(): Boolean {
        return getDJIDeviceType() == DJIDeviceType.NONE
    }

    /**
     * Whether the current device is a smart controller
     */
    @JvmStatic
    fun isSmartController(): Boolean {
        val deviceType = getDJIDeviceType()
        return deviceType == DJIDeviceType.SMART_CONTROLLER ||
                deviceType == DJIDeviceType.MATRICE_300_RTK
    }

    /**
     * Get the current device type
     */
    @JvmStatic
    fun getDJIDeviceType(): DJIDeviceType {
        return DJIDeviceType.find(Build.PRODUCT)
    }
}

/**
 * Type of android device made by DJI
 */
enum class DJIDeviceType(val modelName: String) {
    /**
     * Not a known DJI device
     */
    NONE("Unknown"),

    /**
     * Phantom 4 Pro Remote Controller Built-in Display Device
     */
    PHANTOM_4_PRO("GL300E"),

    /**
     * Phantom 4 Pro V2 Remote Controller Built-in Display Device
     */
    PHANTOM_4_PRO_V2("GL300K"),

    /**
     * CrystalSky 5.5 inch
     */
    CRYSTAL_SKY_A("ZS600A"),

    /**
     * CrystalSky 7.85 inch
     */
    CRYSTAL_SKY_B("ZS600B"),

    /**
     * Phantom 4 RTK Remote Controller Built-in Display Device
     */
    PHANTOM_4_RTK("ag410"),

    /**
     *  Smart Controller Built-in Display Device
     */
    SMART_CONTROLLER("rm500"),

    /**
     *  Matrice 300 RTK Smart Controller Built-in Display Device
     */
    MATRICE_300_RTK("pm430");

    companion object {
        @JvmStatic
        val values = values()

        @JvmStatic
        fun find(modelName: String?): DJIDeviceType {
            if (modelName != null && modelName.trim { it <= ' ' }.isNotEmpty()) {
                return values.find { it.modelName == modelName } ?: NONE
            }

            return NONE
        }
    }
}