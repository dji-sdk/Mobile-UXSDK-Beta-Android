package dji.ux.beta.core.base.panel

import android.graphics.drawable.Drawable

/**
 * Interface for toolbar panel item
 */
interface ToolbarPanelItem {

    /**
     * Title of the tool
     */
    val barTitle: String

    /**
     * Icon for the tool
     */
    val barIcon: Drawable

}