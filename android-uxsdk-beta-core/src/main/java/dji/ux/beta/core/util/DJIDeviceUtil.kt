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